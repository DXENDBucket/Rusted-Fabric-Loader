package io.github.endx.vulkanmod.spi;

/** Native window data needed to create a platform Vulkan surface. */
public final class VulkanSurfaceRequest {
    private final String platform;
    private final long windowHandle;
    private final long instanceHandle;
    private final int width;
    private final int height;

    private VulkanSurfaceRequest(String platform, long windowHandle, long instanceHandle,
                                 int width, int height) {
        if (windowHandle == 0L) throw new IllegalArgumentException("window handle is zero");
        if (instanceHandle == 0L) throw new IllegalArgumentException("instance handle is zero");
        if (width <= 0 || height <= 0) throw new IllegalArgumentException(
                "surface size must be positive");
        this.platform = platform;
        this.windowHandle = windowHandle;
        this.instanceHandle = instanceHandle;
        this.width = width;
        this.height = height;
    }

    public static VulkanSurfaceRequest win32(long hwnd, long hinstance, int width, int height) {
        return new VulkanSurfaceRequest("win32", hwnd, hinstance, width, height);
    }

    public String platform() { return platform; }
    public long windowHandle() { return windowHandle; }
    public long instanceHandle() { return instanceHandle; }
    public int width() { return width; }
    public int height() { return height; }
}
