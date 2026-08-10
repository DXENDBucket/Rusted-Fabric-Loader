package io.github.endx.vulkanmod;

import io.github.endx.vulkanmod.spi.VulkanTextureData;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.LinkedHashMap;
import java.util.Map;

/** Small LRU of antialiased white glyph runs that Vulkan can tint per draw. */
final class VulkanTextTextureCache implements AutoCloseable {
    private static final int MAX_ENTRIES = 768;

    private final VulkanDriverLoader.LoadedDriver driver;
    private final LinkedHashMap<Key, Entry> entries =
            new LinkedHashMap<Key, Entry>(128, 0.75f, true);
    private boolean closed;

    VulkanTextTextureCache(VulkanDriverLoader.LoadedDriver driver) {
        if (driver == null) throw new NullPointerException("driver");
        this.driver = driver;
    }

    synchronized Entry texture(String text, int requestedSize, boolean bold) {
        if (closed) throw new IllegalStateException("text texture cache is closed");
        if (text == null) throw new NullPointerException("text");
        int size = Math.max(4, Math.min(256, requestedSize));
        Key key = new Key(text, size, bold);
        Entry current = entries.get(key);
        if (current != null) return current;
        Raster raster = rasterize(text, size, bold);
        Entry created = new Entry(driver.uploadTexture(
                new VulkanTextureData(raster.width, raster.height, raster.rgba)),
                raster.width, raster.height, raster.lineHeight);
        entries.put(key, created);
        evictOldEntries();
        return created;
    }

    private void evictOldEntries() {
        while (entries.size() > MAX_ENTRIES) {
            Map.Entry<Key, Entry> oldest = entries.entrySet().iterator().next();
            entries.remove(oldest.getKey());
            driver.destroyTexture(oldest.getValue().textureHandle);
        }
    }

    @Override public synchronized void close() {
        if (closed) return;
        closed = true;
        for (Entry entry : entries.values()) {
            driver.destroyTexture(entry.textureHandle);
        }
        entries.clear();
    }

    private static Raster rasterize(String text, int size, boolean bold) {
        Font font = new Font(Font.SANS_SERIF, bold ? Font.BOLD : Font.PLAIN, size);
        BufferedImage measurement = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        Graphics2D measureGraphics = measurement.createGraphics();
        configure(measureGraphics);
        measureGraphics.setFont(font);
        FontMetrics metrics = measureGraphics.getFontMetrics();
        String[] lines = text.split("\\n", -1);
        int width = 1;
        for (String line : lines) width = Math.max(width, metrics.stringWidth(line));
        int lineHeight = Math.max(1, metrics.getHeight());
        int height = Math.max(1, Math.multiplyExact(lineHeight, lines.length));
        measureGraphics.dispose();

        width = Math.min(4096, width);
        height = Math.min(4096, height);
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        configure(graphics);
        graphics.setFont(font);
        graphics.setColor(Color.WHITE);
        int baseline = metrics.getAscent();
        for (int index = 0; index < lines.length; index++) {
            int y = baseline + index * lineHeight;
            if (y > height + lineHeight) break;
            graphics.drawString(lines[index], 0, y);
        }
        graphics.dispose();

        byte[] rgba = new byte[Math.multiplyExact(Math.multiplyExact(width, height), 4)];
        int offset = 0;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int argb = image.getRGB(x, y);
                rgba[offset++] = (byte) 255;
                rgba[offset++] = (byte) 255;
                rgba[offset++] = (byte) 255;
                rgba[offset++] = (byte) (argb >>> 24);
            }
        }
        return new Raster(width, height, lineHeight, rgba);
    }

    private static void configure(Graphics2D graphics) {
        graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        graphics.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS,
                RenderingHints.VALUE_FRACTIONALMETRICS_OFF);
    }

    static final class Entry {
        final long textureHandle;
        final int width;
        final int height;
        final int lineHeight;

        private Entry(long textureHandle, int width, int height, int lineHeight) {
            this.textureHandle = textureHandle;
            this.width = width;
            this.height = height;
            this.lineHeight = lineHeight;
        }
    }

    private static final class Key {
        private final String text;
        private final int size;
        private final boolean bold;

        private Key(String text, int size, boolean bold) {
            this.text = text;
            this.size = size;
            this.bold = bold;
        }

        @Override public boolean equals(Object candidate) {
            if (this == candidate) return true;
            if (!(candidate instanceof Key)) return false;
            Key other = (Key) candidate;
            return size == other.size && bold == other.bold && text.equals(other.text);
        }

        @Override public int hashCode() {
            int result = text.hashCode();
            result = 31 * result + size;
            result = 31 * result + (bold ? 1 : 0);
            return result;
        }
    }

    private static final class Raster {
        private final int width;
        private final int height;
        private final int lineHeight;
        private final byte[] rgba;

        private Raster(int width, int height, int lineHeight, byte[] rgba) {
            this.width = width;
            this.height = height;
            this.lineHeight = lineHeight;
            this.rgba = rgba;
        }
    }
}
