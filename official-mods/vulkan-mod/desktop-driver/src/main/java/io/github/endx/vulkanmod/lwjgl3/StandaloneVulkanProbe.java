package io.github.endx.vulkanmod.lwjgl3;

import io.github.endx.vulkanmod.spi.VulkanDeviceInfo;
import io.github.endx.vulkanmod.spi.VulkanProbeResult;

/** Development entrypoint used before attaching the driver to the game process. */
public final class StandaloneVulkanProbe {
    private StandaloneVulkanProbe() { }

    public static void main(String[] arguments) {
        VulkanProbeResult result = new Lwjgl3VulkanDriver().probe();
        if (!result.available()) {
            throw new IllegalStateException(result.diagnostic());
        }
        System.out.println("Vulkan " + VulkanProbeResult.formatVersion(result.instanceVersion()));
        for (VulkanDeviceInfo device : result.devices()) {
            System.out.println(device.name() + " api="
                    + VulkanProbeResult.formatVersion(device.apiVersion()));
        }
    }
}
