package io.github.endx.vulkanmod.lwjgl3;

import io.github.endx.vulkanmod.spi.VulkanFrameCommands;
import io.github.endx.vulkanmod.spi.VulkanTextureData;
import io.github.endx.vulkanmod.spi.VulkanTexturedQuad;
import io.github.endx.vulkanmod.spi.VulkanWindowRequest;

/** Exercises queued child passes, ring reuse, and render-target sampling without queue-idle waits. */
public final class StandaloneAsyncOffscreenVerification {
    private static final int PASSES = 64;

    private StandaloneAsyncOffscreenVerification() { }

    public static void main(String[] arguments) {
        try (Lwjgl3VulkanDriver driver = new Lwjgl3VulkanDriver()) {
            driver.createNativeWindowSurface(new VulkanWindowRequest(
                    "RustedVK async offscreen verification", 64, 64, false));
            long source = driver.createRenderTarget(8, 8);
            long destination = driver.createRenderTarget(8, 8);
            try {
                long started = System.nanoTime();
                for (int pass = 0; pass < PASSES; pass++) {
                    float red = (pass & 1) == 0 ? 1.0f : 0.0f;
                    float green = 1.0f - red;
                    driver.renderToTexture(source, VulkanFrameCommands.builder(8, 8)
                            .clear(red, green, 0.0f, 1.0f).build());
                    driver.renderToTexture(destination, VulkanFrameCommands.builder(8, 8)
                            .clear(0.0f, 0.0f, 0.0f, 1.0f)
                            .texturedQuad(new VulkanTexturedQuad(source,
                                    0.0f, 0.0f, 8.0f, 8.0f,
                                    0.0f, 0.0f, 1.0f, 1.0f,
                                    1.0f, 1.0f, 1.0f, 1.0f))
                            .build());
                }
                VulkanTextureData result = driver.readTexture(destination);
                byte[] rgba = result.copyRgba();
                // The final odd pass is green. Sampling the source in the immediately following
                // queued pass proves graphics-queue ordering without a host-side queue wait.
                for (int offset = 0; offset < rgba.length; offset += 4) {
                    if ((rgba[offset] & 255) > 1 || (rgba[offset + 1] & 255) < 254
                            || (rgba[offset + 2] & 255) > 1
                            || (rgba[offset + 3] & 255) < 254) {
                        throw new AssertionError("async offscreen dependency produced bad pixel "
                                + (offset / 4));
                    }
                }
                double millis = (System.nanoTime() - started) / 1_000_000.0;
                System.out.println("Native Vulkan async offscreen ring passed: "
                        + (PASSES * 2) + " submissions in " + millis + "ms");
            } finally {
                driver.destroyTexture(destination);
                driver.destroyTexture(source);
            }
        }
    }
}
