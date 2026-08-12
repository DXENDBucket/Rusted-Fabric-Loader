package io.github.endx.vulkanmod;

import io.github.endx.vulkanmod.spi.VulkanTextureData;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import javax.imageio.ImageIO;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.function.LongConsumer;

/** Caches LibRocket/Slick textures that are not represented by GameImage. */
final class SlickImageVulkanTextureCache implements AutoCloseable {
    private final VulkanDriverLoader.LoadedDriver driver;
    private final LongConsumer textureDestroyer;
    private final Map<Object, Entry> entries = new IdentityHashMap<Object, Entry>();
    private final Map<Object, CpuPixels> cpuSources = new IdentityHashMap<Object, CpuPixels>();
    private final Map<Object, Boolean> unavailableSources =
            new IdentityHashMap<Object, Boolean>();
    private boolean closed;

    SlickImageVulkanTextureCache(VulkanDriverLoader.LoadedDriver driver,
                                LongConsumer textureDestroyer) {
        this.driver = driver;
        this.textureDestroyer = textureDestroyer;
    }

    synchronized Entry textureNative(Object holder) {
        if (closed) throw new IllegalStateException("UI texture cache is closed");
        if (holder == null) return null;
        Entry current = entries.get(holder);
        if (current != null) return current.textureHandle == 0L ? null : current;
        CpuPixels pixels = cpuSources.get(holder);
        if (pixels == null && !unavailableSources.containsKey(holder)) {
            pixels = readCpuPixels(holder);
            if (pixels != null) cpuSources.put(holder, pixels);
            else unavailableSources.put(holder, Boolean.TRUE);
        }
        if (pixels == null) return null;
        Entry created = new Entry(0L, 1.0f, 1.0f);
        entries.put(holder, created);
        VulkanTextureData data = new VulkanTextureData(pixels.width, pixels.height, pixels.rgba);
        created.textureHandle = driver.uploadTexture(data);
        return created;
    }

    synchronized void invalidate(Object image) {
        Entry removed = entries.remove(image);
        release(removed);
    }

    synchronized void registerPixels(Object holder, int width, int height, byte[] rgba) {
        if (closed || holder == null || rgba == null || width <= 0 || height <= 0) return;
        int expected = Math.multiplyExact(Math.multiplyExact(width, height), 4);
        if (rgba.length < expected) return;
        byte[] copy = new byte[expected];
        System.arraycopy(rgba, 0, copy, 0, expected);
        cpuSources.put(holder, new CpuPixels(width, height, copy));
        unavailableSources.remove(holder);
    }

    synchronized void observeHolder(Object holder) {
        if (closed || holder == null || cpuSources.containsKey(holder)) return;
        CpuPixels buffered = readPendingBuffer(holder);
        if (buffered != null) {
            cpuSources.put(holder, buffered);
            unavailableSources.remove(holder);
        }
    }

    private void release(Entry entry) {
        if (entry != null && entry.textureHandle != 0L) {
            textureDestroyer.accept(entry.textureHandle);
        }
    }

    static Object imageFromHolder(Object holder) {
        if (holder == null) return null;
        Class<?> type = holder.getClass();
        while (type != null) {
            for (Field field : type.getDeclaredFields()) {
                if ("org.newdawn.slick.Image".equals(field.getType().getName())) {
                    try {
                        field.setAccessible(true);
                        return field.get(holder);
                    } catch (ReflectiveOperationException failure) {
                        throw new IllegalStateException("Could not read LibRocket Slick image", failure);
                    }
                }
            }
            type = type.getSuperclass();
        }
        return null;
    }

    static int intField(Object target, String name) {
        Object value = fieldValue(target, name);
        return value instanceof Number ? ((Number) value).intValue() : 0;
    }

    private static Object fieldValue(Object target, String name) {
        if (target == null) return null;
        Class<?> type = target.getClass();
        while (type != null) {
            try {
                Field field = type.getDeclaredField(name);
                field.setAccessible(true);
                return field.get(target);
            } catch (NoSuchFieldException ignored) {
                type = type.getSuperclass();
            } catch (ReflectiveOperationException failure) {
                throw new IllegalStateException("Could not read Slick " + name, failure);
            }
        }
        return null;
    }

    private static CpuPixels readCpuPixels(Object holder) {
        CpuPixels buffered = readPendingBuffer(holder);
        if (buffered != null) return buffered;
        Object rawPath = fieldValue(holder, "path");
        if (!(rawPath instanceof String) || ((String) rawPath).isEmpty()) return null;
        String path = (String) rawPath;
        try (InputStream input = open(path)) {
            if (input == null) return null;
            BufferedImage image = ImageIO.read(input);
            if (image == null) return null;
            int width = image.getWidth();
            int height = image.getHeight();
            byte[] rgba = new byte[Math.multiplyExact(Math.multiplyExact(width, height), 4)];
            int output = 0;
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    int argb = image.getRGB(x, y);
                    rgba[output++] = (byte) (argb >>> 16);
                    rgba[output++] = (byte) (argb >>> 8);
                    rgba[output++] = (byte) argb;
                    rgba[output++] = (byte) (argb >>> 24);
                }
            }
            return new CpuPixels(width, height, rgba);
        } catch (Exception failure) {
            System.out.println("[Vulkan Mod] Could not retain CPU pixels for UI texture "
                    + path + ": " + failure.getMessage());
            return null;
        }
    }

    private static CpuPixels readPendingBuffer(Object holder) {
        Object buffer = fieldValue(holder, "pendingImageBuffer");
        if (buffer == null) return null;
        Object rgba = call(buffer, "getRGBA");
        int width = intCall(buffer, "getWidth");
        int height = intCall(buffer, "getHeight");
        if (!(rgba instanceof byte[]) || width <= 0 || height <= 0) return null;
        byte[] bytes = (byte[]) rgba;
        int expected = Math.multiplyExact(Math.multiplyExact(width, height), 4);
        if (bytes.length < expected) return null;
        byte[] copy = new byte[expected];
        System.arraycopy(bytes, 0, copy, 0, expected);
        return new CpuPixels(width, height, copy);
    }

    private static InputStream open(String path) throws Exception {
        try {
            Class<?> registry = Class.forName(
                    "rustedwarfare.io.VirtualFileSystemRegistry");
            Object backend = registry.getMethod("getBackendForPath", String.class)
                    .invoke(null, path);
            if (backend != null) {
                Object stream = backend.getClass()
                        .getMethod("openInputStream", String.class, boolean.class)
                        .invoke(backend, path, true);
                if (stream instanceof InputStream) return (InputStream) stream;
            }
        } catch (ReflectiveOperationException ignored) {
            // Ordinary assets do not require the game's archive-aware filesystem.
        }
        String normalized = path.startsWith("drawable:")
                ? "res/drawable/" + path.substring("drawable:".length())
                : path.replace("assets:", "assets/");
        Path file = Paths.get(normalized);
        return Files.isRegularFile(file) ? Files.newInputStream(file) : null;
    }

    private static Object call(Object target, String name) {
        try {
            Method method = target.getClass().getMethod(name);
            return method.invoke(target);
        } catch (ReflectiveOperationException failure) {
            throw new IllegalStateException("Could not call Slick " + name, failure);
        }
    }

    private static int intCall(Object target, String name) {
        return ((Number) call(target, name)).intValue();
    }

    @Override public synchronized void close() {
        if (closed) return;
        closed = true;
        for (Entry entry : entries.values()) release(entry);
        entries.clear();
        cpuSources.clear();
        unavailableSources.clear();
    }

    static final class Entry {
        volatile long textureHandle;
        final float uScale;
        final float vScale;

        private Entry(long textureHandle, float uScale, float vScale) {
            this.textureHandle = textureHandle;
            this.uScale = uScale;
            this.vScale = vScale;
        }
    }

    private static final class CpuPixels {
        private final int width;
        private final int height;
        private final byte[] rgba;

        private CpuPixels(int width, int height, byte[] rgba) {
            this.width = width;
            this.height = height;
            this.rgba = rgba;
        }
    }
}
