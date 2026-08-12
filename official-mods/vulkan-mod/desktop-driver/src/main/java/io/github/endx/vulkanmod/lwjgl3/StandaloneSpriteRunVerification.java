package io.github.endx.vulkanmod.lwjgl3;

import io.github.endx.vulkanmod.framestream.FrameStreamEncoder;
import io.github.endx.vulkanmod.framestream.FrameStreamResourceMapper;
import io.github.endx.vulkanmod.framestream.FrameStreamShaderLayoutResolver;
import io.github.endx.vulkanmod.spi.VulkanDrawCommand;
import io.github.endx.vulkanmod.spi.VulkanDrawState;
import io.github.endx.vulkanmod.spi.VulkanFrameCommands;
import io.github.endx.vulkanmod.spi.VulkanFrameSubmission;
import io.github.endx.vulkanmod.spi.VulkanTextureData;
import io.github.endx.vulkanmod.spi.VulkanTransform2D;
import io.github.endx.vulkanmod.spi.VulkanWindowRequest;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Collections;

/** Verifies high-volume sprite collection remains one recycled Java command and GPU batch. */
public final class StandaloneSpriteRunVerification {
    private static final int SPRITES = 12_000;
    private static final int FRAMES = 8;

    private StandaloneSpriteRunVerification() { }

    public static void main(String[] arguments) {
        VulkanDrawState[] states = new VulkanDrawState[SPRITES];
        for (int index = 0; index < SPRITES; index++) {
            states[index] = VulkanDrawState.transformed(VulkanTransform2D.translation(
                    index & 63, index / 64 & 63));
        }
        try (Lwjgl3VulkanDriver driver = new Lwjgl3VulkanDriver()) {
            driver.createNativeWindowSurface(new VulkanWindowRequest(
                    "RustedVK sprite-run verification", 64, 64, false));
            long texture = driver.uploadTexture(new VulkanTextureData(1, 1,
                    new byte[] {(byte) 255, (byte) 255, (byte) 255, (byte) 255}));
            try {
                FrameStreamEncoder encoder = new FrameStreamEncoder(
                        FrameStreamResourceMapper.generationOneSlots(),
                        FrameStreamShaderLayoutResolver.NO_CUSTOM_SHADERS);
                ByteBuffer arena = ByteBuffer.allocateDirect(4 * 1024 * 1024)
                        .order(ByteOrder.LITTLE_ENDIAN);
                VulkanDrawCommand recycledRun = null;
                long warmedWorkspace = -1L;
                for (int frameIndex = 0; frameIndex < FRAMES; frameIndex++) {
                    VulkanFrameCommands.Builder builder = VulkanFrameCommands
                            .pooledBuilder(64, 64).clear(0, 0, 0, 1);
                    for (int sprite = 0; sprite < SPRITES; sprite++) {
                        builder.texturedQuad(texture, 0, 0, 1, 1, 0, 0, 1, 1,
                                (sprite & 1) == 0 ? 1 : 0,
                                (sprite & 1) == 0 ? 0 : 1,
                                0, 1, states[sprite]);
                    }
                    VulkanFrameCommands frame = builder.build();
                    try {
                        if (frame.commandCount() != 1
                                || frame.texturedQuadRunCount() != 1
                                || frame.texturedQuadRunQuadCount() != SPRITES) {
                            throw new AssertionError("sprite frame did not form one run");
                        }
                        if (recycledRun == null) recycledRun = frame.command(0);
                        else if (frame.command(0) != recycledRun) {
                            throw new AssertionError("sprite run object was not recycled");
                        }
                        VulkanFrameSubmission submission = new VulkanFrameSubmission(
                                Collections.emptyList(), frame);
                        arena.clear();
                        ByteBuffer encoded = encoder.encodeTo(
                                frameIndex + 1L, 0L, submission, arena);
                        if (driver.presentFrameStream(encoded) == null) {
                            throw new AssertionError("sprite run frame was not presented");
                        }
                        if (frameIndex == 0) warmedWorkspace = encoder.directWorkspaceGrowths();
                        else if (encoder.directWorkspaceGrowths() != warmedWorkspace) {
                            throw new AssertionError("encoder workspace grew after warm-up");
                        }
                    } finally {
                        frame.releasePooledCommands();
                    }
                }
                if (driver.frameUploadAllocationCount() != 1L
                        || driver.drawBatchAllocationCount() != 1L) {
                    throw new AssertionError("driver metadata grew beyond one sprite batch");
                }
                if (encoder.directEncodeCount() != FRAMES
                        || encoder.directCapacityMisses() != 0L) {
                    throw new AssertionError("direct encoder missed a sprite stress frame");
                }
            } finally {
                driver.destroyTexture(texture);
            }
        }
        System.out.println("Native Vulkan sprite runs passed: " + FRAMES
                + " x " + SPRITES + " sprites in one recycled command");
    }
}
