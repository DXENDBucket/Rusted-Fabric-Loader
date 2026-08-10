package io.github.endx.vulkanmod.spi;

import java.util.Objects;

/** Binding-neutral summary of a live Vulkan surface and swapchain. */
public final class VulkanSurfaceInfo {
    private final String deviceName;
    private final int width;
    private final int height;
    private final int imageCount;
    private final int imageFormat;
    private final int colorSpace;
    private final int presentMode;
    private final int graphicsQueueFamily;
    private final int presentQueueFamily;

    public VulkanSurfaceInfo(String deviceName, int width, int height, int imageCount,
                             int imageFormat, int colorSpace, int presentMode,
                             int graphicsQueueFamily, int presentQueueFamily) {
        this.deviceName = Objects.requireNonNull(deviceName, "deviceName");
        this.width = width;
        this.height = height;
        this.imageCount = imageCount;
        this.imageFormat = imageFormat;
        this.colorSpace = colorSpace;
        this.presentMode = presentMode;
        this.graphicsQueueFamily = graphicsQueueFamily;
        this.presentQueueFamily = presentQueueFamily;
    }

    public String deviceName() { return deviceName; }
    public int width() { return width; }
    public int height() { return height; }
    public int imageCount() { return imageCount; }
    public int imageFormat() { return imageFormat; }
    public int colorSpace() { return colorSpace; }
    public int presentMode() { return presentMode; }
    public int graphicsQueueFamily() { return graphicsQueueFamily; }
    public int presentQueueFamily() { return presentQueueFamily; }
}
