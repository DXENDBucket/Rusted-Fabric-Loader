package io.github.endx.vulkanmod.lwjgl3;

import io.github.endx.vulkanmod.framestream.FrameStreamEncoder;
import io.github.endx.vulkanmod.framestream.FrameStreamResourceMapper;
import io.github.endx.vulkanmod.framestream.FrameStreamShaderLayoutResolver;
import io.github.endx.vulkanmod.spi.VulkanFrameCommands;
import io.github.endx.vulkanmod.spi.VulkanFrameSubmission;
import io.github.endx.vulkanmod.spi.VulkanRenderTargetPass;
import io.github.endx.vulkanmod.spi.VulkanTextureData;
import io.github.endx.vulkanmod.spi.VulkanTexturedQuad;
import io.github.endx.vulkanmod.spi.VulkanWindowRequest;

import java.util.Arrays;

/** Proves dependent offscreen writes and presentation execute in one queue submission per frame. */
public final class StandaloneFrameGraphVerification {
    private static final int FRAMES = 32;

    private StandaloneFrameGraphVerification() { }

    public static void main(String[] arguments) {
        try (Lwjgl3VulkanDriver driver = new Lwjgl3VulkanDriver()) {
            driver.createNativeWindowSurface(new VulkanWindowRequest(
                    "RustedVK frame graph verification", 64, 64, false));
            long source = driver.createRenderTarget(8, 8);
            long destination = driver.createRenderTarget(8, 8);
            try {
                FrameStreamEncoder encoder = new FrameStreamEncoder(
                        FrameStreamResourceMapper.generationOneSlots(),
                        FrameStreamShaderLayoutResolver.NO_CUSTOM_SHADERS);
                for (int frameIndex = 0; frameIndex < FRAMES; frameIndex++) {
                    float red = (frameIndex & 1) == 0 ? 1.0f : 0.0f;
                    float green = 1.0f - red;
                    VulkanFrameCommands sourceFrame = VulkanFrameCommands.builder(8, 8)
                            .clear(red, green, 0.0f, 1.0f).build();
                    VulkanFrameCommands destinationFrame = VulkanFrameCommands.builder(8, 8)
                            .clear(0.0f, 0.0f, 0.0f, 1.0f)
                            .texturedQuad(fullQuad(source, 8.0f, 8.0f)).build();
                    VulkanFrameCommands presentation = VulkanFrameCommands.builder(64, 64)
                            .clear(0.0f, 0.0f, 0.0f, 1.0f)
                            .texturedQuad(fullQuad(destination, 64.0f, 64.0f)).build();
                    VulkanFrameSubmission graph = new VulkanFrameSubmission(Arrays.asList(
                            new VulkanRenderTargetPass(source, sourceFrame),
                            new VulkanRenderTargetPass(destination, destinationFrame)),
                            presentation);
                    if (driver.presentFrameStream(encoder.encode(
                            frameIndex + 1L, 0L, graph)) == null) {
                        throw new AssertionError("frame graph presentation was unavailable");
                    }
                }
                byte[] rgba = driver.readTexture(destination).copyRgba();
                for (int offset = 0; offset < rgba.length; offset += 4) {
                    if ((rgba[offset] & 255) > 1 || (rgba[offset + 1] & 255) < 254
                            || (rgba[offset + 2] & 255) > 1
                            || (rgba[offset + 3] & 255) < 254) {
                        throw new AssertionError("frame graph dependency produced bad pixel "
                                + (offset / 4));
                    }
                }
                if (driver.frameGraphQueueSubmissionCount() != FRAMES) {
                    throw new AssertionError("expected one graph queue submission per frame");
                }
                if (driver.frameGraphPassCount() != FRAMES * 2L) {
                    throw new AssertionError("not all target passes entered the frame graph");
                }
                if (driver.immediateOffscreenQueueSubmissionCount() != 0L) {
                    throw new AssertionError("frame graph unexpectedly used immediate submits");
                }
            } finally {
                driver.destroyTexture(destination);
                driver.destroyTexture(source);
            }
        }
        System.out.println("Native Vulkan FrameStream graph passed: " + FRAMES
                + " queue submissions for " + (FRAMES * 2) + " dependent target passes");
    }

    private static VulkanTexturedQuad fullQuad(long texture, float width, float height) {
        return new VulkanTexturedQuad(texture, 0.0f, 0.0f, width, height,
                0.0f, 0.0f, 1.0f, 1.0f,
                1.0f, 1.0f, 1.0f, 1.0f);
    }
}
