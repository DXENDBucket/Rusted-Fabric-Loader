package io.github.endx.vulkanmod;

import io.github.endx.vulkanmod.spi.VulkanTextureData;
import io.github.endx.vulkanmod.spi.VulkanTextMetrics;
import io.github.endx.vulkanmod.spi.VulkanGlyphBitmap;
import io.github.endx.vulkanmod.spi.VulkanGlyphPlacement;
import io.github.endx.vulkanmod.spi.VulkanTextLayout;
import io.github.endx.vulkanmod.spi.VulkanTexturedQuadGeometry;

import java.util.LinkedHashMap;
import java.util.ArrayList;
import java.util.Map;
import java.util.function.LongConsumer;

/** Bounded glyph atlas and shaped-run cache used by the native renderer. */
final class VulkanTextTextureCache implements AutoCloseable {
    private static final int MAX_ENTRIES = 768;
    private static final int ATLAS_SIZE = 1024;
    private static final int ATLAS_PADDING = 1;
    private static final int MAX_ATLAS_PAGES = 16;
    private static final int MAX_NEW_GLYPHS_PER_FRAME = 96;

    private final TextureAccess textures;
    private final TextAccess text;
    private final LongConsumer textureDestroyer;
    private final LinkedHashMap<Key, Entry> entries =
            new LinkedHashMap<Key, Entry>(128, 0.75f, true);
    private final LinkedHashMap<Key, VulkanTextLayout> measurements =
            new LinkedHashMap<Key, VulkanTextLayout>(128, 0.75f, true);
    private final LinkedHashMap<Long, AtlasGlyph> glyphs =
            new LinkedHashMap<Long, AtlasGlyph>(256, 0.75f, true);
    private final ArrayList<AtlasPage> atlasPages = new ArrayList<AtlasPage>();
    private boolean closed;
    private int glyphUploadsStartedThisFrame;

    VulkanTextTextureCache(VulkanDriverLoader.LoadedDriver driver,
                           LongConsumer textureDestroyer) {
        this(new DriverTextureAccess(driver), new DriverTextAccess(driver),
                textureDestroyer);
    }

    VulkanTextTextureCache(TextureAccess textures, TextAccess text,
                           LongConsumer textureDestroyer) {
        if (textures == null) throw new NullPointerException("textures");
        if (text == null) throw new NullPointerException("text");
        this.textures = textures;
        this.text = text;
        this.textureDestroyer = textureDestroyer;
    }

    synchronized Entry texture(String text, int requestedSize, boolean bold) {
        return atlasTexture(text, requestedSize, bold);
    }

    private void evictOldEntries() {
        while (entries.size() > MAX_ENTRIES) {
            Map.Entry<Key, Entry> oldest = entries.entrySet().iterator().next();
            entries.remove(oldest.getKey());
        }
    }

    synchronized void beginFrame() {
        glyphUploadsStartedThisFrame = 0;
    }

    @Override public synchronized void close() {
        if (closed) return;
        closed = true;
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
        VulkanTextLayout measured = measureText(text, size, bold);
        ArrayList<Glyph> draws = new ArrayList<Glyph>();
        for (int glyphIndex = 0; glyphIndex < measured.glyphCount(); glyphIndex++) {
            VulkanGlyphPlacement placement = measured.glyph(glyphIndex);
            AtlasGlyph atlas = atlasGlyph(placement.glyphKey());
            if (atlas == null) return null;
            if (atlas.width == 0 || atlas.height == 0) continue;
            draws.add(new Glyph(atlas.textureHandle,
                    placement.x() + atlas.bearingX,
                    placement.y() + atlas.bearingY,
                    atlas.width, atlas.height,
                    atlas.u0, atlas.v0, atlas.u1, atlas.v1));
        }
        Entry created = new Entry(draws.toArray(new Glyph[0]), measured.width(),
                measured.height(), measured.lineHeight());
        entries.put(key, created);
        evictOldEntries();
        return created;
    }

    private AtlasGlyph atlasGlyph(long glyphKey) {
        Long key = Long.valueOf(glyphKey);
        AtlasGlyph cached = glyphs.get(key);
        if (cached != null) return cached;
        if (glyphUploadsStartedThisFrame >= MAX_NEW_GLYPHS_PER_FRAME) return null;
        VulkanGlyphBitmap raster = text.rasterizeGlyph(glyphKey);
        if (raster.empty()) {
            AtlasGlyph empty = AtlasGlyph.empty(raster.bearingX(), raster.bearingY());
            glyphs.put(key, empty);
            return empty;
        }
        int cellWidth = raster.width() + ATLAS_PADDING * 2;
        int cellHeight = raster.height() + ATLAS_PADDING * 2;
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
        byte[] pixels = raster.copyRgba();
        for (int y = 0; y < raster.height(); y++) {
            int source = y * raster.width() * 4;
            int destination = ((y + ATLAS_PADDING) * cellWidth + ATLAS_PADDING) * 4;
            System.arraycopy(pixels, source, padded, destination, raster.width() * 4);
        }
        textures.updateTextureRegion(page.textureHandle, location.x, location.y,
                new VulkanTextureData(cellWidth, cellHeight, padded));
        glyphUploadsStartedThisFrame++;
        float inverse = 1.0f / ATLAS_SIZE;
        AtlasGlyph created = new AtlasGlyph(page.textureHandle,
                raster.bearingX(), raster.bearingY(), raster.width(), raster.height(),
                (location.x + ATLAS_PADDING) * inverse,
                (location.y + ATLAS_PADDING) * inverse,
                (location.x + ATLAS_PADDING + raster.width()) * inverse,
                (location.y + ATLAS_PADDING + raster.height()) * inverse);
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

    synchronized VulkanTextMetrics measure(String text, int requestedSize, boolean bold) {
        if (closed) throw new IllegalStateException("text texture cache is closed");
        if (text == null) text = "";
        int size = Math.max(4, Math.min(256, requestedSize));
        return measureText(text, size, bold).metrics();
    }

    private VulkanTextLayout measureText(String text, int size, boolean bold) {
        Key key = new Key(text, size, bold);
        VulkanTextLayout cached = measurements.get(key);
        if (cached != null) return cached;
        VulkanTextLayout created = this.text.layout(text, size, bold);
        measurements.put(key, created);
        while (measurements.size() > MAX_ENTRIES * 2) {
            measurements.remove(measurements.entrySet().iterator().next().getKey());
        }
        return created;
    }

    static final class Entry {
        final Glyph[] glyphs;
        final GlyphBatch[] batches;
        final int width;
        final int height;
        final int lineHeight;

        private Entry(Glyph[] glyphs, int width, int height, int lineHeight) {
            this.glyphs = glyphs;
            this.batches = batches(glyphs);
            this.width = width;
            this.height = height;
            this.lineHeight = lineHeight;
        }

        private static GlyphBatch[] batches(Glyph[] glyphs) {
            if (glyphs.length == 0) return new GlyphBatch[0];
            ArrayList<GlyphBatch> result = new ArrayList<GlyphBatch>();
            int first = 0;
            while (first < glyphs.length) {
                long texture = glyphs[first].textureHandle;
                int end = first + 1;
                while (end < glyphs.length
                        && end - first < VulkanTexturedQuadGeometry.MAX_QUADS
                        && glyphs[end].textureHandle == texture) end++;
                float[] quads = new float[(end - first) * GlyphBatch.FLOATS_PER_QUAD];
                int offset = 0;
                for (int index = first; index < end; index++) {
                    Glyph glyph = glyphs[index];
                    quads[offset++] = glyph.x;
                    quads[offset++] = glyph.y;
                    quads[offset++] = glyph.width;
                    quads[offset++] = glyph.height;
                    quads[offset++] = glyph.u0;
                    quads[offset++] = glyph.v0;
                    quads[offset++] = glyph.u1;
                    quads[offset++] = glyph.v1;
                }
                result.add(new GlyphBatch(texture, new VulkanTexturedQuadGeometry(quads)));
                first = end;
            }
            return result.toArray(new GlyphBatch[0]);
        }

    }

    interface TextureAccess {
        long uploadTexture(VulkanTextureData texture);
        void updateTextureRegion(long textureHandle, int x, int y, VulkanTextureData texture);
    }

    interface TextAccess {
        VulkanTextLayout layout(String text, int pixelSize, boolean bold);
        VulkanGlyphBitmap rasterizeGlyph(long glyphKey);
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

    private static final class DriverTextAccess implements TextAccess {
        private final VulkanDriverLoader.LoadedDriver driver;

        private DriverTextAccess(VulkanDriverLoader.LoadedDriver driver) {
            if (driver == null) throw new NullPointerException("driver");
            this.driver = driver;
        }

        @Override public VulkanTextLayout layout(String text, int pixelSize, boolean bold) {
            return driver.layoutText(text, pixelSize, bold);
        }

        @Override public VulkanGlyphBitmap rasterizeGlyph(long glyphKey) {
            return driver.rasterizeGlyph(glyphKey);
        }

    }

    static final class Glyph {
        final long textureHandle;
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

    static final class GlyphBatch {
        static final int FLOATS_PER_QUAD = 8;
        final long textureHandle;
        final VulkanTexturedQuadGeometry geometry;

        private GlyphBatch(long textureHandle, VulkanTexturedQuadGeometry geometry) {
            this.textureHandle = textureHandle;
            this.geometry = geometry;
        }

        int quadCount() { return geometry.quadCount(); }
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

}
