package io.github.endx.vulkanmod;

import io.github.endx.vulkanmod.spi.VulkanDeviceInfo;
import io.github.endx.vulkanmod.spi.VulkanProbeResult;

import java.util.Optional;

/** Owns Vulkan startup state without touching the legacy renderer until takeover is implemented. */
public final class VulkanRuntime {
    private static volatile VulkanProbeResult probeResult;
    private static VulkanDriverLoader.LoadedDriver activeDriver;

    private VulkanRuntime() { }

    public static synchronized void initialize(VulkanMode mode) {
        if (mode == VulkanMode.OFF || probeResult != null) return;
        VulkanDriverLoader.LoadedDriver loaded = null;
        try {
            loaded = VulkanDriverLoader.loadDesktop();
            VulkanProbeResult result = loaded.probe();
            probeResult = result;
            if (!result.available()) {
                loaded.close();
                loaded = null;
                log("Vulkan unavailable; retaining Slick/OpenGL: " + result.diagnostic());
                if (mode == VulkanMode.REQUIRED) {
                    throw new IllegalStateException(result.diagnostic());
                }
                return;
            }
            activeDriver = loaded;
            log("Vulkan " + VulkanProbeResult.formatVersion(result.instanceVersion())
                    + " available through " + loaded.name() + "; "
                    + result.devices().size() + " physical device(s)");
            for (VulkanDeviceInfo device : result.devices()) {
                log("  " + device.name() + " (vendor=0x"
                        + Integer.toHexString(device.vendorId()) + ", device=0x"
                        + Integer.toHexString(device.deviceId()) + ", api="
                        + VulkanProbeResult.formatVersion(device.apiVersion()) + ")");
            }
            log("Renderer takeover is not enabled in this foundation build");
        } catch (RuntimeException failure) {
            if (loaded != null && loaded != activeDriver) {
                try {
                    loaded.close();
                } catch (RuntimeException closeFailure) {
                    failure.addSuppressed(closeFailure);
                }
            }
            probeResult = VulkanProbeResult.unavailable(
                    failure.getClass().getSimpleName() + ": " + failure.getMessage());
            if (mode == VulkanMode.REQUIRED) throw failure;
            log("Vulkan probe failed; retaining Slick/OpenGL: " + probeResult.diagnostic());
        }
    }

    public static Optional<VulkanProbeResult> probeResult() {
        return Optional.ofNullable(probeResult);
    }

    private static void log(String message) {
        System.out.println("[Vulkan Mod] " + message);
    }
}
