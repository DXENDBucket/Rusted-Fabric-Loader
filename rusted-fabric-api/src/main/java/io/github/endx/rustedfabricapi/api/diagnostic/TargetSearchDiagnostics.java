package io.github.endx.rustedfabricapi.api.diagnostic;

import io.github.endx.rustedfabricapi.api.util.RustedReflection;

import java.lang.reflect.Array;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class TargetSearchDiagnostics {
    private static final String[] ORDERABLE_UNIT_CLASSES = {
            "rustedwarfare.unit.OrderableUnit",
            "com.corrodinggames.rts.game.units.y"
    };
    private static final String[] NEAREST_UNIT_SEARCH_CALLBACK_CLASSES = {
            "rustedwarfare.unit.search.NearestUnitSearchCallback",
            "com.corrodinggames.rts.game.units.ac"
    };
    private static final String[] PASSIVE_TARGET_SEARCH_CALLBACK_CLASSES = {
            "rustedwarfare.unit.search.PassiveTargetSearchCallback",
            "com.corrodinggames.rts.game.units.ae"
    };
    private static final String[] TURRET_PASSIVE_TARGET_SEARCH_CALLBACK_CLASSES = {
            "rustedwarfare.unit.search.TurretPassiveTargetSearchCallback",
            "com.corrodinggames.rts.game.units.ah"
    };

    private TargetSearchDiagnostics() {
    }

    public static Object newNearestUnitSearchCallback() {
        return RustedReflection.newInstance(NEAREST_UNIT_SEARCH_CALLBACK_CLASSES);
    }

    public static Object newPassiveTargetSearchCallback(boolean requireWeaponTargeting) {
        return RustedReflection.newInstance(PASSIVE_TARGET_SEARCH_CALLBACK_CLASSES,
                Boolean.valueOf(requireWeaponTargeting));
    }

    public static Object newTurretPassiveTargetSearchCallback(boolean requireWeaponTargeting) {
        return RustedReflection.newInstance(TURRET_PASSIVE_TARGET_SEARCH_CALLBACK_CLASSES,
                Boolean.valueOf(requireWeaponTargeting));
    }

    public static void preparePassiveSearchRadius(Object callback, float range) {
        requirePassiveTargetSearchCallback(callback);
        RustedReflection.invokeInstance(callback, new String[]{"prepareSearchRadius", "a"}, Float.valueOf(range));
    }

    public static void prepareTurretSearchRanges(Object callback, Object sourceUnit) {
        requireTurretPassiveTargetSearchCallback(callback);
        requireOrderableUnit(sourceUnit);
        RustedReflection.invokeInstance(callback, new String[]{"prepareTurretSearchRanges", "a"}, sourceUnit);
    }

    public static Map<String, Object> describeNearestUnitSearchCallback(Object callback) {
        requireNearestUnitSearchCallback(callback);
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        putFloatField(result, callback, "searchX", new String[]{"searchX", "a"});
        putFloatField(result, callback, "searchY", new String[]{"searchY", "b"});
        putField(result, callback, "requiredTags", new String[]{"requiredTags", "c"});
        putFloatField(result, callback, "bestDistanceSquared", new String[]{"bestDistanceSquared", "d"});
        putField(result, callback, "bestUnit", new String[]{"bestUnit", "e"});
        putBooleanField(result, callback, "requireReachableTarget",
                new String[]{"requireReachableTarget", "f"});
        putBooleanField(result, callback, "allowIncompleteTarget",
                new String[]{"allowIncompleteTarget", "g"});
        return Collections.unmodifiableMap(result);
    }

    public static Map<String, Object> describePassiveTargetSearchCallback(Object callback) {
        requirePassiveTargetSearchCallback(callback);
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        putIntField(result, callback, "checkedTargetCount", new String[]{"checkedTargetCount", "a"});
        putFloatField(result, callback, "bestDistanceSquared", new String[]{"bestDistanceSquared", "b"});
        putBooleanField(result, callback, "requireWeaponTargeting",
                new String[]{"requireWeaponTargeting", "c"});
        putBooleanField(result, callback, "ready", new String[]{"ready", "d"});
        return Collections.unmodifiableMap(result);
    }

    public static Map<String, Object> describeTurretPassiveTargetSearchCallback(Object callback) {
        requireTurretPassiveTargetSearchCallback(callback);
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        putIntField(result, callback, "checkedTargetCount", new String[]{"checkedTargetCount", "a"});
        putArrayLengthField(result, callback, "bestDistanceSquaredByTurretLength",
                new String[]{"bestDistanceSquaredByTurret", "b"});
        putArrayLengthField(result, callback, "turretNeedsTargetLength",
                new String[]{"turretNeedsTarget", "c"});
        putIntField(result, callback, "turretCount", new String[]{"turretCount", "d"});
        putBooleanField(result, callback, "requireWeaponTargeting",
                new String[]{"requireWeaponTargeting", "e"});
        putBooleanField(result, callback, "ready", new String[]{"ready", "f"});
        return Collections.unmodifiableMap(result);
    }

    private static void requireOrderableUnit(Object unit) {
        requireAny(unit, ORDERABLE_UNIT_CLASSES, "OrderableUnit");
    }

    private static void requireNearestUnitSearchCallback(Object callback) {
        requireAny(callback, NEAREST_UNIT_SEARCH_CALLBACK_CLASSES, "NearestUnitSearchCallback");
    }

    private static void requirePassiveTargetSearchCallback(Object callback) {
        requireAny(callback, PASSIVE_TARGET_SEARCH_CALLBACK_CLASSES, "PassiveTargetSearchCallback");
    }

    private static void requireTurretPassiveTargetSearchCallback(Object callback) {
        requireAny(callback, TURRET_PASSIVE_TARGET_SEARCH_CALLBACK_CLASSES,
                "TurretPassiveTargetSearchCallback");
    }

    private static void requireAny(Object value, String[] classNames, String label) {
        if (value == null) {
            throw new IllegalArgumentException(label + " must not be null");
        }
        if (!RustedReflection.isAnyClass(value.getClass(), classNames)) {
            throw new IllegalArgumentException("Expected " + label + ", got " + value.getClass().getName());
        }
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
        Object value = RustedReflection.getFieldValue(owner, fieldNames);
        result.put(key, Integer.valueOf(value != null && value.getClass().isArray() ? Array.getLength(value) : 0));
    }
}
