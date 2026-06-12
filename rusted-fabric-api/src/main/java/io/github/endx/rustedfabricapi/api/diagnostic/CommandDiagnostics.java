package io.github.endx.rustedfabricapi.api.diagnostic;

import io.github.endx.rustedfabricapi.api.util.RustedReflection;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class CommandDiagnostics {
    private static final String[] COMMAND_CONTROLLER_CLASSES = {
            "rustedwarfare.command.CommandController",
            "com.corrodinggames.rts.gameFramework.c"
    };
    private static final String[] COMMAND_CLASSES = {
            "rustedwarfare.command.Command",
            "com.corrodinggames.rts.gameFramework.e"
    };
    private static final String[] TEAM_CLASSES = {
            "rustedwarfare.game.Team",
            "com.corrodinggames.rts.game.n"
    };
    private static final String[] ORDERABLE_UNIT_CLASSES = {
            "rustedwarfare.unit.OrderableUnit",
            "com.corrodinggames.rts.game.units.y"
    };
    private static final String[] SHARED_PATH_CACHE_ENTRY_CLASSES = {
            "rustedwarfare.path.SharedPathCacheEntry",
            "com.corrodinggames.rts.gameFramework.d"
    };

    private CommandDiagnostics() {
    }

    public static Map<String, Object> describeCommand(Object command) {
        requireCommand(command);
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        putBooleanField(result, command, "appendToExistingOrders", new String[]{"appendToExistingOrders", "e"});
        putBooleanField(result, command, "applyActionToQueuedBuildItems",
                new String[]{"applyActionToQueuedBuildItems", "g"});
        putField(result, command, "team", new String[]{"team", "i"});
        putField(result, command, "unitOrder", new String[]{"unitOrder", "j"});
        putField(result, command, "actionId", new String[]{"actionId", "k"});
        putField(result, command, "actionTargetPoint", new String[]{"actionTargetPoint", "l"});
        putField(result, command, "actionTargetUnit", new String[]{"actionTargetUnit", "m"});
        putField(result, command, "commandTeamOverride", new String[]{"commandTeamOverride", "p"});
        result.put("selectedOrderableUnits", selectedOrderableUnits(command));
        putBooleanField(result, command, "allowExecutionDuringReplay",
                new String[]{"allowExecutionDuringReplay", "a"});
        putField(result, command, "sourceStackTrace", new String[]{"sourceStackTrace", "b"});
        putIntField(result, command, "scheduledFrame", new String[]{"scheduledFrame", "c"});
        putIntField(result, command, "createdFrame", new String[]{"createdFrame", "d"});
        putBooleanField(result, command, "clearNonBuildRepairWaypointsOnly",
                new String[]{"clearNonBuildRepairWaypointsOnly", "f"});
        putBooleanField(result, command, "replaceMatchingMoveWaypoint",
                new String[]{"replaceMatchingMoveWaypoint", "h"});
        putField(result, command, "attackModeOverride", new String[]{"attackModeOverride", "n"});
        putField(result, command, "buildQueueRallyPoint", new String[]{"buildQueueRallyPoint", "z"});
        putBooleanField(result, command, "clearExistingOrdersBeforeIssue",
                new String[]{"clearExistingOrdersBeforeIssue", "o"});
        putShortField(result, command, "knownAlliedTeamMask", new String[]{"knownAlliedTeamMask", "q"});
        putBooleanField(result, command, "hasSystemCommand", new String[]{"hasSystemCommand", "r"});
        putFloatField(result, command, "stepRateCommandValue", new String[]{"stepRateCommandValue", "s"});
        putFloatField(result, command, "systemCommandFloatValue2", new String[]{"systemCommandFloatValue2", "t"});
        putIntField(result, command, "systemActionCode", new String[]{"systemActionCode", "u"});
        result.put("selectedUnitIds", selectedUnitIds(command));
        result.put("sharedPathCacheEntries", sharedPathCacheEntries(command));
        putBooleanField(result, command, "sharedPathCachePrepared", new String[]{"sharedPathCachePrepared", "x"});
        putField(result, command, "commandController", new String[]{"commandController", "y"});
        result.put("teamAccessor", getTeam(command));
        result.put("selectedUnitReferenceCount", Integer.valueOf(getSelectedUnitReferenceCount(command)));
        result.put("emptyCommand", Boolean.valueOf(isEmptyCommand(command)));
        result.put("sharedPathCacheEntriesResolved", Boolean.valueOf(areSharedPathCacheEntriesResolved(command)));
        return Collections.unmodifiableMap(result);
    }

    public static Map<String, Object> describeCommandController(Object controller) {
        requireCommandController(controller);
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("localCommands", localCommands(controller));
        result.put("serverQueuedCommands", serverQueuedCommands(controller));
        putBooleanField(result, controller, "traceCommandSource", new String[]{"traceCommandSource", "a"});
        putIntField(result, controller, "rateLimitedWarningCount", new String[]{"rateLimitedWarningCount", "e"});
        return Collections.unmodifiableMap(result);
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
        putField(result, entry, "movementType", new String[]{"movementType", "h"});
        return Collections.unmodifiableMap(result);
    }

    public static Object createCommandForTeam(Object controller, Object team) {
        requireCommandController(controller);
        requireTeam(team);
        return RustedReflection.invokeInstance(controller, new String[]{"createCommandForTeam", "b"}, team);
    }

    public static Object createCommandForTeamAlias(Object controller, Object team) {
        requireCommandController(controller);
        requireTeam(team);
        return RustedReflection.invokeInstance(controller, new String[]{"createCommandForTeamAlias", "a"}, team);
    }

    public static Object createBareCommand(Object controller) {
        requireCommandController(controller);
        return RustedReflection.invokeInstance(controller, new String[]{"createBareCommand", "b"});
    }

    public static void processPendingCommands(Object controller) {
        requireCommandController(controller);
        RustedReflection.invokeInstance(controller, new String[]{"processPendingCommands", "c"});
    }

    public static void processLocalCommands(Object controller) {
        requireCommandController(controller);
        RustedReflection.invokeInstance(controller, new String[]{"processLocalCommands", "d"});
    }

    public static void processServerQueuedCommands(Object controller) {
        requireCommandController(controller);
        RustedReflection.invokeInstance(controller, new String[]{"processServerQueuedCommands", "e"});
    }

    public static void clearCommandQueues(Object controller) {
        requireCommandController(controller);
        RustedReflection.invokeInstance(controller, new String[]{"clearCommandQueues", "a"});
    }

    public static List<Object> localCommands(Object controller) {
        requireCommandController(controller);
        return Collections.unmodifiableList(RustedReflection.snapshotIterable(
                RustedReflection.getFieldValue(controller, new String[]{"localCommands", "b"})));
    }

    public static List<Object> serverQueuedCommands(Object controller) {
        requireCommandController(controller);
        return Collections.unmodifiableList(RustedReflection.snapshotIterable(
                RustedReflection.getFieldValue(controller, new String[]{"serverQueuedCommands", "d"})));
    }

    public static void addUnit(Object command, Object orderableUnit) {
        requireCommand(command);
        requireOrderableUnit(orderableUnit);
        RustedReflection.invokeInstance(command, new String[]{"addUnit", "a"}, orderableUnit);
    }

    public static void addUnits(Object command, java.util.AbstractList<?> units) {
        requireCommand(command);
        RustedReflection.invokeInstance(command, new String[]{"addUnits", "a"}, units);
    }

    public static void issueCommand(Object command) {
        requireCommand(command);
        RustedReflection.invokeInstance(command, new String[]{"issueCommand", "k"});
    }

    public static boolean prepareAndCheckOnServer(Object command) {
        requireCommand(command);
        Object value = RustedReflection.invokeInstance(command, new String[]{"prepareAndCheckOnServer", "l"});
        return Boolean.TRUE.equals(value);
    }

    public static void freezeSelectedUnitsForSerialization(Object command) {
        requireCommand(command);
        RustedReflection.invokeInstance(command, new String[]{"freezeSelectedUnitsForSerialization", "g"});
    }

    public static void captureSharedPathCacheForCommand(Object command) {
        requireCommand(command);
        RustedReflection.invokeInstance(command, new String[]{"captureSharedPathCacheForCommand", "b"});
    }

    public static boolean areSharedPathCacheEntriesResolved(Object command) {
        requireCommand(command);
        return Boolean.TRUE.equals(RustedReflection.invokeInstance(command,
                new String[]{"areSharedPathCacheEntriesResolved", "a"}));
    }

    public static Object getTeam(Object command) {
        requireCommand(command);
        return RustedReflection.invokeInstance(command, new String[]{"getTeam", "c"});
    }

    public static int getSelectedUnitReferenceCount(Object command) {
        requireCommand(command);
        Object value = RustedReflection.invokeInstance(command,
                new String[]{"getSelectedUnitReferenceCount", "d"});
        return value instanceof Number ? ((Number) value).intValue() : 0;
    }

    public static boolean isEmptyCommand(Object command) {
        requireCommand(command);
        return Boolean.TRUE.equals(RustedReflection.invokeInstance(command, new String[]{"isEmptyCommand", "e"}));
    }

    public static Object copyBySerialization(Object command) {
        requireCommand(command);
        return RustedReflection.invokeInstance(command, new String[]{"copyBySerialization", "f"});
    }

    public static void markClearExistingOrdersBeforeIssue(Object command) {
        requireCommand(command);
        RustedReflection.invokeInstance(command, new String[]{"markClearExistingOrdersBeforeIssue", "h"});
    }

    public static boolean canControlledTeamSeeCommand(Object command, Object controllerTeam, Object targetTeam) {
        requireCommand(command);
        requireTeam(controllerTeam);
        requireTeam(targetTeam);
        return Boolean.TRUE.equals(RustedReflection.invokeInstance(command,
                new String[]{"canControlledTeamSeeCommand", "a"}, controllerTeam, targetTeam));
    }

    public static List<Object> selectedUnitIds(Object command) {
        requireCommand(command);
        return Collections.unmodifiableList(RustedReflection.snapshotIterable(
                RustedReflection.getFieldValue(command, new String[]{"selectedUnitIds", "A"})));
    }

    public static List<Object> selectedOrderableUnits(Object command) {
        requireCommand(command);
        return Collections.unmodifiableList(RustedReflection.snapshotIterable(
                RustedReflection.getFieldValue(command, new String[]{"selectedOrderableUnits", "v"})));
    }

    public static List<Object> sharedPathCacheEntries(Object command) {
        requireCommand(command);
        return Collections.unmodifiableList(RustedReflection.snapshotIterable(
                RustedReflection.getFieldValue(command, new String[]{"sharedPathCacheEntries", "w"})));
    }

    private static void putField(Map<String, Object> result, Object owner, String key, String[] fieldNames) {
        result.put(key, RustedReflection.getFieldValue(owner, fieldNames));
    }

    private static void putIntField(Map<String, Object> result, Object owner, String key, String[] fieldNames) {
        result.put(key, Integer.valueOf(RustedReflection.getIntField(owner, fieldNames)));
    }

    private static void putShortField(Map<String, Object> result, Object owner, String key, String[] fieldNames) {
        Object value = RustedReflection.getFieldValue(owner, fieldNames);
        result.put(key, Short.valueOf(value instanceof Number ? ((Number) value).shortValue() : 0));
    }

    private static void putFloatField(Map<String, Object> result, Object owner, String key, String[] fieldNames) {
        result.put(key, Float.valueOf(RustedReflection.getFloatField(owner, fieldNames)));
    }

    private static void putBooleanField(Map<String, Object> result, Object owner, String key, String[] fieldNames) {
        result.put(key, Boolean.valueOf(RustedReflection.getBooleanField(owner, fieldNames)));
    }

    private static void requireCommandController(Object controller) {
        requireAny(controller, COMMAND_CONTROLLER_CLASSES, "CommandController");
    }

    private static void requireCommand(Object command) {
        requireAny(command, COMMAND_CLASSES, "Command");
    }

    private static void requireTeam(Object team) {
        requireAny(team, TEAM_CLASSES, "Team");
    }

    private static void requireOrderableUnit(Object unit) {
        requireAny(unit, ORDERABLE_UNIT_CLASSES, "OrderableUnit");
    }

    private static void requireSharedPathCacheEntry(Object entry) {
        requireAny(entry, SHARED_PATH_CACHE_ENTRY_CLASSES, "SharedPathCacheEntry");
    }

    private static void requireAny(Object value, String[] classNames, String label) {
        if (value == null) {
            throw new IllegalArgumentException(label + " must not be null");
        }
        if (!RustedReflection.isAnyClass(value.getClass(), classNames)) {
            throw new IllegalArgumentException("Expected " + label + ", got " + value.getClass().getName());
        }
    }
}
