package io.github.endx.vulkanmod;

import io.github.endx.vulkanmod.spi.VulkanTextureData;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.LongConsumer;

/** Small LRU of antialiased white glyph runs that Vulkan can tint per draw. */
final class VulkanTextTextureCache implements AutoCloseable {
    private static final int MAX_ENTRIES = 768;
    private static final int MAX_NEW_UPLOADS_PER_FRAME = 32;

    private final VulkanDriverLoader.LoadedDriver driver;
    private final AsyncVulkanPresenter presenter;
    private final LongConsumer textureDestroyer;
    private final Font regularFont;
    private final Font boldFont;
    private final Font cjkFont;
    private final Font legacyFallbackFont;
    private final LinkedHashMap<Key, Entry> entries =
            new LinkedHashMap<Key, Entry>(128, 0.75f, true);
    private boolean closed;
    private int uploadsStartedThisFrame;

    VulkanTextTextureCache(VulkanDriverLoader.LoadedDriver driver,
                           AsyncVulkanPresenter presenter,
                           LongConsumer textureDestroyer) {
        if (driver == null) throw new NullPointerException("driver");
        this.driver = driver;
        this.presenter = presenter;
        this.textureDestroyer = textureDestroyer;
        regularFont = loadGameFont("font/Roboto-Regular.ttf", Font.PLAIN);
        boldFont = loadGameFont("font/Roboto-Bold.ttf", Font.BOLD);
        cjkFont = loadGameFont("font/NotoSansCJKsc-Regular.otf", Font.PLAIN);
        legacyFallbackFont = loadGameFont("font/DroidSansFallback.ttf", Font.PLAIN);
    }

    synchronized Entry texture(String text, int requestedSize, boolean bold) {
        if (closed) throw new IllegalStateException("text texture cache is closed");
        if (text == null) throw new NullPointerException("text");
        int size = Math.max(4, Math.min(256, requestedSize));
        Key key = new Key(text, size, bold);
        Entry current = entries.get(key);
        if (current != null) return current.textureHandle == 0L ? null : current;
        if (presenter != null && uploadsStartedThisFrame >= MAX_NEW_UPLOADS_PER_FRAME) {
            return null;
        }
        uploadsStartedThisFrame++;
        Raster raster = rasterize(text, size, bold);
        Entry created = new Entry(0L,
                raster.width, raster.height, raster.lineHeight);
        entries.put(key, created);
        VulkanTextureData textureData = new VulkanTextureData(
                raster.width, raster.height, raster.rgba);
        if (presenter == null) {
            created.textureHandle = driver.uploadTexture(textureData);
        } else {
            presenter.uploadTexture(textureData,
                    new AsyncVulkanPresenter.TextureUploadListener() {
                        @Override public void uploaded(long textureHandle) {
                            completeUpload(key, created, textureHandle);
                        }

                        @Override public void failed(Throwable failure) {
                            failUpload(key, created);
                        }
                    });
        }
        evictOldEntries();
        return created.textureHandle == 0L ? null : created;
    }

    private void evictOldEntries() {
        while (entries.size() > MAX_ENTRIES) {
            Map.Entry<Key, Entry> oldest = entries.entrySet().iterator().next();
            entries.remove(oldest.getKey());
            release(oldest.getValue());
        }
    }

    private synchronized void completeUpload(Key key, Entry entry, long textureHandle) {
        if (!closed && entries.get(key) == entry) entry.textureHandle = textureHandle;
        else textureDestroyer.accept(textureHandle);
    }

    private synchronized void failUpload(Key key, Entry entry) {
        if (entries.get(key) == entry) entries.remove(key);
    }

    private void release(Entry entry) {
        if (entry != null && entry.textureHandle != 0L) {
            textureDestroyer.accept(entry.textureHandle);
        }
    }

    synchronized void beginFrame() {
        uploadsStartedThisFrame = 0;
    }

    @Override public synchronized void close() {
        if (closed) return;
        closed = true;
        for (Entry entry : entries.values()) {
            release(entry);
        }
        entries.clear();
    }

    private Raster rasterize(String text, int size, boolean bold) {
        Font primary = (bold ? boldFont : regularFont).deriveFont((float) size);
        Font font = selectFont(primary, text, size);
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

    private Font selectFont(Font primary, String text, int size) {
        if (primary.canDisplayUpTo(text) < 0) return primary;
        Font cjk = cjkFont.deriveFont((float) size);
        if (cjk.canDisplayUpTo(text) < 0) return cjk;
        return legacyFallbackFont.deriveFont((float) size);
    }

    private static Font loadGameFont(String resource, int fallbackStyle) {
        try (InputStream stream = openGameResource(resource)) {
            if (stream == null) throw new IOException("resource not found: " + resource);
            return Font.createFont(Font.TRUETYPE_FONT, stream);
        } catch (Exception failure) {
            System.out.println("[Vulkan Mod] Could not load original game font " + resource
                    + "; using logical SansSerif: " + failure.getMessage());
            return new Font(Font.SANS_SERIF, fallbackStyle, 1);
        }
    }

    private static InputStream openGameResource(String resource) throws IOException {
        ClassLoader context = Thread.currentThread().getContextClassLoader();
        InputStream stream = context == null ? null : context.getResourceAsStream(resource);
        if (stream == null) {
            stream = VulkanTextTextureCache.class.getClassLoader().getResourceAsStream(resource);
        }
        if (stream != null) return stream;
        Path path = Paths.get(resource);
        return Files.isRegularFile(path) ? Files.newInputStream(path) : null;
    }

    private static void configure(Graphics2D graphics) {
        graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        graphics.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS,
                RenderingHints.VALUE_FRACTIONALMETRICS_OFF);
    }

    static final class Entry {
        volatile long textureHandle;
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
