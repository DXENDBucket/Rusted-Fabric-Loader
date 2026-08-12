package io.github.endx.vulkanmod;

import io.github.endx.vulkanmod.spi.VulkanTextureData;
import io.github.endx.vulkanmod.spi.VulkanTextMetrics;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Rectangle;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.Bidi;
import java.util.LinkedHashMap;
import java.util.ArrayList;
import java.util.Map;
import java.util.function.LongConsumer;

/** Small LRU of antialiased white glyph runs that Vulkan can tint per draw. */
final class VulkanTextTextureCache implements AutoCloseable {
    private static final int MAX_ENTRIES = 768;
    private static final int MAX_NEW_UPLOADS_PER_FRAME = 32;
    private static final int ATLAS_SIZE = 1024;
    private static final int ATLAS_PADDING = 1;
    private static final int MAX_ATLAS_PAGES = 16;
    private static final int MAX_NEW_GLYPHS_PER_FRAME = 96;

    private final TextureAccess textures;
    private final AsyncVulkanPresenter presenter;
    private final LongConsumer textureDestroyer;
    private final Font regularFont;
    private final Font boldFont;
    private final Font cjkFont;
    private final Font legacyFallbackFont;
    private final LinkedHashMap<Key, Entry> entries =
            new LinkedHashMap<Key, Entry>(128, 0.75f, true);
    private final LinkedHashMap<Key, MeasuredText> measurements =
            new LinkedHashMap<Key, MeasuredText>(128, 0.75f, true);
    private final LinkedHashMap<GlyphKey, AtlasGlyph> glyphs =
            new LinkedHashMap<GlyphKey, AtlasGlyph>(256, 0.75f, true);
    private final ArrayList<AtlasPage> atlasPages = new ArrayList<AtlasPage>();
    private boolean closed;
    private int uploadsStartedThisFrame;
    private int glyphUploadsStartedThisFrame;

    VulkanTextTextureCache(VulkanDriverLoader.LoadedDriver driver,
                           AsyncVulkanPresenter presenter,
                           LongConsumer textureDestroyer) {
        this(new DriverTextureAccess(driver), presenter, textureDestroyer);
    }

    VulkanTextTextureCache(TextureAccess textures, AsyncVulkanPresenter presenter,
                           LongConsumer textureDestroyer) {
        if (textures == null) throw new NullPointerException("textures");
        this.textures = textures;
        this.presenter = presenter;
        this.textureDestroyer = textureDestroyer;
        regularFont = loadGameFont("font/Roboto-Regular.ttf", Font.PLAIN);
        boldFont = loadGameFont("font/Roboto-Bold.ttf", Font.BOLD);
        cjkFont = loadGameFont("font/NotoSansCJKsc-Regular.otf", Font.PLAIN);
        legacyFallbackFont = loadGameFont("font/DroidSansFallback.ttf", Font.PLAIN);
    }

    synchronized Entry texture(String text, int requestedSize, boolean bold) {
        return presenter == null
                ? atlasTexture(text, requestedSize, bold)
                : legacyTexture(text, requestedSize, bold);
    }

    private Entry legacyTexture(String text, int requestedSize, boolean bold) {
        if (closed) throw new IllegalStateException("text texture cache is closed");
        if (text == null) throw new NullPointerException("text");
        int size = Math.max(4, Math.min(256, requestedSize));
        Key key = new Key(text, size, bold);
        Entry current = entries.get(key);
        if (current != null) return current.glyphs[0].textureHandle == 0L ? null : current;
        if (presenter != null && uploadsStartedThisFrame >= MAX_NEW_UPLOADS_PER_FRAME) {
            return null;
        }
        uploadsStartedThisFrame++;
        Raster raster = rasterize(text, size, bold);
        Entry created = Entry.legacy(raster.width, raster.height, raster.lineHeight);
        entries.put(key, created);
        VulkanTextureData textureData = new VulkanTextureData(
                raster.width, raster.height, raster.rgba);
        if (presenter == null) {
            created.glyphs[0].textureHandle = textures.uploadTexture(textureData);
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
        return created.glyphs[0].textureHandle == 0L ? null : created;
    }

    private void evictOldEntries() {
        while (entries.size() > MAX_ENTRIES) {
            Map.Entry<Key, Entry> oldest = entries.entrySet().iterator().next();
            entries.remove(oldest.getKey());
            release(oldest.getValue());
        }
    }

    private synchronized void completeUpload(Key key, Entry entry, long textureHandle) {
        if (!closed && entries.get(key) == entry) entry.glyphs[0].textureHandle = textureHandle;
        else textureDestroyer.accept(textureHandle);
    }

    private synchronized void failUpload(Key key, Entry entry) {
        if (entries.get(key) == entry) entries.remove(key);
    }

    private void release(Entry entry) {
        if (entry != null && entry.ownsTexture && entry.glyphs.length != 0
                && entry.glyphs[0].textureHandle != 0L) {
            textureDestroyer.accept(entry.glyphs[0].textureHandle);
        }
    }

    synchronized void beginFrame() {
        uploadsStartedThisFrame = 0;
        glyphUploadsStartedThisFrame = 0;
    }

    @Override public synchronized void close() {
        if (closed) return;
        closed = true;
        for (Entry entry : entries.values()) {
            release(entry);
        }
        entries.clear();
        measurements.clear();
        glyphs.clear();
        for (AtlasPage page : atlasPages) textureDestroyer.accept(page.textureHandle);
        atlasPages.clear();
    }

    private Entry atlasTexture(String text, int requestedSize, boolean bold) {
        if (closed) throw new IllegalStateException("text texture cache is closed");
        if (text == null) throw new NullPointerException("text");
        int size = Math.max(4, Math.min(256, requestedSize));
        Key key = new Key(text, size, bold);
        Entry cached = entries.get(key);
        if (cached != null) return cached;
        MeasuredText measured = measureText(text, size, bold);
        ArrayList<Glyph> draws = new ArrayList<Glyph>();
        BufferedImage contextImage = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        Graphics2D contextGraphics = contextImage.createGraphics();
        configure(contextGraphics);
        contextGraphics.setFont(measured.font);
        FontRenderContext context = contextGraphics.getFontRenderContext();
        try {
            String[] lines = text.split("\\n", -1);
            for (int lineIndex = 0; lineIndex < lines.length; lineIndex++) {
                char[] characters = lines[lineIndex].toCharArray();
                int layoutFlags = Bidi.requiresBidi(characters, 0, characters.length)
                        ? Font.LAYOUT_RIGHT_TO_LEFT : Font.LAYOUT_LEFT_TO_RIGHT;
                GlyphVector run = measured.font.layoutGlyphVector(context, characters,
                        0, characters.length, layoutFlags);
                for (int glyphIndex = 0; glyphIndex < run.getNumGlyphs(); glyphIndex++) {
                    int glyphCode = run.getGlyphCode(glyphIndex);
                    AtlasGlyph atlas = atlasGlyph(measured.font, glyphCode, context);
                    if (atlas == null) return null;
                    if (atlas.width == 0 || atlas.height == 0) continue;
                    Point2D position = run.getGlyphPosition(glyphIndex);
                    draws.add(new Glyph(atlas.textureHandle,
                            Math.round((float) position.getX()) + atlas.bearingX,
                            lineIndex * measured.lineHeight + atlas.bearingY,
                            atlas.width, atlas.height,
                            atlas.u0, atlas.v0, atlas.u1, atlas.v1));
                }
            }
        } finally {
            contextGraphics.dispose();
        }
        Entry created = new Entry(draws.toArray(new Glyph[0]), measured.width,
                measured.height, measured.lineHeight, false);
        entries.put(key, created);
        evictOldEntries();
        return created;
    }

    private AtlasGlyph atlasGlyph(Font font, int glyphCode, FontRenderContext context) {
        GlyphKey key = new GlyphKey(font, glyphCode);
        AtlasGlyph cached = glyphs.get(key);
        if (cached != null) return cached;
        if (glyphUploadsStartedThisFrame >= MAX_NEW_GLYPHS_PER_FRAME) return null;
        RasterGlyph raster = rasterizeGlyph(font, glyphCode, context);
        if (raster.width == 0 || raster.height == 0) {
            AtlasGlyph empty = AtlasGlyph.empty(raster.bearingX, raster.bearingY);
            glyphs.put(key, empty);
            return empty;
        }
        int cellWidth = raster.width + ATLAS_PADDING * 2;
        int cellHeight = raster.height + ATLAS_PADDING * 2;
        AtlasPage page = null;
        AtlasLocation location = null;
        for (AtlasPage candidate : atlasPages) {
            location = candidate.allocate(cellWidth, cellHeight);
            if (location != null) {
                page = candidate;
                break;
            }
        }
        if (page == null) {
            if (atlasPages.size() >= MAX_ATLAS_PAGES
                    || cellWidth > ATLAS_SIZE || cellHeight > ATLAS_SIZE) return null;
            page = createAtlasPage();
            location = page.allocate(cellWidth, cellHeight);
            if (location == null) throw new IllegalStateException("empty glyph atlas rejected cell");
        }
        byte[] padded = new byte[Math.multiplyExact(Math.multiplyExact(cellWidth, cellHeight), 4)];
        for (int y = 0; y < raster.height; y++) {
            int source = y * raster.width * 4;
            int destination = ((y + ATLAS_PADDING) * cellWidth + ATLAS_PADDING) * 4;
            System.arraycopy(raster.rgba, source, padded, destination, raster.width * 4);
        }
        textures.updateTextureRegion(page.textureHandle, location.x, location.y,
                new VulkanTextureData(cellWidth, cellHeight, padded));
        glyphUploadsStartedThisFrame++;
        float inverse = 1.0f / ATLAS_SIZE;
        AtlasGlyph created = new AtlasGlyph(page.textureHandle,
                raster.bearingX, raster.bearingY, raster.width, raster.height,
                (location.x + ATLAS_PADDING) * inverse,
                (location.y + ATLAS_PADDING) * inverse,
                (location.x + ATLAS_PADDING + raster.width) * inverse,
                (location.y + ATLAS_PADDING + raster.height) * inverse);
        glyphs.put(key, created);
        return created;
    }

    private AtlasPage createAtlasPage() {
        byte[] transparent = new byte[ATLAS_SIZE * ATLAS_SIZE * 4];
        long textureHandle = textures.uploadTexture(
                new VulkanTextureData(ATLAS_SIZE, ATLAS_SIZE, transparent));
        AtlasPage page = new AtlasPage(textureHandle);
        atlasPages.add(page);
        System.out.println("[Vulkan Mod] Allocated glyph atlas page #" + atlasPages.size()
                + " (" + ATLAS_SIZE + "x" + ATLAS_SIZE + ")");
        return page;
    }

    private static RasterGlyph rasterizeGlyph(Font font, int glyphCode,
                                               FontRenderContext context) {
        GlyphVector vector = font.createGlyphVector(context, new int[] { glyphCode });
        Rectangle bounds = vector.getGlyphPixelBounds(0, context, 0.0f, 0.0f);
        if (bounds.width <= 0 || bounds.height <= 0) {
            return new RasterGlyph(bounds.x, bounds.y, 0, 0, new byte[0]);
        }
        BufferedImage image = new BufferedImage(bounds.width, bounds.height,
                BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        configure(graphics);
        graphics.setFont(font);
        graphics.setColor(Color.WHITE);
        graphics.drawGlyphVector(vector, -bounds.x, -bounds.y);
        graphics.dispose();
        byte[] rgba = new byte[bounds.width * bounds.height * 4];
        int offset = 0;
        for (int y = 0; y < bounds.height; y++) {
            for (int x = 0; x < bounds.width; x++) {
                int alpha = image.getRGB(x, y) >>> 24;
                rgba[offset++] = (byte) 255;
                rgba[offset++] = (byte) 255;
                rgba[offset++] = (byte) 255;
                rgba[offset++] = (byte) alpha;
            }
        }
        return new RasterGlyph(bounds.x, bounds.y, bounds.width, bounds.height, rgba);
    }

    private Raster rasterize(String text, int size, boolean bold) {
        MeasuredText measured = measureText(text, size, bold);
        Font font = measured.font;
        int width = measured.width;
        int height = measured.height;
        int lineHeight = measured.lineHeight;
        int baseline = measured.ascent;
        String[] lines = text.split("\\n", -1);
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        configure(graphics);
        graphics.setFont(font);
        graphics.setColor(Color.WHITE);
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

    synchronized VulkanTextMetrics measure(String text, int requestedSize, boolean bold) {
        if (closed) throw new IllegalStateException("text texture cache is closed");
        if (text == null) text = "";
        int size = Math.max(4, Math.min(256, requestedSize));
        MeasuredText measured = measureText(text, size, bold);
        return new VulkanTextMetrics(measured.width, measured.height, measured.lineHeight);
    }

    private MeasuredText measureText(String text, int size, boolean bold) {
        Key key = new Key(text, size, bold);
        MeasuredText cached = measurements.get(key);
        if (cached != null) return cached;
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
        int ascent = metrics.getAscent();
        measureGraphics.dispose();

        width = Math.min(4096, width);
        height = Math.min(4096, height);
        MeasuredText created = new MeasuredText(font, width, height, lineHeight, ascent);
        measurements.put(key, created);
        while (measurements.size() > MAX_ENTRIES * 2) {
            measurements.remove(measurements.entrySet().iterator().next().getKey());
        }
        return created;
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
        final Glyph[] glyphs;
        final int width;
        final int height;
        final int lineHeight;
        final boolean ownsTexture;

        private Entry(Glyph[] glyphs, int width, int height,
                      int lineHeight, boolean ownsTexture) {
            this.glyphs = glyphs;
            this.width = width;
            this.height = height;
            this.lineHeight = lineHeight;
            this.ownsTexture = ownsTexture;
        }

        private static Entry legacy(int width, int height, int lineHeight) {
            return new Entry(new Glyph[] { new Glyph(0L, 0, -lineHeight,
                    width, height, 0.0f, 0.0f, 1.0f, 1.0f) },
                    width, height, lineHeight, true);
        }
    }

    interface TextureAccess {
        long uploadTexture(VulkanTextureData texture);
        void updateTextureRegion(long textureHandle, int x, int y, VulkanTextureData texture);
    }

    private static final class DriverTextureAccess implements TextureAccess {
        private final VulkanDriverLoader.LoadedDriver driver;

        private DriverTextureAccess(VulkanDriverLoader.LoadedDriver driver) {
            if (driver == null) throw new NullPointerException("driver");
            this.driver = driver;
        }

        @Override public long uploadTexture(VulkanTextureData texture) {
            return driver.uploadTexture(texture);
        }

        @Override public void updateTextureRegion(long textureHandle, int x, int y,
                                                  VulkanTextureData texture) {
            driver.updateTextureRegion(textureHandle, x, y, texture);
        }
    }

    static final class Glyph {
        volatile long textureHandle;
        final float x;
        final float y;
        final float width;
        final float height;
        final float u0;
        final float v0;
        final float u1;
        final float v1;

        private Glyph(long textureHandle, float x, float y, float width, float height,
                      float u0, float v0, float u1, float v1) {
            this.textureHandle = textureHandle;
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.u0 = u0;
            this.v0 = v0;
            this.u1 = u1;
            this.v1 = v1;
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

    private static final class RasterGlyph {
        private final int bearingX;
        private final int bearingY;
        private final int width;
        private final int height;
        private final byte[] rgba;

        private RasterGlyph(int bearingX, int bearingY, int width, int height, byte[] rgba) {
            this.bearingX = bearingX;
            this.bearingY = bearingY;
            this.width = width;
            this.height = height;
            this.rgba = rgba;
        }
    }

    private static final class GlyphKey {
        private final Font font;
        private final int glyphCode;

        private GlyphKey(Font font, int glyphCode) {
            this.font = font;
            this.glyphCode = glyphCode;
        }

        @Override public boolean equals(Object candidate) {
            if (this == candidate) return true;
            if (!(candidate instanceof GlyphKey)) return false;
            GlyphKey other = (GlyphKey) candidate;
            return glyphCode == other.glyphCode && font.equals(other.font);
        }

        @Override public int hashCode() {
            return 31 * font.hashCode() + glyphCode;
        }
    }

    private static final class AtlasGlyph {
        private final long textureHandle;
        private final int bearingX;
        private final int bearingY;
        private final int width;
        private final int height;
        private final float u0;
        private final float v0;
        private final float u1;
        private final float v1;

        private AtlasGlyph(long textureHandle, int bearingX, int bearingY,
                           int width, int height, float u0, float v0, float u1, float v1) {
            this.textureHandle = textureHandle;
            this.bearingX = bearingX;
            this.bearingY = bearingY;
            this.width = width;
            this.height = height;
            this.u0 = u0;
            this.v0 = v0;
            this.u1 = u1;
            this.v1 = v1;
        }

        private static AtlasGlyph empty(int bearingX, int bearingY) {
            return new AtlasGlyph(0L, bearingX, bearingY,
                    0, 0, 0.0f, 0.0f, 0.0f, 0.0f);
        }
    }

    private static final class AtlasPage {
        private final long textureHandle;
        private int x;
        private int y;
        private int rowHeight;

        private AtlasPage(long textureHandle) {
            this.textureHandle = textureHandle;
        }

        private AtlasLocation allocate(int width, int height) {
            if (width > ATLAS_SIZE || height > ATLAS_SIZE) return null;
            if (x + width > ATLAS_SIZE) {
                x = 0;
                y += rowHeight;
                rowHeight = 0;
            }
            if (y + height > ATLAS_SIZE) return null;
            AtlasLocation result = new AtlasLocation(x, y);
            x += width;
            rowHeight = Math.max(rowHeight, height);
            return result;
        }
    }

    private static final class AtlasLocation {
        private final int x;
        private final int y;

        private AtlasLocation(int x, int y) {
            this.x = x;
            this.y = y;
        }
    }

    private static final class MeasuredText {
        private final Font font;
        private final int width;
        private final int height;
        private final int lineHeight;
        private final int ascent;

        private MeasuredText(Font font, int width, int height,
                             int lineHeight, int ascent) {
            this.font = font;
            this.width = width;
            this.height = height;
            this.lineHeight = lineHeight;
            this.ascent = ascent;
        }
    }
}
