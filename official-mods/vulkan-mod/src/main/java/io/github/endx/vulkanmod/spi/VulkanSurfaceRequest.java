package io.github.endx.vulkanmod.spi;

/** Native window data needed to create a platform Vulkan surface. */
public final class VulkanSurfaceRequest {
    private final String platform;
    private final long windowHandle;
    private final long instanceHandle;
    private final int width;
    private final int height;
    private final boolean childOverlay;

    private VulkanSurfaceRequest(String platform, long windowHandle, long instanceHandle,
                                 int width, int height, boolean childOverlay) {
        if (windowHandle == 0L) throw new IllegalArgumentException("window handle is zero");
        if (instanceHandle == 0L) throw new IllegalArgumentException("instance handle is zero");
        if (width <= 0 || height <= 0) throw new IllegalArgumentException(
                "surface size must be positive");
        this.platform = platform;
        this.windowHandle = windowHandle;
        this.instanceHandle = instanceHandle;
        this.width = width;
        this.height = height;
        this.childOverlay = childOverlay;
    }

    public static VulkanSurfaceRequest win32(long hwnd, long hinstance, int width, int height) {
        return new VulkanSurfaceRequest("win32", hwnd, hinstance, width, height, false);
    }

    public VulkanSurfaceRequest asChildOverlay() {
        if (!"win32".equals(platform)) {
            throw new IllegalStateException("child overlays are only supported on Win32");
        }
        return new VulkanSurfaceRequest(platform, windowHandle, instanceHandle,
                width, height, true);
    }

    public String platform() { return platform; }
    public long windowHandle() { return windowHandle; }
    public long instanceHandle() { return instanceHandle; }
    public int width() { return width; }
    public int height() { return height; }
    public boolean childOverlay() { return childOverlay; }
}
