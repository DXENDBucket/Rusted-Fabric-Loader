package io.github.endx.rustedfabricapi.api.diagnostic;

import io.github.endx.rustedfabricapi.api.util.RustedReflection;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
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
        putField(result, unit, "recentlyUnloadedFrom", new String[]{"recentlyUnloadedFrom", "bR"});
        putFloatField(result, unit, "recentlyUnloadedTimer", new String[]{"recentlyUnloadedTimer", "bS"});
        putField(result, unit, "transportingUnit", new String[]{"transportingUnit", "cN"});
        putField(result, unit, "attachmentParentUnit", new String[]{"attachmentParentUnit", "cO"});
        putField(result, unit, "attachmentSlot", new String[]{"attachmentSlot", "cP"});
        putField(result, unit, "activeResourceDelta", new String[]{"activeResourceDelta", "dJ"});
        result.put("damageImmune", Boolean.valueOf(isDamageImmune(unit)));
        result.put("building", Boolean.valueOf(isBuilding(unit)));
        result.put("movementType", getMovementType(unit));
        result.put("zoomedOutIconImage", getZoomedOutIconImage(unit));
        result.put("containingUnit", getContainingUnit(unit));
        result.put("runtimeAttachmentSlot", getAttachmentSlot(unit));
        result.put("transportSlotsNeeded", Integer.valueOf(getTransportSlotsNeeded(unit)));
        result.put("transportBarUsedSlots", Integer.valueOf(getTransportBarUsedSlots(unit)));
        result.put("transportBarMaxSlots", Integer.valueOf(getTransportBarMaxSlots(unit)));
        result.put("hasTransportCapacity", Boolean.valueOf(hasTransportCapacity(unit)));
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

    public static boolean isBuilding(Object unit) {
        requireUnit(unit);
        return Boolean.TRUE.equals(RustedReflection.invokeInstance(unit,
                new String[]{"isBuilding", "isFixedRotation", "bI"}));
    }

    /**
     * @deprecated v0.46 corrected Unit.bI semantics to {@link #isBuilding(Object)}.
     */
    @Deprecated
    public static boolean isFixedRotation(Object unit) {
        return isBuilding(unit);
    }

    public static Object getMovementType(Object unit) {
        requireUnit(unit);
        return RustedReflection.invokeInstance(unit, new String[]{"getMovementType", "h"});
    }

    public static Object getZoomedOutIconImage(Object unit) {
        requireUnit(unit);
        return RustedReflection.invokeInstance(unit, new String[]{"getZoomedOutIconImage", "v"});
    }

    public static Object getTransportingUnit(Object unit) {
        requireUnit(unit);
        return RustedReflection.getFieldValue(unit, new String[]{"transportingUnit", "cN"});
    }

    public static Object getAttachmentParentUnit(Object unit) {
        requireUnit(unit);
        return RustedReflection.getFieldValue(unit, new String[]{"attachmentParentUnit", "cO"});
    }

    public static Object getAttachmentSlotField(Object unit) {
        requireUnit(unit);
        return RustedReflection.getFieldValue(unit, new String[]{"attachmentSlot", "cP"});
    }

    public static Object getRecentlyUnloadedFrom(Object unit) {
        requireUnit(unit);
        return RustedReflection.getFieldValue(unit, new String[]{"recentlyUnloadedFrom", "bR"});
    }

    public static float getRecentlyUnloadedTimer(Object unit) {
        requireUnit(unit);
        return RustedReflection.getFloatField(unit, new String[]{"recentlyUnloadedTimer", "bS"});
    }

    public static Object getAttachmentSlot(Object unit) {
        requireUnit(unit);
        return RustedReflection.invokeInstance(unit, new String[]{"getAttachmentSlot", "dn"});
    }

    public static Object getContainingUnit(Object unit) {
        requireUnit(unit);
        return RustedReflection.invokeInstance(unit, new String[]{"getContainingUnit", "dr"});
    }

    public static boolean canTransportUnitIgnoringCurrentContainer(Object carrier, Object candidate,
                                                                   boolean allowPartial) {
        requireUnit(carrier);
        requireUnit(candidate);
        return Boolean.TRUE.equals(RustedReflection.invokeInstance(carrier,
                new String[]{"canTransportUnitIgnoringCurrentContainer", "c"},
                candidate, Boolean.valueOf(allowPartial)));
    }

    public static boolean canTransportUnit(Object carrier, Object candidate, boolean allowPartial) {
        requireUnit(carrier);
        requireUnit(candidate);
        return Boolean.TRUE.equals(RustedReflection.invokeInstance(carrier,
                new String[]{"canTransportUnit", "d"},
                candidate, Boolean.valueOf(allowPartial)));
    }

    public static boolean tryAddUnitToTransport(Object carrier, Object candidate, boolean allowPartial) {
        requireUnit(carrier);
        requireUnit(candidate);
        return Boolean.TRUE.equals(RustedReflection.invokeInstance(carrier,
                new String[]{"tryAddUnitToTransport", "e"},
                candidate, Boolean.valueOf(allowPartial)));
    }

    public static boolean hasTransportCapacity(Object unit) {
        requireUnit(unit);
        return Boolean.TRUE.equals(RustedReflection.invokeInstance(unit,
                new String[]{"hasTransportCapacity", "cr"}));
    }

    public static int getTransportSlotsNeeded(Object unit) {
        requireUnit(unit);
        Object value = RustedReflection.invokeInstance(unit, new String[]{"getTransportSlotsNeeded", "cw"});
        return value instanceof Number ? ((Number) value).intValue() : 1;
    }

    public static int getTransportBarUsedSlots(Object unit) {
        requireUnit(unit);
        Object value = RustedReflection.invokeInstance(unit, new String[]{"getTransportBarUsedSlots", "bY"});
        return value instanceof Number ? ((Number) value).intValue() : -1;
    }

    public static int getTransportBarMaxSlots(Object unit) {
        requireUnit(unit);
        Object value = RustedReflection.invokeInstance(unit, new String[]{"getTransportBarMaxSlots", "bZ"});
        return value instanceof Number ? ((Number) value).intValue() : -1;
    }

    public static int getTransportedUnitCount(Object unit) {
        requireOrderableUnit(unit);
        Object value = RustedReflection.invokeInstance(unit, new String[]{"getTransportedUnitCount", "bB"});
        return value instanceof Number ? ((Number) value).intValue() : 0;
    }

    public static boolean isTransportUnloading(Object unit) {
        requireOrderableUnit(unit);
        return Boolean.TRUE.equals(RustedReflection.invokeInstance(unit,
                new String[]{"isTransportUnloading", "bA"}));
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

    public static List<Object> getAttachedUnitActions(Object unit, boolean includeUnavailable) {
        requireUnit(unit);
        Object value = RustedReflection.invokeInstance(unit, new String[]{"getAttachedUnitActions", "e"},
                Boolean.valueOf(includeUnavailable));
        return Collections.unmodifiableList(RustedReflection.snapshotIterable(value));
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

    public static void addTurretAimAngle(Object unit, int turretIndex, float angleDelta) {
        requireOrderableUnit(unit);
        RustedReflection.invokeInstance(unit, new String[]{"addTurretAimAngle", "a"},
                Integer.valueOf(turretIndex), Float.valueOf(angleDelta));
    }

    public static int getMainNanoTurretIndex(Object unit) {
        requireOrderableUnit(unit);
        Object value = RustedReflection.invokeInstance(unit, new String[]{"getMainNanoTurretIndex", "aT"});
        return value instanceof Number ? ((Number) value).intValue() : -1;
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
