package io.github.endx.rustedfabricapi.api.diagnostic;

import io.github.endx.rustedfabricapi.api.util.RustedReflection;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class WaypointDiagnostics {
    private static final String[] ORDERABLE_UNIT_CLASSES = {
            "rustedwarfare.unit.OrderableUnit",
            "com.corrodinggames.rts.game.units.y"
    };
    private static final String[] UNIT_ORDER_CLASSES = {
            "rustedwarfare.unit.UnitOrder",
            "com.corrodinggames.rts.game.units.au"
    };
    private static final String[] UNIT_ORDER_TYPE_CLASSES = {
            "rustedwarfare.unit.UnitOrderType",
            "com.corrodinggames.rts.game.units.av"
    };
    private static final String[] UNIT_ATTACK_MODE_CLASSES = {
            "rustedwarfare.unit.UnitAttackMode",
            "com.corrodinggames.rts.game.units.a"
    };
    private static final String[] UNIT_DEATH_EFFECT_TYPE_CLASSES = {
            "rustedwarfare.unit.UnitDeathEffectType",
            "com.corrodinggames.rts.game.units.ab"
    };
    private static final String[] UNIT_ORDER_SOUND_TYPE_CLASSES = {
            "rustedwarfare.unit.UnitOrderSoundType",
            "com.corrodinggames.rts.game.units.ag"
    };
    private static final String[] UNIT_PATH_POINT_CLASSES = {
            "rustedwarfare.unit.path.UnitPathPoint",
            "com.corrodinggames.rts.game.units.af"
    };
    private static final String[] WAYPOINT_UPDATE_STATE_CLASSES = {
            "rustedwarfare.unit.path.WaypointUpdateState",
            "com.corrodinggames.rts.game.units.ad"
    };
    private static final String[] FORMATION_GROUP_CLASSES = {
            "rustedwarfare.formation.FormationGroup",
            "com.corrodinggames.rts.gameFramework.ab"
    };
    private static final String[] FORMATION_MANAGER_CLASSES = {
            "rustedwarfare.formation.FormationManager",
            "com.corrodinggames.rts.gameFramework.aa"
    };
    private static final String[] SHARED_PATH_CACHE_ENTRY_CLASSES = {
            "rustedwarfare.path.SharedPathCacheEntry",
            "com.corrodinggames.rts.gameFramework.d"
    };
    private static final String[] ACTION_EXECUTION_RESULT_CLASSES = {
            "rustedwarfare.unit.action.ActionExecutionResult",
            "com.corrodinggames.rts.game.units.z"
    };

    private static final EnumAlias[] ORDER_TYPE_ALIASES = {
            new EnumAlias("move", new String[]{"move", "a"}),
            new EnumAlias("attack", new String[]{"attack", "b"}),
            new EnumAlias("build", new String[]{"build", "c"}),
            new EnumAlias("repair", new String[]{"repair", "d"}),
            new EnumAlias("loadInto", new String[]{"loadInto", "e"}),
            new EnumAlias("unloadAt", new String[]{"unloadAt", "f"}),
            new EnumAlias("reclaim", new String[]{"reclaim", "g"}),
            new EnumAlias("attackMove", new String[]{"attackMove", "h"}),
            new EnumAlias("loadUp", new String[]{"loadUp", "i"}),
            new EnumAlias("patrol", new String[]{"patrol", "j"}),
            new EnumAlias("guard", new String[]{"guard", "k"}),
            new EnumAlias("guardAt", new String[]{"guardAt", "l"}),
            new EnumAlias("touchTarget", new String[]{"touchTarget", "m"}),
            new EnumAlias("follow", new String[]{"follow", "n"}),
            new EnumAlias("triggerAction", new String[]{"triggerAction", "o"}),
            new EnumAlias("triggerActionWhenInRange", new String[]{"triggerActionWhenInRange", "p"}),
            new EnumAlias("setPassiveTarget", new String[]{"setPassiveTarget", "q"})
    };

    private static final EnumAlias[] ATTACK_MODE_ALIASES = {
            new EnumAlias("outOfRange", new String[]{"outOfRange", "a"}),
            new EnumAlias("onlyInRange", new String[]{"onlyInRange", "b"}),
            new EnumAlias("returnFire", new String[]{"returnFire", "c"}),
            new EnumAlias("holdFire", new String[]{"holdFire", "d"}),
            new EnumAlias("guardArea", new String[]{"guardArea", "e"}),
            new EnumAlias("aggressive", new String[]{"aggressive", "f"}),
            new EnumAlias("mixed", new String[]{"mixed", "g"})
    };

    private static final EnumAlias[] DEATH_EFFECT_TYPE_ALIASES = {
            new EnumAlias("verySmall", new String[]{"verySmall", "a"}),
            new EnumAlias("small", new String[]{"small", "b"}),
            new EnumAlias("normal", new String[]{"normal", "c"}),
            new EnumAlias("large", new String[]{"large", "d"}),
            new EnumAlias("largeUnit", new String[]{"largeUnit", "e"}),
            new EnumAlias("building", new String[]{"building", "f"}),
            new EnumAlias("buildingNoShockwaveOrSmoke", new String[]{"buildingNoShockwaveOrSmoke", "g"}),
            new EnumAlias("veryLargeBuilding", new String[]{"veryLargeBuilding", "h"})
    };

    private static final EnumAlias[] ORDER_SOUND_TYPE_ALIASES = {
            new EnumAlias("attack", new String[]{"attack", "a"}),
            new EnumAlias("move", new String[]{"move", "b"}),
            new EnumAlias("newSelection", new String[]{"newSelection", "c"})
    };

    private WaypointDiagnostics() {
    }

    public static List<String> unitOrderTypeNames() {
        return aliasNames(ORDER_TYPE_ALIASES);
    }

    public static List<Object> unitOrderTypes() {
        return aliasValues(UNIT_ORDER_TYPE_CLASSES, ORDER_TYPE_ALIASES);
    }

    public static List<String> unitAttackModeNames() {
        return aliasNames(ATTACK_MODE_ALIASES);
    }

    public static List<Object> unitAttackModes() {
        return aliasValues(UNIT_ATTACK_MODE_CLASSES, ATTACK_MODE_ALIASES);
    }

    public static List<String> unitDeathEffectTypeNames() {
        return aliasNames(DEATH_EFFECT_TYPE_ALIASES);
    }

    public static List<Object> unitDeathEffectTypes() {
        return aliasValues(UNIT_DEATH_EFFECT_TYPE_CLASSES, DEATH_EFFECT_TYPE_ALIASES);
    }

    public static List<String> unitOrderSoundTypeNames() {
        return aliasNames(ORDER_SOUND_TYPE_ALIASES);
    }

    public static List<Object> unitOrderSoundTypes() {
        return aliasValues(UNIT_ORDER_SOUND_TYPE_CLASSES, ORDER_SOUND_TYPE_ALIASES);
    }

    public static Map<String, Object> describeOrderableUnitWaypoints(Object unit) {
        requireOrderableUnit(unit);
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        putIntField(result, unit, "waypointCount", new String[]{"waypointCount", "f"});
        putArrayLengthField(result, unit, "waypointQueueLength", new String[]{"waypointQueue", "g"});
        Object attackMode = RustedReflection.getFieldValue(unit, new String[]{"P"});
        result.put("attackMode", attackMode);
        result.put("attackModeName", canonicalAliasName(UNIT_ATTACK_MODE_CLASSES, ATTACK_MODE_ALIASES, attackMode));
        result.put("hasNoWaypoints", Boolean.valueOf(waypointsSnapshot(unit).isEmpty()));
        result.put("hasAttackWaypoint", Boolean.valueOf(hasOrderType(unit, "attack")));
        result.put("activeWaypoint", firstOrNull(waypointsSnapshot(unit), 0));
        result.put("nextWaypoint", firstOrNull(waypointsSnapshot(unit), 1));
        result.put("lastWaypoint", lastOrNull(waypointsSnapshot(unit)));
        return Collections.unmodifiableMap(result);
    }

    public static List<Object> waypointsSnapshot(Object unit) {
        requireOrderableUnit(unit);
        int count = Math.max(0, RustedReflection.getIntField(unit, new String[]{"waypointCount", "f"}));
        Object queue = RustedReflection.getFieldValue(unit, new String[]{"waypointQueue", "g"});
        return boundedArraySnapshot(queue, count);
    }

    public static List<Map<String, Object>> describeWaypointsSnapshot(Object unit) {
        List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
        for (Object waypoint : waypointsSnapshot(unit)) {
            if (waypoint != null) {
                result.add(describeUnitOrder(waypoint));
            }
        }
        return Collections.unmodifiableList(result);
    }

    public static Map<String, Object> describeOrderableUnitPathing(Object unit) {
        requireOrderableUnit(unit);
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        putFloatField(result, unit, "pathTargetX", new String[]{"pathTargetX", "o"});
        putFloatField(result, unit, "pathTargetY", new String[]{"pathTargetY", "p"});
        putNumberField(result, unit, "pathRetryCount", new String[]{"pathRetryCount", "q"});
        putBooleanField(result, unit, "activePathTruncated", new String[]{"activePathTruncated", "u"});
        putIntField(result, unit, "activePathPointCountSnapshot",
                new String[]{"activePathPointCountSnapshot", "v"});
        putField(result, unit, "pathTargetAdapter", new String[]{"pathTargetAdapter", "au"});
        putArrayLengthField(result, unit, "activePathPointsLength", new String[]{"activePathPoints", "av"});
        putIntField(result, unit, "activePathPointCount", new String[]{"activePathPointCount", "aw"});
        putField(result, unit, "pendingPathRequest", new String[]{"pendingPathRequest", "aU"});
        putCollectionField(result, "recentImmediatePathRequests",
                RustedReflection.getStaticFieldValue(ORDERABLE_UNIT_CLASSES,
                        new String[]{"recentImmediatePathRequests", "aV"}));
        result.put("currentPathPoint", firstOrNull(activePathPointsSnapshot(unit), 0));
        result.put("nextPathPoint", firstOrNull(activePathPointsSnapshot(unit), 1));
        return Collections.unmodifiableMap(result);
    }

    public static List<Object> activePathPointsSnapshot(Object unit) {
        requireOrderableUnit(unit);
        int count = Math.max(0, RustedReflection.getIntField(unit, new String[]{"activePathPointCount", "aw"}));
        Object points = RustedReflection.getFieldValue(unit, new String[]{"activePathPoints", "av"});
        return boundedArraySnapshot(points, count);
    }

    public static List<Map<String, Object>> describeActivePathPointsSnapshot(Object unit) {
        List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
        for (Object point : activePathPointsSnapshot(unit)) {
            if (point != null) {
                result.add(describeUnitPathPoint(point));
            }
        }
        return Collections.unmodifiableList(result);
    }

    public static Map<String, Object> describeUnitOrder(Object order) {
        requireUnitOrder(order);
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        Object orderType = RustedReflection.getFieldValue(order, new String[]{"orderType", "a"});
        result.put("orderType", orderType);
        result.put("orderTypeName", canonicalAliasName(UNIT_ORDER_TYPE_CLASSES, ORDER_TYPE_ALIASES, orderType));
        putField(result, order, "buildUnitType", new String[]{"buildUnitType", "b"});
        putField(result, order, "actionId", new String[]{"actionId", "c"});
        putIntField(result, order, "buildIndex", new String[]{"buildIndex", "d"});
        putFloatField(result, order, "x", new String[]{"x", "e"});
        putFloatField(result, order, "y", new String[]{"y", "f"});
        putField(result, order, "targetUnitId", new String[]{"targetUnitId", "g"});
        putField(result, order, "targetUnit", new String[]{"targetUnit", "h"});
        putField(result, order, "formationGroup", new String[]{"formationGroup", "i"});
        putBooleanField(result, order, "queueByPlayer", new String[]{"queueByPlayer", "j"});
        putFloatField(result, order, "maxTime", new String[]{"maxTime", "k"});
        putFloatField(result, order, "expiresAtTime", new String[]{"expiresAtTime", "l"});
        putBooleanField(result, order, "repeatable", new String[]{"repeatable", "m"});
        putBooleanField(result, order, "skipAvailabilityChecks", new String[]{"skipAvailabilityChecks", "n"});
        putOptionalBooleanMethod(result, order, "hasUnitTarget", new String[]{"hasUnitTarget", "f"});
        putOptionalFloatMethod(result, order, "targetX", new String[]{"getTargetX", "g"});
        putOptionalFloatMethod(result, order, "targetY", new String[]{"getTargetY", "h"});
        putOptionalMethod(result, order, "targetUnitResolved", new String[]{"getTargetUnit", "i"});
        putOptionalMethod(result, order, "checksum", new String[]{"getChecksum", "j"});
        return Collections.unmodifiableMap(result);
    }

    public static boolean unitOrderMatches(Object left, Object right) {
        requireUnitOrder(left);
        requireUnitOrder(right);
        return Boolean.TRUE.equals(RustedReflection.invokeInstance(left, new String[]{"matchesWaypoint", "b"}, right));
    }

    public static boolean unitOrderNearSamePosition(Object left, Object right) {
        requireUnitOrder(left);
        requireUnitOrder(right);
        return Boolean.TRUE.equals(RustedReflection.invokeInstance(left,
                new String[]{"isNearSamePosition", "a"}, right));
    }

    public static Map<String, Object> describeUnitPathPoint(Object point) {
        requireUnitPathPoint(point);
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        putFloatField(result, point, "x", new String[]{"x", "a"});
        putFloatField(result, point, "y", new String[]{"y", "b"});
        return Collections.unmodifiableMap(result);
    }

    public static Map<String, Object> describeWaypointUpdateState(Object state) {
        requireWaypointUpdateState(state);
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        putBooleanField(result, state, "reachedOrExecuted", new String[]{"reachedOrExecuted", "a"});
        putBooleanField(result, state, "movementHandled", new String[]{"movementHandled", "b"});
        putBooleanField(result, state, "specialActionComplete", new String[]{"specialActionComplete", "c"});
        putBooleanField(result, state, "keepProcessing", new String[]{"keepProcessing", "d"});
        putFloatField(result, state, "resultX", new String[]{"resultX", "e"});
        putFloatField(result, state, "resultY", new String[]{"resultY", "f"});
        return Collections.unmodifiableMap(result);
    }

    public static Map<String, Object> describeFormationManager(Object manager) {
        requireFormationManager(manager);
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        putIntField(result, manager, "nextFormationId", new String[]{"nextFormationId", "a"});
        putField(result, manager, "scratchPoint", new String[]{"scratchPoint", "b"});
        return Collections.unmodifiableMap(result);
    }

    public static Map<String, Object> describeFormationGroup(Object group) {
        requireFormationGroup(group);
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        putCollectionField(result, "members", RustedReflection.getFieldValue(group, new String[]{"members", "a"}));
        putBooleanField(result, group, "active", new String[]{"active", "b"});
        putFloatField(result, group, "centerX", new String[]{"centerX", "c"});
        putFloatField(result, group, "centerY", new String[]{"centerY", "d"});
        putIntField(result, group, "groupId", new String[]{"groupId", "e"});
        putBooleanField(result, group, "hasSharedPaths", new String[]{"hasSharedPaths", "f"});
        putCollectionField(result, "sharedPathCacheEntries",
                RustedReflection.getFieldValue(group, new String[]{"sharedPathCacheEntries", "g"}));
        putField(result, group, "formationManager", new String[]{"formationManager", "h"});
        return Collections.unmodifiableMap(result);
    }

    public static List<Object> formationMembersSnapshot(Object group) {
        requireFormationGroup(group);
        return Collections.unmodifiableList(RustedReflection.snapshotIterable(
                RustedReflection.getFieldValue(group, new String[]{"members", "a"})));
    }

    public static List<Object> sharedPathCacheEntriesSnapshot(Object group) {
        requireFormationGroup(group);
        return Collections.unmodifiableList(RustedReflection.snapshotIterable(
                RustedReflection.getFieldValue(group, new String[]{"sharedPathCacheEntries", "g"})));
    }

    public static Map<String, Object> describeSharedPathCacheEntry(Object entry) {
        requireSharedPathCacheEntry(entry);
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        putField(result, entry, "pathRequest", new String[]{"pathRequest", "a"});
        putField(result, entry, "createdTime", new String[]{"createdTime", "b"});
        putFloatField(result, entry, "startWorldX", new String[]{"startWorldX", "c"});
        putFloatField(result, entry, "startWorldY", new String[]{"startWorldY", "d"});
        putFloatField(result, entry, "targetWorldX", new String[]{"targetWorldX", "e"});
        putFloatField(result, entry, "targetWorldY", new String[]{"targetWorldY", "f"});
        putIntField(result, entry, "createdFrame", new String[]{"createdFrame", "g"});
        Object movementType = RustedReflection.getFieldValue(entry, new String[]{"movementType", "h"});
        result.put("movementType", movementType);
        result.put("movementTypeName", PathingDiagnostics.canonicalMovementTypeName(movementType));
        return Collections.unmodifiableMap(result);
    }

    public static Map<String, Object> describeActionExecutionResult(Object resultObject) {
        requireActionExecutionResult(resultObject);
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        putField(result, resultObject, "sourceUnit", new String[]{"sourceUnit", "a"});
        putField(result, resultObject, "createdUnit", new String[]{"createdUnit", "b"});
        putBooleanField(result, resultObject, "queued", new String[]{"queued", "c"});
        putField(result, resultObject, "action", new String[]{"action", "d"});
        return Collections.unmodifiableMap(result);
    }

    private static boolean hasOrderType(Object unit, String orderTypeName) {
        String normalized = normalize(orderTypeName);
        for (Object waypoint : waypointsSnapshot(unit)) {
            if (waypoint == null) {
                continue;
            }
            Object orderType = RustedReflection.getFieldValue(waypoint, new String[]{"orderType", "a"});
            if (normalize(canonicalAliasName(UNIT_ORDER_TYPE_CLASSES, ORDER_TYPE_ALIASES, orderType))
                    .equals(normalized)) {
                return true;
            }
        }
        return false;
    }

    private static List<String> aliasNames(EnumAlias[] aliases) {
        List<String> result = new ArrayList<String>(aliases.length);
        for (EnumAlias alias : aliases) {
            result.add(alias.name);
        }
        return Collections.unmodifiableList(result);
    }

    private static List<Object> aliasValues(String[] classNames, EnumAlias[] aliases) {
        List<Object> result = new ArrayList<Object>(aliases.length);
        for (EnumAlias alias : aliases) {
            result.add(RustedReflection.getStaticFieldValue(classNames, alias.fieldNames));
        }
        return Collections.unmodifiableList(result);
    }

    private static String canonicalAliasName(String[] classNames, EnumAlias[] aliases, Object value) {
        if (value == null) {
            return null;
        }
        String normalized = normalize(value instanceof Enum ? ((Enum<?>) value).name() : value.toString());
        for (EnumAlias alias : aliases) {
            Object candidate = RustedReflection.getStaticFieldValue(classNames, alias.fieldNames);
            if (candidate == value || candidate.equals(value) || normalize(alias.name).equals(normalized)) {
                return alias.name;
            }
        }
        return value.toString();
    }

    private static Object firstOrNull(List<Object> values, int index) {
        return index >= 0 && index < values.size() ? values.get(index) : null;
    }

    private static Object lastOrNull(List<Object> values) {
        return values.isEmpty() ? null : values.get(values.size() - 1);
    }

    private static List<Object> boundedArraySnapshot(Object value, int maxCount) {
        if (value == null || !value.getClass().isArray() || maxCount <= 0) {
            return Collections.emptyList();
        }
        int length = Math.min(Array.getLength(value), maxCount);
        List<Object> result = new ArrayList<Object>(length);
        for (int i = 0; i < length; i++) {
            result.add(Array.get(value, i));
        }
        return Collections.unmodifiableList(result);
    }

    private static void requireOrderableUnit(Object unit) {
        requireAny(unit, ORDERABLE_UNIT_CLASSES, "OrderableUnit");
    }

    private static void requireUnitOrder(Object order) {
        requireAny(order, UNIT_ORDER_CLASSES, "UnitOrder");
    }

    private static void requireUnitPathPoint(Object point) {
        requireAny(point, UNIT_PATH_POINT_CLASSES, "UnitPathPoint");
    }

    private static void requireWaypointUpdateState(Object state) {
        requireAny(state, WAYPOINT_UPDATE_STATE_CLASSES, "WaypointUpdateState");
    }

    private static void requireFormationManager(Object manager) {
        requireAny(manager, FORMATION_MANAGER_CLASSES, "FormationManager");
    }

    private static void requireFormationGroup(Object group) {
        requireAny(group, FORMATION_GROUP_CLASSES, "FormationGroup");
    }

    private static void requireSharedPathCacheEntry(Object entry) {
        requireAny(entry, SHARED_PATH_CACHE_ENTRY_CLASSES, "SharedPathCacheEntry");
    }

    private static void requireActionExecutionResult(Object result) {
        requireAny(result, ACTION_EXECUTION_RESULT_CLASSES, "ActionExecutionResult");
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

    private static void putArrayLengthField(Map<String, Object> result, Object owner, String key,
                                            String[] fieldNames) {
        Object value = RustedReflection.getFieldValue(owner, fieldNames);
        result.put(key, Integer.valueOf(value != null && value.getClass().isArray() ? Array.getLength(value) : 0));
    }

    private static void putCollectionField(Map<String, Object> result, String key, Object value) {
        result.put(key, value);
        result.put(key + "Size", Integer.valueOf(RustedReflection.snapshotIterable(value).size()));
    }

    private static void putOptionalMethod(Map<String, Object> result, Object owner, String key, String[] methodNames) {
        try {
            result.put(key, RustedReflection.invokeInstance(owner, methodNames));
        } catch (RuntimeException e) {
            result.put(key + "Error", e.getClass().getName() + ": " + e.getMessage());
        }
    }

    private static void putOptionalFloatMethod(Map<String, Object> result, Object owner, String key,
                                               String[] methodNames) {
        try {
            Object value = RustedReflection.invokeInstance(owner, methodNames);
            result.put(key, value instanceof Number ? Float.valueOf(((Number) value).floatValue()) : value);
        } catch (RuntimeException e) {
            result.put(key + "Error", e.getClass().getName() + ": " + e.getMessage());
        }
    }

    private static void putOptionalBooleanMethod(Map<String, Object> result, Object owner, String key,
                                                 String[] methodNames) {
        try {
            result.put(key, Boolean.valueOf(Boolean.TRUE.equals(RustedReflection.invokeInstance(owner, methodNames))));
        } catch (RuntimeException e) {
            result.put(key + "Error", e.getClass().getName() + ": " + e.getMessage());
        }
    }

    private static String normalize(String value) {
        if (value == null) {
            return "";
        }
        StringBuilder result = new StringBuilder();
        String lower = value.trim().toLowerCase(Locale.ROOT);
        for (int i = 0; i < lower.length(); i++) {
            char c = lower.charAt(i);
            if (c != '_' && c != '-' && c != ' ') {
                result.append(c);
            }
        }
        return result.toString();
    }

    private static final class EnumAlias {
        private final String name;
        private final String[] fieldNames;

        private EnumAlias(String name, String[] fieldNames) {
            this.name = name;
            this.fieldNames = fieldNames;
        }
    }
}
