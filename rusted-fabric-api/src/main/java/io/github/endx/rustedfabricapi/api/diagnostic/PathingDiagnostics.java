package io.github.endx.rustedfabricapi.api.diagnostic;

import io.github.endx.rustedfabricapi.api.util.RustedReflection;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Supplier;

public final class PathingDiagnostics {
    private static final String[] GAME_ENGINE_CLASSES = {
            "rustedwarfare.core.GameEngine",
            "com.corrodinggames.rts.gameFramework.l"
    };
    private static final String[] MOVEMENT_TYPE_CLASSES = {
            "rustedwarfare.unit.MovementType",
            "com.corrodinggames.rts.game.units.ao"
    };
    private static final String[] PATH_ENGINE_CLASSES = {
            "rustedwarfare.path.PathEngine",
            "com.corrodinggames.rts.gameFramework.k.l"
    };
    private static final String[] MOVEMENT_COST_MAP_CLASSES = {
            "rustedwarfare.path.MovementCostMap",
            "com.corrodinggames.rts.gameFramework.k.i"
    };
    private static final String[] PATH_REQUEST_CLASSES = {
            "rustedwarfare.path.PathRequest",
            "com.corrodinggames.rts.gameFramework.k.k"
    };
    private static final String[] PATH_NODE_CLASSES = {
            "rustedwarfare.path.PathNode",
            "com.corrodinggames.rts.gameFramework.k.p"
    };

    private static final MovementTypeAlias[] MOVEMENT_TYPE_ALIASES = {
            new MovementTypeAlias("none", "NONE", new String[]{"none", "a"}),
            new MovementTypeAlias("land", "LAND", new String[]{"land", "b"}),
            new MovementTypeAlias("building", "BUILDING", new String[]{"building", "c"}),
            new MovementTypeAlias("air", "AIR", new String[]{"air", "d"}),
            new MovementTypeAlias("water", "WATER", new String[]{"water", "e"}),
            new MovementTypeAlias("hover", "HOVER", new String[]{"hover", "f"}),
            new MovementTypeAlias("overCliff", "OVER_CLIFF", new String[]{"overCliff", "g"}),
            new MovementTypeAlias("overCliffWater", "OVER_CLIFF_WATER", new String[]{"overCliffWater", "h"})
    };

    private PathingDiagnostics() {
    }

    public static List<String> movementTypeNames() {
        List<String> result = new ArrayList<String>(MOVEMENT_TYPE_ALIASES.length);
        for (MovementTypeAlias alias : MOVEMENT_TYPE_ALIASES) {
            result.add(alias.namedName);
        }
        return Collections.unmodifiableList(result);
    }

    public static List<Object> movementTypes() {
        List<Object> result = new ArrayList<Object>(MOVEMENT_TYPE_ALIASES.length);
        for (MovementTypeAlias alias : MOVEMENT_TYPE_ALIASES) {
            result.add(RustedReflection.getStaticFieldValue(MOVEMENT_TYPE_CLASSES, alias.fieldNames));
        }
        return Collections.unmodifiableList(result);
    }

    public static Object movementType(String name) {
        return movementType(name, "movementType");
    }

    public static Object movementType(String name, String key) {
        String normalized = normalizeMovementTypeName(name);
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("movement type name must not be empty");
        }

        for (MovementTypeAlias alias : MOVEMENT_TYPE_ALIASES) {
            if (alias.matches(normalized)) {
                return RustedReflection.getStaticFieldValue(MOVEMENT_TYPE_CLASSES, alias.fieldNames);
            }
        }

        try {
            return RustedReflection.invokeStatic(MOVEMENT_TYPE_CLASSES,
                    new String[]{"parseMovementTypeOrThrow", "a"}, name, key);
        } catch (RuntimeException ignored) {
            throw new IllegalArgumentException("Unknown movement type '" + name
                    + "', expected one of " + movementTypeNames());
        }
    }

    public static Map<String, Object> describeMovementType(Object movementType) {
        requireMovementType(movementType);
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("canonicalName", canonicalMovementTypeName(movementType));
        result.put("value", movementType.toString());
        if (movementType instanceof Enum) {
            Enum<?> enumValue = (Enum<?>) movementType;
            result.put("enumName", enumValue.name());
            result.put("ordinal", Integer.valueOf(enumValue.ordinal()));
        }
        return Collections.unmodifiableMap(result);
    }

    public static String canonicalMovementTypeName(Object movementType) {
        if (movementType == null) {
            return null;
        }

        String enumName = movementType instanceof Enum ? ((Enum<?>) movementType).name() : movementType.toString();
        String normalized = normalizeMovementTypeName(enumName);
        for (MovementTypeAlias alias : MOVEMENT_TYPE_ALIASES) {
            Object candidate = RustedReflection.getStaticFieldValue(MOVEMENT_TYPE_CLASSES, alias.fieldNames);
            if (candidate == movementType || candidate.equals(movementType) || alias.matches(normalized)) {
                return alias.namedName;
            }
        }
        return movementType.toString();
    }

    public static Object getMovementType(Object unitOrUnitType) {
        if (unitOrUnitType == null) {
            throw new IllegalArgumentException("unitOrUnitType must not be null");
        }
        return RustedReflection.invokeInstance(unitOrUnitType, new String[]{"getMovementType", "h", "o"});
    }

    public static Object pathEngineFromGameEngine(Object gameEngine) {
        requireAny(gameEngine, GAME_ENGINE_CLASSES, "GameEngine");
        return RustedReflection.getFieldValue(gameEngine, new String[]{"pathfindingEngine", "bU"});
    }

    public static Map<String, Object> describePathEngine(Object pathEngine) {
        requirePathEngine(pathEngine);
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        putField(result, pathEngine, "mainSolver", new String[]{"mainSolver", "o"});
        putBooleanField(result, pathEngine, "isRunning", new String[]{"isRunning", "p"});
        putField(result, pathEngine, "map", new String[]{"map", "q"});
        putIntField(result, pathEngine, "mapChecksum", new String[]{"mapChecksum", "r"});
        putNumberField(result, pathEngine, "mapWidth", new String[]{"mapWidth", "s"});
        putNumberField(result, pathEngine, "mapHeight", new String[]{"mapHeight", "t"});
        putCollectionField(result, pathEngine, "movementCostMaps", new String[]{"movementCostMaps", "u"});
        putArrayLengthField(result, pathEngine, "movementCostMapArrayLength",
                new String[]{"movementCostMapArray", "v"});
        putField(result, pathEngine, "noneCosts", new String[]{"noneCosts", "x"});
        putField(result, pathEngine, "landCosts", new String[]{"landCosts", "y"});
        putField(result, pathEngine, "buildingCosts", new String[]{"buildingCosts", "z"});
        putField(result, pathEngine, "waterCosts", new String[]{"waterCosts", "A"});
        putField(result, pathEngine, "airCosts", new String[]{"airCosts", "B"});
        putField(result, pathEngine, "hoverCosts", new String[]{"hoverCosts", "C"});
        putField(result, pathEngine, "overCliffCosts", new String[]{"overCliffCosts", "D"});
        putField(result, pathEngine, "overCliffWaterCosts", new String[]{"overCliffWaterCosts", "E"});
        putCollectionField(result, pathEngine, "extraSolvers", new String[]{"extraSolvers", "H"});
        putCollectionField(result, pathEngine, "pendingHighPriorityPaths",
                new String[]{"pendingHighPriorityPaths", "I"});
        putCollectionField(result, pathEngine, "pendingLowPriorityPaths",
                new String[]{"pendingLowPriorityPaths", "J"});
        putOptional(result, "hasPendingPaths", new Supplier<Object>() {
            @Override
            public Object get() {
                return Boolean.valueOf(hasPendingPaths(pathEngine));
            }
        });
        putOptional(result, "queueDebugString", new Supplier<Object>() {
            @Override
            public Object get() {
                return queueDebugString(pathEngine);
            }
        });
        return Collections.unmodifiableMap(result);
    }

    public static List<Object> movementCostMapsSnapshot(Object pathEngine) {
        requirePathEngine(pathEngine);
        return Collections.unmodifiableList(RustedReflection.snapshotIterable(
                RustedReflection.getFieldValue(pathEngine, new String[]{"movementCostMaps", "u"})));
    }

    public static Object getCostsForMovementType(Object pathEngine, Object movementType) {
        requirePathEngine(pathEngine);
        requireMovementType(movementType);
        return RustedReflection.invokeInstance(pathEngine, new String[]{"getCostsForMovementType", "a"}, movementType);
    }

    public static boolean isTileBlocked(Object pathEngine, Object movementType, int tileX, int tileY) {
        requirePathEngine(pathEngine);
        requireMovementType(movementType);
        return Boolean.TRUE.equals(RustedReflection.invokeInstance(pathEngine,
                new String[]{"isTileBlocked", "a"}, movementType, Integer.valueOf(tileX), Integer.valueOf(tileY)));
    }

    public static boolean isTileBlockedIgnoringBuildingCost(Object pathEngine, Object movementType,
                                                            int tileX, int tileY) {
        requirePathEngine(pathEngine);
        requireMovementType(movementType);
        return Boolean.TRUE.equals(RustedReflection.invokeInstance(pathEngine,
                new String[]{"isTileBlockedIgnoringBuildingCost", "b"},
                movementType, Integer.valueOf(tileX), Integer.valueOf(tileY)));
    }

    public static boolean isCostMapTileBlocked(Object pathEngine, Object costMap, int tileX, int tileY) {
        requirePathEngine(pathEngine);
        requireMovementCostMap(costMap);
        return Boolean.TRUE.equals(RustedReflection.invokeInstance(pathEngine,
                new String[]{"isCostMapTileBlocked", "a"}, costMap, Integer.valueOf(tileX), Integer.valueOf(tileY)));
    }

    public static boolean isCostMapTileBlockedWithOptions(Object pathEngine, Object costMap, int tileX, int tileY,
                                                          boolean includeBuildingCost) {
        requirePathEngine(pathEngine);
        requireMovementCostMap(costMap);
        return Boolean.TRUE.equals(RustedReflection.invokeInstance(pathEngine,
                new String[]{"isCostMapTileBlockedWithOptions", "a"}, costMap, Integer.valueOf(tileX),
                Integer.valueOf(tileY), Boolean.valueOf(includeBuildingCost)));
    }

    public static int getTileTotalPathCost(Object pathEngine, Object costMap, int tileX, int tileY) {
        requirePathEngine(pathEngine);
        requireMovementCostMap(costMap);
        Object value = RustedReflection.invokeInstance(pathEngine, new String[]{"getTileTotalPathCost", "b"},
                costMap, Integer.valueOf(tileX), Integer.valueOf(tileY));
        return value instanceof Number ? ((Number) value).intValue() : 0;
    }

    public static int getTileClearanceCost(Object pathEngine, Object costMap, int tileX, int tileY) {
        requirePathEngine(pathEngine);
        requireMovementCostMap(costMap);
        Object value = RustedReflection.invokeInstance(pathEngine, new String[]{"getTileClearanceCost", "c"},
                costMap, Integer.valueOf(tileX), Integer.valueOf(tileY));
        return value instanceof Number ? ((Number) value).intValue() : 0;
    }

    public static boolean hasPendingPaths(Object pathEngine) {
        requirePathEngine(pathEngine);
        return Boolean.TRUE.equals(RustedReflection.invokeInstance(pathEngine, new String[]{"hasPendingPaths", "e"}));
    }

    public static String queueDebugString(Object pathEngine) {
        requirePathEngine(pathEngine);
        Object value = RustedReflection.invokeInstance(pathEngine, new String[]{"getQueueDebugString", "f"});
        return value != null ? value.toString() : null;
    }

    public static Map<String, Object> describeMovementCostMap(Object costMap) {
        requireMovementCostMap(costMap);
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        Object movementType = RustedReflection.getFieldValue(costMap, new String[]{"movementType", "a"});
        result.put("movementType", movementType);
        result.put("movementTypeName", canonicalMovementTypeName(movementType));
        putIntField(result, costMap, "width", new String[]{"width", "b"});
        putIntField(result, costMap, "height", new String[]{"height", "c"});
        putArrayLengthField(result, costMap, "tileCostsLength", new String[]{"tileCosts", "d"});
        putArrayLengthField(result, costMap, "buildingCostsLength", new String[]{"buildingCosts", "e"});
        putArrayLengthField(result, costMap, "objectCostsLength", new String[]{"objectCosts", "f"});
        putArrayLengthField(result, costMap, "isolatedGroupsLength", new String[]{"isolatedGroups", "g"});
        putCollectionField(result, costMap, "isolatedGroupSizes", new String[]{"isolatedGroupSizes", "h"});
        putIntField(result, costMap, "isolatedGroupCount", new String[]{"isolatedGroupCount", "i"});
        putArrayLengthField(result, costMap, "clearanceCostsLength", new String[]{"clearanceCosts", "j"});
        putIntField(result, costMap, "clearanceDirtyTileIndex", new String[]{"clearanceDirtyTileIndex", "k"});
        putIntField(result, costMap, "clearanceRebuildThrottle", new String[]{"clearanceRebuildThrottle", "l"});
        putBooleanField(result, costMap, "clearanceRebuildSkippedWarning",
                new String[]{"clearanceRebuildSkippedWarning", "m"});
        putField(result, costMap, "scratchPoint", new String[]{"scratchPoint", "n"});
        putBooleanField(result, costMap, "costsDirty", new String[]{"costsDirty", "o"});
        putIntField(result, costMap, "lastRefreshFrame", new String[]{"lastRefreshFrame", "p"});
        return Collections.unmodifiableMap(result);
    }

    public static Map<String, Object> sampleMovementCostMapTile(Object costMap, int tileX, int tileY) {
        requireMovementCostMap(costMap);
        int width = RustedReflection.getIntField(costMap, new String[]{"width", "b"});
        int height = RustedReflection.getIntField(costMap, new String[]{"height", "c"});
        boolean inBounds = tileX >= 0 && tileY >= 0 && tileX < width && tileY < height;
        int index = inBounds ? tileX * height + tileY : -1;

        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("x", Integer.valueOf(tileX));
        result.put("y", Integer.valueOf(tileY));
        result.put("index", Integer.valueOf(index));
        result.put("inBounds", Boolean.valueOf(inBounds));
        result.put("tileCost", arrayValueAt(
                RustedReflection.getFieldValue(costMap, new String[]{"tileCosts", "d"}), index));
        result.put("buildingCost", arrayValueAt(
                RustedReflection.getFieldValue(costMap, new String[]{"buildingCosts", "e"}), index));
        result.put("objectCost", arrayValueAt(
                RustedReflection.getFieldValue(costMap, new String[]{"objectCosts", "f"}), index));
        result.put("isolatedGroup", arrayValueAt(
                RustedReflection.getFieldValue(costMap, new String[]{"isolatedGroups", "g"}), index));
        result.put("clearanceCost", arrayValueAt(
                RustedReflection.getFieldValue(costMap, new String[]{"clearanceCosts", "j"}), index));
        return Collections.unmodifiableMap(result);
    }

    public static Map<String, Object> describePathRequest(Object request) {
        requirePathRequest(request);
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        putField(result, request, "pathEngine", new String[]{"pathEngine", "a"});
        putIntField(result, request, "requestId", new String[]{"requestId", "e"});
        putIntField(result, request, "createdFrame", new String[]{"createdFrame", "g"});
        putNumberField(result, request, "startTileX", new String[]{"startTileX", "h"});
        putNumberField(result, request, "startTileY", new String[]{"startTileY", "i"});
        putField(result, request, "targetRadiusOverride", new String[]{"targetRadiusOverride", "j"});
        putBooleanField(result, request, "lowPriority", new String[]{"lowPriority", "k"});
        putNumberField(result, request, "endTileX", new String[]{"endTileX", "l"});
        putNumberField(result, request, "endTileY", new String[]{"endTileY", "m"});
        putNumberField(result, request, "endRadius", new String[]{"endRadius", "n"});
        Object movementType = RustedReflection.getFieldValue(request, new String[]{"movementType", "o"});
        result.put("movementType", movementType);
        result.put("movementTypeName", canonicalMovementTypeName(movementType));
        putBooleanField(result, request, "isQueued", new String[]{"isQueued", "p"});
        putIntField(result, request, "solveFrame", new String[]{"solveFrame", "q"});
        putBooleanField(result, request, "failed", new String[]{"failed", "r"});
        putFloatField(result, request, "elapsedSolveTime", new String[]{"elapsedSolveTime", "s"});
        putFloatField(result, request, "allowedDelay", new String[]{"allowedDelay", "t"});
        putBooleanField(result, request, "returnPathInMultiplayer", new String[]{"returnPathInMultiplayer", "u"});
        putCollectionField(result, request, "pathNodes", new String[]{"pathNodes", "x"});
        putArrayLengthField(result, request, "tileCostsLength", new String[]{"tileCosts", "y"});
        putArrayLengthField(result, request, "buildingCostsLength", new String[]{"buildingCosts", "z"});
        putArrayLengthField(result, request, "objectCostsLength", new String[]{"objectCosts", "A"});
        putArrayLengthField(result, request, "isolatedGroupsLength", new String[]{"isolatedGroups", "B"});
        putArrayLengthField(result, request, "clearanceCostsLength", new String[]{"clearanceCosts", "C"});
        putOptional(result, "isImmediate", new Supplier<Object>() {
            @Override
            public Object get() {
                return Boolean.valueOf(isImmediatePathRequest(request));
            }
        });
        putOptional(result, "hasPath", new Supplier<Object>() {
            @Override
            public Object get() {
                return Boolean.valueOf(pathRequestHasPath(request));
            }
        });
        return Collections.unmodifiableMap(result);
    }

    public static boolean isImmediatePathRequest(Object request) {
        requirePathRequest(request);
        return Boolean.TRUE.equals(RustedReflection.invokeInstance(request, new String[]{"isImmediate", "b"}));
    }

    public static boolean pathRequestHasPath(Object request) {
        requirePathRequest(request);
        return Boolean.TRUE.equals(RustedReflection.invokeInstance(request, new String[]{"hasPath", "c"}));
    }

    public static List<Object> pathRequestPathNodesSnapshot(Object request) {
        requirePathRequest(request);
        return Collections.unmodifiableList(RustedReflection.snapshotIterable(
                RustedReflection.getFieldValue(request, new String[]{"pathNodes", "x"})));
    }

    public static Map<String, Object> describePathNode(Object node) {
        requirePathNode(node);
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        putNumberField(result, node, "x", new String[]{"x", "a"});
        putNumberField(result, node, "y", new String[]{"y", "b"});
        return Collections.unmodifiableMap(result);
    }

    private static void requireMovementType(Object movementType) {
        requireAny(movementType, MOVEMENT_TYPE_CLASSES, "MovementType");
    }

    private static void requirePathEngine(Object pathEngine) {
        requireAny(pathEngine, PATH_ENGINE_CLASSES, "PathEngine");
    }

    private static void requireMovementCostMap(Object costMap) {
        requireAny(costMap, MOVEMENT_COST_MAP_CLASSES, "MovementCostMap");
    }

    private static void requirePathRequest(Object request) {
        requireAny(request, PATH_REQUEST_CLASSES, "PathRequest");
    }

    private static void requirePathNode(Object node) {
        requireAny(node, PATH_NODE_CLASSES, "PathNode");
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

    private static void putArrayLengthField(Map<String, Object> result, Object owner, String key,
                                            String[] fieldNames) {
        result.put(key, Integer.valueOf(arrayLength(RustedReflection.getFieldValue(owner, fieldNames))));
    }

    private static void putIntField(Map<String, Object> result, Object owner, String key, String[] fieldNames) {
        result.put(key, Integer.valueOf(RustedReflection.getIntField(owner, fieldNames)));
    }

    private static void putNumberField(Map<String, Object> result, Object owner, String key, String[] fieldNames) {
        Object value = RustedReflection.getFieldValue(owner, fieldNames);
        result.put(key, value instanceof Number ? Integer.valueOf(((Number) value).intValue()) : value);
    }

    private static void putFloatField(Map<String, Object> result, Object owner, String key, String[] fieldNames) {
        result.put(key, Float.valueOf(RustedReflection.getFloatField(owner, fieldNames)));
    }

    private static void putBooleanField(Map<String, Object> result, Object owner, String key, String[] fieldNames) {
        result.put(key, Boolean.valueOf(RustedReflection.getBooleanField(owner, fieldNames)));
    }

    private static void putOptional(Map<String, Object> result, String key, Supplier<Object> valueSupplier) {
        try {
            result.put(key, valueSupplier.get());
        } catch (RuntimeException e) {
            result.put(key + "Error", e.getClass().getName() + ": " + e.getMessage());
        }
    }

    private static int arrayLength(Object array) {
        return array != null && array.getClass().isArray() ? Array.getLength(array) : 0;
    }

    private static Object arrayValueAt(Object array, int index) {
        if (array == null || !array.getClass().isArray() || index < 0 || index >= Array.getLength(array)) {
            return null;
        }
        Object value = Array.get(array, index);
        return value instanceof Number ? Integer.valueOf(((Number) value).intValue()) : value;
    }

    private static String normalizeMovementTypeName(String name) {
        if (name == null) {
            return "";
        }
        StringBuilder result = new StringBuilder();
        String value = name.trim().toLowerCase(Locale.ROOT);
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c != '_' && c != '-' && c != ' ') {
                result.append(c);
            }
        }
        return result.toString();
    }

    private static final class MovementTypeAlias {
        private final String namedName;
        private final String enumName;
        private final String[] fieldNames;

        private MovementTypeAlias(String namedName, String enumName, String[] fieldNames) {
            this.namedName = namedName;
            this.enumName = enumName;
            this.fieldNames = fieldNames;
        }

        private boolean matches(String normalizedName) {
            return normalizeMovementTypeName(namedName).equals(normalizedName)
                    || normalizeMovementTypeName(enumName).equals(normalizedName);
        }
    }
}
