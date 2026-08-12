package io.github.endx.vulkanmod.lwjgl3;

import io.github.endx.vulkanmod.spi.VulkanDrawState;
import io.github.endx.vulkanmod.spi.VulkanFrameCommands;
import io.github.endx.vulkanmod.spi.VulkanTextureData;
import io.github.endx.vulkanmod.spi.VulkanWindowRequest;

import java.util.Map;

/** Verifies sampled/offscreen image factory creation, retirement, and shutdown ownership. */
public final class StandaloneImageResourceVerification {
    private StandaloneImageResourceVerification() { }

    public static void main(String[] arguments) {
        try (Lwjgl3VulkanDriver driver = new Lwjgl3VulkanDriver()) {
            driver.createNativeWindowSurface(new VulkanWindowRequest(
                    "RustedVK image resource verification", 32, 32, false));
            long sampled = driver.uploadTexture(new VulkanTextureData(
                    4, 4, new byte[4 * 4 * 4]));
            long target = driver.createRenderTarget(8, 8);
            require(stat(driver, "image.sampledCreates") == 1L,
                    "sampled texture bypassed the image factory");
            require(stat(driver, "image.renderTargetCreates") == 1L,
                    "render target bypassed the image factory");
            require(stat(driver, "image.liveResources") == 2L
                            && stat(driver, "image.peakResources") == 2L,
                    "image factory live/peak accounting is incorrect");

            present(driver, sprite(sampled));
            driver.destroyTexture(sampled);
            driver.destroyTexture(target);

            // Mutating an initialized survivor establishes the global fence point that makes the
            // two retired resources safe to destroy deterministically.
            long survivor = driver.uploadTexture(new VulkanTextureData(
                    1, 1, new byte[] {0, 0, 0, (byte) 255}));
            present(driver, sprite(survivor));
            driver.updateTexture(survivor, new VulkanTextureData(
                    1, 1, new byte[] {(byte) 255, (byte) 255, (byte) 255, (byte) 255}));
            present(driver, sprite(survivor));
            require(stat(driver, "image.destroys") >= 2L
                            && stat(driver, "image.liveResources") == 1L,
                    "fence-safe retirement did not return images to the factory");
            driver.destroyTexture(survivor);
        }
        System.out.println("Native Vulkan image resource factory lifecycle passed");
    }

    private static VulkanFrameCommands sprite(long texture) {
        return VulkanFrameCommands.builder(32, 32).clear(0, 0, 0, 1)
                .texturedQuad(texture, 0, 0, 16, 16, 0, 0, 1, 1,
                        1, 1, 1, 1, VulkanDrawState.DEFAULT)
                .build();
    }

    private static void present(Lwjgl3VulkanDriver driver, VulkanFrameCommands frame) {
        if (driver.presentFrame(frame) == null) {
            throw new AssertionError("verification frame was not presented");
        }
    }

    private static long stat(Lwjgl3VulkanDriver driver, String name) {
        Map<String, Long> statistics = driver.performanceStatistics();
        Long value = statistics.get(name);
        if (value == null) throw new AssertionError("missing statistic: " + name);
        return value.longValue();
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
