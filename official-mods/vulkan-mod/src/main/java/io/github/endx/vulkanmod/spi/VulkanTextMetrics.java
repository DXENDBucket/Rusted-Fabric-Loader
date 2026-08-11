package io.github.endx.vulkanmod.spi;

/** Platform-neutral dimensions of a text run as it will be rasterized by the native renderer. */
public final class VulkanTextMetrics {
    private final int width;
    private final int height;
    private final int lineHeight;

    public VulkanTextMetrics(int width, int height, int lineHeight) {
        if (width < 0 || height < 0 || lineHeight < 0) {
            throw new IllegalArgumentException("text metrics must not be negative");
        }
        this.width = width;
        this.height = height;
        this.lineHeight = lineHeight;
    }

    public int width() { return width; }
    public int height() { return height; }
    public int lineHeight() { return lineHeight; }
}
