package io.github.endx.rustedfabricapi.api.diagnostic;

import io.github.endx.rustedfabricapi.api.util.RustedReflection;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class UnitActionDiagnostics {
    private static final String[] UNIT_ACTION_CLASSES = {
            "rustedwarfare.unit.action.UnitAction",
            "com.corrodinggames.rts.game.units.a.s"
    };
    private static final String[] UNIT_ACTION_ID_CLASSES = {
            "rustedwarfare.unit.action.UnitActionId",
            "com.corrodinggames.rts.game.units.a.c"
    };
    private static final String[] UNIT_ACTION_FILTER_CLASSES = {
            "rustedwarfare.unit.action.UnitActionFilter",
            "com.corrodinggames.rts.game.units.a.b"
    };
    private static final String[] EDITOR_OR_BUILDER_UNIT_CLASSES = {
            "rustedwarfare.unit.special.EditorOrBuilderUnit",
            "com.corrodinggames.rts.game.units.h"
    };
    private static final String[] EDITOR_ACTION_AVAILABILITY_FILTER_CLASSES = {
            "rustedwarfare.unit.special.EditorActionAvailabilityFilter",
            "com.corrodinggames.rts.game.units.h$16"
    };
    private static final String[] ATTACK_MODE_ACTION_CLASSES = {
            "rustedwarfare.unit.action.AttackModeAction",
            "com.corrodinggames.rts.game.units.a.d"
    };
    private static final String[] ATTACK_MOVE_ACTION_CLASSES = {
            "rustedwarfare.unit.action.AttackMoveAction",
            "com.corrodinggames.rts.game.units.a.e"
    };
    private static final String[] GUARD_UNIT_ACTION_CLASSES = {
            "rustedwarfare.unit.action.GuardUnitAction",
            "com.corrodinggames.rts.game.units.a.f"
    };
    private static final String[] UNIT_SPECIFIC_ACTION_PROXY_CLASSES = {
            "rustedwarfare.unit.action.UnitSpecificActionProxy",
            "com.corrodinggames.rts.game.units.a.g"
    };
    private static final String[] FILTERED_UNIT_ACTION_CLASSES = {
            "rustedwarfare.unit.action.FilteredUnitAction",
            "com.corrodinggames.rts.game.units.a.h"
    };
    private static final String[] PATROL_ACTION_CLASSES = {
            "rustedwarfare.unit.action.PatrolAction",
            "com.corrodinggames.rts.game.units.a.i"
    };
    private static final String[] PING_MAP_ACTION_CLASSES = {
            "rustedwarfare.unit.action.PingMapAction",
            "com.corrodinggames.rts.game.units.a.j"
    };
    private static final String[] MAP_PING_TYPE_CLASSES = {
            "rustedwarfare.unit.action.MapPingType",
            "com.corrodinggames.rts.game.units.a.k"
    };
    private static final String[] TEAM_CHAT_ACTION_CLASSES = {
            "rustedwarfare.unit.action.TeamChatAction",
            "com.corrodinggames.rts.game.units.a.q"
    };
    private static final String[] MAP_PING_SHORTCUT_ACTION_CLASSES = {
            "rustedwarfare.unit.action.MapPingShortcutAction",
            "com.corrodinggames.rts.game.units.a.r"
    };
    private static final String[] SELECTED_UNIT_INFO_ACTION_CLASSES = {
            "rustedwarfare.unit.action.SelectedUnitInfoAction",
            "com.corrodinggames.rts.game.units.a.y"
    };
    private static final String[] GROUPED_UNIT_INFO_ACTION_CLASSES = {
            "rustedwarfare.unit.action.GroupedUnitInfoAction",
            "com.corrodinggames.rts.game.units.a.z"
    };
    private static final String[] CUSTOM_ACTION_CLASSES = {
            "rustedwarfare.custom.action.CustomAction",
            "com.corrodinggames.rts.game.units.custom.a.g"
    };
    private static final String[] UNIT_CLASSES = {
            "rustedwarfare.unit.Unit",
            "com.corrodinggames.rts.game.units.am"
    };

    private static final PingTypeAlias[] PING_TYPE_ALIASES = {
            new PingTypeAlias("normal", new String[]{"normal", "a"}),
            new PingTypeAlias("attack", new String[]{"attack", "b"}),
            new PingTypeAlias("defend", new String[]{"defend", "c"}),
            new PingTypeAlias("nuke", new String[]{"nuke", "d"}),
            new PingTypeAlias("build", new String[]{"build", "e"}),
            new PingTypeAlias("upgrade", new String[]{"upgrade", "f"}),
            new PingTypeAlias("ok", new String[]{"ok", "g"}),
            new PingTypeAlias("no", new String[]{"no", "h"}),
            new PingTypeAlias("happy", new String[]{"happy", "i"}),
            new PingTypeAlias("sad", new String[]{"sad", "j"}),
            new PingTypeAlias("retreat", new String[]{"retreat", "k"})
    };

    private UnitActionDiagnostics() {
    }

    public static boolean isUnitAction(Object value) {
        return isAny(value, UNIT_ACTION_CLASSES);
    }

    public static boolean isUnitActionId(Object value) {
        return isAny(value, UNIT_ACTION_ID_CLASSES);
    }

    public static boolean isUnitActionFilter(Object value) {
        return isAny(value, UNIT_ACTION_FILTER_CLASSES);
    }

    public static boolean isEditorActionAvailabilityFilter(Object value) {
        return isAny(value, EDITOR_ACTION_AVAILABILITY_FILTER_CLASSES);
    }

    public static boolean isAttackModeAction(Object value) {
        return isAny(value, ATTACK_MODE_ACTION_CLASSES);
    }

    public static boolean isAttackMoveAction(Object value) {
        return isAny(value, ATTACK_MOVE_ACTION_CLASSES);
    }

    public static boolean isGuardUnitAction(Object value) {
        return isAny(value, GUARD_UNIT_ACTION_CLASSES);
    }

    public static boolean isUnitSpecificActionProxy(Object value) {
        return isAny(value, UNIT_SPECIFIC_ACTION_PROXY_CLASSES);
    }

    public static boolean isFilteredUnitAction(Object value) {
        return isAny(value, FILTERED_UNIT_ACTION_CLASSES);
    }

    public static boolean isPatrolAction(Object value) {
        return isAny(value, PATROL_ACTION_CLASSES);
    }

    public static boolean isPingMapAction(Object value) {
        return isAny(value, PING_MAP_ACTION_CLASSES);
    }

    public static boolean isMapPingType(Object value) {
        return isAny(value, MAP_PING_TYPE_CLASSES);
    }

    public static boolean isTeamChatAction(Object value) {
        return isAny(value, TEAM_CHAT_ACTION_CLASSES);
    }

    public static boolean isMapPingShortcutAction(Object value) {
        return isAny(value, MAP_PING_SHORTCUT_ACTION_CLASSES);
    }

    public static boolean isSelectedUnitInfoAction(Object value) {
        return isAny(value, SELECTED_UNIT_INFO_ACTION_CLASSES);
    }

    public static boolean isGroupedUnitInfoAction(Object value) {
        return isAny(value, GROUPED_UNIT_INFO_ACTION_CLASSES);
    }

    public static boolean isCustomAction(Object value) {
        return isAny(value, CUSTOM_ACTION_CLASSES);
    }

    public static Map<String, Object> describeUnitAction(Object action) {
        requireAny(action, UNIT_ACTION_CLASSES, "UnitAction");
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("className", action.getClass().getName());
        result.put("actionKind", actionKind(action));
        result.put("attackModeAction", Boolean.valueOf(isAttackModeAction(action)));
        result.put("attackMoveAction", Boolean.valueOf(isAttackMoveAction(action)));
        result.put("guardUnitAction", Boolean.valueOf(isGuardUnitAction(action)));
        result.put("patrolAction", Boolean.valueOf(isPatrolAction(action)));
        result.put("teamChatAction", Boolean.valueOf(isTeamChatAction(action)));
        result.put("mapPingShortcutAction", Boolean.valueOf(isMapPingShortcutAction(action)));
        result.put("pingMapAction", Boolean.valueOf(isPingMapAction(action)));
        result.put("unitSpecificActionProxy", Boolean.valueOf(isUnitSpecificActionProxy(action)));
        result.put("filteredUnitAction", Boolean.valueOf(isFilteredUnitAction(action)));
        result.put("selectedUnitInfoAction", Boolean.valueOf(isSelectedUnitInfoAction(action)));
        result.put("groupedUnitInfoAction", Boolean.valueOf(isGroupedUnitInfoAction(action)));
        result.put("customAction", Boolean.valueOf(isCustomAction(action)));
        putOptionalField(result, action, "actionIdField", new String[]{"actionId", "a"});
        putOptionalField(result, action, "cachedCreditPrice", new String[]{"cachedCreditPrice", "b"});
        putOptionalFloatField(result, action, "displayPriority", new String[]{"displayPriority", "g"});
        putOptionalField(result, action, "availabilityFilter", new String[]{"availabilityFilter", "h"});
        result.put("actionId", invokeOrNull(action, new String[]{"getActionId", "N"}));
        result.put("actionIdString", invokeStringOrEmpty(action, new String[]{"getActionIdString", "O"}));
        result.put("actionIdForSerialization", invokeOrNull(action, new String[]{"getActionIdForSerialization", "z"}));
        result.put("description", invokeStringOrEmpty(action, new String[]{"getDescription", "a"}));
        result.put("text", invokeStringOrEmpty(action, new String[]{"getText", "b"}));
        result.put("displayTextWithCount", invokeStringOrEmpty(action, new String[]{"getDisplayTextWithCount", "d"}));
        result.put("creditCost", Integer.valueOf(invokeIntOrZero(action, new String[]{"getCreditCost", "c"})));
        result.put("actionCommandType", invokeOrNull(action, new String[]{"getActionCommandType", "e"}));
        result.put("displayType", invokeOrNull(action, new String[]{"getDisplayType", "f"}));
        result.put("buildAction", Boolean.valueOf(invokeBooleanOrFalse(action, new String[]{"isBuildAction", "g"})));
        result.put("showText", Boolean.valueOf(invokeBooleanOrFalse(action, new String[]{"shouldShowText", "h_"})));
        result.put("queuedAction", Boolean.valueOf(invokeBooleanOrFalse(action, new String[]{"isQueuedAction", "u"})));
        result.put("usesActionTargetPoint", Boolean.valueOf(usesActionTargetPoint(action)));
        result.put("buildUnitType", invokeOrNull(action, new String[]{"getBuildUnitType", "i"}));
        result.put("guiBuildUnitType", invokeOrNull(action, new String[]{"getGuiBuildUnitType", "y"}));
        if (isAttackModeAction(action)) {
            result.put("attackModeDetails", describeAttackModeAction(action));
        }
        if (isUnitSpecificActionProxy(action)) {
            result.put("unitSpecificProxyDetails", describeUnitSpecificActionProxy(action));
        }
        if (isFilteredUnitAction(action)) {
            result.put("filteredActionDetails", describeFilteredUnitAction(action));
        }
        if (isPingMapAction(action)) {
            result.put("pingMapDetails", describePingMapAction(action));
        }
        if (isSelectedUnitInfoAction(action)) {
            result.put("selectedUnitInfoDetails", describeSelectedUnitInfoAction(action));
        }
        if (isGroupedUnitInfoAction(action)) {
            result.put("groupedUnitInfoDetails", describeGroupedUnitInfoAction(action));
        }
        if (isCustomAction(action)) {
            result.put("customActionDetails", describeCustomAction(action));
        }
        return Collections.unmodifiableMap(result);
    }

    public static boolean usesActionTargetPoint(Object action) {
        requireAny(action, UNIT_ACTION_CLASSES, "UnitAction");
        return Boolean.TRUE.equals(RustedReflection.invokeInstance(action,
                new String[]{"usesActionTargetPoint", "A"}));
    }

    public static Map<String, Object> describeActionId(Object actionId) {
        requireAny(actionId, UNIT_ACTION_ID_CLASSES, "UnitActionId");
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("className", actionId.getClass().getName());
        putOptionalStringField(result, actionId, "idString", new String[]{"idString", "b"});
        result.put("asString", invokeStringOrEmpty(actionId, new String[]{"asString", "a"}));
        return Collections.unmodifiableMap(result);
    }

    public static Object unitActionIdOf(String id) {
        return RustedReflection.invokeStatic(UNIT_ACTION_ID_CLASSES, new String[]{"of", "a"}, id);
    }

    public static boolean isSameActionId(Object first, Object second) {
        requireAny(first, UNIT_ACTION_ID_CLASSES, "UnitActionId");
        requireAny(second, UNIT_ACTION_ID_CLASSES, "UnitActionId");
        return Boolean.TRUE.equals(RustedReflection.invokeInstance(first, new String[]{"isSame", "a"}, second));
    }

    public static Object emptyActionFilter() {
        return RustedReflection.getStaticFieldValue(UNIT_ACTION_FILTER_CLASSES,
                new String[]{"emptyActionFilter", "emptyActionFilter"});
    }

    public static Object editorActionFilter() {
        return RustedReflection.getStaticFieldValue(EDITOR_OR_BUILDER_UNIT_CLASSES,
                new String[]{"editorActionFilter", "K"});
    }

    public static Map<String, Object> describeUnitActionFilter(Object filter) {
        requireAny(filter, UNIT_ACTION_FILTER_CLASSES, "UnitActionFilter");
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("className", filter.getClass().getName());
        result.put("emptyActionFilter", Boolean.valueOf(filter == safeStaticField(UNIT_ACTION_FILTER_CLASSES,
                new String[]{"emptyActionFilter", "emptyActionFilter"})));
        result.put("editorActionAvailabilityFilter", Boolean.valueOf(isEditorActionAvailabilityFilter(filter)));
        return Collections.unmodifiableMap(result);
    }

    public static boolean isFilterActionAvailable(Object filter, Object action, Object unit) {
        requireAny(filter, UNIT_ACTION_FILTER_CLASSES, "UnitActionFilter");
        requireAny(action, UNIT_ACTION_CLASSES, "UnitAction");
        if (unit != null) {
            requireAny(unit, UNIT_CLASSES, "Unit");
        }
        return Boolean.TRUE.equals(RustedReflection.invokeInstance(filter,
                new String[]{"isAvailable", "isAvailable"}, action, unit));
    }

    public static Map<String, Object> describeAttackModeAction(Object action) {
        requireAny(action, ATTACK_MODE_ACTION_CLASSES, "AttackModeAction");
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("className", action.getClass().getName());
        putOptionalIntField(result, action, "lastSelectionGeneration", new String[]{"lastSelectionGeneration", "a"});
        putOptionalField(result, action, "cachedAttackMode", new String[]{"cachedAttackMode", "b"});
        result.put("selectedAttackModeCached", invokeOrNull(action,
                new String[]{"getSelectedAttackModeCached", "r"}));
        return Collections.unmodifiableMap(result);
    }

    public static Object getSelectedAttackMode(Object action) {
        requireAny(action, ATTACK_MODE_ACTION_CLASSES, "AttackModeAction");
        return RustedReflection.invokeInstance(action, new String[]{"getSelectedAttackMode", "q"});
    }

    public static Object getSelectedAttackModeCached(Object action) {
        requireAny(action, ATTACK_MODE_ACTION_CLASSES, "AttackModeAction");
        return RustedReflection.invokeInstance(action, new String[]{"getSelectedAttackModeCached", "r"});
    }

    public static Object getNextAttackMode(Object action, Object currentAttackMode) {
        requireAny(action, ATTACK_MODE_ACTION_CLASSES, "AttackModeAction");
        return RustedReflection.invokeInstance(action, new String[]{"getNextAttackMode", "a"}, currentAttackMode);
    }

    public static Map<String, Object> describeUnitSpecificActionProxy(Object action) {
        requireAny(action, UNIT_SPECIFIC_ACTION_PROXY_CLASSES, "UnitSpecificActionProxy");
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("className", action.getClass().getName());
        putOptionalField(result, action, "delegateAction", new String[]{"delegateAction", "a"});
        putOptionalField(result, action, "boundUnit", new String[]{"boundUnit", "b"});
        putOptionalField(result, action, "actionFilter", new String[]{"actionFilter", "c"});
        putOptionalField(result, action, "savedSelectedUnitsCache", new String[]{"savedSelectedUnitsCache", "d"});
        putOptionalField(result, action, "singleSelectedUnitCache", new String[]{"singleSelectedUnitCache", "e"});
        result.put("delegateActionFromGetter", invokeOrNull(action, new String[]{"getDelegateAction", "p_"}));
        return Collections.unmodifiableMap(result);
    }

    public static Object getUnitSpecificDelegateAction(Object action) {
        requireAny(action, UNIT_SPECIFIC_ACTION_PROXY_CLASSES, "UnitSpecificActionProxy");
        return RustedReflection.invokeInstance(action, new String[]{"getDelegateAction", "p_"});
    }

    public static void pushBoundUnitSelectionContext(Object action) {
        requireAny(action, UNIT_SPECIFIC_ACTION_PROXY_CLASSES, "UnitSpecificActionProxy");
        RustedReflection.invokeInstance(action, new String[]{"pushBoundUnitSelectionContext", "K"});
    }

    public static void popBoundUnitSelectionContext(Object action) {
        requireAny(action, UNIT_SPECIFIC_ACTION_PROXY_CLASSES, "UnitSpecificActionProxy");
        RustedReflection.invokeInstance(action, new String[]{"popBoundUnitSelectionContext", "L"});
    }

    public static Map<String, Object> describeFilteredUnitAction(Object action) {
        requireAny(action, FILTERED_UNIT_ACTION_CLASSES, "FilteredUnitAction");
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("className", action.getClass().getName());
        putOptionalField(result, action, "delegateAction", new String[]{"delegateAction", "a"});
        putOptionalField(result, action, "actionFilter", new String[]{"actionFilter", "b"});
        putOptionalBooleanField(result, action, "respectDelegateAvailability",
                new String[]{"respectDelegateAvailability", "c"});
        putOptionalIntField(result, action, "disabledOverlayColor", new String[]{"disabledOverlayColor", "f"});
        result.put("delegateActionFromGetter", invokeOrNull(action, new String[]{"getDelegateAction", "q_"}));
        return Collections.unmodifiableMap(result);
    }

    public static Object getFilteredDelegateAction(Object action) {
        requireAny(action, FILTERED_UNIT_ACTION_CLASSES, "FilteredUnitAction");
        return RustedReflection.invokeInstance(action, new String[]{"getDelegateAction", "q_"});
    }

    public static Map<String, Object> describePingMapAction(Object action) {
        requireAny(action, PING_MAP_ACTION_CLASSES, "PingMapAction");
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("className", action.getClass().getName());
        Object pingType = optionalField(action, new String[]{"pingType", "a"});
        Object allPingActions = optionalField(action, new String[]{"allPingActions", "b"});
        result.put("pingType", pingType);
        result.put("pingTypeDescription", pingType != null && isMapPingType(pingType)
                ? describeMapPingType(pingType)
                : Collections.emptyMap());
        result.put("allPingActions", allPingActions);
        result.put("allPingActionsSize", Integer.valueOf(sizeOf(allPingActions)));
        putOptionalField(result, action, "pingIconSrcRect", new String[]{"pingIconSrcRect", "c"});
        result.put("pingLocalizationKey", invokeStringOrEmpty(action,
                new String[]{"getPingLocalizationKey", "K"}));
        return Collections.unmodifiableMap(result);
    }

    public static Object findPingActionByActionId(Object actionId) {
        requireAny(actionId, UNIT_ACTION_ID_CLASSES, "UnitActionId");
        return RustedReflection.invokeStatic(PING_MAP_ACTION_CLASSES, new String[]{"findByActionId", "a"}, actionId);
    }

    public static List<Object> getNestedPingActions(Object action, Object unit) {
        requireAny(action, PING_MAP_ACTION_CLASSES, "PingMapAction");
        if (unit != null) {
            requireAny(unit, UNIT_CLASSES, "Unit");
        }
        return Collections.unmodifiableList(RustedReflection.snapshotIterable(
                RustedReflection.invokeInstance(action, new String[]{"getNestedPingActions", "q"}, unit)));
    }

    public static List<Object> pingMapTypes() {
        List<Object> result = new ArrayList<Object>();
        for (PingTypeAlias alias : PING_TYPE_ALIASES) {
            Object value = safeStaticField(MAP_PING_TYPE_CLASSES, alias.fieldNames);
            if (value != null) {
                result.add(value);
            }
        }
        return Collections.unmodifiableList(result);
    }

    public static List<Map<String, Object>> describePingMapTypes() {
        List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
        for (Object pingType : pingMapTypes()) {
            result.add(describeMapPingType(pingType));
        }
        return Collections.unmodifiableList(result);
    }

    public static Map<String, Object> describeMapPingType(Object pingType) {
        requireAny(pingType, MAP_PING_TYPE_CLASSES, "MapPingType");
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("className", pingType.getClass().getName());
        result.put("fieldName", pingTypeFieldName(pingType));
        result.put("enumName", pingType instanceof Enum<?> ? ((Enum<?>) pingType).name() : String.valueOf(pingType));
        result.put("displaySuffix", invokeStringOrEmpty(pingType, new String[]{"getDisplaySuffix", "a"}));
        result.put("localizedName", invokeStringOrEmpty(pingType, new String[]{"getLocalizedName", "b"}));
        result.put("localizationKey", invokeStringOrEmpty(pingType, new String[]{"getLocalizationKey", "c"}));
        return Collections.unmodifiableMap(result);
    }

    public static Map<String, Object> describeSelectedUnitInfoAction(Object action) {
        requireAny(action, SELECTED_UNIT_INFO_ACTION_CLASSES, "SelectedUnitInfoAction");
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("className", action.getClass().getName());
        result.put("firstSelectedOrderableUnit", invokeOrNull(action,
                new String[]{"findFirstSelectedOrderableUnit", "K"}));
        result.put("canShowPlayerInfoForSelectedUnit", Boolean.valueOf(invokeBooleanOrFalse(action,
                new String[]{"canShowPlayerInfoForSelectedUnit", "L"})));
        return Collections.unmodifiableMap(result);
    }

    public static Object findFirstSelectedOrderableUnit(Object action) {
        requireAny(action, SELECTED_UNIT_INFO_ACTION_CLASSES, "SelectedUnitInfoAction");
        return RustedReflection.invokeInstance(action, new String[]{"findFirstSelectedOrderableUnit", "K"});
    }

    public static boolean canShowPlayerInfoForSelectedUnit(Object action) {
        requireAny(action, SELECTED_UNIT_INFO_ACTION_CLASSES, "SelectedUnitInfoAction");
        return Boolean.TRUE.equals(RustedReflection.invokeInstance(action,
                new String[]{"canShowPlayerInfoForSelectedUnit", "L"}));
    }

    public static Map<String, Object> describeGroupedUnitInfoAction(Object action) {
        requireAny(action, GROUPED_UNIT_INFO_ACTION_CLASSES, "GroupedUnitInfoAction");
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("className", action.getClass().getName());
        putOptionalField(result, action, "unitType", new String[]{"unitType", "a"});
        putOptionalIntField(result, action, "selectedCount", new String[]{"selectedCount", "c"});
        putOptionalBooleanField(result, action, "hasMixedSelection", new String[]{"hasMixedSelection", "d"});
        putOptionalField(result, action, "firstSelectedUnitOfType", new String[]{"firstSelectedUnitOfType", "e"});
        putOptionalIntField(result, action, "lastSelectionGeneration", new String[]{"lastSelectionGeneration", "f"});
        return Collections.unmodifiableMap(result);
    }

    public static Map<String, Object> describeCustomAction(Object action) {
        requireAny(action, CUSTOM_ACTION_CLASSES, "CustomAction");
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("className", action.getClass().getName());
        putOptionalField(result, action, "config", new String[]{"config", "a"});
        putOptionalField(result, action, "unitTypeReference", new String[]{"unitTypeReference", "b"});
        putOptionalField(result, action, "aiUse", new String[]{"aiUse", "c"});
        result.put("usesActionTargetPoint", Boolean.valueOf(usesActionTargetPoint(action)));
        result.put("price", invokeOrNull(action, new String[]{"getPrice", "B"}));
        result.put("aiConsiderSameAsBuildingUnitType",
                invokeOrNull(action, new String[]{"getAiConsiderSameAsBuildingUnitType", "E"}));
        result.put("highPriorityQueue", Boolean.valueOf(invokeBooleanOrFalse(action,
                new String[]{"isHighPriorityQueue", "H"})));
        result.put("onlyOneUnitAtATime", Boolean.valueOf(invokeBooleanOrFalse(action,
                new String[]{"isOnlyOneUnitAtATime", "I"})));
        return Collections.unmodifiableMap(result);
    }

    public static void refreshSelectedUnitTypeCache(Object action) {
        requireAny(action, GROUPED_UNIT_INFO_ACTION_CLASSES, "GroupedUnitInfoAction");
        RustedReflection.invokeInstance(action, new String[]{"refreshSelectedUnitTypeCache", "K"});
    }

    public static String actionKind(Object action) {
        if (isAttackModeAction(action)) {
            return "AttackModeAction";
        }
        if (isAttackMoveAction(action)) {
            return "AttackMoveAction";
        }
        if (isGuardUnitAction(action)) {
            return "GuardUnitAction";
        }
        if (isPatrolAction(action)) {
            return "PatrolAction";
        }
        if (isTeamChatAction(action)) {
            return "TeamChatAction";
        }
        if (isMapPingShortcutAction(action)) {
            return "MapPingShortcutAction";
        }
        if (isPingMapAction(action)) {
            return "PingMapAction";
        }
        if (isUnitSpecificActionProxy(action)) {
            return "UnitSpecificActionProxy";
        }
        if (isFilteredUnitAction(action)) {
            return "FilteredUnitAction";
        }
        if (isSelectedUnitInfoAction(action)) {
            return "SelectedUnitInfoAction";
        }
        if (isGroupedUnitInfoAction(action)) {
            return "GroupedUnitInfoAction";
        }
        if (isCustomAction(action)) {
            return "CustomAction";
        }
        return isUnitAction(action) ? "UnitAction" : "unknown";
    }

    private static boolean isAny(Object value, String[] classNames) {
        return value != null && RustedReflection.isAnyClass(value.getClass(), classNames);
    }

    private static void requireAny(Object value, String[] classNames, String label) {
        if (value == null || !RustedReflection.isAnyClass(value.getClass(), classNames)) {
            throw new IllegalArgumentException("Expected " + label + ", got " + describe(value));
        }
    }

    private static Object safeStaticField(String[] classNames, String[] fieldNames) {
        try {
            return RustedReflection.getStaticFieldValue(classNames, fieldNames);
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private static Object optionalField(Object owner, String[] fieldNames) {
        try {
            return RustedReflection.getFieldValue(owner, fieldNames);
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private static Object invokeOrNull(Object owner, String[] methodNames, Object... args) {
        try {
            return RustedReflection.invokeInstance(owner, methodNames, args);
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private static String invokeStringOrEmpty(Object owner, String[] methodNames, Object... args) {
        Object value = invokeOrNull(owner, methodNames, args);
        return value != null ? value.toString() : "";
    }

    private static boolean invokeBooleanOrFalse(Object owner, String[] methodNames, Object... args) {
        return Boolean.TRUE.equals(invokeOrNull(owner, methodNames, args));
    }

    private static int invokeIntOrZero(Object owner, String[] methodNames, Object... args) {
        Object value = invokeOrNull(owner, methodNames, args);
        return value instanceof Number ? ((Number) value).intValue() : 0;
    }

    private static int sizeOf(Object value) {
        return value == null ? 0 : RustedReflection.snapshotIterable(value).size();
    }

    private static void putOptionalField(Map<String, Object> result, Object owner, String key, String[] fieldNames) {
        try {
            result.put(key, RustedReflection.getFieldValue(owner, fieldNames));
        } catch (RuntimeException ignored) {
        }
    }

    private static void putOptionalStringField(Map<String, Object> result, Object owner, String key, String[] fieldNames) {
        try {
            result.put(key, RustedReflection.getStringField(owner, fieldNames));
        } catch (RuntimeException ignored) {
        }
    }

    private static void putOptionalIntField(Map<String, Object> result, Object owner, String key, String[] fieldNames) {
        try {
            result.put(key, Integer.valueOf(RustedReflection.getIntField(owner, fieldNames)));
        } catch (RuntimeException ignored) {
        }
    }

    private static void putOptionalFloatField(Map<String, Object> result, Object owner, String key, String[] fieldNames) {
        try {
            result.put(key, Float.valueOf(RustedReflection.getFloatField(owner, fieldNames)));
        } catch (RuntimeException ignored) {
        }
    }

    private static void putOptionalBooleanField(Map<String, Object> result, Object owner, String key, String[] fieldNames) {
        try {
            result.put(key, Boolean.valueOf(RustedReflection.getBooleanField(owner, fieldNames)));
        } catch (RuntimeException ignored) {
        }
    }

    private static String pingTypeFieldName(Object pingType) {
        for (PingTypeAlias alias : PING_TYPE_ALIASES) {
            Object value = safeStaticField(MAP_PING_TYPE_CLASSES, alias.fieldNames);
            if (value == pingType) {
                return alias.name;
            }
        }
        return "";
    }

    private static String describe(Object value) {
        return value == null ? "null" : value.getClass().getName();
    }

    private static final class PingTypeAlias {
        private final String name;
        private final String[] fieldNames;

        private PingTypeAlias(String name, String[] fieldNames) {
            this.name = name;
            this.fieldNames = fieldNames;
        }
    }
}
