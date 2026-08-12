package io.github.endx.vulkanmod.spi;

import java.util.Arrays;

/** Platform-rasterized white glyph coverage in tightly packed RGBA8 form. */
public final class VulkanGlyphBitmap {
    private final int bearingX;
    private final int bearingY;
    private final int width;
    private final int height;
    private final byte[] rgba;

    public VulkanGlyphBitmap(int bearingX, int bearingY, int width, int height, byte[] rgba) {
        if (width < 0 || height < 0) {
            throw new IllegalArgumentException("glyph dimensions must not be negative");
        }
        if (rgba == null) throw new NullPointerException("rgba");
        int expected = Math.multiplyExact(Math.multiplyExact(width, height), 4);
        if (rgba.length != expected) {
            throw new IllegalArgumentException("expected " + expected
                    + " glyph RGBA bytes, got " + rgba.length);
        }
        this.bearingX = bearingX;
        this.bearingY = bearingY;
        this.width = width;
        this.height = height;
        this.rgba = Arrays.copyOf(rgba, rgba.length);
    }

    public int bearingX() { return bearingX; }
    public int bearingY() { return bearingY; }
    public int width() { return width; }
    public int height() { return height; }
    public boolean empty() { return width == 0 || height == 0; }
    public byte[] copyRgba() { return Arrays.copyOf(rgba, rgba.length); }
}
