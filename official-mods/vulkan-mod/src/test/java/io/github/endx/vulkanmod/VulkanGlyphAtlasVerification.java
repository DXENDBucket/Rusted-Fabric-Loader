package io.github.endx.vulkanmod;

import io.github.endx.vulkanmod.spi.VulkanTextureData;

import java.util.HashSet;
import java.util.Set;

/** Verifies that native text reuses glyph regions instead of uploading whole strings. */
public final class VulkanGlyphAtlasVerification {
    private VulkanGlyphAtlasVerification() { }

    public static void main(String[] arguments) throws Exception {
        RecordingDriver backend = new RecordingDriver();
        VulkanTextTextureCache cache = new VulkanTextTextureCache(
                backend, null, backend::destroyTexture);
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
}
