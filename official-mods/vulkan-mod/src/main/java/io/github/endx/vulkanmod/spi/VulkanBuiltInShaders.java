package io.github.endx.vulkanmod.spi;

/** Stable mapping for every fragment shader shipped with the stock desktop game. */
public final class VulkanBuiltInShaders {
    private VulkanBuiltInShaders() { }

    public static int effectForName(String name) {
        if (name == null || name.isEmpty() || "plain".equalsIgnoreCase(name)) {
            return VulkanShaderState.PLAIN;
        }
        if ("pureGreenTeamColor".equalsIgnoreCase(name)) {
            return VulkanShaderState.PURE_GREEN_TEAM_COLOR;
        }
        if ("hueAddTeamColor".equalsIgnoreCase(name)) {
            return VulkanShaderState.HUE_ADD_TEAM_COLOR;
        }
        if ("hueShiftTeamColor".equalsIgnoreCase(name)) {
            return VulkanShaderState.HUE_SHIFT_TEAM_COLOR;
        }
        if ("post_base".equalsIgnoreCase(name)) return VulkanShaderState.POST_BASE;
        if ("post_displacement".equalsIgnoreCase(name)) {
            return VulkanShaderState.POST_DISPLACEMENT;
        }
        if ("error".equalsIgnoreCase(name)) return VulkanShaderState.ERROR;
        return -1;
    }
}
