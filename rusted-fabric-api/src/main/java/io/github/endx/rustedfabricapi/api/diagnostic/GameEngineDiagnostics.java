package io.github.endx.rustedfabricapi.api.diagnostic;

import io.github.endx.rustedfabricapi.api.util.RustedReflection;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class GameEngineDiagnostics {
    private static final String[] GAME_ENGINE_CLASSES = {
            "rustedwarfare.core.GameEngine",
            "com.corrodinggames.rts.gameFramework.l"
    };
    private static final String[] GRAPHICS_ENGINE_CLASSES = {
            "rustedwarfare.render.GraphicsEngine",
            "com.corrodinggames.rts.gameFramework.m.y"
    };

    private GameEngineDiagnostics() {
    }

    public static Object currentEngine() {
        return RustedReflection.invokeStatic(GAME_ENGINE_CLASSES, new String[]{"getInstance", "B"});
    }

    public static Object currentEngineOrNull() {
        try {
            return currentEngine();
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    public static boolean isGameEngine(Object value) {
        return value != null && RustedReflection.isAnyClass(value.getClass(), GAME_ENGINE_CLASSES);
    }

    public static boolean isGraphicsEngine(Object value) {
        return value != null && RustedReflection.isAnyClass(value.getClass(), GRAPHICS_ENGINE_CLASSES);
    }

    public static Map<String, Object> describeCurrentEngine() {
        Object engine = currentEngineOrNull();
        return engine != null ? describeEngine(engine) : Collections.emptyMap();
    }

    public static Map<String, Object> describeEngine(Object engine) {
        requireGameEngine(engine);
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("className", engine.getClass().getName());
        putBooleanField(result, engine, "noResources", new String[]{"noResources", "aB"});
        putBooleanField(result, engine, "disableTextureAtlas", new String[]{"disableTextureAtlas", "aC"});
        putBooleanField(result, engine, "useCanvasGl", new String[]{"useCanvasGl", "aD"});
        putBooleanField(result, engine, "printUnits", new String[]{"printUnits", "aE"});
        putBooleanField(result, engine, "outputUnitImages", new String[]{"outputUnitImages", "aF"});
        putBooleanField(result, engine, "oldReplays", new String[]{"oldReplays", "aG"});
        putBooleanField(result, engine, "steamEnabled", new String[]{"steamEnabled", "aH"});
        putBooleanField(result, engine, "sandboxMode", new String[]{"sandboxMode", "aI"});
        putBooleanField(result, engine, "disableMods", new String[]{"disableMods", "aJ"});
        putStringField(result, engine, "pendingConnectLobby", new String[]{"pendingConnectLobby", "aK"});
        putBooleanField(result, engine, "postProcessing", new String[]{"postProcessing", "aM"});
        putBooleanField(result, engine, "teamShaders", new String[]{"teamShaders", "aN"});
        putBooleanField(result, engine, "safeMode", new String[]{"safeMode", "aO"});
        putBooleanField(result, engine, "extraSafeMode", new String[]{"extraSafeMode", "aP"});
        putStringField(result, engine, "devDebug", new String[]{"devDebug", "aQ"});
        putBooleanField(result, engine, "desktopRuntime", new String[]{"desktopRuntime", "aU"});
        putBooleanField(result, engine, "useHardwareRendering", new String[]{"useHardwareRendering", "aW"});
        putBooleanField(result, engine, "useDesktopOpenGL", new String[]{"useDesktopOpenGL", "aX"});
        putBooleanField(result, engine, "replayDebug", new String[]{"replayDebug", "aw"});
        putBooleanField(result, engine, "logColorEnabled", new String[]{"logColorEnabled", "ax"});
        putBooleanField(result, engine, "noBackgroundMode", new String[]{"noBackgroundMode", "ay"});
        putBooleanField(result, engine, "gameEngineInitStarted", new String[]{"gameEngineInitStarted", "bb"});
        putField(result, engine, "graphicsEngine", new String[]{"graphicsEngine", "bO"});
        putField(result, engine, "mapEngine", new String[]{"mapEngine", "bL"});
        putField(result, engine, "settings", new String[]{"settings", "bQ"});
        putField(result, engine, "interfaceEngine", new String[]{"interfaceEngine", "bS"});
        putField(result, engine, "pathEngine", new String[]{"pathEngine", "bU"});
        putField(result, engine, "minimap", new String[]{"minimap", "bW"});
        putField(result, engine, "networkEngine", new String[]{"networkEngine", "bX"});
        putField(result, engine, "statsEngine", new String[]{"statsEngine", "bY"});
        putField(result, engine, "modManager", new String[]{"modManager", "bZ"});
        putField(result, engine, "rendererClass", new String[]{"rendererClass", "bg"});
        putField(result, engine, "gameSaver", new String[]{"gameSaver", "ca"});
        putField(result, engine, "replayEngine", new String[]{"replayEngine", "cb"});
        putField(result, engine, "unitSpatialIndex", new String[]{"unitSpatialIndex", "cc"});
        putField(result, engine, "missionEngine", new String[]{"missionEngine", "ce"});
        putField(result, engine, "commandController", new String[]{"commandController", "cf"});
        putField(result, engine, "initialScreenSize", new String[]{"initialScreenSize", "ck"});
        putStringField(result, engine, "currentMapPath", new String[]{"currentMapPath", "dl"});
        putField(result, engine, "currentMapInputStream", new String[]{"currentMapInputStream", "dm"});
        putStringField(result, engine, "buildNumber", new String[]{"buildNumber", "dz"});
        putField(result, engine, "effectEngine", new String[]{"effectEngine", "bR"});
        result.put("currentMapPathMethod", invokeStringOrEmpty(engine, new String[]{"getCurrentMapPath", "ak"}));
        result.put("currentMapDisplayName", invokeStringOrEmpty(engine, new String[]{"getCurrentMapDisplayName", "al"}));
        result.put("currentMapBaseName", invokeStringOrEmpty(engine, new String[]{"getCurrentMapBaseName", "am"}));
        return Collections.unmodifiableMap(result);
    }

    public static Object currentGraphicsEngine() {
        Object engine = currentEngineOrNull();
        return engine != null ? graphicsEngine(engine) : null;
    }

    public static Object graphicsEngine(Object engine) {
        requireGameEngine(engine);
        return RustedReflection.getFieldValue(engine, new String[]{"graphicsEngine", "bO"});
    }

    public static Object settings(Object engine) {
        requireGameEngine(engine);
        return RustedReflection.getFieldValue(engine, new String[]{"settings", "bQ"});
    }

    public static Object mapEngine(Object engine) {
        requireGameEngine(engine);
        return RustedReflection.getFieldValue(engine, new String[]{"mapEngine", "bL"});
    }

    public static Object minimap(Object engine) {
        requireGameEngine(engine);
        return RustedReflection.getFieldValue(engine, new String[]{"minimap", "bW"});
    }

    public static String currentMapPath(Object engine) {
        requireGameEngine(engine);
        String path = invokeStringOrEmpty(engine, new String[]{"getCurrentMapPath", "ak"});
        if (path == null || path.trim().isEmpty()) {
            path = RustedReflection.getStringField(engine, new String[]{"currentMapPath", "dl"});
        }
        return path;
    }

    public static String currentMapDisplayName(Object engine) {
        requireGameEngine(engine);
        return invokeStringOrEmpty(engine, new String[]{"getCurrentMapDisplayName", "al"});
    }

    public static String currentMapBaseName(Object engine) {
        requireGameEngine(engine);
        return invokeStringOrEmpty(engine, new String[]{"getCurrentMapBaseName", "am"});
    }

    public static void addMessage(String title, String message) {
        Object engine = currentEngineOrNull();
        if (engine != null) {
            RustedReflection.invokeInstance(engine, new String[]{"addMessage", "f"}, title, message);
        }
    }

    private static String invokeStringOrEmpty(Object owner, String[] methodNames, Object... args) {
        try {
            Object value = RustedReflection.invokeInstance(owner, methodNames, args);
            return value != null ? value.toString() : "";
        } catch (RuntimeException ignored) {
            return "";
        }
    }

    private static void requireGameEngine(Object value) {
        if (value == null || !RustedReflection.isAnyClass(value.getClass(), GAME_ENGINE_CLASSES)) {
            throw new IllegalArgumentException("Expected GameEngine, got " + describe(value));
        }
    }

    private static String describe(Object value) {
        return value == null ? "null" : value.getClass().getName();
    }

    private static void putField(Map<String, Object> result, Object owner, String key, String[] fieldNames) {
        try {
            result.put(key, RustedReflection.getFieldValue(owner, fieldNames));
        } catch (RuntimeException ignored) {
        }
    }

    private static void putStringField(Map<String, Object> result, Object owner, String key, String[] fieldNames) {
        try {
            result.put(key, RustedReflection.getStringField(owner, fieldNames));
        } catch (RuntimeException ignored) {
        }
    }

    private static void putBooleanField(Map<String, Object> result, Object owner, String key, String[] fieldNames) {
        try {
            result.put(key, Boolean.valueOf(RustedReflection.getBooleanField(owner, fieldNames)));
        } catch (RuntimeException ignored) {
        }
    }
}
