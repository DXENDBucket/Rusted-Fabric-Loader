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
        putBooleanField(result, engine, "isHeadlessMode", new String[]{"isHeadlessMode", "aB"});
        putBooleanField(result, engine, "isTextureAtlasDisabled", new String[]{"isTextureAtlasDisabled", "aC"});
        putBooleanField(result, engine, "isCanvasGLEnabled", new String[]{"isCanvasGLEnabled", "aD"});
        putBooleanField(result, engine, "isUnitImageGenerationMode", new String[]{"isUnitImageGenerationMode", "aE"});
        putBooleanField(result, engine, "isUnitValidationMode", new String[]{"isUnitValidationMode", "aF"});
        putBooleanField(result, engine, "isOldReplayMode", new String[]{"isOldReplayMode", "aG"});
        putBooleanField(result, engine, "isSteamModeEnabled", new String[]{"isSteamModeEnabled", "aH"});
        putBooleanField(result, engine, "isLaunchSandbox", new String[]{"isLaunchSandbox", "aI"});
        putBooleanField(result, engine, "isModsDisabled", new String[]{"isModsDisabled", "aJ"});
        putStringField(result, engine, "pendingSteamLobbyId", new String[]{"pendingSteamLobbyId", "aK"});
        putBooleanField(result, engine, "isPostProcessingEnabled", new String[]{"isPostProcessingEnabled", "aM"});
        putBooleanField(result, engine, "isTeamShadersEnabled", new String[]{"isTeamShadersEnabled", "aN"});
        putBooleanField(result, engine, "isCommandLineMode", new String[]{"isCommandLineMode", "aO"});
        putBooleanField(result, engine, "isAutomatedTesting", new String[]{"isAutomatedTesting", "aP"});
        putStringField(result, engine, "platformName", new String[]{"platformName", "aQ"});
        putBooleanField(result, engine, "isNonAndroidVersion", new String[]{"isNonAndroidVersion", "aU"});
        putBooleanField(result, engine, "isPCOrIOSVersion", new String[]{"isPCOrIOSVersion", "aW"});
        putBooleanField(result, engine, "isJavaDesktopVersion", new String[]{"isJavaDesktopVersion", "aX"});
        putBooleanField(result, engine, "isReplayDebugMode", new String[]{"isReplayDebugMode", "aw"});
        putBooleanField(result, engine, "isLogColorEnabled", new String[]{"isLogColorEnabled", "ax"});
        putBooleanField(result, engine, "isMenuBackgroundDisabled", new String[]{"isMenuBackgroundDisabled", "ay"});
        putBooleanField(result, engine, "isDesktopInitialized", new String[]{"isDesktopInitialized", "bb"});
        putBooleanField(result, engine, "isSafeMode", new String[]{"isSafeMode", "ee"});
        putBooleanField(result, engine, "isExtraSafeMode", new String[]{"isExtraSafeMode", "eh"});
        putBooleanField(result, engine, "isExtraSafeModeLevel2", new String[]{"isExtraSafeModeLevel2", "ei"});
        putField(result, engine, "graphicsEngine", new String[]{"graphicsEngine", "bh"});
        putField(result, engine, "renderGraphicsEngine", new String[]{"renderGraphicsEngine", "bO"});
        putField(result, engine, "tileMap", new String[]{"tileMap", "bL"});
        putField(result, engine, "settingsEngine", new String[]{"settingsEngine", "bQ"});
        putField(result, engine, "gameUI", new String[]{"gameUI", "bS"});
        putField(result, engine, "pathfindingEngine", new String[]{"pathfindingEngine", "bU"});
        putField(result, engine, "minimap", new String[]{"minimap", "bW"});
        putField(result, engine, "networkEngine", new String[]{"networkEngine", "bX"});
        putField(result, engine, "gameStatistics", new String[]{"gameStatistics", "bY"});
        putField(result, engine, "modManager", new String[]{"modManager", "bZ"});
        putField(result, engine, "gameEngineClass", new String[]{"gameEngineClass", "bg"});
        putField(result, engine, "gameSaver", new String[]{"gameSaver", "ca"});
        putField(result, engine, "replayEngine", new String[]{"replayEngine", "cb"});
        putField(result, engine, "unitSpatialIndex", new String[]{"unitSpatialIndex", "cc"});
        putField(result, engine, "missionEngine", new String[]{"missionEngine", "ce"});
        putField(result, engine, "commandController", new String[]{"commandController", "cf"});
        putField(result, engine, "screenSize", new String[]{"screenSize", "ck"});
        putStringField(result, engine, "currentMapPath", new String[]{"currentMapPath", "dl"});
        putField(result, engine, "remoteMapStream", new String[]{"remoteMapStream", "dm"});
        putStringField(result, engine, "buildVersion", new String[]{"buildVersion", "dz"});
        putField(result, engine, "effectManager", new String[]{"effectManager", "bR"});
        result.put("currentMapPathMethod", invokeStringOrEmpty(engine, new String[]{"getCurrentMapPath", "ak"}));
        result.put("currentMapDisplayName", invokeStringOrEmpty(engine, new String[]{"getCurrentMapDisplayName", "al"}));
        result.put("currentMapBaseName", invokeStringOrEmpty(engine, new String[]{"getCurrentMapBaseName", "am"}));
        return Collections.unmodifiableMap(result);
    }

    public static Object currentGraphicsEngine() {
        Object engine = currentEngineOrNull();
        return engine != null ? graphicsEngine(engine) : null;
    }

    public static Object currentInterfaceEngine() {
        return currentGameUI();
    }

    public static Object currentStatsEngine() {
        return currentGameStatistics();
    }

    public static Object currentGameUI() {
        Object engine = currentEngineOrNull();
        return engine != null ? gameUI(engine) : null;
    }

    public static Object currentGameStatistics() {
        Object engine = currentEngineOrNull();
        return engine != null ? gameStatistics(engine) : null;
    }

    public static Object graphicsEngine(Object engine) {
        requireGameEngine(engine);
        return RustedReflection.getFieldValue(engine, new String[]{"graphicsEngine", "bh"});
    }

    public static Object interfaceEngine(Object engine) {
        return gameUI(engine);
    }

    public static Object statsEngine(Object engine) {
        return gameStatistics(engine);
    }

    public static Object gameUI(Object engine) {
        requireGameEngine(engine);
        return RustedReflection.getFieldValue(engine, new String[]{"gameUI", "bS"});
    }

    public static Object gameStatistics(Object engine) {
        requireGameEngine(engine);
        return RustedReflection.getFieldValue(engine, new String[]{"gameStatistics", "bY"});
    }

    public static Object settings(Object engine) {
        return settingsEngine(engine);
    }

    public static Object settingsEngine(Object engine) {
        requireGameEngine(engine);
        return RustedReflection.getFieldValue(engine, new String[]{"settingsEngine", "bQ"});
    }

    public static Object mapEngine(Object engine) {
        return tileMap(engine);
    }

    public static Object tileMap(Object engine) {
        requireGameEngine(engine);
        return RustedReflection.getFieldValue(engine, new String[]{"tileMap", "bL"});
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
