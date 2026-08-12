package io.github.endx.vulkanmod;

import io.github.endx.vulkanmod.spi.VulkanTextureData;
import io.github.endx.vulkanmod.mixin.SlickBitmapOrTextureDataAccessor;
import io.github.endx.vulkanmod.render.VulkanGameImage;
import rustedwarfare.client.render.GameImage;

import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.function.LongConsumer;

/** Converts game-owned ARGB images to cached driver-owned RGBA8 textures. */
final class GameImageVulkanTextureCache implements AutoCloseable {
    private static final int FAST_LOOKUP_SIZE = 256;
    private static Method slickTextureUnbind;
    private static boolean slickTextureUnbindUnavailable;

    private final VulkanDriverLoader.LoadedDriver driver;
    private final LongConsumer textureDestroyer;
    private final Map<GameImage, Entry> entries = new IdentityHashMap<GameImage, Entry>();
    private final Map<GameImage, Boolean> renderTargets =
            new IdentityHashMap<GameImage, Boolean>();
    private final GameImage[] fastImages = new GameImage[FAST_LOOKUP_SIZE];
    private final Entry[] fastEntries = new Entry[FAST_LOOKUP_SIZE];
    private boolean closed;

    GameImageVulkanTextureCache(VulkanDriverLoader.LoadedDriver driver,
                                LongConsumer textureDestroyer) {
        if (driver == null) throw new NullPointerException("driver");
        this.driver = driver;
        this.textureDestroyer = textureDestroyer;
    }

    synchronized long texture(GameImage source) {
        if (closed) throw new IllegalStateException("texture cache is closed");
        if (source == null) throw new NullPointerException("source");
        GameImage image = source.getRealImage();
        if (image == null) image = source;
        if (image instanceof VulkanGameImage) {
            long renderTarget = ((VulkanGameImage) image).nativeRenderTargetHandle();
            if (renderTarget != 0L) return renderTarget;
        }
        int fastSlot = System.identityHashCode(image) & (FAST_LOOKUP_SIZE - 1);
        Entry current = fastImages[fastSlot] == image
                ? fastEntries[fastSlot] : entries.get(image);
        if (current != null && current.version == image.version) {
            fastImages[fastSlot] = image;
            fastEntries[fastSlot] = current;
            return current.textureHandle;
        }
        int width = image.getWidth();
        int height = image.getHeight();
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("game image has invalid dimensions: "
                    + width + "x" + height);
        }
        VulkanTextureData pixels = readRgba(image, width, height);
        if (Boolean.getBoolean("rusted.fabric.vulkan.debugRenderTargets")
                && renderTargets.containsKey(image)) {
            logRenderTarget(image, pixels);
        }
        if (current != null && current.textureHandle != 0L
                && current.width == width && current.height == height) {
            driver.updateTexture(current.textureHandle, pixels);
            current.version = image.version;
            return current.textureHandle;
        }
        Entry replacement = new Entry(0L, image.version, width, height);
        entries.put(image, replacement);
        fastImages[fastSlot] = image;
        fastEntries[fastSlot] = replacement;
        release(current);
        replacement.textureHandle = driver.uploadTexture(pixels);
        return replacement.textureHandle;
    }

    synchronized void invalidate(Object candidate) {
        if (closed || !(candidate instanceof GameImage)) return;
        GameImage image = ((GameImage) candidate).getRealImage();
        if (image == null) image = (GameImage) candidate;
        Entry removed = entries.remove(image);
        int fastSlot = System.identityHashCode(image) & (FAST_LOOKUP_SIZE - 1);
        if (fastImages[fastSlot] == image) {
            fastImages[fastSlot] = null;
            fastEntries[fastSlot] = null;
        }
        release(removed);
    }

    synchronized void markRenderTarget(Object candidate) {
        if (closed || !(candidate instanceof GameImage)) return;
        GameImage image = ((GameImage) candidate).getRealImage();
        if (image == null) image = (GameImage) candidate;
        renderTargets.put(image, Boolean.TRUE);
        invalidate(image);
    }

    synchronized boolean isRenderTarget(GameImage candidate) {
        if (closed || candidate == null) return false;
        GameImage image = candidate.getRealImage();
        if (image == null) image = candidate;
        return renderTargets.containsKey(image);
    }

    synchronized int size() {
        return entries.size();
    }

    private void release(Entry entry) {
        if (entry != null && entry.textureHandle != 0L) {
            textureDestroyer.accept(entry.textureHandle);
        }
    }

    @Override public synchronized void close() {
        if (closed) return;
        closed = true;
        for (Entry entry : entries.values()) {
            release(entry);
        }
        entries.clear();
        renderTargets.clear();
        java.util.Arrays.fill(fastImages, null);
        java.util.Arrays.fill(fastEntries, null);
    }

    private static VulkanTextureData readRgba(GameImage image, int width, int height) {
        if (image instanceof VulkanGameImage) {
            return VulkanTextureData.fromArgb(width, height, image.pixelBuffer);
        }
        int byteCount = Math.multiplyExact(Math.multiplyExact(width, height), 4);
        byte[] rgba = new byte[byteCount];
        invalidateSlickTextureBindingCache();
        image.ensureImageDataAvailable();
        repairMissingBytesPerPixel(image, width, height);
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

    private static void repairMissingBytesPerPixel(GameImage image, int width, int height) {
        if (!(image instanceof SlickBitmapOrTextureDataAccessor)) return;
        SlickBitmapOrTextureDataAccessor accessor =
                (SlickBitmapOrTextureDataAccessor) (Object) image;
        if (accessor.vulkanmod$getBytesPerPixel() > 0) return;
        ByteBuffer raw = accessor.vulkanmod$getImageByteBuffer();
        if (raw == null) return;
        int inferred = TextureReadbackLayout.inferBytesPerPixel(raw.limit(), width, height);
        if (inferred == 3 || inferred == 4) {
            // SlickTextureReadbackImageData replaces the buffer for dynamic FBO images but the
            // game wrapper does not refresh its separately cached bytesPerPixel field. A zero
            // stride makes every getPixel(x,y) read pixel 0, producing one giant colour block.
            accessor.vulkanmod$setBytesPerPixel(inferred);
        }
    }

    private static void invalidateSlickTextureBindingCache() {
        if (slickTextureUnbindUnavailable) return;
        try {
            if (slickTextureUnbind == null) {
                Class<?> texture = Class.forName("org.newdawn.slick.opengl.TextureImpl");
                slickTextureUnbind = texture.getMethod("unbind");
            }
            // FBO context switches can change GL_TEXTURE_BINDING_2D without updating Slick's
            // process-wide lastBind shortcut. getTextureData() calls Texture.bind(), so reset
            // that shortcut first and make the following glGetTexImage bind the intended image.
            slickTextureUnbind.invoke(null);
        } catch (ReflectiveOperationException | LinkageError failure) {
            slickTextureUnbindUnavailable = true;
            System.out.println("[Vulkan Mod] Could not reset Slick texture binding cache: "
                    + failure.getClass().getSimpleName() + ": " + failure.getMessage());
        }
    }

    private static void logRenderTarget(GameImage image, VulkanTextureData pixels) {
        byte[] rgba = pixels.copyRgba();
        int[] colors = new int[16];
        int colorCount = 0;
        for (int y = 0; y < pixels.height() && colorCount < colors.length; y++) {
            for (int x = 0; x < pixels.width() && colorCount < colors.length; x++) {
                int offset = (y * pixels.width() + x) * 4;
                int packed = (rgba[offset] & 255) << 24
                        | (rgba[offset + 1] & 255) << 16
                        | (rgba[offset + 2] & 255) << 8
                        | (rgba[offset + 3] & 255);
                boolean known = false;
                for (int index = 0; index < colorCount; index++) {
                    if (colors[index] == packed) {
                        known = true;
                        break;
                    }
                }
                if (!known && colorCount < colors.length) colors[colorCount++] = packed;
            }
        }
        StringBuilder samples = new StringBuilder();
        for (int index = 0; index < Math.min(colorCount, 6); index++) {
            if (index > 0) samples.append(',');
            samples.append(String.format("%08x", colors[index]));
        }
        String rawSummary = "unavailable";
        if (image instanceof SlickBitmapOrTextureDataAccessor) {
            SlickBitmapOrTextureDataAccessor accessor =
                    (SlickBitmapOrTextureDataAccessor) (Object) image;
            ByteBuffer raw = accessor.vulkanmod$getImageByteBuffer();
            int bytesPerPixel = accessor.vulkanmod$getBytesPerPixel();
            int rawColors = countRawColors(raw, bytesPerPixel);
            rawSummary = "bpp=" + bytesPerPixel + ",colors=" + rawColors;
        }
        System.out.println("[Vulkan Mod] Render-target upload " + image.getName()
                + " " + pixels.width() + "x" + pixels.height()
                + " sampledColors=" + colorCount + " [" + samples + "] raw="
                + rawSummary);
    }

    private static int countRawColors(ByteBuffer raw, int bytesPerPixel) {
        if (raw == null || bytesPerPixel < 3) return 0;
        int[] colors = new int[16];
        int count = 0;
        for (int offset = 0; offset + bytesPerPixel <= raw.limit() && count < colors.length;
             offset += bytesPerPixel) {
            int packed = (raw.get(offset) & 255) << 24
                    | (raw.get(offset + 1) & 255) << 16
                    | (raw.get(offset + 2) & 255) << 8
                    | (bytesPerPixel >= 4 ? raw.get(offset + 3) & 255 : 255);
            boolean known = false;
            for (int index = 0; index < count; index++) {
                if (colors[index] == packed) {
                    known = true;
                    break;
                }
            }
            if (!known) colors[count++] = packed;
        }
        return count;
    }

    private static final class Entry {
        private volatile long textureHandle;
        private int version;
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
