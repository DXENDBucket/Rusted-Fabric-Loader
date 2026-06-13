package io.github.endx.rustedfabricapi.api.diagnostic;

import io.github.endx.rustedfabricapi.api.util.RustedReflection;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class SlickRuntimeDiagnostics {
    private static final String[] SLICK_GAME_CLASSES = {
            "rustedwarfare.client.SlickGame",
            "com.corrodinggames.rts.java.u"
    };

    private SlickRuntimeDiagnostics() {
    }

    public static boolean isSlickGame(Object value) {
        return value != null && RustedReflection.isAnyClass(value.getClass(), SLICK_GAME_CLASSES);
    }

    public static Map<String, Object> describeSlickGame(Object slickGame) {
        requireSlickGame(slickGame);
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("className", slickGame.getClass().getName());
        putField(result, slickGame, "gameContainer", new String[]{"gameContainer", "a"});
        putField(result, slickGame, "main", new String[]{"main", "b"});
        putField(result, slickGame, "appGameContainer", new String[]{"appGameContainer", "c"});
        putField(result, slickGame, "graphicsContext", new String[]{"graphicsContext", "d"});
        putField(result, slickGame, "gameEngine", new String[]{"gameEngine", "e"});
        putField(result, slickGame, "appFramework", new String[]{"appFramework", "f"});
        putField(result, slickGame, "loadLock", new String[]{"loadLock", "h"});
        putBooleanField(result, slickGame, "noDisplay", new String[]{"noDisplay", "i"});
        putBooleanField(result, slickGame, "noSound", new String[]{"noSound", "j"});
        putBooleanField(result, slickGame, "noMusic", new String[]{"noMusic", "k"});
        putBooleanField(result, slickGame, "useScalableGame", new String[]{"useScalableGame", "l"});
        putField(result, slickGame, "loadingLogo", new String[]{"loadingLogo", "m"});
        putField(result, slickGame, "pointerImage", new String[]{"pointerImage", "n"});
        putBooleanField(result, slickGame, "threadedLoadStarted", new String[]{"threadedLoadStarted", "p"});
        putBooleanField(result, slickGame, "nonThreadedLoadStarted", new String[]{"nonThreadedLoadStarted", "q"});
        putBooleanField(result, slickGame, "finishedInitialLoad", new String[]{"finishedInitialLoad", "r"});
        putBooleanField(result, slickGame, "loadingScreenDrawn", new String[]{"loadingScreenDrawn", "s"});
        putIntField(result, slickGame, "lastDeltaMs", new String[]{"lastDeltaMs", "t"});
        putBooleanField(result, slickGame, "mouseGrabbed", new String[]{"mouseGrabbed", "v"});
        return Collections.unmodifiableMap(result);
    }

    public static Object gameContainer(Object slickGame) {
        requireSlickGame(slickGame);
        return RustedReflection.getFieldValue(slickGame, new String[]{"gameContainer", "a"});
    }

    public static Object graphicsContext(Object slickGame) {
        requireSlickGame(slickGame);
        return RustedReflection.getFieldValue(slickGame, new String[]{"graphicsContext", "d"});
    }

    public static Object gameEngine(Object slickGame) {
        requireSlickGame(slickGame);
        return RustedReflection.getFieldValue(slickGame, new String[]{"gameEngine", "e"});
    }

    public static int lastDeltaMs(Object slickGame) {
        requireSlickGame(slickGame);
        return RustedReflection.getIntField(slickGame, new String[]{"lastDeltaMs", "t"});
    }

    private static void requireSlickGame(Object value) {
        if (value == null || !RustedReflection.isAnyClass(value.getClass(), SLICK_GAME_CLASSES)) {
            throw new IllegalArgumentException("Expected SlickGame, got " + describe(value));
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

    private static void putIntField(Map<String, Object> result, Object owner, String key, String[] fieldNames) {
        try {
            result.put(key, Integer.valueOf(RustedReflection.getIntField(owner, fieldNames)));
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
