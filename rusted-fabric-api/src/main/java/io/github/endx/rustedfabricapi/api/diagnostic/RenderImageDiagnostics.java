package io.github.endx.rustedfabricapi.api.diagnostic;

import io.github.endx.rustedfabricapi.api.util.RustedReflection;

import java.lang.reflect.Array;
import java.nio.Buffer;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class RenderImageDiagnostics {
    private static final String[] GAME_IMAGE_CLASSES = {
            "rustedwarfare.client.render.GameImage",
            "com.corrodinggames.rts.gameFramework.m.e"
    };
    private static final String[] MUTABLE_BITMAP_OR_TEXTURE_CLASSES = {
            "rustedwarfare.client.render.MutableBitmapOrTexture",
            "com.corrodinggames.rts.gameFramework.m.ad"
    };
    private static final String[] LAZY_TEAM_COLOR_IMAGE_CLASSES = {
            "rustedwarfare.client.render.LazyTeamColorImage",
            "com.corrodinggames.rts.gameFramework.m.h"
    };
    private static final String[] TEAM_COLOR_SHADER_PROGRAM_CLASSES = {
            "rustedwarfare.render.TeamColorShaderProgram",
            "com.corrodinggames.rts.gameFramework.m.i"
    };
    private static final String[] SLICK_BITMAP_OR_TEXTURE_CLASSES = {
            "rustedwarfare.client.render.SlickBitmapOrTexture",
            "com.corrodinggames.rts.java.s"
    };
    private static final String[] SLICK_FALLBACK_BITMAP_OR_TEXTURE_CLASSES = {
            "rustedwarfare.client.render.SlickFallbackBitmapOrTexture",
            "com.corrodinggames.rts.java.a"
    };
    private static final String[] SLICK_TEXTURE_READBACK_IMAGE_DATA_CLASSES = {
            "rustedwarfare.client.render.SlickTextureReadbackImageData",
            "com.corrodinggames.rts.java.t"
    };
    private static final String[] BUFFERED_SLICK_IMAGE_DATA_CLASSES = {
            "rustedwarfare.client.render.BufferedSlickImageData",
            "com.corrodinggames.rts.java.a.a"
    };
    private static final String[] SLICK_GRAPHICS_BACKEND_CLASSES = {
            "rustedwarfare.client.render.SlickGraphicsBackend",
            "com.corrodinggames.rts.java.e"
    };

    private RenderImageDiagnostics() {
    }

    public static boolean isGameImage(Object value) {
        return isAny(value, GAME_IMAGE_CLASSES);
    }

    public static boolean isMutableBitmapOrTexture(Object value) {
        return isAny(value, MUTABLE_BITMAP_OR_TEXTURE_CLASSES);
    }

    public static boolean isLazyTeamColorImage(Object value) {
        return isAny(value, LAZY_TEAM_COLOR_IMAGE_CLASSES);
    }

    public static boolean isTeamColorShaderProgram(Object value) {
        return isAny(value, TEAM_COLOR_SHADER_PROGRAM_CLASSES);
    }

    public static boolean isSlickBitmapOrTexture(Object value) {
        return isAny(value, SLICK_BITMAP_OR_TEXTURE_CLASSES);
    }

    public static boolean isSlickFallbackBitmapOrTexture(Object value) {
        return isAny(value, SLICK_FALLBACK_BITMAP_OR_TEXTURE_CLASSES);
    }

    public static boolean isSlickTextureReadbackImageData(Object value) {
        return isAny(value, SLICK_TEXTURE_READBACK_IMAGE_DATA_CLASSES);
    }

    public static boolean isBufferedSlickImageData(Object value) {
        return isAny(value, BUFFERED_SLICK_IMAGE_DATA_CLASSES);
    }

    public static boolean isSlickGraphicsBackend(Object value) {
        return isAny(value, SLICK_GRAPHICS_BACKEND_CLASSES);
    }

    public static Map<String, Object> describeGameImage(Object image) {
        requireAny(image, GAME_IMAGE_CLASSES, "GameImage");
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("className", image.getClass().getName());
        result.put("mutableBitmapOrTexture", Boolean.valueOf(isMutableBitmapOrTexture(image)));
        result.put("lazyTeamColorImage", Boolean.valueOf(isLazyTeamColorImage(image)));
        result.put("slickBitmapOrTexture", Boolean.valueOf(isSlickBitmapOrTexture(image)));
        result.put("slickFallbackBitmapOrTexture", Boolean.valueOf(isSlickFallbackBitmapOrTexture(image)));
        putOptionalField(result, image, "name", new String[]{"name", "g"});
        putOptionalField(result, image, "bitmap", new String[]{"bitmap", "k"});
        putOptionalIntField(result, image, "imageId", new String[]{"imageId", "d"});
        putOptionalIntField(result, image, "version", new String[]{"version", "e"});
        putOptionalIntField(result, image, "width", new String[]{"width", "p"});
        putOptionalIntField(result, image, "height", new String[]{"height", "q"});
        putOptionalFloatField(result, image, "halfWidth", new String[]{"halfWidth", "t"});
        putOptionalFloatField(result, image, "halfHeight", new String[]{"halfHeight", "u"});
        putOptionalBooleanField(result, image, "smooth", new String[]{"smooth", "o"});
        putOptionalBooleanField(result, image, "keepInGpuMemory", new String[]{"keepInGpuMemory", "w"});
        putOptionalArrayLengthField(result, image, "pixelBufferLength", new String[]{"pixelBuffer", "j"});
        putOptionalArrayLengthField(result, image, "teamColorCachePureGreenLength",
                new String[]{"teamColorCachePureGreen", "a"});
        putOptionalArrayLengthField(result, image, "teamColorCacheHueAddLength",
                new String[]{"teamColorCacheHueAdd", "b"});
        putOptionalArrayLengthField(result, image, "teamColorCacheHueShiftLength",
                new String[]{"teamColorCacheHueShift", "c"});
        putOptionalField(result, image, "shader", new String[]{"shader", "i"});
        putOptionalInvoke(result, image, "nameFromGetter", new String[]{"getName", "a"});
        putOptionalIntInvoke(result, image, "widthFromGetter", new String[]{"getWidth", "m"});
        putOptionalIntInvoke(result, image, "heightFromGetter", new String[]{"getHeight", "l"});
        putOptionalIntInvoke(result, image, "estimatedMemoryBytes", new String[]{"estimateMemoryBytes", "u"});
        putOptionalBooleanInvoke(result, image, "canReadPixels", new String[]{"canReadPixels", "k"});
        putOptionalBooleanInvoke(result, image, "outOfMemoryFallback", new String[]{"isOutOfMemoryFallback", "A"});
        putOptionalInvoke(result, image, "shaderFromGetter", new String[]{"getShader", "B"});
        return Collections.unmodifiableMap(result);
    }

    public static Map<String, Object> describeMutableBitmapOrTexture(Object image) {
        requireAny(image, MUTABLE_BITMAP_OR_TEXTURE_CLASSES, "MutableBitmapOrTexture");
        Map<String, Object> result = new LinkedHashMap<String, Object>(describeGameImage(image));
        putOptionalField(result, image, "wrappedImage", new String[]{"wrappedImage", "x"});
        return Collections.unmodifiableMap(result);
    }

    public static Map<String, Object> describeLazyTeamColorImage(Object image) {
        requireAny(image, LAZY_TEAM_COLOR_IMAGE_CLASSES, "LazyTeamColorImage");
        Map<String, Object> result = new LinkedHashMap<String, Object>(describeGameImage(image));
        putOptionalBooleanField(result, image, "loaded", new String[]{"loaded", "B"});
        putOptionalBooleanField(result, image, "usingShaderColoring", new String[]{"usingShaderColoring", "C"});
        putOptionalField(result, image, "sourceImage", new String[]{"sourceImage", "H"});
        putOptionalField(result, image, "coloredImage", new String[]{"coloredImage", "I"});
        putOptionalIntField(result, image, "teamColor", new String[]{"teamColor", "D"});
        putOptionalIntField(result, image, "teamIndex", new String[]{"teamIndex", "E"});
        putOptionalField(result, image, "coloringMode", new String[]{"coloringMode", "F"});
        putOptionalFloatField(result, image, "totalColoringTime", new String[]{"totalColoringTime", "G"});
        putOptionalStaticField(result, "pureGreenTeamColorShader",
                LAZY_TEAM_COLOR_IMAGE_CLASSES, new String[]{"pureGreenTeamColorShader", "x"});
        putOptionalStaticField(result, "hueAddTeamColorShader",
                LAZY_TEAM_COLOR_IMAGE_CLASSES, new String[]{"hueAddTeamColorShader", "y"});
        putOptionalStaticField(result, "hueShiftTeamColorShader",
                LAZY_TEAM_COLOR_IMAGE_CLASSES, new String[]{"hueShiftTeamColorShader", "z"});
        putOptionalStaticBooleanField(result, "teamShadersLoaded",
                LAZY_TEAM_COLOR_IMAGE_CLASSES, new String[]{"teamShadersLoaded", "A"});
        return Collections.unmodifiableMap(result);
    }

    public static Map<String, Object> describeTeamColorShaderProgram(Object shaderProgram) {
        requireAny(shaderProgram, TEAM_COLOR_SHADER_PROGRAM_CLASSES, "TeamColorShaderProgram");
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("className", shaderProgram.getClass().getName());
        putOptionalIntField(result, shaderProgram, "lastTeamColor", new String[]{"lastTeamColor", "a"});
        return Collections.unmodifiableMap(result);
    }

    public static Map<String, Object> describeSlickBitmapOrTexture(Object image) {
        requireAny(image, SLICK_BITMAP_OR_TEXTURE_CLASSES, "SlickBitmapOrTexture");
        Map<String, Object> result = new LinkedHashMap<String, Object>(describeGameImage(image));
        putOptionalField(result, image, "slickImage", new String[]{"slickImage", "x"});
        putOptionalField(result, image, "slickImageData", new String[]{"slickImageData", "z"});
        putOptionalField(result, image, "imageByteBuffer", new String[]{"imageByteBuffer", "A"});
        putOptionalBufferRemaining(result, image, "imageByteBufferRemaining", new String[]{"imageByteBuffer", "A"});
        putOptionalIntField(result, image, "bytesPerPixel", new String[]{"bytesPerPixel", "B"});
        putOptionalField(result, image, "filePath", new String[]{"filePath", "C"});
        putOptionalBooleanField(result, image, "cloned", new String[]{"cloned", "D"});
        putOptionalIntField(result, image, "slickFilter", new String[]{"slickFilter", "E"});
        putOptionalBooleanField(result, image, "discardImageDataAfterUpload",
                new String[]{"discardImageDataAfterUpload", "F"});
        putOptionalBooleanField(result, image, "pixelBufferLoaded", new String[]{"pixelBufferLoaded", "G"});
        putOptionalBooleanField(result, image, "pixelBufferDiscarded", new String[]{"pixelBufferDiscarded", "H"});
        putOptionalBooleanField(result, image, "autoReleaseOnFinalize", new String[]{"autoReleaseOnFinalize", "I"});
        putOptionalLongField(result, image, "lastModifiedTimestamp", new String[]{"lastModifiedTimestamp", "J"});
        putOptionalIntField(result, image, "reloadImageCounter", new String[]{"reloadImageCounter", "K"});
        return Collections.unmodifiableMap(result);
    }

    public static Map<String, Object> describeSlickFallbackBitmapOrTexture(Object image) {
        requireAny(image, SLICK_FALLBACK_BITMAP_OR_TEXTURE_CLASSES, "SlickFallbackBitmapOrTexture");
        Map<String, Object> result = new LinkedHashMap<String, Object>(describeSlickBitmapOrTexture(image));
        putOptionalField(result, image, "fallbackImage", new String[]{"fallbackImage", "x"});
        putOptionalBooleanInvoke(result, image, "outOfMemoryFallback", new String[]{"isOutOfMemoryFallback", "A"});
        return Collections.unmodifiableMap(result);
    }

    public static Map<String, Object> describeSlickTextureReadbackImageData(Object imageData) {
        requireAny(imageData, SLICK_TEXTURE_READBACK_IMAGE_DATA_CLASSES, "SlickTextureReadbackImageData");
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("className", imageData.getClass().getName());
        putOptionalIntField(result, imageData, "depth", new String[]{"depth", "a"});
        putOptionalField(result, imageData, "ownerImage", new String[]{"ownerImage", "b"});
        putOptionalIntField(result, imageData, "imageWidth", new String[]{"imageWidth", "c"});
        putOptionalIntField(result, imageData, "imageHeight", new String[]{"imageHeight", "d"});
        putOptionalIntField(result, imageData, "textureWidth", new String[]{"textureWidth", "e"});
        putOptionalIntField(result, imageData, "textureHeight", new String[]{"textureHeight", "f"});
        putOptionalArrayLengthField(result, imageData, "rawTextureDataLength", new String[]{"rawTextureData", "g"});
        return Collections.unmodifiableMap(result);
    }

    public static Map<String, Object> describeBufferedSlickImageData(Object imageData) {
        requireAny(imageData, BUFFERED_SLICK_IMAGE_DATA_CLASSES, "BufferedSlickImageData");
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("className", imageData.getClass().getName());
        putOptionalField(result, imageData, "delegate", new String[]{"delegate", "a"});
        putOptionalField(result, imageData, "imageBufferData", new String[]{"imageBufferData", "b"});
        putOptionalBufferRemaining(result, imageData, "imageBufferDataRemaining", new String[]{"imageBufferData", "b"});
        return Collections.unmodifiableMap(result);
    }

    public static Map<String, Object> describeSlickGraphicsBackend(Object backend) {
        requireAny(backend, SLICK_GRAPHICS_BACKEND_CLASSES, "SlickGraphicsBackend");
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("className", backend.getClass().getName());
        putOptionalField(result, backend, "outOfMemoryErrorImage", new String[]{"outOfMemoryErrorImage", "r"});
        putOptionalField(result, backend, "generalErrorImage", new String[]{"generalErrorImage", "s"});
        putOptionalField(result, backend, "tooLargeThumbnailErrorImage",
                new String[]{"tooLargeThumbnailErrorImage", "t"});
        putOptionalField(result, backend, "pendingImageDataDiscards", new String[]{"pendingImageDataDiscards", "E"});
        putOptionalSizeField(result, backend, "pendingImageDataDiscardsSize",
                new String[]{"pendingImageDataDiscards", "E"});
        putOptionalIntField(result, backend, "loadedImageCount", new String[]{"loadedImageCount", "F"});
        return Collections.unmodifiableMap(result);
    }

    public static List<Object> liveSlickImagesSnapshot() {
        try {
            Object value = RustedReflection.getStaticFieldValue(
                    SLICK_BITMAP_OR_TEXTURE_CLASSES, new String[]{"liveImages", "y"});
            return Collections.unmodifiableList(RustedReflection.snapshotIterable(value));
        } catch (RuntimeException ignored) {
            return Collections.emptyList();
        }
    }

    public static List<Object> pendingImageDataDiscardsSnapshot(Object backend) {
        requireAny(backend, SLICK_GRAPHICS_BACKEND_CLASSES, "SlickGraphicsBackend");
        try {
            Object value = RustedReflection.getFieldValue(backend, new String[]{"pendingImageDataDiscards", "E"});
            return Collections.unmodifiableList(RustedReflection.snapshotIterable(value));
        } catch (RuntimeException ignored) {
            return Collections.emptyList();
        }
    }

    private static boolean isAny(Object value, String[] classNames) {
        return value != null && RustedReflection.isAnyClass(value.getClass(), classNames);
    }

    private static void requireAny(Object value, String[] classNames, String label) {
        if (value == null || !RustedReflection.isAnyClass(value.getClass(), classNames)) {
            throw new IllegalArgumentException("Expected " + label + ", got " + describe(value));
        }
    }

    private static String describe(Object value) {
        return value == null ? "null" : value.getClass().getName();
    }

    private static void putOptionalField(Map<String, Object> result, Object owner, String key, String[] fieldNames) {
        try {
            result.put(key, RustedReflection.getFieldValue(owner, fieldNames));
        } catch (RuntimeException ignored) {
        }
    }

    private static void putOptionalIntField(Map<String, Object> result, Object owner, String key,
                                            String[] fieldNames) {
        try {
            result.put(key, Integer.valueOf(RustedReflection.getIntField(owner, fieldNames)));
        } catch (RuntimeException ignored) {
        }
    }

    private static void putOptionalLongField(Map<String, Object> result, Object owner, String key,
                                             String[] fieldNames) {
        try {
            Object value = RustedReflection.getFieldValue(owner, fieldNames);
            result.put(key, value instanceof Number ? Long.valueOf(((Number) value).longValue()) : Long.valueOf(0L));
        } catch (RuntimeException ignored) {
        }
    }

    private static void putOptionalFloatField(Map<String, Object> result, Object owner, String key,
                                              String[] fieldNames) {
        try {
            result.put(key, Float.valueOf(RustedReflection.getFloatField(owner, fieldNames)));
        } catch (RuntimeException ignored) {
        }
    }

    private static void putOptionalBooleanField(Map<String, Object> result, Object owner, String key,
                                                String[] fieldNames) {
        try {
            result.put(key, Boolean.valueOf(RustedReflection.getBooleanField(owner, fieldNames)));
        } catch (RuntimeException ignored) {
        }
    }

    private static void putOptionalArrayLengthField(Map<String, Object> result, Object owner, String key,
                                                    String[] fieldNames) {
        try {
            result.put(key, Integer.valueOf(lengthOf(RustedReflection.getFieldValue(owner, fieldNames))));
        } catch (RuntimeException ignored) {
        }
    }

    private static void putOptionalSizeField(Map<String, Object> result, Object owner, String key,
                                             String[] fieldNames) {
        try {
            result.put(key, Integer.valueOf(RustedReflection.snapshotIterable(
                    RustedReflection.getFieldValue(owner, fieldNames)).size()));
        } catch (RuntimeException ignored) {
        }
    }

    private static void putOptionalBufferRemaining(Map<String, Object> result, Object owner, String key,
                                                   String[] fieldNames) {
        try {
            Object value = RustedReflection.getFieldValue(owner, fieldNames);
            if (value instanceof Buffer) {
                result.put(key, Integer.valueOf(((Buffer) value).remaining()));
            }
        } catch (RuntimeException ignored) {
        }
    }

    private static void putOptionalStaticField(Map<String, Object> result, String key,
                                               String[] classNames, String[] fieldNames) {
        try {
            result.put(key, RustedReflection.getStaticFieldValue(classNames, fieldNames));
        } catch (RuntimeException ignored) {
        }
    }

    private static void putOptionalStaticBooleanField(Map<String, Object> result, String key,
                                                      String[] classNames, String[] fieldNames) {
        try {
            Object value = RustedReflection.getStaticFieldValue(classNames, fieldNames);
            result.put(key, Boolean.valueOf(Boolean.TRUE.equals(value)));
        } catch (RuntimeException ignored) {
        }
    }

    private static void putOptionalInvoke(Map<String, Object> result, Object owner, String key,
                                          String[] methodNames) {
        try {
            result.put(key, RustedReflection.invokeInstance(owner, methodNames));
        } catch (RuntimeException ignored) {
        }
    }

    private static void putOptionalIntInvoke(Map<String, Object> result, Object owner, String key,
                                             String[] methodNames) {
        try {
            Object value = RustedReflection.invokeInstance(owner, methodNames);
            result.put(key, value instanceof Number ? Integer.valueOf(((Number) value).intValue()) : Integer.valueOf(0));
        } catch (RuntimeException ignored) {
        }
    }

    private static void putOptionalBooleanInvoke(Map<String, Object> result, Object owner, String key,
                                                 String[] methodNames) {
        try {
            result.put(key, Boolean.valueOf(Boolean.TRUE.equals(RustedReflection.invokeInstance(owner, methodNames))));
        } catch (RuntimeException ignored) {
        }
    }

    private static int lengthOf(Object value) {
        if (value == null) {
            return 0;
        }
        if (value.getClass().isArray()) {
            return Array.getLength(value);
        }
        return RustedReflection.snapshotIterable(value).size();
    }
}
