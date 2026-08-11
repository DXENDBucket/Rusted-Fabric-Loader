package io.github.endx.vulkanmod.lwjgl3;

import io.github.endx.vulkanmod.spi.VulkanDrawState;
import io.github.endx.vulkanmod.spi.VulkanFrameCommands;
import io.github.endx.vulkanmod.spi.VulkanTextureData;
import io.github.endx.vulkanmod.spi.VulkanWindowRequest;

/** Reproduces the many-unit workload that previously exhausted LWJGL's MemoryStack. */
public final class StandaloneLargeBatchVerification {
    private static final int DRAW_BATCHES = 20_000;
    private static final int FRAMES = 8;

    private StandaloneLargeBatchVerification() { }

    public static void main(String[] arguments) {
        try (Lwjgl3VulkanDriver driver = new Lwjgl3VulkanDriver()) {
            driver.createNativeWindowSurface(new VulkanWindowRequest(
                    "RustedVK large-batch verification", 64, 64, false));
            long texture = driver.uploadTexture(new VulkanTextureData(1, 1,
                    new byte[] {(byte) 255, (byte) 255, (byte) 255, (byte) 255}));
            try {
                for (int frameIndex = 0; frameIndex < FRAMES; frameIndex++) {
                    VulkanFrameCommands.Builder builder = VulkanFrameCommands
                            .pooledBuilder(64, 64).clear(0.0f, 0.0f, 0.0f, 1.0f);
                    VulkanDrawState state = VulkanDrawState.DEFAULT;
                    for (int index = 0; index < DRAW_BATCHES; index++) {
                        float position = index & 63;
                        if ((index & 1) == 0) {
                            builder.coloredQuad(position, position, 1.0f, 1.0f,
                                    1.0f, 0.0f, 0.0f, 1.0f, state);
                        } else {
                            builder.texturedQuad(texture,
                                    position, position, 1.0f, 1.0f,
                                    0.0f, 0.0f, 1.0f, 1.0f,
                                    1.0f, 1.0f, 1.0f, 1.0f, state);
                        }
                    }
                    VulkanFrameCommands frame = builder.build();
                    try {
                        if (driver.presentFrame(frame) == null) {
                            throw new AssertionError("large native batch frame was not presented");
                        }
                    } finally {
                        frame.releasePooledCommands();
                    }
                }
                if (driver.frameUploadAllocationCount() != 1L) {
                    throw new AssertionError("frame upload metadata was not recycled");
                }
                if (driver.drawBatchAllocationCount() != DRAW_BATCHES) {
                    throw new AssertionError("draw batch metadata grew after the first frame: "
                            + driver.drawBatchAllocationCount());
                }
            } finally {
                driver.destroyTexture(texture);
            }
        }
        System.out.println("Native Vulkan pooled large-batch frames passed: " + FRAMES
                + " x " + DRAW_BATCHES + " commands");
    }
}
