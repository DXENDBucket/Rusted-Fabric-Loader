package io.github.endx.vulkanmod.render;

import rustedwarfare.client.render.GameImage;

import java.util.Arrays;

/** CPU-backed game image whose pixels can be uploaded without an OpenGL readback. */
public final class VulkanGameImage extends GameImage {
    public VulkanGameImage(int width, int height, int[] argb) {
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
        updateCenterValues();
    }

    public static VulkanGameImage empty(int width, int height) {
        return new VulkanGameImage(width, height,
                new int[Math.multiplyExact(width, height)]);
    }

    @Override public boolean canReadPixels() { return true; }
    @Override public void ensurePixelBuffer() { }
    @Override public void readPixelsFromBitmap() { }
    @Override public void ensureImageDataAvailable() { }
    @Override public void releaseImageData() { }
    @Override public void dropPixelBuffer() { }
    @Override public void discardPixelBuffer() { }
    @Override public void flushPixelBufferToBitmap() { version++; }
    @Override public void releaseBitmap() { }

    @Override public GameImage copyImage() {
        VulkanGameImage copy = new VulkanGameImage(width, height,
                Arrays.copyOf(pixelBuffer, pixelBuffer.length));
        copy.smooth = smooth;
        return copy;
    }

    @Override public GameImage createImageCopyWithSize(int newWidth, int newHeight,
                                                       boolean copyPixels) {
        VulkanGameImage copy = empty(newWidth, newHeight);
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
