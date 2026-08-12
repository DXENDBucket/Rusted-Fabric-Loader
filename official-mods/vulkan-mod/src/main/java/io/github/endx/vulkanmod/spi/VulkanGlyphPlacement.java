package io.github.endx.vulkanmod.spi;

/** One positioned glyph in a platform-shaped text run. */
public final class VulkanGlyphPlacement {
    private final long glyphKey;
    private final float x;
    private final float y;

    public VulkanGlyphPlacement(long glyphKey, float x, float y) {
        if (glyphKey <= 0L) throw new IllegalArgumentException("glyphKey must be positive");
        if (!Float.isFinite(x) || !Float.isFinite(y)) {
            throw new IllegalArgumentException("glyph position must be finite");
        }
        this.glyphKey = glyphKey;
        this.x = x;
        this.y = y;
    }

    public long glyphKey() { return glyphKey; }
    public float x() { return x; }
    public float y() { return y; }
}
