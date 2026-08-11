package io.github.endx.vulkanmod.spi;

import java.nio.ByteBuffer;
import java.util.Arrays;

/** Immutable, tightly packed RGBA8 texture payload crossing the platform-driver boundary. */
public final class VulkanTextureData {
    private final int width;
    private final int height;
    private final byte[] rgba;
    private final int[] argb;

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
        this.argb = null;
    }

    private VulkanTextureData(int width, int height, int[] argb) {
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("texture dimensions must be positive");
        }
        if (argb == null) throw new NullPointerException("argb");
        int expected = Math.multiplyExact(width, height);
        if (argb.length != expected) {
            throw new IllegalArgumentException("expected " + expected
                    + " ARGB pixels, got " + argb.length);
        }
        this.width = width;
        this.height = height;
        this.rgba = null;
        // The render target can be reused immediately after its draw command is recorded. Keep a
        // cheap native array-copy snapshot and perform the channel conversion only once, directly
        // into mapped Vulkan staging memory.
        this.argb = Arrays.copyOf(argb, argb.length);
    }

    public static VulkanTextureData fromArgb(int width, int height, int[] argb) {
        return new VulkanTextureData(width, height, argb);
    }

    public int width() { return width; }
    public int height() { return height; }
    public int byteSize() { return Math.multiplyExact(Math.multiplyExact(width, height), 4); }

    /** Copies this payload straight into mapped staging memory without another heap array. */
    public void writeTo(ByteBuffer destination) {
        if (destination == null) throw new NullPointerException("destination");
        int byteSize = byteSize();
        if (destination.remaining() < byteSize) {
            throw new IllegalArgumentException("destination has only "
                    + destination.remaining() + " bytes remaining");
        }
        if (rgba != null) {
            destination.put(rgba);
            return;
        }
        for (int pixel : argb) {
            destination.put((byte) (pixel >>> 16));
            destination.put((byte) (pixel >>> 8));
            destination.put((byte) pixel);
            destination.put((byte) (pixel >>> 24));
        }
    }

    /** Returns a defensive copy so a submitted upload cannot be mutated by its caller. */
    public byte[] copyRgba() {
        if (rgba != null) return Arrays.copyOf(rgba, rgba.length);
        byte[] copy = new byte[byteSize()];
        writeTo(java.nio.ByteBuffer.wrap(copy));
        return copy;
    }
}
