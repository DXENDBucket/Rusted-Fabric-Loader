package io.github.endx.vulkanmod;

import net.fabricmc.api.ClientModInitializer;

/** Experimental Vulkan renderer entrypoint and pre-game renderer provider. */
public final class VulkanMod implements ClientModInitializer {
    public static final String MOD_ID = "vulkan_mod";

    @Override
    public void onInitializeClient() {
        VulkanMode mode = VulkanMode.configured();
        if (mode == VulkanMode.NATIVE) {
            String requested = System.getProperty("rusted.fabric.renderer", "auto").trim();
            if (requested.isEmpty() || "auto".equalsIgnoreCase(requested)) {
                System.setProperty("rusted.fabric.renderer", "vulkan");
            }
        }
        VulkanRuntime.initialize(mode);
        if (mode == VulkanMode.NATIVE) {
            System.setProperty("rusted.fabric.renderer.provider.vulkan",
                    VulkanRuntime.isVulkanAvailable() ? "available" : "unavailable");
        }
    }
}
