package io.github.endx.rustedfabricapi.api.diagnostic;

import io.github.endx.rustedfabricapi.api.util.RustedReflection;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class TeamStatsDiagnostics {
    private static final String[] TEAM_CLASSES = {
            "rustedwarfare.game.Team",
            "com.corrodinggames.rts.game.n"
    };
    private static final String[] TEAM_STATS_CLASSES = {
            "rustedwarfare.game.TeamStats",
            "com.corrodinggames.rts.game.s"
    };
    private static final String[] UNIT_TAG_STATS_LIST_CLASSES = {
            "rustedwarfare.game.UnitTagStatsList",
            "com.corrodinggames.rts.game.t"
    };
    private static final String[] UNIT_TAG_STATS_CLASSES = {
            "rustedwarfare.game.UnitTagStats",
            "com.corrodinggames.rts.game.p"
    };
    private static final String[] TEAM_RELATION_CLASSES = {
            "rustedwarfare.game.TeamRelation",
            "com.corrodinggames.rts.game.q"
    };
    private static final String[] TEAM_COLORING_MODE_CLASSES = {
            "rustedwarfare.game.TeamColoringMode",
            "com.corrodinggames.rts.game.o"
    };
    private static final String[] RESOURCE_TYPE_CLASSES = {
            "rustedwarfare.custom.resource.ResourceType",
            "com.corrodinggames.rts.game.units.custom.e.a"
    };

    private TeamStatsDiagnostics() {
    }

    public static Map<String, Object> describeTeam(Object team) {
        requireTeam(team);
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("stats", getStats(team));
        putBooleanField(result, team, "statsDirty", new String[]{"statsDirty", "S"});
        putIntField(result, team, "statsValidationFailureCount",
                new String[]{"statsValidationFailureCount", "ad"});
        result.put("totalUnitCountIncludingQueued", Integer.valueOf(getTotalUnitCountIncludingQueued(team)));
        result.put("nonBuildingUnitCountIncludingQueued",
                Integer.valueOf(getNonBuildingUnitCountIncludingQueued(team)));
        result.put("maxUnitCount", Integer.valueOf(getMaxUnitCount(team)));
        result.put("incomeRate", Integer.valueOf(getIncomeRate(team)));
        result.put("displayIncomeRate", Integer.valueOf(getDisplayIncomeRate(team)));
        return Collections.unmodifiableMap(result);
    }

    public static Map<String, Object> describeTeamStats(Object stats) {
        requireTeamStats(stats);
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        putIntField(result, stats, "maxUnitCount", new String[]{"maxUnitCount", "a"});
        putIntField(result, stats, "nonBuildingUnitCountIncludingQueued",
                new String[]{"nonBuildingUnitCountIncludingQueued", "b"});
        putIntField(result, stats, "completedUnitCount", new String[]{"completedUnitCount", "c"});
        putIntField(result, stats, "totalUnitCount", new String[]{"totalUnitCount", "d"});
        putIntField(result, stats, "queuedUnitCount", new String[]{"queuedUnitCount", "e"});
        putIntField(result, stats, "incompleteUnitCount", new String[]{"incompleteUnitCount", "f"});
        putIntField(result, stats, "incomeRate", new String[]{"incomeRate", "g"});
        putField(result, stats, "customIncomeRate", new String[]{"customIncomeRate", "h"});
        putField(result, stats, "customIncomeRatePositive", new String[]{"customIncomeRatePositive", "i"});
        putField(result, stats, "customIncomeRateNegative", new String[]{"customIncomeRateNegative", "j"});
        putField(result, stats, "streamingRatePositive", new String[]{"streamingRatePositive", "k"});
        putField(result, stats, "streamingRateNegative", new String[]{"streamingRateNegative", "l"});
        putBooleanField(result, stats, "hasUnitThatPreventsDefeat",
                new String[]{"hasUnitThatPreventsDefeat", "m"});
        putIntField(result, stats, "nonBuildingCreditValue", new String[]{"nonBuildingCreditValue", "n"});
        putIntField(result, stats, "buildingCreditValue", new String[]{"buildingCreditValue", "o"});
        result.put("hotTagStats", describeUnitTagStatsListOrRaw(
                RustedReflection.getFieldValue(stats, new String[]{"hotTagStats", "p"})));
        result.put("allTagStats", describeUnitTagStatsListOrRaw(
                RustedReflection.getFieldValue(stats, new String[]{"allTagStats", "q"})));
        return Collections.unmodifiableMap(result);
    }

    public static Map<String, Object> describeUnitTagStatsList(Object statsList) {
        requireUnitTagStatsList(statsList);
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("entries", unitTagStatsEntries(statsList));
        putIntField(result, statsList, "size", new String[]{"size", "c"});
        return Collections.unmodifiableMap(result);
    }

    public static Map<String, Object> describeUnitTagStats(Object tagStats) {
        requireUnitTagStats(tagStats);
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        putField(result, tagStats, "tag", new String[]{"tag", "a"});
        putIntField(result, tagStats, "completedUnitCount", new String[]{"completedUnitCount", "b"});
        putIntField(result, tagStats, "incompleteUnitCount", new String[]{"incompleteUnitCount", "c"});
        putIntField(result, tagStats, "queuedUnitCount", new String[]{"queuedUnitCount", "d"});
        putShortField(result, tagStats, "cacheMissCount", new String[]{"cacheMissCount", "e"});
        return Collections.unmodifiableMap(result);
    }

    public static Object getStats(Object team) {
        requireTeam(team);
        return RustedReflection.getFieldValue(team, new String[]{"stats", "T"});
    }

    public static boolean isStatsDirty(Object team) {
        requireTeam(team);
        return RustedReflection.getBooleanField(team, new String[]{"statsDirty", "S"});
    }

    public static int getStatsValidationFailureCount(Object team) {
        requireTeam(team);
        return RustedReflection.getIntField(team, new String[]{"statsValidationFailureCount", "ad"});
    }

    public static int getTotalUnitCountIncludingQueued(Object team) {
        requireTeam(team);
        Object value = RustedReflection.invokeInstance(team,
                new String[]{"getTotalUnitCountIncludingQueued", "s"});
        return value instanceof Number ? ((Number) value).intValue() : 0;
    }

    public static int getNonBuildingUnitCountIncludingQueued(Object team) {
        requireTeam(team);
        Object value = RustedReflection.invokeInstance(team,
                new String[]{"getNonBuildingUnitCountIncludingQueued", "w"});
        return value instanceof Number ? ((Number) value).intValue() : 0;
    }

    public static int getMaxUnitCount(Object team) {
        requireTeam(team);
        Object value = RustedReflection.invokeInstance(team, new String[]{"getMaxUnitCount", "x"});
        return value instanceof Number ? ((Number) value).intValue() : 0;
    }

    public static boolean validateCachedTeamStats(Object team) {
        requireTeam(team);
        return Boolean.TRUE.equals(RustedReflection.invokeInstance(team,
                new String[]{"validateCachedTeamStats", "t"}));
    }

    public static void refreshCachedTeamStats(Object team, boolean force) {
        requireTeam(team);
        RustedReflection.invokeInstance(team, new String[]{"refreshCachedTeamStats", "d"},
                Boolean.valueOf(force));
    }

    public static Object rebuildTeamStatsSnapshot(Object team, boolean includeQueued) {
        requireTeam(team);
        return RustedReflection.invokeInstance(team, new String[]{"rebuildTeamStatsSnapshot", "e"},
                Boolean.valueOf(includeQueued));
    }

    public static int getIncomeRate(Object team) {
        requireTeam(team);
        Object value = RustedReflection.invokeInstance(team, new String[]{"getIncomeRate", "u"});
        return value instanceof Number ? ((Number) value).intValue() : 0;
    }

    public static int getDisplayIncomeRate(Object team) {
        requireTeam(team);
        Object value = RustedReflection.invokeInstance(team, new String[]{"getDisplayIncomeRate", "v"});
        return value instanceof Number ? ((Number) value).intValue() : 0;
    }

    public static int getNegativeStreamingRateForResource(Object team, Object resourceType) {
        requireTeam(team);
        requireResourceType(resourceType);
        Object value = RustedReflection.invokeInstance(team,
                new String[]{"getNegativeStreamingRateForResource", "a"}, resourceType);
        return value instanceof Number ? ((Number) value).intValue() : 0;
    }

    public static int getPositiveIncomeAndStreamingRateForResource(Object team, Object resourceType) {
        requireTeam(team);
        requireResourceType(resourceType);
        Object value = RustedReflection.invokeInstance(team,
                new String[]{"getPositiveIncomeAndStreamingRateForResource", "b"}, resourceType);
        return value instanceof Number ? ((Number) value).intValue() : 0;
    }

    public static int getUnitCountWithTagAndOptions(Object team, Object tag,
                                                    boolean includeIncomplete,
                                                    boolean includeQueued) {
        requireTeam(team);
        requireNonNull(tag, "tag");
        Object value = RustedReflection.invokeInstance(team,
                new String[]{"getUnitCountWithTagAndOptions", "a"},
                tag, Boolean.valueOf(includeIncomplete), Boolean.valueOf(includeQueued));
        return value instanceof Number ? ((Number) value).intValue() : 0;
    }

    public static int getEnemyUnitCountWithTagAndOptions(Object team, Object tag,
                                                         boolean includeIncomplete,
                                                         boolean includeQueued) {
        requireTeam(team);
        requireNonNull(tag, "tag");
        Object value = RustedReflection.invokeInstance(team,
                new String[]{"getEnemyUnitCountWithTagAndOptions", "b"},
                tag, Boolean.valueOf(includeIncomplete), Boolean.valueOf(includeQueued));
        return value instanceof Number ? ((Number) value).intValue() : 0;
    }

    public static int getAllyNotOwnUnitCountWithTagAndOptions(Object team, Object tag,
                                                              boolean includeIncomplete,
                                                              boolean includeQueued) {
        requireTeam(team);
        requireNonNull(tag, "tag");
        Object value = RustedReflection.invokeInstance(team,
                new String[]{"getAllyNotOwnUnitCountWithTagAndOptions", "c"},
                tag, Boolean.valueOf(includeIncomplete), Boolean.valueOf(includeQueued));
        return value instanceof Number ? ((Number) value).intValue() : 0;
    }

    public static boolean matchesTeamRelation(Object team, Object relation, Object otherTeam) {
        requireTeam(team);
        requireTeamRelation(relation);
        requireTeam(otherTeam);
        return Boolean.TRUE.equals(RustedReflection.invokeInstance(team,
                new String[]{"matchesTeamRelation", "a"}, relation, otherTeam));
    }

    public static boolean isEnemy(Object team, Object otherTeam) {
        requireTeam(team);
        requireTeam(otherTeam);
        return Boolean.TRUE.equals(RustedReflection.invokeInstance(team, new String[]{"isEnemy", "c"}, otherTeam));
    }

    public static boolean isAlly(Object team, Object otherTeam) {
        requireTeam(team);
        requireTeam(otherTeam);
        return Boolean.TRUE.equals(RustedReflection.invokeInstance(team, new String[]{"isAlly", "d"}, otherTeam));
    }

    public static Object getOrCreateTagStats(Object stats, Object tag) {
        requireTeamStats(stats);
        requireNonNull(tag, "tag");
        return RustedReflection.invokeInstance(stats, new String[]{"getOrCreateTagStats", "a"}, tag);
    }

    public static List<Object> unitTagStatsEntries(Object statsList) {
        requireUnitTagStatsList(statsList);
        Object entries = RustedReflection.getFieldValue(statsList, new String[]{"entries", "b"});
        int size = RustedReflection.getIntField(statsList, new String[]{"size", "c"});
        List<Object> snapshot = RustedReflection.snapshotIterable(entries);
        List<Object> result = new ArrayList<Object>();
        for (int i = 0; i < snapshot.size() && i < size; i++) {
            Object item = snapshot.get(i);
            if (item != null) {
                result.add(item);
            }
        }
        return Collections.unmodifiableList(result);
    }

    public static List<Object> teamRelations() {
        return Collections.unmodifiableList(RustedReflection.snapshotIterable(
                RustedReflection.invokeStatic(TEAM_RELATION_CLASSES, new String[]{"values"})));
    }

    public static Object teamRelation(String name) {
        return RustedReflection.invokeStatic(TEAM_RELATION_CLASSES, new String[]{"valueOf"}, name);
    }

    public static List<Object> teamColoringModes() {
        return Collections.unmodifiableList(RustedReflection.snapshotIterable(
                RustedReflection.invokeStatic(TEAM_COLORING_MODE_CLASSES, new String[]{"values"})));
    }

    public static Object teamColoringMode(String name) {
        return RustedReflection.invokeStatic(TEAM_COLORING_MODE_CLASSES, new String[]{"valueOf"}, name);
    }

    private static Object describeUnitTagStatsListOrRaw(Object statsList) {
        if (statsList == null) {
            return null;
        }
        try {
            return describeUnitTagStatsList(statsList);
        } catch (RuntimeException e) {
            return statsList;
        }
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

    private static void putBooleanField(Map<String, Object> result, Object owner, String key, String[] fieldNames) {
        result.put(key, Boolean.valueOf(RustedReflection.getBooleanField(owner, fieldNames)));
    }

    private static void requireTeam(Object team) {
        requireAny(team, TEAM_CLASSES, "Team");
    }

    private static void requireTeamStats(Object stats) {
        requireAny(stats, TEAM_STATS_CLASSES, "TeamStats");
    }

    private static void requireUnitTagStatsList(Object statsList) {
        requireAny(statsList, UNIT_TAG_STATS_LIST_CLASSES, "UnitTagStatsList");
    }

    private static void requireUnitTagStats(Object tagStats) {
        requireAny(tagStats, UNIT_TAG_STATS_CLASSES, "UnitTagStats");
    }

    private static void requireTeamRelation(Object relation) {
        requireAny(relation, TEAM_RELATION_CLASSES, "TeamRelation");
    }

    private static void requireResourceType(Object resourceType) {
        requireAny(resourceType, RESOURCE_TYPE_CLASSES, "ResourceType");
    }

    private static void requireAny(Object value, String[] classNames, String label) {
        requireNonNull(value, label);
        if (!RustedReflection.isAnyClass(value.getClass(), classNames)) {
            throw new IllegalArgumentException("Expected " + label + ", got " + value.getClass().getName());
        }
    }

    private static void requireNonNull(Object value, String label) {
        if (value == null) {
            throw new IllegalArgumentException(label + " must not be null");
        }
    }
}
