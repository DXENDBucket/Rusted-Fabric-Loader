package io.github.endx.rustedfabricapi.api.diagnostic;

import io.github.endx.rustedfabricapi.api.util.RustedReflection;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class SteamRuntimeDiagnostics {
    private static final String[] STEAM_ENGINE_CLASSES = {
            "rustedwarfare.steam.SteamEngine",
            "com.corrodinggames.rts.gameFramework.o.a"
    };
    private static final String[] JAVA_STEAM_ENGINE_CLASSES = {
            "rustedwarfare.steam.JavaSteamEngine",
            "com.corrodinggames.rts.java.c.b"
    };
    private static final String[] STEAM_WORKSHOP_MANAGER_CLASSES = {
            "rustedwarfare.steam.SteamWorkshopManager",
            "com.corrodinggames.rts.java.c.g"
    };

    private SteamRuntimeDiagnostics() {
    }

    public static Object currentSteamEngine() {
        try {
            return RustedReflection.getStaticFieldValue(STEAM_ENGINE_CLASSES, new String[]{"instance", "a"});
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    public static Object currentWorkshopManager() {
        Object steamEngine = currentSteamEngine();
        if (steamEngine == null) {
            return null;
        }

        try {
            Object value = RustedReflection.getFieldValue(steamEngine, new String[]{"workshopManager", "g"});
            if (isSteamWorkshopManager(value)) {
                return value;
            }
        } catch (RuntimeException ignored) {
        }

        return null;
    }

    public static boolean isSteamEngine(Object value) {
        return isAny(value, STEAM_ENGINE_CLASSES);
    }

    public static boolean isJavaSteamEngine(Object value) {
        return isAny(value, JAVA_STEAM_ENGINE_CLASSES);
    }

    public static boolean isSteamWorkshopManager(Object value) {
        return isAny(value, STEAM_WORKSHOP_MANAGER_CLASSES);
    }

    public static Map<String, Object> describeCurrentSteamEngine() {
        Object steamEngine = currentSteamEngine();
        return steamEngine != null ? describeSteamEngine(steamEngine) : Collections.emptyMap();
    }

    public static Map<String, Object> describeSteamEngine(Object steamEngine) {
        requireAny(steamEngine, STEAM_ENGINE_CLASSES, "SteamEngine");
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("className", steamEngine.getClass().getName());
        result.put("javaSteamEngine", Boolean.valueOf(isJavaSteamEngine(steamEngine)));
        result.put("steamEnabled", invokeBooleanOrFalse(steamEngine, new String[]{"isSteamEnabled", "e"}));
        result.put("steamDisabled", invokeBooleanOrFalse(steamEngine, new String[]{"isSteamDisabled", "f"}));
        result.put("personaName", invokeStringOrEmpty(steamEngine, new String[]{"getPersonaName", "c"}));
        if (isJavaSteamEngine(steamEngine)) {
            putBooleanField(result, steamEngine, "initialized", new String[]{"initialized", "k"});
            putCollectionSizeField(result, steamEngine, "activeSteamSocketsSize",
                    new String[]{"activeSteamSockets", "l"});
            putField(result, steamEngine, "activeLobby", new String[]{"activeLobby", "n"});
            putBooleanField(result, steamEngine, "isLobbyHost", new String[]{"isLobbyHost", "o"});
            putField(result, steamEngine, "lobbyOwnerSteamId", new String[]{"lobbyOwnerSteamId", "p"});
            putField(result, steamEngine, "workshopManager", new String[]{"workshopManager", "g"});
            putField(result, steamEngine, "steamFriends", new String[]{"steamFriends", "c"});
            putField(result, steamEngine, "steamMatchmaking", new String[]{"steamMatchmaking", "d"});
            putField(result, steamEngine, "steamNetworking", new String[]{"steamNetworking", "h"});
            putField(result, steamEngine, "steamUtils", new String[]{"steamUtils", "j"});
        }
        return Collections.unmodifiableMap(result);
    }

    public static Map<String, Object> describeCurrentWorkshopManager() {
        Object manager = currentWorkshopManager();
        return manager != null ? describeWorkshopManager(manager) : Collections.emptyMap();
    }

    public static Map<String, Object> describeWorkshopManager(Object manager) {
        requireAny(manager, STEAM_WORKSHOP_MANAGER_CLASSES, "SteamWorkshopManager");
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("className", manager.getClass().getName());
        putField(result, manager, "steamEngine", new String[]{"steamEngine", "b"});
        putField(result, manager, "ugcCallback", new String[]{"ugcCallback", "c"});
        putField(result, manager, "steamUGC", new String[]{"steamUGC", "d"});
        result.put("hasUgcCallback", Boolean.valueOf(result.get("ugcCallback") != null));
        result.put("hasSteamUGC", Boolean.valueOf(result.get("steamUGC") != null));
        return Collections.unmodifiableMap(result);
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

    private static String invokeStringOrEmpty(Object owner, String[] methodNames, Object... args) {
        try {
            Object value = RustedReflection.invokeInstance(owner, methodNames, args);
            return value != null ? value.toString() : "";
        } catch (RuntimeException ignored) {
            return "";
        }
    }

    private static boolean invokeBooleanOrFalse(Object owner, String[] methodNames, Object... args) {
        try {
            return Boolean.TRUE.equals(RustedReflection.invokeInstance(owner, methodNames, args));
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    private static int collectionSize(Object value) {
        if (value == null) {
            return 0;
        }
        if (value instanceof java.util.Collection<?>) {
            return ((java.util.Collection<?>) value).size();
        }
        if (value instanceof Map<?, ?>) {
            return ((Map<?, ?>) value).size();
        }
        if (value.getClass().isArray()) {
            return java.lang.reflect.Array.getLength(value);
        }
        return 1;
    }

    private static void putField(Map<String, Object> result, Object owner, String key, String[] fieldNames) {
        try {
            result.put(key, RustedReflection.getFieldValue(owner, fieldNames));
        } catch (RuntimeException ignored) {
        }
    }

    private static void putBooleanField(Map<String, Object> result, Object owner, String key, String[] fieldNames) {
        try {
            result.put(key, Boolean.valueOf(RustedReflection.getBooleanField(owner, fieldNames)));
        } catch (RuntimeException ignored) {
        }
    }

    private static void putCollectionSizeField(Map<String, Object> result, Object owner, String key,
                                               String[] fieldNames) {
        try {
            result.put(key, Integer.valueOf(collectionSize(RustedReflection.getFieldValue(owner, fieldNames))));
        } catch (RuntimeException ignored) {
        }
    }
}
