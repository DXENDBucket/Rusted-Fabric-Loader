package io.github.endx.vulkanmod;

import io.github.endx.vulkanmod.spi.VulkanTextureData;
import io.github.endx.vulkanmod.spi.VulkanGlyphBitmap;
import io.github.endx.vulkanmod.spi.VulkanGlyphPlacement;
import io.github.endx.vulkanmod.spi.VulkanTextLayout;

import java.util.HashSet;
import java.util.Set;

/** Verifies that native text reuses glyph regions instead of uploading whole strings. */
public final class VulkanGlyphAtlasVerification {
    private VulkanGlyphAtlasVerification() { }

    public static void main(String[] arguments) throws Exception {
        RecordingDriver backend = new RecordingDriver();
        VulkanTextTextureCache cache = new VulkanTextTextureCache(
                backend, new FixedTextRasterizer(), null, backend::destroyTexture);
        try {
            VulkanTextTextureCache.Entry repeated = cache.texture("AAAA", 24, false);
            require(repeated != null && repeated.glyphs.length == 4,
                    "repeated glyph run was not generated");
            require(backend.uploads == 1 && backend.regionUpdates == 1,
                    "one repeated glyph did not use one atlas page and one region upload");
            require(sameRegion(repeated.glyphs[0], repeated.glyphs[3]),
                    "repeated glyph did not reuse its atlas region");

            VulkanTextTextureCache.Entry mixed = cache.texture("ABBA", 24, false);
            require(mixed != null && mixed.glyphs.length == 4,
                    "mixed glyph run was not generated");
            require(backend.uploads == 1 && backend.regionUpdates == 2,
                    "a second string uploaded more than its one new glyph");
            require(cache.texture("ABBA", 24, false) == mixed
                            && backend.regionUpdates == 2,
                    "cached text layout repeated glyph uploads");

            VulkanTextTextureCache.Entry multiline = cache.texture("A\nA", 24, false);
            require(multiline != null && multiline.glyphs.length == 2
                            && multiline.glyphs[1].y > multiline.glyphs[0].y,
                    "multiline glyph baselines were not retained");
            VulkanTextTextureCache.Entry blankLines = cache.texture("\n", 24, false);
            require(blankLines != null && blankLines.glyphs.length == 0,
                    "blank multiline text did not retain an empty layout");
            require(cache.measure("ABBA", 24, false).width() == mixed.width,
                    "atlas layout and text measurement disagree");

            Set<Long> textures = new HashSet<Long>();
            for (VulkanTextTextureCache.Glyph glyph : mixed.glyphs) {
                require(glyph.textureHandle != 0L && glyph.u0 >= 0.0f && glyph.v0 >= 0.0f
                                && glyph.u1 <= 1.0f && glyph.v1 <= 1.0f
                                && glyph.u1 > glyph.u0 && glyph.v1 > glyph.v0,
                        "glyph atlas coordinates are invalid");
                textures.add(glyph.textureHandle);
            }
            require(textures.size() == 1, "small glyph run crossed atlas pages unexpectedly");
            require(mixed.batches.length == 1 && mixed.batches[0].quadCount() == 4,
                    "one-page text was not compacted into one quad batch");
            StringBuilder longRunText = new StringBuilder(2_000);
            for (int index = 0; index < 2_000; index++) longRunText.append('A');
            VulkanTextTextureCache.Entry longRun = cache.texture(
                    longRunText.toString(), 24, false);
            require(longRun != null && longRun.glyphs.length == 2_000
                            && longRun.batches.length == 1
                            && longRun.batches[0].quadCount() == 2_000
                            && backend.regionUpdates == 2,
                    "large repeated text did not collapse to one cached batch");
        } finally {
            cache.close();
        }
        require(backend.destroyed == 1, "glyph atlas page was not released exactly once");
        System.out.println("Native Vulkan glyph atlas contracts passed");
    }

    private static boolean sameRegion(VulkanTextTextureCache.Glyph first,
                                      VulkanTextTextureCache.Glyph second) {
        return first.textureHandle == second.textureHandle
                && first.u0 == second.u0 && first.v0 == second.v0
                && first.u1 == second.u1 && first.v1 == second.v1;
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static final class RecordingDriver
            implements VulkanTextTextureCache.TextureAccess {
        private long nextTexture = 1L;
        private int uploads;
        private int regionUpdates;
        private int destroyed;

        @Override public long uploadTexture(VulkanTextureData texture) {
            require(texture.width() == 1024 && texture.height() == 1024,
                    "atlas page has unexpected dimensions");
            uploads++;
            return nextTexture++;
        }
        @Override public void updateTextureRegion(long textureHandle, int x, int y,
                                                  VulkanTextureData texture) {
            require(textureHandle > 0L && x >= 0 && y >= 0
                            && x + texture.width() <= 1024
                            && y + texture.height() <= 1024,
                    "atlas region update is out of bounds");
            regionUpdates++;
        }
        private void destroyTexture(long textureHandle) { destroyed++; }
    }

    private static final class FixedTextRasterizer
            implements VulkanTextTextureCache.TextAccess {
        @Override public VulkanTextLayout layout(String text, int pixelSize, boolean bold) {
            java.util.ArrayList<VulkanGlyphPlacement> glyphs =
                    new java.util.ArrayList<VulkanGlyphPlacement>();
            int x = 0;
            int y = 0;
            int width = 1;
            for (int index = 0; index < text.length(); index++) {
                char character = text.charAt(index);
                if (character == '\n') {
                    width = Math.max(width, x);
                    x = 0;
                    y += 20;
                    continue;
                }
                glyphs.add(new VulkanGlyphPlacement(character, x, y));
                x += 10;
            }
            width = Math.max(width, x);
            return new VulkanTextLayout(width, y + 20, 20,
                    glyphs.toArray(new VulkanGlyphPlacement[0]));
        }

        @Override public VulkanGlyphBitmap rasterizeGlyph(long glyphKey) {
            byte[] rgba = new byte[4 * 6 * 4];
            java.util.Arrays.fill(rgba, (byte) 255);
            return new VulkanGlyphBitmap(0, -6, 4, 6, rgba);
        }

        @Override public VulkanTextureData rasterizeText(
                String text, int pixelSize, boolean bold) {
            VulkanTextLayout layout = layout(text, pixelSize, bold);
            return new VulkanTextureData(Math.max(1, layout.width()),
                    Math.max(1, layout.height()),
                    new byte[Math.max(1, layout.width())
                            * Math.max(1, layout.height()) * 4]);
        }
    }
}
