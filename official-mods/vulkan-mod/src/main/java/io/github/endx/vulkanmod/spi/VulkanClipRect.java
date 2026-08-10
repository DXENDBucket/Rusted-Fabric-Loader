package io.github.endx.vulkanmod.spi;

/** Screen-space scissor rectangle in pixels. */
public final class VulkanClipRect {
    private final float x;
    private final float y;
    private final float width;
    private final float height;

    public VulkanClipRect(float x, float y, float width, float height) {
        if (!Float.isFinite(x) || !Float.isFinite(y)
                || !Float.isFinite(width) || !Float.isFinite(height)
                || !Float.isFinite(x + width) || !Float.isFinite(y + height)) {
            throw new IllegalArgumentException("clip rectangle must be finite");
        }
        if (width < 0.0f || height < 0.0f) {
            throw new IllegalArgumentException("clip dimensions must be non-negative");
        }
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public float x() { return x; }
    public float y() { return y; }
    public float width() { return width; }
    public float height() { return height; }

    @Override public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof VulkanClipRect)) return false;
        VulkanClipRect that = (VulkanClipRect) other;
        return Float.compare(x, that.x) == 0 && Float.compare(y, that.y) == 0
                && Float.compare(width, that.width) == 0
                && Float.compare(height, that.height) == 0;
    }

    @Override public int hashCode() {
        int result = Float.floatToIntBits(x);
        result = 31 * result + Float.floatToIntBits(y);
        result = 31 * result + Float.floatToIntBits(width);
        return 31 * result + Float.floatToIntBits(height);
    }
}
