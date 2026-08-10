package io.github.endx.vulkanmod;

import net.fabricmc.api.ClientModInitializer;

/** Experimental Vulkan renderer entrypoint; currently performs a non-invasive desktop probe. */
public final class VulkanMod implements ClientModInitializer {
    public static final String MOD_ID = "vulkan_mod";

    @Override
    public void onInitializeClient() {
        VulkanRuntime.initialize(VulkanMode.configured());
    }
}
