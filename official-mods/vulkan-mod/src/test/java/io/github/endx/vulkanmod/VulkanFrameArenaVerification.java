package io.github.endx.vulkanmod;

import io.github.endx.vulkanmod.spi.VulkanColoredQuad;
import io.github.endx.vulkanmod.spi.VulkanDrawCommand;
import io.github.endx.vulkanmod.spi.VulkanDrawState;
import io.github.endx.vulkanmod.spi.VulkanFrameCommands;

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
}
