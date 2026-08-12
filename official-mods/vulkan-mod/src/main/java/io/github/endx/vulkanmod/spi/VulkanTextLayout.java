package io.github.endx.vulkanmod.spi;

import java.util.Arrays;

/** Immutable platform-shaped text run consumed by the shared glyph atlas. */
public final class VulkanTextLayout {
    private final int width;
    private final int height;
    private final int lineHeight;
    private final VulkanGlyphPlacement[] glyphs;

    public VulkanTextLayout(int width, int height, int lineHeight,
                            VulkanGlyphPlacement[] glyphs) {
        if (width < 0 || height < 0 || lineHeight < 0) {
            throw new IllegalArgumentException("text layout dimensions must not be negative");
        }
        if (glyphs == null) throw new NullPointerException("glyphs");
        this.width = width;
        this.height = height;
        this.lineHeight = lineHeight;
        this.glyphs = Arrays.copyOf(glyphs, glyphs.length);
        for (VulkanGlyphPlacement glyph : this.glyphs) {
            if (glyph == null) throw new NullPointerException("glyph");
        }
    }

    public int width() { return width; }
    public int height() { return height; }
    public int lineHeight() { return lineHeight; }
    public int glyphCount() { return glyphs.length; }

    public VulkanGlyphPlacement glyph(int index) {
        if (index < 0 || index >= glyphs.length) throw new IndexOutOfBoundsException(index);
        return glyphs[index];
    }

    public VulkanTextMetrics metrics() {
        return new VulkanTextMetrics(width, height, lineHeight);
    }
}
