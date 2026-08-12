package io.github.endx.vulkanmod.lwjgl3;

import io.github.endx.vulkanmod.spi.VulkanDrawState;
import io.github.endx.vulkanmod.spi.VulkanFrameCommands;
import io.github.endx.vulkanmod.spi.VulkanTextureData;
import io.github.endx.vulkanmod.spi.VulkanWindowRequest;

import java.util.Map;

/** Verifies compatible batches reuse Vulkan command-buffer state across texture changes. */
public final class StandaloneCommandStateReuseVerification {
    private static final int BATCHES = 64;

    private StandaloneCommandStateReuseVerification() { }

    public static void main(String[] arguments) {
        try (Lwjgl3VulkanDriver driver = new Lwjgl3VulkanDriver()) {
            driver.createNativeWindowSurface(new VulkanWindowRequest(
                    "RustedVK command-state verification", 64, 64, false));
            long first = uploadPixel(driver, (byte) 255, (byte) 0);
            long second = uploadPixel(driver, (byte) 0, (byte) 255);
            try {
                VulkanFrameCommands.Builder builder = VulkanFrameCommands.builder(64, 64)
                        .clear(0, 0, 0, 1);
                for (int index = 0; index < BATCHES; index++) {
                    builder.texturedQuad((index & 1) == 0 ? first : second,
                            index & 7, index >> 3, 1, 1, 0, 0, 1, 1,
                            1, 1, 1, 1, VulkanDrawState.DEFAULT);
                }
                if (driver.presentFrame(builder.build()) == null) {
                    throw new AssertionError("command-state frame was not presented");
                }
                require(stat(driver, "command.pipelineBindCalls") == 1L
                                && stat(driver, "command.pipelineBindSkips") == BATCHES - 1L,
                        "textured pipeline was rebound for compatible batches");
                require(stat(driver, "command.vertexBindCalls") == 1L
                                && stat(driver, "command.vertexBindSkips") == BATCHES - 1L,
                        "shared object vertex range was rebound for every batch");
                require(stat(driver, "command.scissorSetCalls") == 1L
                                && stat(driver, "command.scissorSetSkips") == BATCHES - 1L,
                        "unchanged full-target scissor was recorded for every batch");
                require(stat(driver, "command.shaderPushCalls") == 1L
                                && stat(driver, "command.shaderPushSkips") == BATCHES - 1L,
                        "unchanged shader push constants were recorded for every batch");
                require(stat(driver, "descriptor.bindCalls") == BATCHES,
                        "alternating textures unexpectedly reused a descriptor binding");
            } finally {
                driver.destroyTexture(first);
                driver.destroyTexture(second);
            }
        }
        System.out.println("Native Vulkan command-state reuse passed: " + BATCHES
                + " batches, one pipeline/vertex/scissor/push recording");
    }

    private static long uploadPixel(Lwjgl3VulkanDriver driver, byte red, byte green) {
        return driver.uploadTexture(new VulkanTextureData(1, 1,
                new byte[] {red, green, 0, (byte) 255}));
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
