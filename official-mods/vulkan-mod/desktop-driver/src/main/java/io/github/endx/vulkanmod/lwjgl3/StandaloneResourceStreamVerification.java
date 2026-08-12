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

import java.util.Collections;

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
            if (driver.submitResourceStream(create.toDirectBuffer()) != 3L) {
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
            if (driver.submitResourceStream(createShader.toDirectBuffer()) != 4L
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

            ResourceStreamWriter destroy = new ResourceStreamWriter(5L, 0, 0L);
            ResourceStreamRecords.textureDestroy(destroy, target);
            ResourceStreamRecords.textureDestroy(destroy, source);
            ResourceStreamRecords.shaderProgramDestroy(destroy, shader);
            if (driver.submitResourceStream(destroy.toDirectBuffer()) != 7L) {
                throw new AssertionError("ResourceStream destroy sequence did not advance");
            }
            try {
                driver.presentFrameStream(encoder.encode(2L, 8L,
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
