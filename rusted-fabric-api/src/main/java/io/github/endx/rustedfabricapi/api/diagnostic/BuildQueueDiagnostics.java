package io.github.endx.rustedfabricapi.api.diagnostic;

import io.github.endx.rustedfabricapi.api.util.RustedReflection;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class BuildQueueDiagnostics {
    private static final String[] BUILD_QUEUE_CLASSES = {
            "rustedwarfare.unit.build.BuildQueue",
            "com.corrodinggames.rts.game.units.d.k"
    };
    private static final String[] BUILD_QUEUE_ITEM_CLASSES = {
            "rustedwarfare.unit.build.BuildQueueItem",
            "com.corrodinggames.rts.game.units.d.j"
    };
    private static final String[] BUILD_QUEUE_HOST_CLASSES = {
            "rustedwarfare.unit.build.BuildQueueHost",
            "com.corrodinggames.rts.game.units.d.l"
    };
    private static final String[] PRODUCTION_BUILDING_UNIT_BASE_CLASSES = {
            "rustedwarfare.unit.building.ProductionBuildingUnitBase",
            "com.corrodinggames.rts.game.units.d.i"
    };

    private BuildQueueDiagnostics() {
    }

    public static Map<String, Object> describeBuildQueue(Object buildQueue) {
        requireBuildQueue(buildQueue);
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("hostUnit", RustedReflection.getFieldValue(buildQueue, new String[]{"hostUnit", "a"}));
        result.put("rallyPoint", RustedReflection.getFieldValue(buildQueue, new String[]{"rallyPoint", "b"}));
        result.put("activeQueueItems", activeQueueItems(buildQueue));
        result.put("stagedQueueChanges", stagedQueueChanges(buildQueue));
        putFloatField(result, buildQueue, "buildProgress", new String[]{"buildProgress", "e"});
        result.put("currentQueueItem", getCurrentQueueItem(buildQueue));
        result.put("currentStreamingResourceCost", getCurrentStreamingResourceCost(buildQueue));
        result.put("currentAction", getCurrentAction(buildQueue));
        result.put("empty", Boolean.valueOf(isEmpty(buildQueue)));
        return Collections.unmodifiableMap(result);
    }

    public static Map<String, Object> describeBuildQueueItem(Object queueItem) {
        requireBuildQueueItem(queueItem);
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        putIntField(result, queueItem, "quantity", new String[]{"quantity", "a"});
        putFloatField(result, queueItem, "buildSpeedMultiplier", new String[]{"buildSpeedMultiplier", "b"});
        result.put("priceResources", RustedReflection.getFieldValue(queueItem, new String[]{"priceResources", "c"}));
        result.put("streamingCostResources",
                RustedReflection.getFieldValue(queueItem, new String[]{"streamingCostResources", "d"}));
        result.put("queueTags", RustedReflection.getFieldValue(queueItem, new String[]{"queueTags", "e"}));
        putBooleanField(result, queueItem, "producesUnit", new String[]{"producesUnit", "f"});
        result.put("producedUnitType", RustedReflection.getFieldValue(queueItem, new String[]{"producedUnitType", "g"}));
        result.put("queuedRallyPoint", RustedReflection.getFieldValue(queueItem, new String[]{"queuedRallyPoint", "h"}));
        result.put("queuedTargetUnit", RustedReflection.getFieldValue(queueItem, new String[]{"queuedTargetUnit", "i"}));
        result.put("actionId", RustedReflection.getFieldValue(queueItem, new String[]{"actionId", "j"}));
        putBooleanField(result, queueItem, "stagedCancellation", new String[]{"stagedCancellation", "k"});
        putBooleanField(result, queueItem, "highPriority", new String[]{"highPriority", "l"});
        putFloatField(result, queueItem, "activationProgressMarker",
                new String[]{"activationProgressMarker", "m"});
        putDoubleField(result, queueItem, "streamedResourcesPaidFraction",
                new String[]{"streamedResourcesPaidFraction", "n"});
        return Collections.unmodifiableMap(result);
    }

    public static Map<String, Object> describeBuildQueueHost(Object host) {
        requireBuildQueueHost(host);
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("currentQueueItem", getHostCurrentBuildQueueItem(host));
        result.put("queueItems", getHostBuildQueueItems(host));
        result.put("empty", Boolean.valueOf(isBuildQueueEmpty(host)));
        result.put("limitExceeded", Boolean.valueOf(isBuildQueueLimitExceeded(host)));
        result.put("queuedItemsIncludingStaged", Integer.valueOf(countQueuedItems(host, true)));
        result.put("queuedItemsActiveOnly", Integer.valueOf(countQueuedItems(host, false)));
        return Collections.unmodifiableMap(result);
    }

    public static Object getProductionBuildingBuildQueue(Object productionBuilding) {
        requireProductionBuildingUnitBase(productionBuilding);
        return RustedReflection.getFieldValue(productionBuilding, new String[]{"buildQueue", "z"});
    }

    public static boolean isEmpty(Object buildQueue) {
        requireBuildQueue(buildQueue);
        return Boolean.TRUE.equals(RustedReflection.invokeInstance(buildQueue, new String[]{"isEmpty", "a"}));
    }

    public static Object getCurrentQueueItem(Object buildQueue) {
        requireBuildQueue(buildQueue);
        return RustedReflection.invokeInstance(buildQueue, new String[]{"getCurrentQueueItem", "b"});
    }

    public static Object getCurrentStreamingResourceCost(Object buildQueue) {
        requireBuildQueue(buildQueue);
        return RustedReflection.invokeInstance(buildQueue, new String[]{"getCurrentStreamingResourceCost", "c"});
    }

    public static Object getCurrentAction(Object buildQueue) {
        requireBuildQueue(buildQueue);
        return RustedReflection.invokeInstance(buildQueue, new String[]{"getCurrentAction", "d"});
    }

    public static List<Object> getQueueItems(Object buildQueue) {
        requireBuildQueue(buildQueue);
        return Collections.unmodifiableList(RustedReflection.snapshotIterable(
                RustedReflection.invokeInstance(buildQueue, new String[]{"getQueueItems", "f"})));
    }

    public static List<Object> activeQueueItems(Object buildQueue) {
        requireBuildQueue(buildQueue);
        return Collections.unmodifiableList(RustedReflection.snapshotIterable(
                RustedReflection.getFieldValue(buildQueue, new String[]{"activeQueueItems", "c"})));
    }

    public static List<Object> stagedQueueChanges(Object buildQueue) {
        requireBuildQueue(buildQueue);
        return Collections.unmodifiableList(RustedReflection.snapshotIterable(
                RustedReflection.getFieldValue(buildQueue, new String[]{"stagedQueueChanges", "d"})));
    }

    public static Object getHostCurrentBuildQueueItem(Object host) {
        requireBuildQueueHost(host);
        return RustedReflection.invokeInstance(host, new String[]{"getCurrentBuildQueueItem", "dw"});
    }

    public static List<Object> getHostBuildQueueItems(Object host) {
        requireBuildQueueHost(host);
        return Collections.unmodifiableList(RustedReflection.snapshotIterable(
                RustedReflection.invokeInstance(host, new String[]{"getBuildQueueItems", "dx"})));
    }

    public static boolean isBuildQueueEmpty(Object host) {
        requireBuildQueueHost(host);
        return Boolean.TRUE.equals(RustedReflection.invokeInstance(host, new String[]{"isBuildQueueEmpty", "dy"}));
    }

    public static void forceBuildQueueProgressComplete(Object host) {
        requireBuildQueueHost(host);
        RustedReflection.invokeInstance(host, new String[]{"forceBuildQueueProgressComplete", "dz"});
    }

    public static int countQueuedItems(Object host, boolean includeStaged) {
        requireBuildQueueHost(host);
        Object value = RustedReflection.invokeInstance(host,
                new String[]{"countQueuedItems", "f"},
                Boolean.valueOf(includeStaged));
        return value instanceof Number ? ((Number) value).intValue() : 0;
    }

    public static int countQueuedUnitType(Object host, Object unitType) {
        requireBuildQueueHost(host);
        Object value = RustedReflection.invokeInstance(host,
                new String[]{"countQueuedUnitType", "h"},
                unitType);
        return value instanceof Number ? ((Number) value).intValue() : 0;
    }

    public static boolean isBuildQueueLimitExceeded(Object host) {
        requireBuildQueueHost(host);
        return Boolean.TRUE.equals(RustedReflection.invokeInstance(host,
                new String[]{"isBuildQueueLimitExceeded", "dA"}));
    }

    private static void requireBuildQueue(Object buildQueue) {
        requireAny(buildQueue, BUILD_QUEUE_CLASSES, "BuildQueue");
    }

    private static void requireBuildQueueItem(Object queueItem) {
        requireAny(queueItem, BUILD_QUEUE_ITEM_CLASSES, "BuildQueueItem");
    }

    private static void requireBuildQueueHost(Object host) {
        requireAny(host, BUILD_QUEUE_HOST_CLASSES, "BuildQueueHost");
    }

    private static void requireProductionBuildingUnitBase(Object productionBuilding) {
        requireAny(productionBuilding, PRODUCTION_BUILDING_UNIT_BASE_CLASSES, "ProductionBuildingUnitBase");
    }

    private static void requireAny(Object value, String[] classNames, String label) {
        if (value == null) {
            throw new IllegalArgumentException(label + " must not be null");
        }
        if (!RustedReflection.isAnyClass(value.getClass(), classNames)) {
            throw new IllegalArgumentException("Expected " + label + ", got " + value.getClass().getName());
        }
    }

    private static void putIntField(Map<String, Object> result, Object owner, String key, String[] fieldNames) {
        result.put(key, Integer.valueOf(RustedReflection.getIntField(owner, fieldNames)));
    }

    private static void putFloatField(Map<String, Object> result, Object owner, String key, String[] fieldNames) {
        result.put(key, Float.valueOf(RustedReflection.getFloatField(owner, fieldNames)));
    }

    private static void putDoubleField(Map<String, Object> result, Object owner, String key, String[] fieldNames) {
        Object value = RustedReflection.getFieldValue(owner, fieldNames);
        result.put(key, Double.valueOf(value instanceof Number ? ((Number) value).doubleValue() : 0.0D));
    }

    private static void putBooleanField(Map<String, Object> result, Object owner, String key, String[] fieldNames) {
        result.put(key, Boolean.valueOf(RustedReflection.getBooleanField(owner, fieldNames)));
    }
}
