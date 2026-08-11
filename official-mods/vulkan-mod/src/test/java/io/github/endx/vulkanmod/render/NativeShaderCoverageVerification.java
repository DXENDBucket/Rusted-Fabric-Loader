package io.github.endx.vulkanmod.render;

import io.github.endx.vulkanmod.spi.VulkanShaderState;
import io.github.endx.vulkanmod.spi.VulkanBuiltInShaders;

/** Locks native coverage to every fragment shader shipped by the stock desktop game. */
public final class NativeShaderCoverageVerification {
    private NativeShaderCoverageVerification() { }

    public static void main(String[] args) {
        require("plain", VulkanShaderState.PLAIN);
        require("pureGreenTeamColor", VulkanShaderState.PURE_GREEN_TEAM_COLOR);
        require("hueAddTeamColor", VulkanShaderState.HUE_ADD_TEAM_COLOR);
        require("hueShiftTeamColor", VulkanShaderState.HUE_SHIFT_TEAM_COLOR);
        require("post_base", VulkanShaderState.POST_BASE);
        require("post_displacement", VulkanShaderState.POST_DISPLACEMENT);
        require("error", VulkanShaderState.ERROR);
        if (VulkanBuiltInShaders.effectForName("third_party_shader") != -1) {
            throw new AssertionError("unknown shader was classified as a stock shader");
        }
        System.out.println("Native stock shader coverage contracts passed");
    }

    private static void require(String name, int expected) {
        int actual = VulkanBuiltInShaders.effectForName(name);
        if (actual != expected) {
            throw new AssertionError(name + " mapped to " + actual + ", expected " + expected);
        }
    }
}
