package io.github.endx.vulkanmod.lwjgl3;

import io.github.endx.vulkanmod.framestream.FrameResourceHandle;
import io.github.endx.vulkanmod.framestream.FrameStreamEncoder;
import io.github.endx.vulkanmod.framestream.FrameStreamResourceMapper;
import io.github.endx.vulkanmod.framestream.FrameStreamShaderLayoutResolver;
import io.github.endx.vulkanmod.resourcestream.ResourceStreamFormat;
import io.github.endx.vulkanmod.resourcestream.ResourceStreamRecords;
import io.github.endx.vulkanmod.resourcestream.ResourceStreamWriter;
import io.github.endx.vulkanmod.render.LegacyFragmentShaderTranslator;
import io.github.endx.vulkanmod.spi.VulkanCustomFragmentShader;
import io.github.endx.vulkanmod.spi.VulkanFrameCommands;
import io.github.endx.vulkanmod.spi.VulkanFrameSubmission;
import io.github.endx.vulkanmod.spi.VulkanRenderTargetPass;
import io.github.endx.vulkanmod.spi.VulkanTextureData;
import io.github.endx.vulkanmod.spi.VulkanTexturedQuad;
import io.github.endx.vulkanmod.spi.VulkanWindowRequest;
import io.github.endx.vulkanmod.spi.VulkanResourceStreamResult;

import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.Map;

/** Executes logical texture lifetime and FrameStream dependencies on the real desktop backend. */
public final class StandaloneResourceStreamVerification {
    private StandaloneResourceStreamVerification() { }

    public static void main(String[] arguments) {
        long source = FrameResourceHandle.encode(FrameResourceHandle.TYPE_TEXTURE, 3, 11);
        long target = FrameResourceHandle.encode(FrameResourceHandle.TYPE_TEXTURE, 2, 12);
        long shader = FrameResourceHandle.encode(FrameResourceHandle.TYPE_SHADER_PROGRAM, 4, 13);
        try (Lwjgl3VulkanDriver driver = new Lwjgl3VulkanDriver()) {
            driver.createNativeWindowSurface(new VulkanWindowRequest(
                    "RustedVK resource stream verification", 32, 32, false));
            if (!driver.supportsResourceStream()) {
                throw new AssertionError("desktop driver does not advertise ResourceStream");
            }
            VulkanTextureData red = new VulkanTextureData(1, 1,
                    new byte[] {(byte) 255, 0, 0, (byte) 255});
            ResourceStreamWriter create = new ResourceStreamWriter(1L, 0, 0L);
            ResourceStreamRecords.textureCreate(create, source, 1, 1, 1,
                    ResourceStreamFormat.FORMAT_RGBA8_UNORM,
                    ResourceStreamFormat.TEXTURE_USAGE_SAMPLED
                            | ResourceStreamFormat.TEXTURE_USAGE_TRANSFER_DESTINATION,
                    ResourceStreamFormat.SAMPLER_CLAMP_TO_EDGE);
            ResourceStreamRecords.textureUpload(create, source, red);
            ResourceStreamRecords.renderTargetCreate(create, target, 8, 8,
                    ResourceStreamFormat.FORMAT_RGBA8_UNORM,
                    ResourceStreamFormat.SAMPLER_CLAMP_TO_EDGE);
            if (driver.submitResourceStream(create.toDirectBuffer()).appliedSequence() != 3L) {
                throw new AssertionError("desktop ResourceStream sequence did not advance");
            }
            String translated = LegacyFragmentShaderTranslator.translate(
                    "#version 130\nvarying vec4 v_color;\n"
                            + "varying vec2 v_texCoords;\n"
                            + "uniform sampler2D u_texture;\n"
                            + "void main(){gl_FragColor=texture2D(u_texture,v_texCoords)"
                            + "*v_color;}").source();
            ResourceStreamWriter createShader = new ResourceStreamWriter(4L, 0, 0L);
            ResourceStreamRecords.fragmentShaderCreate(createShader, shader,
                    new VulkanCustomFragmentShader("resource-verification", translated));
            if (driver.submitResourceStream(createShader.toDirectBuffer()).appliedSequence() != 4L
                    || driver.customShaderUsesExpandedVertexInput(shader)) {
                throw new AssertionError("logical ResourceStream shader was not compiled");
            }

            VulkanFrameCommands targetPass = VulkanFrameCommands.builder(8, 8)
                    .clear(0.0f, 0.0f, 0.0f, 1.0f)
                    .texturedQuad(fullQuad(source, 8.0f, 8.0f)).build();
            VulkanFrameCommands presentation = VulkanFrameCommands.builder(32, 32)
                    .clear(0.0f, 0.0f, 0.0f, 1.0f)
                    .texturedQuad(fullQuad(target, 32.0f, 32.0f)).build();
            VulkanFrameSubmission graph = new VulkanFrameSubmission(Collections.singletonList(
                    new VulkanRenderTargetPass(target, targetPass)), presentation);
            FrameStreamEncoder encoder = new FrameStreamEncoder(
                    FrameStreamResourceMapper.typedHandles(),
                    FrameStreamShaderLayoutResolver.NO_CUSTOM_SHADERS);
            if (driver.presentFrameStream(encoder.encode(1L, 4L, graph)) == null) {
                throw new AssertionError("ResourceStream-dependent frame was unavailable");
            }
            byte[] rgba = driver.readTexture(target).copyRgba();
            for (int offset = 0; offset < rgba.length; offset += 4) {
                if ((rgba[offset] & 255) < 254 || (rgba[offset + 1] & 255) > 1
                        || (rgba[offset + 2] & 255) > 1
                        || (rgba[offset + 3] & 255) < 254) {
                    throw new AssertionError("logical ResourceStream texture produced bad pixel");
                }
            }

            ByteBuffer arena = ByteBuffer.allocateDirect(8 * 8 * 4);
            for (int pixel = 0; pixel < 8 * 8; pixel++) {
                arena.put((byte) 255).put((byte) 0).put((byte) 0).put((byte) 255);
            }
            arena.flip();
            io.github.endx.vulkanmod.spi.VulkanResourceArenaRegistration registration =
                    driver.registerResourceUploadArena(99L, arena);
            if (!registration.hasNativeAddress() || registration.capacity() != arena.capacity()) {
                throw new AssertionError("desktop arena did not expose stable native registration");
            }
            ResourceStreamWriter mutations = new ResourceStreamWriter(5L,
                    ResourceStreamFormat.FLAG_REQUIRES_COMPLETION, 54L);
            ResourceStreamRecords.externalTextureTransfer(mutations,
                    ResourceStreamFormat.TEXTURE_REGION_UPDATE, target,
                    0, 0, 8, 8, 32, ResourceStreamFormat.FORMAT_RGBA8_UNORM,
                    8 * 8 * 4, 99L, 0L);
            ResourceStreamRecords.textureRegionUpdate(mutations, target, 0, 0,
                    new VulkanTextureData(1, 1,
                            new byte[] {0, (byte) 255, 0, (byte) 255}));
            VulkanResourceStreamResult acceptedMutations =
                    driver.submitResourceStream(mutations.toDirectBuffer());
            if (!acceptedMutations.completionPending()
                    || acceptedMutations.appliedSequence() != 6L) {
                throw new AssertionError("external/partial texture updates were not accepted");
            }
            try {
                driver.unregisterResourceUploadArena(99L);
                throw new AssertionError("queued external arena was unregistered before decode");
            } catch (IllegalStateException expected) {
                // The decode owns this registration until completion 54 becomes ready.
            }
            VulkanResourceStreamResult consumed =
                    driver.awaitResourceStreamCompletion(54L, -1L);
            if (!consumed.completionReady() || consumed.textureReadback() != null
                    || consumed.appliedSequence() != 6L) {
                throw new AssertionError("external arena was not released after decode");
            }
            driver.unregisterResourceUploadArena(99L);
            ResourceStreamWriter read = new ResourceStreamWriter(7L,
                    ResourceStreamFormat.FLAG_REQUIRES_COMPLETION, 55L);
            ResourceStreamRecords.textureReadback(read, target, 0, 0, 8, 8,
                    ResourceStreamFormat.FORMAT_RGBA8_UNORM);
            VulkanResourceStreamResult acceptedRead =
                    driver.submitResourceStream(read.toDirectBuffer());
            if (!acceptedRead.completionPending()
                    || acceptedRead.appliedSequence() != 7L
                    || acceptedRead.completionId() != 55L) {
                throw new AssertionError("desktop readback was not accepted asynchronously");
            }
            VulkanResourceStreamResult readResult =
                    driver.awaitResourceStreamCompletion(55L, -1L);
            byte[] mutated = readResult.textureReadback().copyRgba();
            if (readResult.appliedSequence() != 7L || readResult.completionId() != 55L
                    || (mutated[0] & 255) != 0 || (mutated[1] & 255) != 255
                    || (mutated[2] & 255) != 0 || (mutated[3] & 255) != 255
                    || (mutated[4] & 255) != 255 || (mutated[5] & 255) != 0) {
                throw new AssertionError("external upload, partial update, or readback mismatch");
            }
            ResourceStreamWriter partialRead = new ResourceStreamWriter(8L,
                    ResourceStreamFormat.FLAG_REQUIRES_COMPLETION, 56L);
            ResourceStreamRecords.textureReadback(partialRead, target, 0, 0, 2, 1,
                    ResourceStreamFormat.FORMAT_RGBA8_UNORM);
            VulkanResourceStreamResult acceptedPartial =
                    driver.submitResourceStream(partialRead.toDirectBuffer());
            if (!acceptedPartial.completionPending()
                    || acceptedPartial.appliedSequence() != 8L) {
                throw new AssertionError("partial readback was not accepted asynchronously");
            }
            VulkanTextureData partial = driver.awaitResourceStreamCompletion(56L, -1L)
                    .textureReadback();
            byte[] partialRgba = partial.copyRgba();
            if (partial.width() != 2 || partial.height() != 1
                    || (partialRgba[0] & 255) != 0 || (partialRgba[1] & 255) != 255
                    || (partialRgba[4] & 255) != 255 || (partialRgba[5] & 255) != 0) {
                throw new AssertionError("partial ResourceStream readback returned wrong pixels");
            }
            Map<String, Long> statistics = driver.performanceStatistics();
            if (statistics.get("resource.accepted") < 5L
                    || statistics.get("resource.decoded") < 5L
                    || statistics.get("resource.pending") != 0L
                    || statistics.get("texture.uploadBatches") < 1L
                    || statistics.get("texture.uploadBytes") < 8L * 8L * 4L
                    || statistics.get("texture.uploadSlotGrowths") < 1L) {
                throw new AssertionError("ResourceStream performance counters were not updated: "
                        + statistics);
            }

            ResourceStreamWriter destroy = new ResourceStreamWriter(9L, 0, 0L);
            ResourceStreamRecords.textureDestroy(destroy, target);
            ResourceStreamRecords.textureDestroy(destroy, source);
            ResourceStreamRecords.shaderProgramDestroy(destroy, shader);
            if (driver.submitResourceStream(destroy.toDirectBuffer()).appliedSequence() != 11L) {
                throw new AssertionError("ResourceStream destroy sequence did not advance");
            }
            try {
                driver.presentFrameStream(encoder.encode(2L, 12L,
                        new VulkanFrameSubmission(Collections.emptyList(),
                                VulkanFrameCommands.builder(32, 32)
                                        .clear(0.0f, 0.0f, 0.0f, 1.0f).build())));
                throw new AssertionError("frame overtook unapplied resource sequence");
            } catch (IllegalStateException expected) {
                if (!expected.getMessage().contains("requires resource sequence")) throw expected;
            }
        }
        System.out.println("Native Vulkan ResourceStream texture lifecycle passed");
    }

    private static VulkanTexturedQuad fullQuad(long texture, float width, float height) {
        return new VulkanTexturedQuad(texture, 0.0f, 0.0f, width, height,
                0.0f, 0.0f, 1.0f, 1.0f,
                1.0f, 1.0f, 1.0f, 1.0f);
    }
}
