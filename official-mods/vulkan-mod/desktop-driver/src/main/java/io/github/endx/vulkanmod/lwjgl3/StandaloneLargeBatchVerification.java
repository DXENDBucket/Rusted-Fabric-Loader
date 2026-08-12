package io.github.endx.vulkanmod.lwjgl3;

import io.github.endx.vulkanmod.framestream.FrameStreamEncoder;
import io.github.endx.vulkanmod.framestream.FrameStreamResourceMapper;
import io.github.endx.vulkanmod.framestream.FrameStreamShaderLayoutResolver;
import io.github.endx.vulkanmod.spi.VulkanDrawState;
import io.github.endx.vulkanmod.spi.VulkanFrameCommands;
import io.github.endx.vulkanmod.spi.VulkanFrameSubmission;
import io.github.endx.vulkanmod.spi.VulkanTextureData;
import io.github.endx.vulkanmod.spi.VulkanWindowRequest;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Collections;

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
                FrameStreamEncoder encoder = new FrameStreamEncoder(
                        FrameStreamResourceMapper.generationOneSlots(),
                        FrameStreamShaderLayoutResolver.NO_CUSTOM_SHADERS);
                ByteBuffer arena = ByteBuffer.allocateDirect(8 * 1024 * 1024)
                        .order(ByteOrder.LITTLE_ENDIAN);
                long warmedWorkspace = -1L;
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
                        VulkanFrameSubmission submission = new VulkanFrameSubmission(
                                Collections.emptyList(), frame);
                        arena.clear();
                        ByteBuffer encoded = encoder.encodeTo(
                                frameIndex + 1L, 0L, submission, arena);
                        if (driver.presentFrameStream(encoded) == null) {
                            throw new AssertionError("large native batch frame was not presented");
                        }
                        if (frameIndex == 0) {
                            warmedWorkspace = encoder.directWorkspaceGrowths();
                        } else if (encoder.directWorkspaceGrowths() != warmedWorkspace) {
                            throw new AssertionError(
                                    "direct encoder workspace grew after warm-up");
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
                if (encoder.directEncodeCount() != FRAMES
                        || encoder.directCapacityMisses() != 0L) {
                    throw new AssertionError("direct encoder did not own every stress frame");
                }
                if (driver.performanceStatistics().get("frame.materialCacheHits") <= 0L
                        || driver.performanceStatistics().get(
                                "frame.materialCacheMisses") != FRAMES) {
                    throw new AssertionError("decoded material cache was not reused per frame");
                }
            } finally {
                driver.destroyTexture(texture);
            }
        }
        System.out.println("Native Vulkan FrameStream large-batch frames passed: " + FRAMES
                + " x " + DRAW_BATCHES + " commands");
    }
}
