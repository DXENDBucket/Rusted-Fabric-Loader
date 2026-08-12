package io.github.endx.vulkanmod;

import io.github.endx.vulkanmod.spi.VulkanColoredQuad;
import io.github.endx.vulkanmod.spi.VulkanDrawCommand;
import io.github.endx.vulkanmod.spi.VulkanDrawState;
import io.github.endx.vulkanmod.spi.VulkanFrameCommands;
import io.github.endx.vulkanmod.spi.VulkanTexturedQuadGeometry;
import io.github.endx.vulkanmod.spi.VulkanTexturedQuadRun;
import io.github.endx.vulkanmod.spi.VulkanTransform2D;

/** Verifies pooled native frames recycle storage while ordinary SPI frames remain immutable. */
public final class VulkanFrameArenaVerification {
    private VulkanFrameArenaVerification() { }

    public static void main(String[] arguments) {
        VulkanFrameCommands first = pooledFrame(1.0f);
        VulkanDrawCommand firstCommand = first.command(0);
        if (first.commandCount() != 1 || first.coloredQuadCount() != 1
                || first.commands().get(0) != first.coloredQuads().get(0)) {
            throw new AssertionError("pooled frame views lost command order or identity");
        }
        first.releasePooledCommands();
        try {
            first.command(0);
            throw new AssertionError("released pooled frame remained readable");
        } catch (IllegalStateException expected) {
            // Explicit ownership boundary works.
        }

        VulkanFrameCommands second = pooledFrame(0.5f);
        if (second.command(0) != firstCommand) {
            throw new AssertionError("pooled command object was not reused");
        }
        if (Math.abs(((VulkanColoredQuad) second.command(0)).red() - 0.5f) > 0.0001f) {
            throw new AssertionError("recycled command retained stale values");
        }
        second.releasePooledCommands();

        VulkanTexturedQuadGeometry glyphs = new VulkanTexturedQuadGeometry(
                new float[] {0, 0, 4, 6, 0, 0, 1, 1});
        VulkanFrameCommands firstBatch = VulkanFrameCommands.pooledBuilder(16, 16)
                .texturedQuadBatch(1, 2, 3, glyphs,
                        1, 1, 1, 1, VulkanDrawState.DEFAULT).build();
        VulkanDrawCommand batchCommand = firstBatch.command(0);
        firstBatch.releasePooledCommands();
        VulkanFrameCommands secondBatch = VulkanFrameCommands.pooledBuilder(16, 16)
                .texturedQuadBatch(1, 4, 5, glyphs,
                        1, 1, 1, 1, VulkanDrawState.DEFAULT).build();
        if (secondBatch.command(0) != batchCommand
                || secondBatch.texturedQuadBatchCount() != 1) {
            throw new AssertionError("pooled textured batch command was not reused");
        }
        secondBatch.releasePooledCommands();

        VulkanFrameCommands firstRun = pooledSpriteRun();
        VulkanDrawCommand firstRunCommand = firstRun.command(0);
        if (!(firstRunCommand instanceof VulkanTexturedQuadRun)
                || firstRun.commandCount() != 1
                || firstRun.texturedQuadRunCount() != 1
                || firstRun.texturedQuadRunQuadCount() != 2) {
            throw new AssertionError("adjacent sprite quads did not form one pooled run");
        }
        firstRun.releasePooledCommands();
        VulkanFrameCommands secondRun = pooledSpriteRun();
        if (secondRun.command(0) != firstRunCommand) {
            throw new AssertionError("pooled sprite run object was not reused");
        }
        secondRun.releasePooledCommands();

        VulkanFrameCommands ordered = VulkanFrameCommands.pooledBuilder(16, 16)
                .texturedQuad(1, 0, 0, 1, 1, 0, 0, 1, 1,
                        1, 1, 1, 1, VulkanDrawState.DEFAULT)
                .coloredQuad(0, 0, 1, 1, 1, 1, 1, 1, VulkanDrawState.DEFAULT)
                .texturedQuad(1, 1, 0, 1, 1, 0, 0, 1, 1,
                        1, 1, 1, 1, VulkanDrawState.DEFAULT)
                .build();
        if (ordered.commandCount() != 3 || ordered.texturedQuadRunCount() != 2) {
            throw new AssertionError("sprite compaction crossed an intervening draw command");
        }
        ordered.releasePooledCommands();

        VulkanColoredQuad external = new VulkanColoredQuad(
                1.0f, 2.0f, 3.0f, 4.0f, 0.25f, 0.5f, 0.75f, 1.0f);
        VulkanFrameCommands ordinary = VulkanFrameCommands.builder(16, 16)
                .coloredQuad(external).build();
        ordinary.releasePooledCommands();
        if (ordinary.command(0) != external || ordinary.coloredQuads().get(0) != external) {
            throw new AssertionError("ordinary frame did not preserve immutable SPI command");
        }
        System.out.println("Vulkan pooled frame arena contracts passed");
    }

    private static VulkanFrameCommands pooledFrame(float red) {
        return VulkanFrameCommands.pooledBuilder(16, 16)
                .coloredQuad(1.0f, 2.0f, 3.0f, 4.0f,
                        red, 0.25f, 0.5f, 1.0f, VulkanDrawState.DEFAULT)
                .build();
    }

    private static VulkanFrameCommands pooledSpriteRun() {
        return VulkanFrameCommands.pooledBuilder(16, 16)
                .texturedQuad(1, 0, 0, 1, 1, 0, 0, 1, 1,
                        1, 0, 0, 1, VulkanDrawState.DEFAULT)
                .texturedQuad(1, 0, 0, 1, 1, 0, 0, 1, 1,
                        0, 1, 0, 1, VulkanDrawState.transformed(
                                VulkanTransform2D.translation(2, 0)))
                .build();
    }
}
