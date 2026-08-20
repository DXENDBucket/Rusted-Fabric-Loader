package io.github.endx.vulkanmod;

import io.github.endx.vulkanmod.spi.VulkanTextureData;

/** Verifies that regenerated LibRocket font atlases replace their live GPU contents. */
public final class SlickImageVulkanTextureCacheVerification {
    private SlickImageVulkanTextureCacheVerification() { }

    public static void main(String[] arguments) {
        RecordingTextures textures = new RecordingTextures();
        Object holder = new Object();
        try (SlickImageVulkanTextureCache cache =
                     new SlickImageVulkanTextureCache(textures, textures::destroy)) {
            cache.registerPixels(holder, 2, 2, solid(2, 2, 10));
            SlickImageVulkanTextureCache.Entry first = cache.textureNative(holder);
            require(first != null && first.textureHandle == 1L && textures.uploads == 1,
                    "initial LibRocket texture was not uploaded");

            cache.registerPixels(holder, 2, 2, solid(2, 2, 90));
            SlickImageVulkanTextureCache.Entry updated = cache.textureNative(holder);
            require(updated == first && updated.textureHandle == 1L
                            && textures.updates == 1 && textures.lastByte == 90,
                    "same-sized regenerated font atlas did not update its live texture");

            cache.registerPixels(holder, 4, 2, solid(4, 2, 120));
            SlickImageVulkanTextureCache.Entry resized = cache.textureNative(holder);
            require(resized != null && resized.textureHandle == 2L && textures.uploads == 2
                            && textures.destroyed == 1,
                    "resized regenerated texture did not replace its GPU allocation");

            cache.invalidate(holder);
            require(cache.textureNative(holder) == null && textures.destroyed == 2,
                    "released LibRocket holder retained stale CPU or GPU texture state");
        }
        require(textures.destroyed == 2, "released UI texture was destroyed more than once");
        System.out.println("LibRocket regenerated texture cache contracts passed");
    }

    private static byte[] solid(int width, int height, int value) {
        byte[] rgba = new byte[width * height * 4];
        java.util.Arrays.fill(rgba, (byte) value);
        return rgba;
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static final class RecordingTextures
            implements SlickImageVulkanTextureCache.TextureAccess {
        private long nextHandle = 1L;
        private int uploads;
        private int updates;
        private int destroyed;
        private int lastByte;

        @Override public long uploadTexture(VulkanTextureData texture) {
            uploads++;
            lastByte = texture.copyRgba()[0] & 255;
            return nextHandle++;
        }

        @Override public void updateTexture(long textureHandle, VulkanTextureData texture) {
            require(textureHandle > 0L, "invalid texture update handle");
            updates++;
            lastByte = texture.copyRgba()[0] & 255;
        }

        private void destroy(long textureHandle) {
            require(textureHandle > 0L, "invalid texture destroy handle");
            destroyed++;
        }
    }
}
