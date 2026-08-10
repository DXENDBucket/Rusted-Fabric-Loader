package io.github.endx.vulkanmod;

import io.github.endx.vulkanmod.spi.VulkanDeviceInfo;
import io.github.endx.vulkanmod.spi.VulkanProbeResult;
import io.github.endx.vulkanmod.spi.VulkanSurfaceInfo;

import java.util.Optional;

/** Owns Vulkan startup state without touching the legacy renderer until takeover is implemented. */
public final class VulkanRuntime {
    private static volatile VulkanProbeResult probeResult;
    private static VulkanDriverLoader.LoadedDriver activeDriver;
    private static volatile VulkanSurfaceInfo surfaceInfo;
    private static VulkanMode configuredMode = VulkanMode.OFF;
    private static boolean frameTestAttempted;

    private VulkanRuntime() { }

    public static synchronized void initialize(VulkanMode mode) {
        configuredMode = mode;
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

    /** Called immediately after Slick presents an OpenGL frame. */
    public static synchronized void afterOpenGlPresent() {
        if (configuredMode != VulkanMode.FRAME_TEST || frameTestAttempted
                || activeDriver == null || surfaceInfo == null) return;
        frameTestAttempted = true;
        try {
            io.github.endx.vulkanmod.spi.VulkanSurfaceRequest window =
                    Lwjgl2Win32Window.current();
            VulkanSurfaceInfo updated = activeDriver.presentClearFrame(
                    window.width(), window.height(), 0.035f, 0.075f, 0.16f, 1.0f);
            surfaceInfo = updated;
            log("Presented one Vulkan frame-test clear at "
                    + updated.width() + "x" + updated.height());
        } catch (Throwable failure) {
            log("Vulkan frame test failed; retaining Slick/OpenGL: "
                    + failure.getClass().getSimpleName() + ": " + failure.getMessage());
        }
    }

    public static synchronized void attachToCurrentWindow() {
        if (activeDriver == null || surfaceInfo != null) return;
        try {
            VulkanSurfaceInfo created = activeDriver.createSurface(Lwjgl2Win32Window.current());
            surfaceInfo = created;
            log("Win32 surface and swapchain ready on " + created.deviceName() + ": "
                    + created.width() + "x" + created.height() + ", images="
                    + created.imageCount() + ", format=" + created.imageFormat()
                    + ", presentMode=" + created.presentMode() + ", queues="
                    + created.graphicsQueueFamily() + "/" + created.presentQueueFamily());
        } catch (Throwable failure) {
            log("Vulkan surface validation failed; retaining Slick/OpenGL: "
                    + failure.getClass().getSimpleName() + ": " + failure.getMessage());
            if (configuredMode == VulkanMode.REQUIRED) {
                if (failure instanceof Error) throw (Error) failure;
                throw (RuntimeException) failure;
            }
        }
    }

    public static Optional<VulkanProbeResult> probeResult() {
        return Optional.ofNullable(probeResult);
    }

    public static Optional<VulkanSurfaceInfo> surfaceInfo() {
        return Optional.ofNullable(surfaceInfo);
    }

    private static void log(String message) {
        System.out.println("[Vulkan Mod] " + message);
    }
}
