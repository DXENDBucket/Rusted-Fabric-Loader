package io.github.endx.rustedfabricapi.api.diagnostic;

import io.github.endx.rustedfabricapi.api.util.RustedReflection;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class UnitRuntimeDiagnostics {
    private static final String[] UNIT_CLASSES = {
            "rustedwarfare.unit.Unit",
            "com.corrodinggames.rts.game.units.am"
    };

    private UnitRuntimeDiagnostics() {
    }

    public static Map<String, Object> describeUnitRuntime(Object unit) {
        requireUnit(unit);
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        putBooleanField(result, unit, "createdFromMap", new String[]{"createdFromMap", "bM"});
        putBooleanField(result, unit, "mapAiDisabled", new String[]{"mapAiDisabled", "bN"});
        putBooleanField(result, unit, "mapPlacedBuilderMarker", new String[]{"mapPlacedBuilderMarker", "bO"});
        putBooleanField(result, unit, "mapPlacedCommandCenterMarker",
                new String[]{"mapPlacedCommandCenterMarker", "bP"});
        putBooleanField(result, unit, "dead", new String[]{"dead", "bV"});
        putBooleanField(result, unit, "registeredWithTeam", new String[]{"registeredWithTeam", "bY"});
        putFloatField(result, unit, "direction", new String[]{"direction", "cg"});
        putFloatField(result, unit, "hp", new String[]{"hp", "cu"});
        putIntField(result, unit, "lastDamagedFrame", new String[]{"lastDamagedFrame", "bs"});
        putField(result, unit, "lastDamagedBy", new String[]{"lastDamagedBy", "bt"});
        putLongField(result, unit, "deathFrame", new String[]{"deathFrame", "bW"});
        return Collections.unmodifiableMap(result);
    }

    public static Object getRecentDamager(Object unit, float seconds) {
        requireUnit(unit);
        return RustedReflection.invokeInstance(unit, new String[]{"getRecentDamager", "q"},
                Float.valueOf(seconds));
    }

    public static void checkDeathState(Object unit) {
        requireUnit(unit);
        RustedReflection.invokeInstance(unit, new String[]{"checkDeathState", "ch"});
    }

    private static void requireUnit(Object unit) {
        if (unit == null) {
            throw new IllegalArgumentException("Unit must not be null");
        }
        if (!RustedReflection.isAnyClass(unit.getClass(), UNIT_CLASSES)) {
            throw new IllegalArgumentException("Expected Unit, got " + unit.getClass().getName());
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

    private static void putFloatField(Map<String, Object> result, Object owner, String key, String[] fieldNames) {
        result.put(key, Float.valueOf(RustedReflection.getFloatField(owner, fieldNames)));
    }

    private static void putBooleanField(Map<String, Object> result, Object owner, String key, String[] fieldNames) {
        result.put(key, Boolean.valueOf(RustedReflection.getBooleanField(owner, fieldNames)));
    }
}
