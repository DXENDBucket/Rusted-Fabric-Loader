package io.github.endx.vulkanmod;

import io.github.endx.vulkanmod.spi.VulkanTextureData;
import rustedwarfare.client.render.GameImage;

import java.util.IdentityHashMap;
import java.util.Map;

/** Converts game-owned ARGB images to cached driver-owned RGBA8 textures. */
final class GameImageVulkanTextureCache implements AutoCloseable {
    private final VulkanDriverLoader.LoadedDriver driver;
    private final Map<GameImage, Entry> entries = new IdentityHashMap<GameImage, Entry>();
    private boolean closed;

    GameImageVulkanTextureCache(VulkanDriverLoader.LoadedDriver driver) {
        if (driver == null) throw new NullPointerException("driver");
        this.driver = driver;
    }

    synchronized long texture(GameImage source) {
        if (closed) throw new IllegalStateException("texture cache is closed");
        if (source == null) throw new NullPointerException("source");
        GameImage image = source.getRealImage();
        if (image == null) image = source;
        int width = image.getWidth();
        int height = image.getHeight();
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("game image has invalid dimensions: "
                    + width + "x" + height);
        }
        Entry current = entries.get(image);
        if (current != null && current.version == image.version
                && current.width == width && current.height == height) {
            return current.textureHandle;
        }
        long uploaded = driver.uploadTexture(readRgba(image, width, height));
        Entry replacement = new Entry(uploaded, image.version, width, height);
        entries.put(image, replacement);
        if (current != null) driver.destroyTexture(current.textureHandle);
        return uploaded;
    }

    synchronized void invalidate(Object candidate) {
        if (closed || !(candidate instanceof GameImage)) return;
        GameImage image = ((GameImage) candidate).getRealImage();
        if (image == null) image = (GameImage) candidate;
        Entry removed = entries.remove(image);
        if (removed != null) driver.destroyTexture(removed.textureHandle);
    }

    synchronized int size() {
        return entries.size();
    }

    @Override public synchronized void close() {
        if (closed) return;
        closed = true;
        for (Entry entry : entries.values()) {
            driver.destroyTexture(entry.textureHandle);
        }
        entries.clear();
    }

    private static VulkanTextureData readRgba(GameImage image, int width, int height) {
        int byteCount = Math.multiplyExact(Math.multiplyExact(width, height), 4);
        byte[] rgba = new byte[byteCount];
        image.ensureImageDataAvailable();
        try {
            int offset = 0;
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    int argb = image.getPixel(x, y);
                    rgba[offset++] = (byte) (argb >>> 16);
                    rgba[offset++] = (byte) (argb >>> 8);
                    rgba[offset++] = (byte) argb;
                    rgba[offset++] = (byte) (argb >>> 24);
                }
            }
        } finally {
            image.releaseImageData();
        }
        return new VulkanTextureData(width, height, rgba);
    }

    private static final class Entry {
        private final long textureHandle;
        private final int version;
        private final int width;
        private final int height;

        private Entry(long textureHandle, int version, int width, int height) {
            this.textureHandle = textureHandle;
            this.version = version;
            this.width = width;
            this.height = height;
        }
    }
}
