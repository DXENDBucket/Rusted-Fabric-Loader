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
    private static final String[] ORDERABLE_UNIT_CLASSES = {
            "rustedwarfare.unit.OrderableUnit",
            "com.corrodinggames.rts.game.units.y"
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
        putField(result, unit, "activeResourceDelta", new String[]{"activeResourceDelta", "dJ"});
        result.put("damageImmune", Boolean.valueOf(isDamageImmune(unit)));
        result.put("fixedRotation", Boolean.valueOf(isFixedRotation(unit)));
        result.put("movementType", getMovementType(unit));
        result.put("zoomedOutIconImage", getZoomedOutIconImage(unit));
        result.put("baseReclaimPrice", getBaseReclaimPrice(unit));
        result.put("reclaimPriceOverride", getReclaimPriceOverride(unit));
        result.put("similarResourcesHaveTag", getSimilarResourcesHaveTag(unit));
        result.put("runtimeTags", getRuntimeTags(unit));
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

    public static boolean isDamageImmune(Object unit) {
        requireUnit(unit);
        return Boolean.TRUE.equals(RustedReflection.invokeInstance(unit, new String[]{"isDamageImmune", "J"}));
    }

    public static boolean isFixedRotation(Object unit) {
        requireUnit(unit);
        return Boolean.TRUE.equals(RustedReflection.invokeInstance(unit, new String[]{"isFixedRotation", "bI"}));
    }

    public static Object getMovementType(Object unit) {
        requireUnit(unit);
        return RustedReflection.invokeInstance(unit, new String[]{"getMovementType", "h"});
    }

    public static Object getZoomedOutIconImage(Object unit) {
        requireUnit(unit);
        return RustedReflection.invokeInstance(unit, new String[]{"getZoomedOutIconImage", "v"});
    }

    public static Object getBaseReclaimPrice(Object unit) {
        requireUnit(unit);
        return RustedReflection.invokeInstance(unit, new String[]{"getBaseReclaimPrice", "cM"});
    }

    public static Object getReclaimPriceOverride(Object unit) {
        requireUnit(unit);
        return RustedReflection.invokeInstance(unit, new String[]{"getReclaimPriceOverride", "cN"});
    }

    public static void setConstructionProgress(Object unit, float progress) {
        requireUnit(unit);
        RustedReflection.invokeInstance(unit, new String[]{"setConstructionProgress", "r"}, Float.valueOf(progress));
    }

    public static Object getSimilarResourcesHaveTag(Object unit) {
        requireUnit(unit);
        return RustedReflection.invokeInstance(unit, new String[]{"getSimilarResourcesHaveTag", "cR"});
    }

    public static Object getRuntimeTags(Object unit) {
        requireUnit(unit);
        return RustedReflection.invokeInstance(unit, new String[]{"getRuntimeTags", "de"});
    }

    public static void refreshActiveResourceDelta(Object unit) {
        requireUnit(unit);
        RustedReflection.invokeInstance(unit, new String[]{"refreshActiveResourceDelta", "bC"});
    }

    public static Object getActiveResourceDelta(Object unit) {
        requireUnit(unit);
        return RustedReflection.invokeInstance(unit, new String[]{"getActiveResourceDelta", "dq"});
    }

    public static boolean canRepairTarget(Object unit, Object target) {
        requireOrderableUnit(unit);
        requireUnit(target);
        return Boolean.TRUE.equals(RustedReflection.invokeInstance(unit,
                new String[]{"canRepairTarget", "a"},
                target));
    }

    public static boolean canReclaimUnitTarget(Object unit, Object target) {
        requireOrderableUnit(unit);
        requireUnit(target);
        return Boolean.TRUE.equals(RustedReflection.invokeInstance(unit,
                new String[]{"canReclaimUnitTarget", "l"},
                target));
    }

    public static float getBuildProgressSpeedForTarget(Object unit, Object target) {
        requireOrderableUnit(unit);
        requireUnit(target);
        Object value = RustedReflection.invokeInstance(unit,
                new String[]{"getBuildProgressSpeedForTarget", "a_"},
                target);
        return value instanceof Number ? ((Number) value).floatValue() : 0.0F;
    }

    public static float getUnbuildSpeedForTarget(Object unit, Object target) {
        requireOrderableUnit(unit);
        requireUnit(target);
        Object value = RustedReflection.invokeInstance(unit,
                new String[]{"getUnbuildSpeedForTarget", "f"},
                target);
        return value instanceof Number ? ((Number) value).floatValue() : 0.0F;
    }

    public static Object getBuildPriceForTarget(Object unit, Object target) {
        requireOrderableUnit(unit);
        requireUnit(target);
        return RustedReflection.invokeInstance(unit, new String[]{"getBuildPriceForTarget", "g"}, target);
    }

    public static Object getBuildQueueResourceDelta(Object unit) {
        requireOrderableUnit(unit);
        return RustedReflection.invokeInstance(unit, new String[]{"getBuildQueueResourceDelta", "bD"});
    }

    public static Object getRepairReclaimResourceDelta(Object unit) {
        requireOrderableUnit(unit);
        return RustedReflection.invokeInstance(unit, new String[]{"getRepairReclaimResourceDelta", "bE"});
    }

    public static Object findNearestReclaimResourceTarget(Object searcher, float x, float y, float range,
                                                          Object requiredTags) {
        requireOrderableUnit(searcher);
        return RustedReflection.invokeStatic(ORDERABLE_UNIT_CLASSES,
                new String[]{"findNearestReclaimResourceTarget", "a"},
                searcher, Float.valueOf(x), Float.valueOf(y), Float.valueOf(range), requiredTags);
    }

    public static int getDeathSmokeParticleCount(Object unit) {
        requireOrderableUnit(unit);
        Object value = RustedReflection.invokeInstance(unit, new String[]{"getDeathSmokeParticleCount", "bp"});
        return value instanceof Number ? ((Number) value).intValue() : 0;
    }

    private static void requireUnit(Object unit) {
        if (unit == null) {
            throw new IllegalArgumentException("Unit must not be null");
        }
        if (!RustedReflection.isAnyClass(unit.getClass(), UNIT_CLASSES)) {
            throw new IllegalArgumentException("Expected Unit, got " + unit.getClass().getName());
        }
    }

    private static void requireOrderableUnit(Object unit) {
        if (unit == null) {
            throw new IllegalArgumentException("OrderableUnit must not be null");
        }
        if (!RustedReflection.isAnyClass(unit.getClass(), ORDERABLE_UNIT_CLASSES)) {
            throw new IllegalArgumentException("Expected OrderableUnit, got " + unit.getClass().getName());
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
