package io.github.endx.rustedfabricapi.api.diagnostic;

import io.github.endx.rustedfabricapi.api.util.RustedReflection;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class RenderCanvasDiagnostics {
    private static final String[] CANVAS_DRAW_TARGET_CLASSES = {
            "rustedwarfare.render.CanvasDrawTarget",
            "com.corrodinggames.rts.gameFramework.m.l"
    };
    private static final String[] ANDROID_CANVAS_DRAW_TARGET_CLASSES = {
            "rustedwarfare.render.AndroidCanvasDrawTarget",
            "com.corrodinggames.rts.gameFramework.m.j"
    };
    private static final String[] GL_CANVAS_DRAW_TARGET_CLASSES = {
            "rustedwarfare.render.GlCanvasDrawTarget",
            "com.corrodinggames.rts.gameFramework.m.k"
    };
    private static final String[] NO_OP_CANVAS_DRAW_TARGET_CLASSES = {
            "rustedwarfare.render.NoOpCanvasDrawTarget",
            "com.corrodinggames.rts.gameFramework.m.n"
    };
    private static final String[] QUEUED_CANVAS_DRAW_TARGET_CLASSES = {
            "rustedwarfare.render.QueuedCanvasDrawTarget",
            "com.corrodinggames.rts.gameFramework.m.o"
    };
    private static final String[] CANVAS_OPERATION_CLASSES = {
            "rustedwarfare.render.CanvasOperation",
            "com.corrodinggames.rts.gameFramework.m.p"
    };
    private static final String[] REUSABLE_DRAW_OBJECT_POOL_CLASSES = {
            "rustedwarfare.render.ReusableDrawObjectPool",
            "com.corrodinggames.rts.gameFramework.m.q"
    };
    private static final String[] QUEUED_CANVAS_COMMAND_CLASSES = {
            "rustedwarfare.render.QueuedCanvasCommand",
            "com.corrodinggames.rts.gameFramework.m.s"
    };
    private static final String[] QUEUED_CANVAS_COMMAND_POOL_CLASSES = {
            "rustedwarfare.render.QueuedCanvasCommandPool",
            "com.corrodinggames.rts.gameFramework.m.t"
    };
    private static final String[] SHADER_BLEND_MODE_CLASSES = {
            "rustedwarfare.render.ShaderBlendMode",
            "com.corrodinggames.rts.gameFramework.m.w"
    };
    private static final String[] SHADER_COLOR_FILTER_CLASSES = {
            "rustedwarfare.render.ShaderColorFilter",
            "com.corrodinggames.rts.gameFramework.m.v"
    };
    private static final String[] SHADER_PROGRAM_CLASSES = {
            "rustedwarfare.render.ShaderProgram",
            "com.corrodinggames.rts.gameFramework.m.ae"
    };
    private static final String[] SHADER_PARAMETER_CLASSES = {
            "rustedwarfare.render.ShaderParameter",
            "com.corrodinggames.rts.gameFramework.m.af"
    };

    private static final Alias[] SHADER_BLEND_MODE_ALIASES = {
            new Alias("normal", new String[]{"normal", "a"}),
            new Alias("copy", new String[]{"copy", "b"}),
            new Alias("additive", new String[]{"additive", "c"})
    };

    private RenderCanvasDiagnostics() {
    }

    public static boolean isCanvasDrawTarget(Object value) {
        return value != null && RustedReflection.isAnyClass(value.getClass(), CANVAS_DRAW_TARGET_CLASSES);
    }

    public static boolean isQueuedCanvasDrawTarget(Object value) {
        return value != null && RustedReflection.isAnyClass(value.getClass(), QUEUED_CANVAS_DRAW_TARGET_CLASSES);
    }

    public static boolean isGlCanvasDrawTarget(Object value) {
        return value != null && RustedReflection.isAnyClass(value.getClass(), GL_CANVAS_DRAW_TARGET_CLASSES);
    }

    public static boolean isNoOpCanvasDrawTarget(Object value) {
        return value != null && RustedReflection.isAnyClass(value.getClass(), NO_OP_CANVAS_DRAW_TARGET_CLASSES);
    }

    public static boolean isCanvasOperation(Object value) {
        return value != null && RustedReflection.isAnyClass(value.getClass(), CANVAS_OPERATION_CLASSES);
    }

    public static boolean isShaderProgram(Object value) {
        return value != null && RustedReflection.isAnyClass(value.getClass(), SHADER_PROGRAM_CLASSES);
    }

    public static Map<String, Object> describeCanvasDrawTarget(Object target) {
        requireCanvasDrawTarget(target);
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        boolean queued = isQueuedCanvasDrawTarget(target);
        boolean androidCanvas = isAny(target, ANDROID_CANVAS_DRAW_TARGET_CLASSES);
        boolean glCanvas = isAny(target, GL_CANVAS_DRAW_TARGET_CLASSES);
        boolean noOp = isAny(target, NO_OP_CANVAS_DRAW_TARGET_CLASSES);
        result.put("className", target.getClass().getName());
        result.put("queued", Boolean.valueOf(queued));
        result.put("queuedCanvasDrawTarget", Boolean.valueOf(queued));
        result.put("androidCanvas", Boolean.valueOf(androidCanvas));
        result.put("androidCanvasDrawTarget", Boolean.valueOf(androidCanvas));
        result.put("glCanvas", Boolean.valueOf(glCanvas));
        result.put("glCanvasDrawTarget", Boolean.valueOf(glCanvas));
        result.put("noOp", Boolean.valueOf(noOp));
        result.put("noOpCanvasDrawTarget", Boolean.valueOf(noOp));
        putOptionalBooleanField(result, target, "recordingEnabled",
                new String[]{"recordingEnabled", "k", "o", "b", "a"});
        if (queued) {
            putOptionalField(result, target, "target", new String[]{"target", "a"});
            putOptionalIntField(result, target, "saveStackSize", new String[]{"saveStackSize", "b"});
            putOptionalIntField(result, target, "commandCount", new String[]{"commandCount", "j"});
        }
        if (androidCanvas) {
            putOptionalField(result, target, "canvas", new String[]{"canvas", "a"});
        }
        if (glCanvas) {
            putOptionalField(result, target, "glCanvas", new String[]{"glCanvas", "a"});
            putOptionalField(result, target, "glRenderer", new String[]{"glRenderer", "b"});
            putOptionalField(result, target, "scratchGlPaint", new String[]{"scratchGlPaint", "c"});
            putOptionalField(result, target, "scratchSrcRect", new String[]{"scratchSrcRect", "e"});
            putOptionalField(result, target, "scratchDstRectF", new String[]{"scratchDstRectF", "f"});
            putOptionalField(result, target, "currentBitmapImage", new String[]{"currentBitmapImage", "n"});
            putOptionalBooleanField(result, target, "recordingEnabled",
                    new String[]{"recordingEnabled", "o"});
        }
        return Collections.unmodifiableMap(result);
    }

    public static Object glCanvasFromGlCanvasDrawTarget(Object target) {
        requireAny(target, GL_CANVAS_DRAW_TARGET_CLASSES, "GlCanvasDrawTarget");
        return RustedReflection.getFieldValue(target, new String[]{"glCanvas", "a"});
    }

    public static Object glRendererFromGlCanvasDrawTarget(Object target) {
        requireAny(target, GL_CANVAS_DRAW_TARGET_CLASSES, "GlCanvasDrawTarget");
        return RustedReflection.getFieldValue(target, new String[]{"glRenderer", "b"});
    }

    public static Object scratchGlPaintFromGlCanvasDrawTarget(Object target) {
        requireAny(target, GL_CANVAS_DRAW_TARGET_CLASSES, "GlCanvasDrawTarget");
        return RustedReflection.getFieldValue(target, new String[]{"scratchGlPaint", "c"});
    }

    public static Object currentBitmapImageFromGlCanvasDrawTarget(Object target) {
        requireAny(target, GL_CANVAS_DRAW_TARGET_CLASSES, "GlCanvasDrawTarget");
        return RustedReflection.getFieldValue(target, new String[]{"currentBitmapImage", "n"});
    }

    public static boolean isGlCanvasDrawTargetRecording(Object target) {
        requireAny(target, GL_CANVAS_DRAW_TARGET_CLASSES, "GlCanvasDrawTarget");
        return RustedReflection.getBooleanField(target, new String[]{"recordingEnabled", "o"});
    }

    public static void bindImageTexture(Object target, Object image) {
        requireAny(target, GL_CANVAS_DRAW_TARGET_CLASSES, "GlCanvasDrawTarget");
        RustedReflection.invokeInstance(target, new String[]{"bindImageTexture", "b"}, image);
    }

    public static void flushGlState(Object target) {
        requireAny(target, GL_CANVAS_DRAW_TARGET_CLASSES, "GlCanvasDrawTarget");
        RustedReflection.invokeInstance(target, new String[]{"flushGlState", "d"});
    }

    public static Object getGlPaintState(Object target, Object paint) {
        requireAny(target, GL_CANVAS_DRAW_TARGET_CLASSES, "GlCanvasDrawTarget");
        return RustedReflection.invokeInstance(target, new String[]{"getGlPaintState", "a"}, paint);
    }

    public static Object getOrCreateGlTexture(Object target, Object bitmap, Object image) {
        requireAny(target, GL_CANVAS_DRAW_TARGET_CLASSES, "GlCanvasDrawTarget");
        return RustedReflection.invokeInstance(target, new String[]{"getOrCreateGlTexture", "a"}, bitmap, image);
    }

    public static Object ensureFloatArrayCapacity(Object target, int capacity) {
        requireAny(target, GL_CANVAS_DRAW_TARGET_CLASSES, "GlCanvasDrawTarget");
        return RustedReflection.invokeInstance(target, new String[]{"ensureFloatArrayCapacity", "b"},
                Integer.valueOf(capacity));
    }

    public static Map<String, Object> describeQueuedCanvasDrawTarget(Object target) {
        requireQueuedCanvasDrawTarget(target);
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        putField(result, target, "target", new String[]{"target", "a"});
        putIntField(result, target, "saveStackSize", new String[]{"saveStackSize", "b"});
        putField(result, target, "objectPools", new String[]{"objectPools", "c"});
        putField(result, target, "paintPool", new String[]{"paintPool", "d"});
        putField(result, target, "rectPool", new String[]{"rectPool", "e"});
        putField(result, target, "rectFPool", new String[]{"rectFPool", "f"});
        putField(result, target, "matrixPool", new String[]{"matrixPool", "g"});
        putField(result, target, "markerPool", new String[]{"markerPool", "h"});
        Object commandPool = RustedReflection.getFieldValue(target, new String[]{"commandPool", "i"});
        result.put("commandPool", commandPool);
        putIntField(result, target, "commandCount", new String[]{"commandCount", "j"});
        putBooleanField(result, target, "recordingEnabled", new String[]{"recordingEnabled", "k"});
        if (commandPool != null) {
            result.put("commandPoolSize", Integer.valueOf(commandPoolSize(commandPool)));
            result.put("commandPoolCapacity", Integer.valueOf(commandPoolCapacity(commandPool)));
        }
        return Collections.unmodifiableMap(result);
    }

    public static List<Object> queuedCanvasCommandsSnapshot(Object target) {
        requireQueuedCanvasDrawTarget(target);
        Object commandPool = RustedReflection.getFieldValue(target, new String[]{"commandPool", "i"});
        if (commandPool == null) {
            return Collections.emptyList();
        }
        int commandCount = Math.max(0, RustedReflection.getIntField(target, new String[]{"commandCount", "j"}));
        int size = commandCount > 0 ? commandCount : commandPoolSize(commandPool);
        Object commands = RustedReflection.getFieldValue(commandPool, new String[]{"commands", "b"});
        return boundedArraySnapshot(commands, size);
    }

    public static Map<String, Object> describeQueuedCanvasCommandPool(Object pool) {
        requireAny(pool, QUEUED_CANVAS_COMMAND_POOL_CLASSES, "QueuedCanvasCommandPool");
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        putIntField(result, pool, "size", new String[]{"size", "a"});
        putArrayLengthField(result, pool, "commandsLength", new String[]{"commands", "b"});
        return Collections.unmodifiableMap(result);
    }

    public static Map<String, Object> describeReusableDrawObjectPool(Object pool) {
        requireAny(pool, REUSABLE_DRAW_OBJECT_POOL_CLASSES, "ReusableDrawObjectPool");
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        putField(result, pool, "pooledObjects", new String[]{"pooledObjects", "a"});
        putIntField(result, pool, "borrowedCount", new String[]{"borrowedCount", "b"});
        putField(result, pool, "objectClass", new String[]{"objectClass", "c"});
        return Collections.unmodifiableMap(result);
    }

    public static Map<String, Object> describeQueuedCanvasCommand(Object command) {
        requireAny(command, QUEUED_CANVAS_COMMAND_CLASSES, "QueuedCanvasCommand");
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        Object operation = RustedReflection.getFieldValue(command, new String[]{"operation", "a"});
        result.put("operation", operation);
        result.put("operationName", canvasOperationName(operation));
        putField(result, command, "args", new String[]{"args", "b"});
        putArrayLengthField(result, command, "argsLength", new String[]{"args", "b"});
        putFloatField(result, command, "floatArg0", new String[]{"floatArg0", "c"});
        putFloatField(result, command, "floatArg1", new String[]{"floatArg1", "d"});
        putFloatField(result, command, "floatArg2", new String[]{"floatArg2", "e"});
        putFloatField(result, command, "floatArg3", new String[]{"floatArg3", "f"});
        putField(result, command, "owner", new String[]{"owner", "g"});
        return Collections.unmodifiableMap(result);
    }

    public static List<Object> canvasOperations() {
        return Collections.unmodifiableList(enumValues(CANVAS_OPERATION_CLASSES, new String[]{"$VALUES", "au"}));
    }

    public static List<String> canvasOperationNames() {
        List<String> result = new ArrayList<String>();
        for (Object value : canvasOperations()) {
            result.add(canvasOperationName(value));
        }
        return Collections.unmodifiableList(result);
    }

    public static String canvasOperationName(Object operation) {
        if (operation == null) {
            return "";
        }
        if (operation instanceof Enum) {
            return ((Enum<?>) operation).name();
        }
        return operation.toString();
    }

    public static boolean setRecordingEnabled(Object target, boolean recordingEnabled) {
        requireQueuedCanvasDrawTarget(target);
        RustedReflection.invokeInstance(target, new String[]{"setRecordingEnabled", "a"},
                Boolean.valueOf(recordingEnabled));
        return isRecordingEnabled(target);
    }

    public static boolean isRecordingEnabled(Object target) {
        requireQueuedCanvasDrawTarget(target);
        return Boolean.TRUE.equals(RustedReflection.invokeInstance(target, new String[]{"isRecordingEnabled", "c"}));
    }

    public static boolean compileShader(Object target, Object shaderProgram) {
        requireCanvasDrawTarget(target);
        requireShaderProgram(shaderProgram);
        Object value = RustedReflection.invokeInstance(target, new String[]{"compileShader", "a"}, shaderProgram);
        return Boolean.TRUE.equals(value);
    }

    public static List<String> shaderBlendModeNames() {
        List<String> result = new ArrayList<String>();
        for (Alias alias : SHADER_BLEND_MODE_ALIASES) {
            result.add(alias.name);
        }
        return Collections.unmodifiableList(result);
    }

    public static List<Object> shaderBlendModes() {
        List<Object> result = new ArrayList<Object>();
        for (Alias alias : SHADER_BLEND_MODE_ALIASES) {
            result.add(RustedReflection.getStaticFieldValue(SHADER_BLEND_MODE_CLASSES, alias.fieldNames));
        }
        return Collections.unmodifiableList(result);
    }

    public static Object shaderBlendMode(String name) {
        String normalized = normalize(name);
        for (Alias alias : SHADER_BLEND_MODE_ALIASES) {
            if (alias.name.equals(normalized)) {
                return RustedReflection.getStaticFieldValue(SHADER_BLEND_MODE_CLASSES, alias.fieldNames);
            }
        }
        throw new IllegalArgumentException("Unknown shader blend mode: " + name);
    }

    public static String canonicalShaderBlendModeName(Object blendMode) {
        requireAny(blendMode, SHADER_BLEND_MODE_CLASSES, "ShaderBlendMode");
        for (Alias alias : SHADER_BLEND_MODE_ALIASES) {
            Object value = RustedReflection.getStaticFieldValue(SHADER_BLEND_MODE_CLASSES, alias.fieldNames);
            if (value == blendMode || value.equals(blendMode)) {
                return alias.name;
            }
        }
        return blendMode.toString();
    }

    public static Map<String, Object> describeShaderColorFilter(Object colorFilter) {
        requireAny(colorFilter, SHADER_COLOR_FILTER_CLASSES, "ShaderColorFilter");
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        Object blendMode = RustedReflection.getFieldValue(colorFilter, new String[]{"blendMode", "a"});
        result.put("blendMode", blendMode);
        result.put("blendModeName", canonicalShaderBlendModeName(blendMode));
        return Collections.unmodifiableMap(result);
    }

    public static Map<String, Object> describeShaderProgram(Object shaderProgram) {
        requireShaderProgram(shaderProgram);
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        putField(result, shaderProgram, "name", new String[]{"name", "c"});
        putField(result, shaderProgram, "vertexSource", new String[]{"vertexSource", "e"});
        putField(result, shaderProgram, "fragmentSource", new String[]{"fragmentSource", "f"});
        putField(result, shaderProgram, "vertexShaderPath", new String[]{"vertexShaderPath", "i"});
        putField(result, shaderProgram, "fragmentShaderPath", new String[]{"fragmentShaderPath", "j"});
        putField(result, shaderProgram, "vertexLastModified", new String[]{"vertexLastModified", "k"});
        putField(result, shaderProgram, "fragmentLastModified", new String[]{"fragmentLastModified", "l"});
        putBooleanField(result, shaderProgram, "reloadPending", new String[]{"reloadPending", "m"});
        putIntField(result, shaderProgram, "compileFailureState", new String[]{"compileFailureState", "o"});
        putArrayLengthField(result, shaderProgram, "parametersLength", new String[]{"parameters", "p"});
        putField(result, shaderProgram, "backendShaderObject", new String[]{"backendShaderObject", "q"});
        putIntField(result, shaderProgram, "warningCount", new String[]{"warningCount", "r"});
        putIntField(result, shaderProgram, "reloadCheckCounter", new String[]{"reloadCheckCounter", "s"});
        return Collections.unmodifiableMap(result);
    }

    public static List<Object> shaderParametersSnapshot(Object shaderProgram) {
        requireShaderProgram(shaderProgram);
        return snapshotField(shaderProgram, new String[]{"parameters", "p"});
    }

    public static Object getOrCreateShaderParameter(Object shaderProgram, String name) {
        requireShaderProgram(shaderProgram);
        return RustedReflection.invokeInstance(shaderProgram, new String[]{"getOrCreateParameter", "a"}, name);
    }

    public static void setUniformFloat(Object shaderProgram, String name, float value) {
        requireShaderProgram(shaderProgram);
        RustedReflection.invokeInstance(shaderProgram, new String[]{"setUniformFloat", "a"}, name, Float.valueOf(value));
    }

    public static void setUniformVec2(Object shaderProgram, String name, float x, float y) {
        requireShaderProgram(shaderProgram);
        RustedReflection.invokeInstance(shaderProgram, new String[]{"setUniformVec2", "a"},
                name, Float.valueOf(x), Float.valueOf(y));
    }

    public static void setUniformColor(Object shaderProgram, String name, int color) {
        requireShaderProgram(shaderProgram);
        RustedReflection.invokeInstance(shaderProgram, new String[]{"setUniformColor", "a"},
                name, Integer.valueOf(color));
    }

    public static void setUniformTexture(Object shaderProgram, String name, Object image) {
        requireShaderProgram(shaderProgram);
        RustedReflection.invokeInstance(shaderProgram, new String[]{"setUniformTexture", "a"}, name, image);
    }

    public static void setUniformTextureSecondary(Object shaderProgram, String name, Object image) {
        requireShaderProgram(shaderProgram);
        RustedReflection.invokeInstance(shaderProgram, new String[]{"setUniformTextureSecondary", "b"}, name, image);
    }

    public static void loadShaderSources(Object shaderProgram, String vertexSource, String fragmentSource) {
        requireShaderProgram(shaderProgram);
        RustedReflection.invokeInstance(shaderProgram, new String[]{"loadSources", "a"}, vertexSource, fragmentSource);
    }

    public static void reloadShaderSources(Object shaderProgram) {
        requireShaderProgram(shaderProgram);
        RustedReflection.invokeInstance(shaderProgram, new String[]{"reloadSources", "d"});
    }

    public static boolean checkShaderSourceFilesChanged(Object shaderProgram) {
        requireShaderProgram(shaderProgram);
        return Boolean.TRUE.equals(RustedReflection.invokeInstance(shaderProgram,
                new String[]{"checkSourceFilesChanged", "e"}));
    }

    public static void hotReloadShaderIfNeeded(Object shaderProgram) {
        requireShaderProgram(shaderProgram);
        RustedReflection.invokeInstance(shaderProgram, new String[]{"hotReloadIfNeeded", "f"});
    }

    public static void compileShaderOnGraphicsEngine(Object shaderProgram) {
        requireShaderProgram(shaderProgram);
        RustedReflection.invokeInstance(shaderProgram, new String[]{"compileOnGraphicsEngine", "c"});
    }

    public static Map<String, Object> describeShaderParameter(Object parameter) {
        requireAny(parameter, SHADER_PARAMETER_CLASSES, "ShaderParameter");
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        putField(result, parameter, "name", new String[]{"name", "a"});
        putIntField(result, parameter, "location", new String[]{"location", "b"});
        putBooleanField(result, parameter, "dirty", new String[]{"dirty", "c"});
        putBooleanField(result, parameter, "textureDirty", new String[]{"textureDirty", "d"});
        putField(result, parameter, "floatValues", new String[]{"floatValues", "e"});
        putArrayLengthField(result, parameter, "floatValuesLength", new String[]{"floatValues", "e"});
        putField(result, parameter, "texture", new String[]{"texture", "f"});
        putBooleanField(result, parameter, "secondaryTexture", new String[]{"secondaryTexture", "g"});
        return Collections.unmodifiableMap(result);
    }

    public static void setShaderParameterFloat(Object parameter, float value) {
        requireAny(parameter, SHADER_PARAMETER_CLASSES, "ShaderParameter");
        RustedReflection.invokeInstance(parameter, new String[]{"setFloat", "a"}, Float.valueOf(value));
    }

    public static void setShaderParameterVec2(Object parameter, float x, float y) {
        requireAny(parameter, SHADER_PARAMETER_CLASSES, "ShaderParameter");
        RustedReflection.invokeInstance(parameter, new String[]{"setVec2", "a"}, Float.valueOf(x), Float.valueOf(y));
    }

    public static void setShaderParameterVec4(Object parameter, float x, float y, float z, float w) {
        requireAny(parameter, SHADER_PARAMETER_CLASSES, "ShaderParameter");
        RustedReflection.invokeInstance(parameter, new String[]{"setVec4", "a"},
                Float.valueOf(x), Float.valueOf(y), Float.valueOf(z), Float.valueOf(w));
    }

    public static void setShaderParameterTexture(Object parameter, Object image) {
        requireAny(parameter, SHADER_PARAMETER_CLASSES, "ShaderParameter");
        RustedReflection.invokeInstance(parameter, new String[]{"setTexture", "a"}, image);
    }

    public static void setShaderParameterTextureSecondary(Object parameter, Object image) {
        requireAny(parameter, SHADER_PARAMETER_CLASSES, "ShaderParameter");
        RustedReflection.invokeInstance(parameter, new String[]{"setTextureSecondary", "b"}, image);
    }

    private static int commandPoolSize(Object commandPool) {
        return RustedReflection.getIntField(commandPool, new String[]{"size", "a"});
    }

    private static int commandPoolCapacity(Object commandPool) {
        return arrayLength(RustedReflection.getFieldValue(commandPool, new String[]{"commands", "b"}));
    }

    private static List<Object> enumValues(String[] classNames, String[] valuesFieldNames) {
        Object values = fieldValueOrNull(classNames, valuesFieldNames);
        if (values == null) {
            values = staticInvokeOrNull(classNames, new String[]{"values"});
        }
        return RustedReflection.snapshotIterable(values);
    }

    private static Object fieldValueOrNull(String[] classNames, String[] fieldNames) {
        try {
            return RustedReflection.getStaticFieldValue(classNames, fieldNames);
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private static Object staticInvokeOrNull(String[] classNames, String[] methodNames) {
        try {
            return RustedReflection.invokeStatic(classNames, methodNames);
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private static List<Object> snapshotField(Object owner, String[] fieldNames) {
        return Collections.unmodifiableList(RustedReflection.snapshotIterable(
                RustedReflection.getFieldValue(owner, fieldNames)));
    }

    private static List<Object> boundedArraySnapshot(Object array, int count) {
        if (array == null || !array.getClass().isArray()) {
            return Collections.emptyList();
        }
        int length = Math.min(Math.max(0, count), Array.getLength(array));
        List<Object> result = new ArrayList<Object>(length);
        for (int i = 0; i < length; i++) {
            Object value = Array.get(array, i);
            if (value != null) {
                result.add(value);
            }
        }
        return Collections.unmodifiableList(result);
    }

    private static int arrayLength(Object array) {
        return array != null && array.getClass().isArray() ? Array.getLength(array) : 0;
    }

    private static boolean isAny(Object value, String[] classNames) {
        return value != null && RustedReflection.isAnyClass(value.getClass(), classNames);
    }

    private static void requireCanvasDrawTarget(Object value) {
        requireAny(value, CANVAS_DRAW_TARGET_CLASSES, "CanvasDrawTarget");
    }

    private static void requireQueuedCanvasDrawTarget(Object value) {
        requireAny(value, QUEUED_CANVAS_DRAW_TARGET_CLASSES, "QueuedCanvasDrawTarget");
    }

    private static void requireShaderProgram(Object value) {
        requireAny(value, SHADER_PROGRAM_CLASSES, "ShaderProgram");
    }

    private static void requireAny(Object value, String[] classNames, String label) {
        if (value == null || !RustedReflection.isAnyClass(value.getClass(), classNames)) {
            throw new IllegalArgumentException("Expected " + label + ", got " + describe(value));
        }
    }

    private static String describe(Object value) {
        return value == null ? "null" : value.getClass().getName();
    }

    private static void putField(Map<String, Object> result, Object owner, String key, String[] fieldNames) {
        result.put(key, RustedReflection.getFieldValue(owner, fieldNames));
    }

    private static void putIntField(Map<String, Object> result, Object owner, String key, String[] fieldNames) {
        result.put(key, Integer.valueOf(RustedReflection.getIntField(owner, fieldNames)));
    }

    private static void putFloatField(Map<String, Object> result, Object owner, String key, String[] fieldNames) {
        result.put(key, Float.valueOf(RustedReflection.getFloatField(owner, fieldNames)));
    }

    private static void putBooleanField(Map<String, Object> result, Object owner, String key, String[] fieldNames) {
        result.put(key, Boolean.valueOf(RustedReflection.getBooleanField(owner, fieldNames)));
    }

    private static void putArrayLengthField(Map<String, Object> result, Object owner, String key,
                                            String[] fieldNames) {
        result.put(key, Integer.valueOf(arrayLength(RustedReflection.getFieldValue(owner, fieldNames))));
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

    private static void putOptionalBooleanField(Map<String, Object> result, Object owner, String key,
                                                String[] fieldNames) {
        try {
            result.put(key, Boolean.valueOf(RustedReflection.getBooleanField(owner, fieldNames)));
        } catch (RuntimeException ignored) {
        }
    }

    private static String normalize(String value) {
        return value != null ? value.trim().toLowerCase(Locale.ROOT) : "";
    }

    private static final class Alias {
        private final String name;
        private final String[] fieldNames;

        private Alias(String name, String[] fieldNames) {
            this.name = name;
            this.fieldNames = fieldNames;
        }
    }
}
