package io.github.endx.rustedfabricapi.api.diagnostic;

import io.github.endx.rustedfabricapi.api.util.RustedReflection;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class HudCommandDiagnostics {
    private static final String[] INTERFACE_ENGINE_CLASSES = {
            "rustedwarfare.ui.InterfaceEngine",
            "com.corrodinggames.rts.gameFramework.f.g"
    };
    private static final String[] COMMAND_INTERFACE_CLASSES = {
            "rustedwarfare.ui.CommandInterface",
            "com.corrodinggames.rts.gameFramework.f.a"
    };

    private HudCommandDiagnostics() {
    }

    public static boolean isInterfaceEngine(Object value) {
        return value != null && RustedReflection.isAnyClass(value.getClass(), INTERFACE_ENGINE_CLASSES);
    }

    public static boolean isCommandInterface(Object value) {
        return value != null && RustedReflection.isAnyClass(value.getClass(), COMMAND_INTERFACE_CLASSES);
    }

    public static Map<String, Object> describeInterfaceEngine(Object interfaceEngine) {
        requireInterfaceEngine(interfaceEngine);
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        putField(result, interfaceEngine, "attackMoveAction", new String[]{"attackMoveAction", "l"});
        putField(result, interfaceEngine, "guardUnitAction", new String[]{"guardUnitAction", "m"});
        putField(result, interfaceEngine, "patrolAction", new String[]{"patrolAction", "n"});
        putField(result, interfaceEngine, "attackModeAction", new String[]{"attackModeAction", "o"});
        putField(result, interfaceEngine, "pingMapAction", new String[]{"pingMapAction", "p"});
        putField(result, interfaceEngine, "mapPingAction", new String[]{"mapPingAction", "q"});
        putField(result, interfaceEngine, "teamChatAction", new String[]{"teamChatAction", "r"});
        putField(result, interfaceEngine, "resourceDisplayScratch", new String[]{"resourceDisplayScratch", "bT"});
        putLongField(result, interfaceEngine, "lastMapPingBroadcastMillis",
                new String[]{"lastMapPingBroadcastMillis", "bW"});
        putIntField(result, interfaceEngine, "interfaceLayoutRevision",
                new String[]{"interfaceLayoutRevision", "cd"});
        putBooleanField(result, interfaceEngine, "interfaceLayoutDirty",
                new String[]{"interfaceLayoutDirty", "ce"});
        return Collections.unmodifiableMap(result);
    }

    public static Object attackMoveAction(Object interfaceEngine) {
        requireInterfaceEngine(interfaceEngine);
        return RustedReflection.getFieldValue(interfaceEngine, new String[]{"attackMoveAction", "l"});
    }

    public static Object guardUnitAction(Object interfaceEngine) {
        requireInterfaceEngine(interfaceEngine);
        return RustedReflection.getFieldValue(interfaceEngine, new String[]{"guardUnitAction", "m"});
    }

    public static Object patrolAction(Object interfaceEngine) {
        requireInterfaceEngine(interfaceEngine);
        return RustedReflection.getFieldValue(interfaceEngine, new String[]{"patrolAction", "n"});
    }

    public static Object pingMapAction(Object interfaceEngine) {
        requireInterfaceEngine(interfaceEngine);
        return RustedReflection.getFieldValue(interfaceEngine, new String[]{"pingMapAction", "p"});
    }

    public static Object mapPingAction(Object interfaceEngine) {
        requireInterfaceEngine(interfaceEngine);
        return RustedReflection.getFieldValue(interfaceEngine, new String[]{"mapPingAction", "q"});
    }

    public static void markInterfaceLayoutDirty(Object interfaceEngine) {
        requireInterfaceEngine(interfaceEngine);
        RustedReflection.invokeInstance(interfaceEngine, new String[]{"markInterfaceLayoutDirty", "K"});
    }

    public static void resetInterfaceState(Object interfaceEngine, boolean keepSelectedUnits) {
        requireInterfaceEngine(interfaceEngine);
        RustedReflection.invokeInstance(interfaceEngine, new String[]{"resetInterfaceState", "a"},
                Boolean.valueOf(keepSelectedUnits));
    }

    public static void reloadCommandInterfaceStrings(Object interfaceEngine) {
        requireInterfaceEngine(interfaceEngine);
        RustedReflection.invokeInstance(interfaceEngine, new String[]{"reloadCommandInterfaceStrings", "e"});
    }

    private static void requireInterfaceEngine(Object value) {
        requireAny(value, INTERFACE_ENGINE_CLASSES, "InterfaceEngine");
    }

    private static void requireAny(Object value, String[] classNames, String label) {
        if (value == null || !RustedReflection.isAnyClass(value.getClass(), classNames)) {
            throw new IllegalArgumentException("Expected " + label + ", got " + describe(value));
        }
    }

    private static void putField(Map<String, Object> result, Object owner, String key, String[] fieldNames) {
        result.put(key, RustedReflection.getFieldValue(owner, fieldNames));
    }

    private static void putIntField(Map<String, Object> result, Object owner, String key, String[] fieldNames) {
        result.put(key, Integer.valueOf(RustedReflection.getIntField(owner, fieldNames)));
    }

    private static void putLongField(Map<String, Object> result, Object owner, String key, String[] fieldNames) {
        Object value = RustedReflection.getFieldValue(owner, fieldNames);
        result.put(key, Long.valueOf(value instanceof Number ? ((Number) value).longValue() : 0L));
    }

    private static void putBooleanField(Map<String, Object> result, Object owner, String key, String[] fieldNames) {
        result.put(key, Boolean.valueOf(RustedReflection.getBooleanField(owner, fieldNames)));
    }

    private static String describe(Object value) {
        return value == null ? "null" : value.getClass().getName();
    }
}
