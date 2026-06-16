package io.github.endx.rustedfabricapi.api.diagnostic;

import io.github.endx.rustedfabricapi.api.util.RustedReflection;

import java.lang.reflect.Array;
import java.nio.Buffer;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class SlickGraphicsBackendDiagnostics {
    private static final String[] GRAPHICS_ENGINE_CLASSES = {
            "rustedwarfare.render.GraphicsEngine",
            "com.corrodinggames.rts.gameFramework.m.y"
    };
    private static final String[] CANVAS_GRAPHICS_ENGINE_CLASSES = {
            "rustedwarfare.render.CanvasGraphicsEngine",
            "com.corrodinggames.rts.gameFramework.m.x"
    };
    private static final String[] NULL_GRAPHICS_ENGINE_CLASSES = {
            "rustedwarfare.render.NullGraphicsEngine",
            "com.corrodinggames.rts.gameFramework.m.z"
    };
    private static final String[] SLICK_GRAPHICS_BACKEND_CLASSES = {
            "rustedwarfare.client.render.SlickGraphicsBackend",
            "com.corrodinggames.rts.java.e"
    };
    private static final String[] SLICK_FONT_KEY_CLASSES = {
            "rustedwarfare.client.render.SlickFontKey",
            "com.corrodinggames.rts.java.f"
    };
    private static final String[] SLICK_TRANSFORM_STATE_CLASSES = {
            "rustedwarfare.client.render.SlickTransformState",
            "com.corrodinggames.rts.java.g"
    };

    private SlickGraphicsBackendDiagnostics() {
    }

    public static Object currentGraphicsBackend() {
        return GameEngineDiagnostics.currentGraphicsEngine();
    }

    public static boolean isGraphicsEngine(Object value) {
        return isAny(value, GRAPHICS_ENGINE_CLASSES);
    }

    public static boolean isCanvasGraphicsEngine(Object value) {
        return isAny(value, CANVAS_GRAPHICS_ENGINE_CLASSES);
    }

    public static boolean isNullGraphicsEngine(Object value) {
        return isAny(value, NULL_GRAPHICS_ENGINE_CLASSES);
    }

    public static boolean isSlickGraphicsBackend(Object value) {
        return isAny(value, SLICK_GRAPHICS_BACKEND_CLASSES);
    }

    public static boolean isSlickFontKey(Object value) {
        return isAny(value, SLICK_FONT_KEY_CLASSES);
    }

    public static boolean isSlickTransformState(Object value) {
        return isAny(value, SLICK_TRANSFORM_STATE_CLASSES);
    }

    public static Map<String, Object> describeGraphicsBackend(Object backend) {
        requireAny(backend, GRAPHICS_ENGINE_CLASSES, "GraphicsEngine");
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("className", backend.getClass().getName());
        result.put("canvasGraphicsEngine", Boolean.valueOf(isCanvasGraphicsEngine(backend)));
        result.put("nullGraphicsEngine", Boolean.valueOf(isNullGraphicsEngine(backend)));
        result.put("slickGraphicsBackend", Boolean.valueOf(isSlickGraphicsBackend(backend)));
        result.put("width", Integer.valueOf(getWidth(backend)));
        result.put("height", Integer.valueOf(getHeight(backend)));
        return Collections.unmodifiableMap(result);
    }

    public static Map<String, Object> describeSlickGraphicsBackend(Object backend) {
        requireAny(backend, SLICK_GRAPHICS_BACKEND_CLASSES, "SlickGraphicsBackend");
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("className", backend.getClass().getName());
        putOptionalBooleanField(result, backend, "shadersEnabled", new String[]{"shadersEnabled", "a"});
        putOptionalBooleanField(result, backend, "paintStateDirty", new String[]{"paintStateDirty", "b"});
        putOptionalField(result, backend, "slickGraphics", new String[]{"slickGraphics", "f"});
        putOptionalField(result, backend, "renderTargetImage", new String[]{"renderTargetImage", "g"});
        putOptionalIntField(result, backend, "targetWidth", new String[]{"targetWidth", "h"});
        putOptionalIntField(result, backend, "targetHeight", new String[]{"targetHeight", "i"});
        putOptionalField(result, backend, "currentSlickGraphics", new String[]{"currentSlickGraphics", "k"});
        putOptionalField(result, backend, "activeBackend", new String[]{"activeBackend", "l"});
        putOptionalField(result, backend, "activeShader", new String[]{"activeShader", "m"});
        putOptionalField(result, backend, "scratchSrcRect", new String[]{"scratchSrcRect", "n"});
        putOptionalField(result, backend, "scratchDstRect", new String[]{"scratchDstRect", "o"});
        putOptionalField(result, backend, "scratchDstRectF", new String[]{"scratchDstRectF", "p"});
        putOptionalField(result, backend, "scratchPoint", new String[]{"scratchPoint", "q"});
        Object fontCacheKeys = optionalField(backend, new String[]{"fontCacheKeys", "u"});
        result.put("fontCacheKeys", fontCacheKeys);
        result.put("fontCacheKeysSize", Integer.valueOf(sizeOf(fontCacheKeys)));
        putOptionalIntField(result, backend, "currentDrawMode", new String[]{"currentDrawMode", "v"});
        putOptionalField(result, backend, "currentPaint", new String[]{"currentPaint", "w"});
        putOptionalField(result, backend, "currentPaintImage", new String[]{"currentPaintImage", "x"});
        putOptionalField(result, backend, "scratchPaint", new String[]{"scratchPaint", "z"});
        putOptionalFloatField(result, backend, "currentLineWidth", new String[]{"currentLineWidth", "B"});
        putOptionalField(result, backend, "fontKeyScratch", new String[]{"fontKeyScratch", "C"});
        putOptionalArrayLengthField(result, backend, "pixelScratchBytesLength", new String[]{"pixelScratchBytes", "D"});
        Object pendingImageDataDiscards = optionalField(backend, new String[]{"pendingImageDataDiscards", "E"});
        result.put("pendingImageDataDiscards", pendingImageDataDiscards);
        result.put("pendingImageDataDiscardsSize", Integer.valueOf(sizeOf(pendingImageDataDiscards)));
        putOptionalIntField(result, backend, "loadedImageCount", new String[]{"loadedImageCount", "F"});
        putOptionalField(result, backend, "scratchRectF", new String[]{"scratchRectF", "G"});
        putOptionalFloatField(result, backend, "uiScale", new String[]{"uiScale", "L"});
        Object sharedFloatBuffer = optionalField(backend, new String[]{"sharedFloatBuffer", "N"});
        result.put("sharedFloatBuffer", sharedFloatBuffer);
        result.put("sharedFloatBufferCapacity", Integer.valueOf(bufferCapacity(sharedFloatBuffer)));
        result.put("sharedFloatBufferRemaining", Integer.valueOf(bufferRemaining(sharedFloatBuffer)));
        putOptionalArrayLengthField(result, backend, "sharedFloatArrayLength", new String[]{"sharedFloatArray", "O"});
        putOptionalIntField(result, backend, "cachedCircleSegments", new String[]{"cachedCircleSegments", "P"});
        putOptionalFloatField(result, backend, "cachedCircleAngleStep", new String[]{"cachedCircleAngleStep", "Q"});
        putOptionalFloatField(result, backend, "cachedCircleCosStep", new String[]{"cachedCircleCosStep", "R"});
        putOptionalFloatField(result, backend, "cachedCircleSinStep", new String[]{"cachedCircleSinStep", "S"});
        putOptionalField(result, backend, "transformState", new String[]{"transformState", "T"});
        Object transformStack = optionalField(backend, new String[]{"transformStack", "U"});
        Object transformStatePool = optionalField(backend, new String[]{"transformStatePool", "V"});
        result.put("transformStack", transformStack);
        result.put("transformStackSize", Integer.valueOf(sizeOf(transformStack)));
        result.put("transformStatePool", transformStatePool);
        result.put("transformStatePoolSize", Integer.valueOf(sizeOf(transformStatePool)));
        putOptionalField(result, backend, "slickRenderer", new String[]{"slickRenderer", "W"});
        putOptionalField(result, backend, "lineStripRenderer", new String[]{"lineStripRenderer", "X"});
        return Collections.unmodifiableMap(result);
    }

    public static Map<String, Object> describeSlickFontKey(Object fontKey) {
        requireAny(fontKey, SLICK_FONT_KEY_CLASSES, "SlickFontKey");
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("className", fontKey.getClass().getName());
        putOptionalIntField(result, fontKey, "fontSize", new String[]{"fontSize", "a"});
        putOptionalBooleanField(result, fontKey, "bold", new String[]{"bold", "b"});
        putOptionalBooleanField(result, fontKey, "fallback", new String[]{"fallback", "c"});
        putOptionalField(result, fontKey, "font", new String[]{"font", "d"});
        putOptionalIntField(result, fontKey, "recentTextIndex", new String[]{"recentTextIndex", "e"});
        Object recentTexts = optionalField(fontKey, new String[]{"recentTexts", "f"});
        result.put("recentTexts", recentTexts);
        result.put("recentTextsLength", Integer.valueOf(lengthOf(recentTexts)));
        result.put("recentTextsUsed", Integer.valueOf(nonNullCount(recentTexts)));
        putOptionalField(result, fontKey, "owner", new String[]{"owner", "g"});
        result.put("text", String.valueOf(fontKey));
        return Collections.unmodifiableMap(result);
    }

    public static Map<String, Object> describeSlickTransformState(Object transformState) {
        requireAny(transformState, SLICK_TRANSFORM_STATE_CLASSES, "SlickTransformState");
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("className", transformState.getClass().getName());
        putOptionalFloatField(result, transformState, "translateX", new String[]{"translateX", "a"});
        putOptionalFloatField(result, transformState, "translateY", new String[]{"translateY", "b"});
        putOptionalFloatField(result, transformState, "rotationDegrees", new String[]{"rotationDegrees", "c"});
        putOptionalFloatField(result, transformState, "scaleX", new String[]{"scaleX", "d"});
        putOptionalFloatField(result, transformState, "scaleY", new String[]{"scaleY", "e"});
        putOptionalField(result, transformState, "clipRect", new String[]{"clipRect", "f"});
        putOptionalFloatField(result, transformState, "rotationPivotX", new String[]{"rotationPivotX", "g"});
        putOptionalFloatField(result, transformState, "rotationPivotY", new String[]{"rotationPivotY", "h"});
        return Collections.unmodifiableMap(result);
    }

    public static int getWidth(Object backend) {
        requireAny(backend, GRAPHICS_ENGINE_CLASSES, "GraphicsEngine");
        return invokeIntOrZero(backend, new String[]{"getWidth", "m"});
    }

    public static int getHeight(Object backend) {
        requireAny(backend, GRAPHICS_ENGINE_CLASSES, "GraphicsEngine");
        return invokeIntOrZero(backend, new String[]{"getHeight", "n"});
    }

    public static float getUiScale(Object backend) {
        requireAny(backend, SLICK_GRAPHICS_BACKEND_CLASSES, "SlickGraphicsBackend");
        Object value = invokeOrNull(backend, new String[]{"getUiScale", "s"});
        return value instanceof Number ? ((Number) value).floatValue() : 1.0F;
    }

    public static boolean shouldScaleTextForUi(Object backend) {
        requireAny(backend, SLICK_GRAPHICS_BACKEND_CLASSES, "SlickGraphicsBackend");
        return Boolean.TRUE.equals(invokeOrNull(backend, new String[]{"shouldScaleTextForUi", "x"}));
    }

    public static int getTextWidth(Object backend, String text, Object paint) {
        requireAny(backend, GRAPHICS_ENGINE_CLASSES, "GraphicsEngine");
        Object value = RustedReflection.invokeInstance(backend, new String[]{"getTextWidth", "b"}, text, paint);
        return value instanceof Number ? ((Number) value).intValue() : 0;
    }

    public static int getTextHeight(Object backend, String text, Object paint) {
        requireAny(backend, GRAPHICS_ENGINE_CLASSES, "GraphicsEngine");
        Object value = RustedReflection.invokeInstance(backend, new String[]{"getTextHeight", "a"}, text, paint);
        return value instanceof Number ? ((Number) value).intValue() : 0;
    }

    public static boolean containsNonAscii(String text) {
        return Boolean.TRUE.equals(RustedReflection.invokeStatic(
                SLICK_GRAPHICS_BACKEND_CLASSES, new String[]{"containsNonAscii", "a"}, text));
    }

    public static boolean hasGlyphsForText(Object fontKey, String text) {
        requireAny(fontKey, SLICK_FONT_KEY_CLASSES, "SlickFontKey");
        return Boolean.TRUE.equals(RustedReflection.invokeInstance(fontKey,
                new String[]{"hasGlyphsForText", "a"}, text));
    }

    public static void rememberFontKeyText(Object fontKey, String text) {
        requireAny(fontKey, SLICK_FONT_KEY_CLASSES, "SlickFontKey");
        RustedReflection.invokeInstance(fontKey, new String[]{"rememberText", "b"}, text);
    }

    public static Object copySlickFontKey(Object fontKey) {
        requireAny(fontKey, SLICK_FONT_KEY_CLASSES, "SlickFontKey");
        return RustedReflection.invokeInstance(fontKey, new String[]{"copyKey", "a"});
    }

    public static Object copySlickTransformState(Object transformState) {
        requireAny(transformState, SLICK_TRANSFORM_STATE_CLASSES, "SlickTransformState");
        Object copy = RustedReflection.newInstance(SLICK_TRANSFORM_STATE_CLASSES);
        RustedReflection.invokeInstance(transformState, new String[]{"copyInto", "a"}, copy);
        return copy;
    }

    public static List<Object> fontCacheKeysSnapshot(Object backend) {
        requireAny(backend, SLICK_GRAPHICS_BACKEND_CLASSES, "SlickGraphicsBackend");
        return Collections.unmodifiableList(RustedReflection.snapshotIterable(
                optionalField(backend, new String[]{"fontCacheKeys", "u"})));
    }

    public static Object transformState(Object backend) {
        requireAny(backend, SLICK_GRAPHICS_BACKEND_CLASSES, "SlickGraphicsBackend");
        return optionalField(backend, new String[]{"transformState", "T"});
    }

    public static List<Object> transformStackSnapshot(Object backend) {
        requireAny(backend, SLICK_GRAPHICS_BACKEND_CLASSES, "SlickGraphicsBackend");
        return Collections.unmodifiableList(RustedReflection.snapshotIterable(
                optionalField(backend, new String[]{"transformStack", "U"})));
    }

    private static boolean isAny(Object value, String[] classNames) {
        return value != null && RustedReflection.isAnyClass(value.getClass(), classNames);
    }

    private static void requireAny(Object value, String[] classNames, String label) {
        if (value == null || !RustedReflection.isAnyClass(value.getClass(), classNames)) {
            throw new IllegalArgumentException("Expected " + label + ", got " + describe(value));
        }
    }

    private static Object invokeOrNull(Object owner, String[] methodNames, Object... args) {
        try {
            return RustedReflection.invokeInstance(owner, methodNames, args);
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private static int invokeIntOrZero(Object owner, String[] methodNames, Object... args) {
        Object value = invokeOrNull(owner, methodNames, args);
        return value instanceof Number ? ((Number) value).intValue() : 0;
    }

    private static Object optionalField(Object owner, String[] fieldNames) {
        try {
            return RustedReflection.getFieldValue(owner, fieldNames);
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private static int sizeOf(Object value) {
        return RustedReflection.snapshotIterable(value).size();
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

    private static int nonNullCount(Object value) {
        int count = 0;
        for (Object item : RustedReflection.snapshotIterable(value)) {
            if (item != null) {
                count++;
            }
        }
        return count;
    }

    private static int bufferCapacity(Object value) {
        return value instanceof Buffer ? ((Buffer) value).capacity() : 0;
    }

    private static int bufferRemaining(Object value) {
        return value instanceof Buffer ? ((Buffer) value).remaining() : 0;
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

    private static String describe(Object value) {
        return value == null ? "null" : value.getClass().getName();
    }
}
