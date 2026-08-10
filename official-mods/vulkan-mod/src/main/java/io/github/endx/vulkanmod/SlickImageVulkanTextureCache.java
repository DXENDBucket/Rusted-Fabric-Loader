package io.github.endx.vulkanmod;

import io.github.endx.vulkanmod.spi.VulkanTextureData;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.IdentityHashMap;
import java.util.Map;

/** Caches LibRocket/Slick textures that are not represented by GameImage. */
final class SlickImageVulkanTextureCache implements AutoCloseable {
    private final VulkanDriverLoader.LoadedDriver driver;
    private final Map<Object, Entry> entries = new IdentityHashMap<Object, Entry>();
    private boolean closed;

    SlickImageVulkanTextureCache(VulkanDriverLoader.LoadedDriver driver) {
        this.driver = driver;
    }

    synchronized Entry texture(Object image) {
        if (closed) throw new IllegalStateException("Slick image texture cache is closed");
        if (image == null || booleanCall(image, "isDestroyed")) return null;
        Entry current = entries.get(image);
        if (current != null) return current;
        Object source = call(image, "getTexture");
        if (source == null) return null;
        int width = intCall(source, "getTextureWidth");
        int height = intCall(source, "getTextureHeight");
        byte[] sourceBytes = (byte[]) call(source, "getTextureData");
        int components = booleanCall(source, "hasAlpha") ? 4 : 3;
        int expected = Math.multiplyExact(Math.multiplyExact(width, height), components);
        if (sourceBytes == null || sourceBytes.length < expected) {
            throw new IllegalArgumentException("Slick texture data is incomplete");
        }
        byte[] rgba = new byte[Math.multiplyExact(Math.multiplyExact(width, height), 4)];
        int input = 0;
        int output = 0;
        for (int pixel = 0; pixel < width * height; pixel++) {
            rgba[output++] = sourceBytes[input++];
            rgba[output++] = sourceBytes[input++];
            rgba[output++] = sourceBytes[input++];
            rgba[output++] = components == 4 ? sourceBytes[input++] : (byte) 255;
        }
        Entry created = new Entry(driver.uploadTexture(
                new VulkanTextureData(width, height, rgba)),
                floatCall(image, "getTextureWidth"), floatCall(image, "getTextureHeight"));
        entries.put(image, created);
        return created;
    }

    synchronized void invalidate(Object image) {
        Entry removed = entries.remove(image);
        if (removed != null) driver.destroyTexture(removed.textureHandle);
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

    private static Object call(Object target, String name) {
        try {
            Method method = target.getClass().getMethod(name);
            return method.invoke(target);
        } catch (ReflectiveOperationException failure) {
            throw new IllegalStateException("Could not call Slick " + name, failure);
        }
    }

    private static boolean booleanCall(Object target, String name) {
        return (Boolean) call(target, name);
    }

    private static int intCall(Object target, String name) {
        return ((Number) call(target, name)).intValue();
    }

    private static float floatCall(Object target, String name) {
        return ((Number) call(target, name)).floatValue();
    }

    @Override public synchronized void close() {
        if (closed) return;
        closed = true;
        for (Entry entry : entries.values()) driver.destroyTexture(entry.textureHandle);
        entries.clear();
    }

    static final class Entry {
        final long textureHandle;
        final float uScale;
        final float vScale;

        private Entry(long textureHandle, float uScale, float vScale) {
            this.textureHandle = textureHandle;
            this.uScale = uScale;
            this.vScale = vScale;
        }
    }
}
