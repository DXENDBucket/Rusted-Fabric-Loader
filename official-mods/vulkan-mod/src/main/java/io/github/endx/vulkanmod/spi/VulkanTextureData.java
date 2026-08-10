package io.github.endx.vulkanmod.spi;

import java.util.Arrays;

/** Immutable, tightly packed RGBA8 texture payload crossing the platform-driver boundary. */
public final class VulkanTextureData {
    private final int width;
    private final int height;
    private final byte[] rgba;

    public VulkanTextureData(int width, int height, byte[] rgba) {
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("texture dimensions must be positive");
        }
        if (rgba == null) throw new NullPointerException("rgba");
        int expected = Math.multiplyExact(Math.multiplyExact(width, height), 4);
        if (rgba.length != expected) {
            throw new IllegalArgumentException("expected " + expected
                    + " RGBA bytes, got " + rgba.length);
        }
        this.width = width;
        this.height = height;
        this.rgba = Arrays.copyOf(rgba, rgba.length);
    }

    public int width() { return width; }
    public int height() { return height; }

    /** Returns a defensive copy so a submitted upload cannot be mutated by its caller. */
    public byte[] copyRgba() { return Arrays.copyOf(rgba, rgba.length); }
}
