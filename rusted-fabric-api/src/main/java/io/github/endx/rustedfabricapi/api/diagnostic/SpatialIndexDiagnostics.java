package io.github.endx.rustedfabricapi.api.diagnostic;

import io.github.endx.rustedfabricapi.api.util.RustedReflection;

import java.lang.reflect.Array;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class SpatialIndexDiagnostics {
    private static final String[] GAME_ENGINE_CLASSES = {
            "rustedwarfare.core.GameEngine",
            "com.corrodinggames.rts.gameFramework.l"
    };
    private static final String[] MAP_ENGINE_CLASSES = {
            "rustedwarfare.map.MapEngine",
            "com.corrodinggames.rts.game.b.b"
    };
    private static final String[] UNIT_CLASSES = {
            "rustedwarfare.unit.Unit",
            "com.corrodinggames.rts.game.units.am"
    };
    private static final String[] UNIT_SPATIAL_INDEX_CLASSES = {
            "rustedwarfare.unit.spatial.UnitSpatialIndex",
            "com.corrodinggames.rts.game.units.f.c"
    };
    private static final String[] UNIT_SPATIAL_CELL_CLASSES = {
            "rustedwarfare.unit.spatial.UnitSpatialCell",
            "com.corrodinggames.rts.game.units.f.a"
    };
    private static final String[] UNIT_BUCKET_CLASSES = {
            "rustedwarfare.unit.spatial.UnitBucket",
            "com.corrodinggames.rts.game.units.f.b"
    };
    private static final String[] UNIT_SEARCH_RESULT_ITERATOR_CLASSES = {
            "rustedwarfare.unit.spatial.UnitSearchResultIterator",
            "com.corrodinggames.rts.game.units.f.f"
    };

    private SpatialIndexDiagnostics() {
    }

    public static Object unitSpatialIndexFromGameEngine(Object gameEngine) {
        requireAny(gameEngine, GAME_ENGINE_CLASSES, "GameEngine");
        return RustedReflection.getFieldValue(gameEngine, new String[]{"unitSpatialIndex", "cc"});
    }

    public static List<Object> allUnitsSnapshot() {
        return Collections.unmodifiableList(RustedReflection.snapshotIterable(
                RustedReflection.getStaticFieldValue(UNIT_CLASSES, new String[]{"allUnits", "bE"})));
    }

    public static Map<String, Object> describeUnitSpatialIndex(Object index) {
        requireSpatialIndex(index);
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        putIntField(result, index, "cellWorldWidth", new String[]{"cellWorldWidth", "a"});
        putIntField(result, index, "cellWorldHeight", new String[]{"cellWorldHeight", "b"});
        putFloatField(result, index, "worldToCellXScale", new String[]{"worldToCellXScale", "c"});
        putFloatField(result, index, "worldToCellYScale", new String[]{"worldToCellYScale", "d"});
        Object cells = RustedReflection.getFieldValue(index, new String[]{"cells", "e"});
        result.put("cells", cells);
        result.put("cellColumnCount", Integer.valueOf(arrayLength(cells)));
        result.put("cellRowCount", Integer.valueOf(nestedArrayLength(cells)));
        putField(result, index, "circleFilter", new String[]{"circleFilter", "f"});
        putField(result, index, "rectFilter", new String[]{"rectFilter", "g"});
        putField(result, index, "rectWithUnitRadiusFilter", new String[]{"rectWithUnitRadiusFilter", "h"});
        putCollectionField(result, index, "scratchUnitList", new String[]{"scratchUnitList", "i"});
        putField(result, index, "scratchResultIterator", new String[]{"scratchResultIterator", "j"});
        putField(result, index, "scratchCellRect", new String[]{"scratchCellRect", "k"});
        putIntField(result, index, "gridSize", new String[]{"GRID_SIZE", "l"});
        putIntField(result, index, "highestTeamIdSeen", new String[]{"highestTeamIdSeen", "m"});
        return Collections.unmodifiableMap(result);
    }

    public static Map<String, Object> describeUnitSpatialCell(Object cell) {
        requireSpatialCell(cell);
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        Object allUnitsBucket = RustedReflection.getFieldValue(cell, new String[]{"allUnitsBucket", "a"});
        Object teamBuckets = RustedReflection.getFieldValue(cell, new String[]{"teamBuckets", "b"});
        Object teamIdMinusTwoBucket = RustedReflection.getFieldValue(cell,
                new String[]{"teamIdMinusTwoBucket", "c"});
        Object teamIdMinusOneBucket = RustedReflection.getFieldValue(cell,
                new String[]{"teamIdMinusOneBucket", "d"});
        result.put("allUnitsBucket", allUnitsBucket);
        result.put("allUnitsBucketSize", Integer.valueOf(bucketSizeOrZero(allUnitsBucket)));
        result.put("teamBuckets", teamBuckets);
        result.put("teamBucketsLength", Integer.valueOf(arrayLength(teamBuckets)));
        result.put("teamIdMinusTwoBucket", teamIdMinusTwoBucket);
        result.put("teamIdMinusTwoBucketSize", Integer.valueOf(bucketSizeOrZero(teamIdMinusTwoBucket)));
        result.put("teamIdMinusOneBucket", teamIdMinusOneBucket);
        result.put("teamIdMinusOneBucketSize", Integer.valueOf(bucketSizeOrZero(teamIdMinusOneBucket)));
        putFloatField(result, cell, "maxUnitRadius", new String[]{"maxUnitRadius", "e"});
        return Collections.unmodifiableMap(result);
    }

    public static Map<String, Object> describeUnitBucket(Object bucket) {
        requireBucket(bucket);
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        Object units = RustedReflection.getFieldValue(bucket, new String[]{"units", "c"});
        putIntField(result, bucket, "size", new String[]{"size", "b"});
        result.put("units", units);
        result.put("unitsLength", Integer.valueOf(arrayLength(units)));
        return Collections.unmodifiableMap(result);
    }

    public static List<Object> bucketUnitsSnapshot(Object bucket) {
        requireBucket(bucket);
        Object units = RustedReflection.getFieldValue(bucket, new String[]{"units", "c"});
        int size = RustedReflection.getIntField(bucket, new String[]{"size", "b"});
        return Collections.unmodifiableList(arrayPrefixSnapshot(units, size));
    }

    public static Map<String, Object> describeUnitSearchResultIterator(Object iterator) {
        requireResultIterator(iterator);
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        Object units = RustedReflection.getFieldValue(iterator, new String[]{"units", "b"});
        putIntField(result, iterator, "remainingCount", new String[]{"remainingCount", "a"});
        result.put("units", units);
        result.put("unitsLength", Integer.valueOf(arrayLength(units)));
        return Collections.unmodifiableMap(result);
    }

    public static Map<String, Object> describeUnitSpatialFields(Object unit) {
        requireUnit(unit);
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        putIntField(result, unit, "spatialCellX", new String[]{"spatialCellX", "dl"});
        putIntField(result, unit, "spatialCellY", new String[]{"spatialCellY", "dm"});
        putIntField(result, unit, "spatialTeamBucketId", new String[]{"spatialTeamBucketId", "dn"});
        return Collections.unmodifiableMap(result);
    }

    public static Object cellAt(Object index, int cellX, int cellY) {
        requireSpatialIndex(index);
        Object cells = RustedReflection.getFieldValue(index, new String[]{"cells", "e"});
        Object column = arrayValueAt(cells, cellX);
        return arrayValueAt(column, cellY);
    }

    public static int worldToCellX(Object index, float worldX) {
        requireSpatialIndex(index);
        Object value = RustedReflection.invokeInstance(index, new String[]{"worldToCellX", "a"},
                Float.valueOf(worldX));
        return value instanceof Number ? ((Number) value).intValue() : 0;
    }

    public static int worldToCellY(Object index, float worldY) {
        requireSpatialIndex(index);
        Object value = RustedReflection.invokeInstance(index, new String[]{"worldToCellY", "b"},
                Float.valueOf(worldY));
        return value instanceof Number ? ((Number) value).intValue() : 0;
    }

    public static Object getUnitsInRadius(Object index, float x, float y, float radius) {
        requireSpatialIndex(index);
        return RustedReflection.invokeInstance(index, new String[]{"getUnitsInRadius", "a"},
                Float.valueOf(x), Float.valueOf(y), Float.valueOf(radius));
    }

    public static Object getUnitsInRadiusIncludingCollisionRadius(Object index, float x, float y, float radius) {
        requireSpatialIndex(index);
        return RustedReflection.invokeInstance(index, new String[]{"getUnitsInRadiusIncludingCollisionRadius", "b"},
                Float.valueOf(x), Float.valueOf(y), Float.valueOf(radius));
    }

    public static void collectUnitsInRadius(Object index, float x, float y, float radius, Object outList) {
        requireSpatialIndex(index);
        RustedReflection.invokeInstance(index, new String[]{"collectUnitsInRadius", "a"},
                Float.valueOf(x), Float.valueOf(y), Float.valueOf(radius), outList);
    }

    public static void collectUnitsInRadiusIncludingCollisionRadius(Object index, float x, float y, float radius,
                                                                    Object outList) {
        requireSpatialIndex(index);
        RustedReflection.invokeInstance(index, new String[]{"collectUnitsInRadiusIncludingCollisionRadius", "b"},
                Float.valueOf(x), Float.valueOf(y), Float.valueOf(radius), outList);
    }

    public static void collectTeamUnitsInRadiusIncludingCollisionRadius(Object index, Object team,
                                                                        float x, float y, float radius,
                                                                        Object outList) {
        requireSpatialIndex(index);
        RustedReflection.invokeInstance(index,
                new String[]{"collectTeamUnitsInRadiusIncludingCollisionRadius", "a"},
                team, Float.valueOf(x), Float.valueOf(y), Float.valueOf(radius), outList);
    }

    public static void refreshAllUnits(Object index) {
        requireSpatialIndex(index);
        RustedReflection.invokeInstance(index, new String[]{"refreshAllUnits", "a"});
    }

    public static void updateUnitSpatialIndex(Object index, Object unit) {
        requireSpatialIndex(index);
        requireUnit(unit);
        RustedReflection.invokeInstance(index, new String[]{"updateUnitSpatialIndex", "a"}, unit);
    }

    public static void initializeForMap(Object index, Object map) {
        requireSpatialIndex(index);
        requireAny(map, MAP_ENGINE_CLASSES, "MapEngine");
        RustedReflection.invokeInstance(index, new String[]{"initializeForMap", "a"}, map);
    }

    public static void clearIndex(Object index) {
        requireSpatialIndex(index);
        RustedReflection.invokeInstance(index, new String[]{"clearIndex", "b"});
    }

    public static void update(Object index, float delta) {
        requireSpatialIndex(index);
        RustedReflection.invokeInstance(index, new String[]{"update", "c"}, Float.valueOf(delta));
    }

    private static void requireSpatialIndex(Object index) {
        requireAny(index, UNIT_SPATIAL_INDEX_CLASSES, "UnitSpatialIndex");
    }

    private static void requireSpatialCell(Object cell) {
        requireAny(cell, UNIT_SPATIAL_CELL_CLASSES, "UnitSpatialCell");
    }

    private static void requireBucket(Object bucket) {
        requireAny(bucket, UNIT_BUCKET_CLASSES, "UnitBucket");
    }

    private static void requireResultIterator(Object iterator) {
        requireAny(iterator, UNIT_SEARCH_RESULT_ITERATOR_CLASSES, "UnitSearchResultIterator");
    }

    private static void requireUnit(Object unit) {
        requireAny(unit, UNIT_CLASSES, "Unit");
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

    private static void putCollectionField(Map<String, Object> result, Object owner, String key,
                                           String[] fieldNames) {
        Object value = RustedReflection.getFieldValue(owner, fieldNames);
        result.put(key, value);
        result.put(key + "Size", Integer.valueOf(RustedReflection.snapshotIterable(value).size()));
    }

    private static void putIntField(Map<String, Object> result, Object owner, String key, String[] fieldNames) {
        result.put(key, Integer.valueOf(RustedReflection.getIntField(owner, fieldNames)));
    }

    private static void putFloatField(Map<String, Object> result, Object owner, String key, String[] fieldNames) {
        result.put(key, Float.valueOf(RustedReflection.getFloatField(owner, fieldNames)));
    }

    private static int bucketSizeOrZero(Object bucket) {
        if (bucket == null || !RustedReflection.isAnyClass(bucket.getClass(), UNIT_BUCKET_CLASSES)) {
            return 0;
        }
        return RustedReflection.getIntField(bucket, new String[]{"size", "b"});
    }

    private static int arrayLength(Object array) {
        return array != null && array.getClass().isArray() ? Array.getLength(array) : 0;
    }

    private static int nestedArrayLength(Object array) {
        Object first = arrayValueAt(array, 0);
        return arrayLength(first);
    }

    private static Object arrayValueAt(Object array, int index) {
        if (array == null || !array.getClass().isArray() || index < 0 || index >= Array.getLength(array)) {
            return null;
        }
        return Array.get(array, index);
    }

    private static List<Object> arrayPrefixSnapshot(Object array, int limit) {
        if (array == null || !array.getClass().isArray() || limit <= 0) {
            return Collections.emptyList();
        }
        int length = Math.min(Array.getLength(array), limit);
        java.util.ArrayList<Object> result = new java.util.ArrayList<Object>(length);
        for (int i = 0; i < length; i++) {
            result.add(Array.get(array, i));
        }
        return result;
    }
}
