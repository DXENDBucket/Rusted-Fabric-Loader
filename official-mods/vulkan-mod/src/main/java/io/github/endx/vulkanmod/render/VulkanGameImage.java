package io.github.endx.vulkanmod.render;

import rustedwarfare.client.render.GameImage;

import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferInt;
import java.awt.image.DirectColorModel;
import java.awt.image.WritableRaster;
import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.Map;

/** CPU-backed game image whose pixels can be uploaded without an OpenGL readback. */
public final class VulkanGameImage extends GameImage {
    private transient BufferedImage bufferedImage;
    private final boolean opaque;
    private transient long nativeRenderTargetHandle;
    private transient Runnable nativeRenderTargetFlusher;
    private transient Map<Object, Runnable> pendingNativeConsumers;

    public VulkanGameImage(int width, int height, int[] argb) {
        this(width, height, argb, false);
    }

    public VulkanGameImage(int width, int height, int[] argb, boolean opaque) {
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("image size must be positive");
        }
        int pixelCount = Math.multiplyExact(width, height);
        if (argb == null || argb.length != pixelCount) {
            throw new IllegalArgumentException("expected " + pixelCount + " ARGB pixels");
        }
        this.width = width;
        this.height = height;
        this.pixelBuffer = argb;
        this.opaque = opaque;
        updateCenterValues();
    }

    public static VulkanGameImage empty(int width, int height) {
        return empty(width, height, true);
    }

    public static VulkanGameImage empty(int width, int height, boolean alpha) {
        int[] pixels = new int[Math.multiplyExact(width, height)];
        if (!alpha) Arrays.fill(pixels, 0xff000000);
        return new VulkanGameImage(width, height, pixels, !alpha);
    }

    public boolean isOpaque() { return opaque; }

    public long nativeRenderTargetHandle() { return nativeRenderTargetHandle; }

    public void setNativeRenderTargetHandle(long handle) {
        if (handle < 0L) throw new IllegalArgumentException("negative Vulkan texture handle");
        nativeRenderTargetHandle = handle;
    }

    public void setNativeRenderTargetFlusher(Runnable flusher) {
        nativeRenderTargetFlusher = flusher;
    }

    /** Makes pending native draw commands visible before this image is sampled as a texture. */
    public void submitPendingNativeDraws() {
        if (nativeRenderTargetFlusher != null) nativeRenderTargetFlusher.run();
    }

    /** Records a deferred render target whose commands currently sample this image. */
    public void registerPendingNativeConsumer(Object key, Runnable submitter) {
        if (key == null || submitter == null) throw new NullPointerException();
        if (pendingNativeConsumers == null) {
            pendingNativeConsumers = new IdentityHashMap<Object, Runnable>();
        }
        pendingNativeConsumers.put(key, submitter);
    }

    /** Executes deferred consumers before this scratch image is overwritten. */
    public void submitPendingNativeConsumers() {
        if (pendingNativeConsumers == null || pendingNativeConsumers.isEmpty()) return;
        Runnable[] submitters = pendingNativeConsumers.values().toArray(new Runnable[0]);
        pendingNativeConsumers.clear();
        for (Runnable submitter : submitters) submitter.run();
    }

    @Override public boolean canReadPixels() { return true; }
    @Override public void ensurePixelBuffer() { }
    @Override public void readPixelsFromBitmap() { }
    @Override public void ensureImageDataAvailable() { }
    @Override public void releaseImageData() { }
    @Override public void dropPixelBuffer() { }
    @Override public void discardPixelBuffer() { }
    @Override public void flushPixelBufferToBitmap() {
        submitPendingNativeDraws();
        version++;
    }
    @Override public void releaseBitmap() { }

    /** Returns a persistent Java2D view over this image's stable ARGB pixel array. */
    public BufferedImage bufferedImage() {
        if (bufferedImage == null) {
            DataBufferInt buffer = new DataBufferInt(pixelBuffer, width * height);
            int[] masks = { 0x00ff0000, 0x0000ff00, 0x000000ff, 0xff000000 };
            WritableRaster raster = java.awt.image.Raster.createPackedRaster(
                    buffer, width, height, width, masks, null);
            DirectColorModel colors = new DirectColorModel(
                    ColorSpace.getInstance(ColorSpace.CS_sRGB), 32,
                    masks[0], masks[1], masks[2], masks[3], false, DataBuffer.TYPE_INT);
            bufferedImage = new BufferedImage(colors, raster, false, null);
        }
        return bufferedImage;
    }

    /** Converts the stable backing array without virtual getPixel calls. */
    public byte[] copyRgbaBytes() {
        byte[] rgba = new byte[Math.multiplyExact(pixelBuffer.length, 4)];
        int offset = 0;
        for (int argb : pixelBuffer) {
            rgba[offset++] = (byte) (argb >>> 16);
            rgba[offset++] = (byte) (argb >>> 8);
            rgba[offset++] = (byte) argb;
            rgba[offset++] = (byte) (argb >>> 24);
        }
        return rgba;
    }

    @Override public GameImage copyImage() {
        VulkanGameImage copy = new VulkanGameImage(width, height,
                Arrays.copyOf(pixelBuffer, pixelBuffer.length), opaque);
        copy.smooth = smooth;
        return copy;
    }

    @Override public GameImage createImageCopyWithSize(int newWidth, int newHeight,
                                                       boolean copyPixels) {
        VulkanGameImage copy = empty(newWidth, newHeight, !opaque);
        copy.smooth = smooth;
        if (copyPixels) {
            int rows = Math.min(height, newHeight);
            int columns = Math.min(width, newWidth);
            for (int y = 0; y < rows; y++) {
                System.arraycopy(pixelBuffer, y * width,
                        copy.pixelBuffer, y * newWidth, columns);
            }
        }
        return copy;
    }
}
