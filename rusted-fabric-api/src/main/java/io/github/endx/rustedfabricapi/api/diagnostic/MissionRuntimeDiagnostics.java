package io.github.endx.rustedfabricapi.api.diagnostic;

import io.github.endx.rustedfabricapi.api.util.RustedReflection;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class MissionRuntimeDiagnostics {
    private static final String[] GAME_ENGINE_CLASSES = {
            "rustedwarfare.core.GameEngine",
            "com.corrodinggames.rts.gameFramework.l"
    };
    private static final String[] UNIT_CLASSES = {
            "rustedwarfare.unit.Unit",
            "com.corrodinggames.rts.game.units.am"
    };
    private static final String[] MISSION_ENGINE_CLASSES = {
            "rustedwarfare.mission.MissionEngine",
            "com.corrodinggames.rts.gameFramework.n.f"
    };
    private static final String[] MISSION_TRIGGER_CLASSES = {
            "rustedwarfare.mission.MissionTrigger",
            "com.corrodinggames.rts.gameFramework.n.a"
    };
    private static final String[] MISSION_TRIGGER_CONDITION_CLASSES = {
            "rustedwarfare.mission.condition.MissionTriggerCondition",
            "com.corrodinggames.rts.gameFramework.n.a.a"
    };
    private static final String[] TEAM_TAG_CONDITION_CLASSES = {
            "rustedwarfare.mission.condition.TeamTagCondition",
            "com.corrodinggames.rts.gameFramework.n.a.b"
    };
    private static final String[] UNIT_DETECT_CONDITION_CLASSES = {
            "rustedwarfare.mission.condition.UnitDetectCondition",
            "com.corrodinggames.rts.gameFramework.n.a.c"
    };
    private static final String[] MISSION_TRIGGER_CONDITION_SET_CLASSES = {
            "rustedwarfare.mission.MissionTriggerConditionSet",
            "com.corrodinggames.rts.gameFramework.n.b"
    };
    private static final String[] MISSION_TRIGGER_TYPE_CLASSES = {
            "rustedwarfare.mission.MissionTriggerType",
            "com.corrodinggames.rts.gameFramework.n.e"
    };
    private static final String[] MISSION_OBJECTIVE_PANEL_CLASSES = {
            "rustedwarfare.mission.MissionObjectivePanel",
            "com.corrodinggames.rts.gameFramework.n.g"
    };
    private static final String[] MISSION_UNIT_COUNTER_CLASSES = {
            "rustedwarfare.mission.MissionUnitCounter",
            "com.corrodinggames.rts.gameFramework.n.i"
    };
    private static final String[] MISSION_UNIT_COUNT_ENTRY_CLASSES = {
            "rustedwarfare.mission.MissionUnitCountEntry",
            "com.corrodinggames.rts.gameFramework.n.j"
    };
    private static final String[] MISSION_WAVE_UNIT_CLASSES = {
            "rustedwarfare.mission.MissionWaveUnit",
            "com.corrodinggames.rts.gameFramework.n.k"
    };
    private static final String[] MISSION_TRIGGER_REFERENCE_CLASSES = {
            "rustedwarfare.mission.MissionTriggerReference",
            "com.corrodinggames.rts.gameFramework.n.m"
    };

    private static final Alias[] MISSION_TRIGGER_TYPE_ALIASES = {
            new Alias("objective", new String[]{"objective", "a"}),
            new Alias("eventMove", new String[]{"eventMove", "b"}),
            new Alias("eventChangeCredits", new String[]{"eventChangeCredits", "c"}),
            new Alias("eventTeamTags", new String[]{"eventTeamTags", "d"}),
            new Alias("eventUnitAdd", new String[]{"eventUnitAdd", "e"}),
            new Alias("eventUnitRemove", new String[]{"eventUnitRemove", "f"}),
            new Alias("mapText", new String[]{"mapText", "g"}),
            new Alias("moveCamera", new String[]{"moveCamera", "h"}),
            new Alias("triggerUnitDetect", new String[]{"triggerUnitDetect", "i"}),
            new Alias("triggerTeamTagDetect", new String[]{"triggerTeamTagDetect", "j"}),
            new Alias("triggerBasic", new String[]{"triggerBasic", "k"})
    };

    private MissionRuntimeDiagnostics() {
    }

    public static Object missionEngineFromGameEngine(Object gameEngine) {
        requireAny(gameEngine, GAME_ENGINE_CLASSES, "GameEngine");
        return RustedReflection.getFieldValue(gameEngine, new String[]{"missionEngine", "ce"});
    }

    public static Map<String, Object> describeMissionEngine(Object engine) {
        requireMissionEngine(engine);
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        putField(result, engine, "difficulty", new String[]{"difficulty", "C"});
        putCollectionField(result, engine, "objectiveLines", new String[]{"objectiveLines", "J"});
        putCollectionField(result, engine, "survivalWaves", new String[]{"survivalWaves", "O"});
        putField(result, engine, "playerTeam", new String[]{"playerTeam", "d"});
        putField(result, engine, "loseCondition", new String[]{"loseCondition", "e"});
        putField(result, engine, "winCondition", new String[]{"winCondition", "f"});
        putCollectionField(result, engine, "triggers", new String[]{"triggers", "g"});
        putField(result, engine, "briefingText", new String[]{"briefingText", "h"});
        putBooleanField(result, engine, "hasMissionStartCamera", new String[]{"hasMissionStartCamera", "q"});
        putIntField(result, engine, "survivalWave", new String[]{"survivalWave", "r"});
        result.put("hasMissionData", Boolean.valueOf(hasMissionData(engine)));
        result.put("missionActive", Boolean.valueOf(isMissionActive(engine)));
        result.put("activeObjectives", Boolean.valueOf(hasActiveObjectives(engine)));
        result.put("missionOver", Boolean.valueOf(isMissionOver(engine)));
        return Collections.unmodifiableMap(result);
    }

    public static List<Object> triggersSnapshot(Object engine) {
        requireMissionEngine(engine);
        return snapshotField(engine, new String[]{"triggers", "g"});
    }

    public static List<Object> objectiveLinesSnapshot(Object engine) {
        requireMissionEngine(engine);
        return snapshotField(engine, new String[]{"objectiveLines", "J"});
    }

    public static List<Object> survivalWavesSnapshot(Object engine) {
        requireMissionEngine(engine);
        return snapshotField(engine, new String[]{"survivalWaves", "O"});
    }

    public static boolean hasMissionData(Object engine) {
        requireMissionEngine(engine);
        return Boolean.TRUE.equals(RustedReflection.invokeInstance(engine, new String[]{"hasMissionData", "a"}));
    }

    public static boolean isMissionActive(Object engine) {
        requireMissionEngine(engine);
        return Boolean.TRUE.equals(RustedReflection.invokeInstance(engine, new String[]{"isMissionActive", "b"}));
    }

    public static boolean hasActiveObjectives(Object engine) {
        requireMissionEngine(engine);
        return Boolean.TRUE.equals(RustedReflection.invokeInstance(engine, new String[]{"hasActiveObjectives", "g"}));
    }

    public static boolean isMissionOver(Object engine) {
        requireMissionEngine(engine);
        return Boolean.TRUE.equals(RustedReflection.invokeInstance(engine, new String[]{"isMissionOver", "h"}));
    }

    public static Object getTriggerByName(Object engine, String name) {
        requireMissionEngine(engine);
        return RustedReflection.invokeInstance(engine, new String[]{"getTriggerByName", "d"}, name);
    }

    public static Object getTriggerById(Object engine, String id) {
        requireMissionEngine(engine);
        return RustedReflection.invokeInstance(engine, new String[]{"getTriggerById", "e"}, id);
    }

    public static Object getPointFromMapObject(Object engine, String objectName) {
        requireMissionEngine(engine);
        return RustedReflection.invokeInstance(engine, new String[]{"getPointFromMapObject", "f"}, objectName);
    }

    public static Map<String, Object> describeMissionTrigger(Object trigger) {
        requireMissionTrigger(trigger);
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        putField(result, trigger, "id", new String[]{"id", "a"});
        putField(result, trigger, "name", new String[]{"name", "b"});
        putField(result, trigger, "typeString", new String[]{"typeString", "c"});
        putField(result, trigger, "allConditions", new String[]{"allConditions", "d"});
        putField(result, trigger, "anyConditions", new String[]{"anyConditions", "e"});
        putCollectionField(result, trigger, "linkedTriggers", new String[]{"linkedTriggers", "f"});
        Object triggerType = RustedReflection.getFieldValue(trigger, new String[]{"triggerType", "g"});
        result.put("triggerType", triggerType);
        result.put("triggerTypeName", getTriggerTypeName(triggerType));
        putField(result, trigger, "sourceObject", new String[]{"sourceObject", "t"});
        putField(result, trigger, "spawnUnits", new String[]{"spawnUnits", "v"});
        putFloatField(result, trigger, "textOffsetX", new String[]{"textOffsetX", "w"});
        putFloatField(result, trigger, "textOffsetY", new String[]{"textOffsetY", "x"});
        putField(result, trigger, "team", new String[]{"team", "y"});
        putField(result, trigger, "text", new String[]{"text", "z"});
        putField(result, trigger, "globalMessage", new String[]{"globalMessage", "A"});
        putBooleanField(result, trigger, "allToActivate", new String[]{"allToActivate", "h"});
        putBooleanField(result, trigger, "active", new String[]{"active", "j"});
        putIntField(result, trigger, "activationFrame", new String[]{"activationFrame", "k"});
        putIntField(result, trigger, "conditionStartFrame", new String[]{"conditionStartFrame", "n"});
        putIntField(result, trigger, "repeatCount", new String[]{"repeatCount", "o"});
        putIntField(result, trigger, "repeatDelay", new String[]{"repeatDelay", "p"});
        putIntField(result, trigger, "resetActivationAfter", new String[]{"resetActivationAfter", "q"});
        putIntField(result, trigger, "delay", new String[]{"delay", "r"});
        putIntField(result, trigger, "warmup", new String[]{"warmup", "s"});
        putBooleanField(result, trigger, "pendingActivation", new String[]{"pendingActivation", "u"});
        putField(result, trigger, "textPaint", new String[]{"textPaint", "B"});
        putBooleanField(result, trigger, "textStyleArrow", new String[]{"textStyleArrow", "C"});
        result.put("x", Integer.valueOf(getTriggerX(trigger)));
        result.put("y", Integer.valueOf(getTriggerY(trigger)));
        result.put("activeNow", Boolean.valueOf(isTriggerActive(trigger)));
        return Collections.unmodifiableMap(result);
    }

    public static int getTriggerX(Object trigger) {
        requireMissionTrigger(trigger);
        Object value = RustedReflection.invokeInstance(trigger, new String[]{"getX", "b"});
        return value instanceof Number ? ((Number) value).intValue() : 0;
    }

    public static int getTriggerY(Object trigger) {
        requireMissionTrigger(trigger);
        Object value = RustedReflection.invokeInstance(trigger, new String[]{"getY", "c"});
        return value instanceof Number ? ((Number) value).intValue() : 0;
    }

    public static Object getTriggerTeam(Object trigger) {
        requireMissionTrigger(trigger);
        return RustedReflection.invokeInstance(trigger, new String[]{"getTeam", "a"});
    }

    public static boolean isTriggerActive(Object trigger) {
        requireMissionTrigger(trigger);
        return Boolean.TRUE.equals(RustedReflection.invokeInstance(trigger, new String[]{"isActive", "d"}));
    }

    public static boolean triggerContainsUnit(Object trigger, Object unit) {
        requireMissionTrigger(trigger);
        requireAny(unit, UNIT_CLASSES, "Unit");
        return Boolean.TRUE.equals(RustedReflection.invokeInstance(trigger, new String[]{"containsUnit", "a"}, unit));
    }

    public static boolean isUnitInsideTrigger(Object trigger, Object unit) {
        requireMissionTrigger(trigger);
        requireAny(unit, UNIT_CLASSES, "Unit");
        return Boolean.TRUE.equals(RustedReflection.invokeInstance(trigger, new String[]{"isUnitInside", "b"}, unit));
    }

    public static Map<String, Object> describeConditionSet(Object conditionSet) {
        requireAny(conditionSet, MISSION_TRIGGER_CONDITION_SET_CLASSES, "MissionTriggerConditionSet");
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        putCollectionField(result, conditionSet, "conditions", new String[]{"conditions", "a"});
        putBooleanField(result, conditionSet, "requireAll", new String[]{"requireAll", "b"});
        result.put("allSatisfied", Boolean.valueOf(areAllConditionsSatisfied(conditionSet)));
        result.put("anySatisfied", Boolean.valueOf(isAnyConditionSatisfied(conditionSet)));
        return Collections.unmodifiableMap(result);
    }

    public static List<Object> conditionSetConditionsSnapshot(Object conditionSet) {
        requireAny(conditionSet, MISSION_TRIGGER_CONDITION_SET_CLASSES, "MissionTriggerConditionSet");
        return snapshotField(conditionSet, new String[]{"conditions", "a"});
    }

    public static boolean areAllConditionsSatisfied(Object conditionSet) {
        requireAny(conditionSet, MISSION_TRIGGER_CONDITION_SET_CLASSES, "MissionTriggerConditionSet");
        return Boolean.TRUE.equals(RustedReflection.invokeInstance(conditionSet,
                new String[]{"areAllSatisfied", "a"}));
    }

    public static boolean isAnyConditionSatisfied(Object conditionSet) {
        requireAny(conditionSet, MISSION_TRIGGER_CONDITION_SET_CLASSES, "MissionTriggerConditionSet");
        return Boolean.TRUE.equals(RustedReflection.invokeInstance(conditionSet,
                new String[]{"isAnySatisfied", "b"}));
    }

    public static Map<String, Object> describeUnitDetectCondition(Object condition) {
        requireAny(condition, UNIT_DETECT_CONDITION_CLASSES, "UnitDetectCondition");
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        putField(result, condition, "maxCount", new String[]{"maxCount", "a"});
        putField(result, condition, "minCount", new String[]{"minCount", "b"});
        putField(result, condition, "team", new String[]{"team", "c"});
        putField(result, condition, "unitType", new String[]{"unitType", "d"});
        putBooleanField(result, condition, "onlyBuildings", new String[]{"onlyBuildings", "e"});
        putBooleanField(result, condition, "onlyIdle", new String[]{"onlyIdle", "f"});
        putBooleanField(result, condition, "onlyMainBuildings", new String[]{"onlyMainBuildings", "g"});
        putBooleanField(result, condition, "onlyOnResourcePool", new String[]{"onlyOnResourcePool", "h"});
        putBooleanField(result, condition, "onlyEmptyQueue", new String[]{"onlyEmptyQueue", "i"});
        putBooleanField(result, condition, "onlyBuilders", new String[]{"onlyBuilders", "j"});
        putIntField(result, condition, "onlyTechLevel", new String[]{"onlyTechLevel", "k"});
        putBooleanField(result, condition, "onlyAttack", new String[]{"onlyAttack", "l"});
        putBooleanField(result, condition, "onlyAttackAir", new String[]{"onlyAttackAir", "m"});
        putBooleanField(result, condition, "onlyIfEmpty", new String[]{"onlyIfEmpty", "n"});
        putField(result, condition, "tags", new String[]{"tags", "o"});
        putBooleanField(result, condition, "includeIncomplete", new String[]{"includeIncomplete", "p"});
        return Collections.unmodifiableMap(result);
    }

    public static boolean testUnitDetectCondition(Object condition, Object trigger) {
        requireAny(condition, UNIT_DETECT_CONDITION_CLASSES, "UnitDetectCondition");
        requireMissionTrigger(trigger);
        return Boolean.TRUE.equals(RustedReflection.invokeInstance(condition, new String[]{"test", "b"}, trigger));
    }

    public static boolean unitDetectConditionMatchesTriggerArea(Object condition, Object trigger) {
        requireAny(condition, UNIT_DETECT_CONDITION_CLASSES, "UnitDetectCondition");
        requireMissionTrigger(trigger);
        return Boolean.TRUE.equals(RustedReflection.invokeInstance(condition,
                new String[]{"matchesTriggerArea", "e"}, trigger));
    }

    public static Map<String, Object> describeTeamTagCondition(Object condition) {
        requireAny(condition, TEAM_TAG_CONDITION_CLASSES, "TeamTagCondition");
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        putField(result, condition, "team", new String[]{"team", "a"});
        putField(result, condition, "tags", new String[]{"tags", "b"});
        return Collections.unmodifiableMap(result);
    }

    public static boolean testMissionTriggerCondition(Object condition, Object trigger) {
        requireAny(condition, MISSION_TRIGGER_CONDITION_CLASSES, "MissionTriggerCondition");
        requireMissionTrigger(trigger);
        return Boolean.TRUE.equals(RustedReflection.invokeInstance(condition, new String[]{"test", "b"}, trigger));
    }

    public static boolean isMissionTriggerConditionSatisfied(Object condition, Object trigger) {
        requireAny(condition, MISSION_TRIGGER_CONDITION_CLASSES, "MissionTriggerCondition");
        requireMissionTrigger(trigger);
        return Boolean.TRUE.equals(RustedReflection.invokeInstance(condition,
                new String[]{"isSatisfied", "a"}, trigger));
    }

    public static boolean isMissionTriggerConditionReady(Object condition, Object trigger) {
        requireAny(condition, MISSION_TRIGGER_CONDITION_CLASSES, "MissionTriggerCondition");
        requireMissionTrigger(trigger);
        return Boolean.TRUE.equals(RustedReflection.invokeInstance(condition, new String[]{"isReady", "c"}, trigger));
    }

    public static List<String> missionTriggerTypeNames() {
        List<String> result = new ArrayList<String>(MISSION_TRIGGER_TYPE_ALIASES.length);
        for (Alias alias : MISSION_TRIGGER_TYPE_ALIASES) {
            result.add(alias.name);
        }
        return Collections.unmodifiableList(result);
    }

    public static List<Object> missionTriggerTypes() {
        List<Object> result = new ArrayList<Object>(MISSION_TRIGGER_TYPE_ALIASES.length);
        for (Alias alias : MISSION_TRIGGER_TYPE_ALIASES) {
            result.add(RustedReflection.getStaticFieldValue(MISSION_TRIGGER_TYPE_CLASSES, alias.fieldNames));
        }
        return Collections.unmodifiableList(result);
    }

    public static Object missionTriggerType(String name) {
        String normalized = normalizeAlias(name);
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("mission trigger type name must not be empty");
        }
        for (Alias alias : MISSION_TRIGGER_TYPE_ALIASES) {
            if (alias.matches(normalized)) {
                return RustedReflection.getStaticFieldValue(MISSION_TRIGGER_TYPE_CLASSES, alias.fieldNames);
            }
        }
        return RustedReflection.invokeStatic(MISSION_TRIGGER_TYPE_CLASSES,
                new String[]{"fromTypeName", "a"}, name);
    }

    public static String getTriggerTypeName(Object triggerType) {
        if (triggerType == null) {
            return null;
        }
        requireAny(triggerType, MISSION_TRIGGER_TYPE_CLASSES, "MissionTriggerType");
        Object value = RustedReflection.invokeInstance(triggerType, new String[]{"getTypeName", "a"});
        return value != null ? value.toString() : null;
    }

    public static String canonicalMissionTriggerTypeName(Object triggerType) {
        if (triggerType == null) {
            return null;
        }
        String typeName = getTriggerTypeName(triggerType);
        String normalized = normalizeAlias(typeName);
        for (Alias alias : MISSION_TRIGGER_TYPE_ALIASES) {
            Object candidate = RustedReflection.getStaticFieldValue(MISSION_TRIGGER_TYPE_CLASSES, alias.fieldNames);
            if (candidate == triggerType || candidate.equals(triggerType) || alias.matches(normalized)) {
                return alias.name;
            }
        }
        return typeName;
    }

    public static Map<String, Object> describeObjectivePanel(Object panel) {
        requireAny(panel, MISSION_OBJECTIVE_PANEL_CLASSES, "MissionObjectivePanel");
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        putCollectionField(result, panel, "lines", new String[]{"lines", "a"});
        putField(result, panel, "title", new String[]{"title", "f"});
        return Collections.unmodifiableMap(result);
    }

    public static Map<String, Object> describeMissionUnitCounter(Object counter) {
        requireAny(counter, MISSION_UNIT_COUNTER_CLASSES, "MissionUnitCounter");
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        putBooleanField(result, counter, "includeIncomplete", new String[]{"includeIncomplete", "a"});
        putCollectionField(result, counter, "entries", new String[]{"entries", "b"});
        return Collections.unmodifiableMap(result);
    }

    public static List<Object> missionUnitCounterEntriesSnapshot(Object counter) {
        requireAny(counter, MISSION_UNIT_COUNTER_CLASSES, "MissionUnitCounter");
        return snapshotField(counter, new String[]{"entries", "b"});
    }

    public static Map<String, Object> describeMissionUnitCountEntry(Object entry) {
        requireAny(entry, MISSION_UNIT_COUNT_ENTRY_CLASSES, "MissionUnitCountEntry");
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        putField(result, entry, "unitType", new String[]{"unitType", "a"});
        putIntField(result, entry, "count", new String[]{"count", "b"});
        return Collections.unmodifiableMap(result);
    }

    public static Map<String, Object> describeMissionWaveUnit(Object waveUnit) {
        requireAny(waveUnit, MISSION_WAVE_UNIT_CLASSES, "MissionWaveUnit");
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        putField(result, waveUnit, "unitType", new String[]{"unitType", "a"});
        putFloatField(result, waveUnit, "amount", new String[]{"amount", "b"});
        return Collections.unmodifiableMap(result);
    }

    public static Map<String, Object> describeMissionTriggerReference(Object reference) {
        requireAny(reference, MISSION_TRIGGER_REFERENCE_CLASSES, "MissionTriggerReference");
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        putField(result, reference, "trigger", new String[]{"trigger", "a"});
        result.put("name", getMissionTriggerReferenceName(reference));
        result.put("resolved", Boolean.valueOf(isMissionTriggerReferenceResolved(reference)));
        return Collections.unmodifiableMap(result);
    }

    public static String getMissionTriggerReferenceName(Object reference) {
        requireAny(reference, MISSION_TRIGGER_REFERENCE_CLASSES, "MissionTriggerReference");
        Object value = RustedReflection.invokeInstance(reference, new String[]{"getName", "a"});
        return value != null ? value.toString() : null;
    }

    public static boolean isMissionTriggerReferenceResolved(Object reference) {
        requireAny(reference, MISSION_TRIGGER_REFERENCE_CLASSES, "MissionTriggerReference");
        return Boolean.TRUE.equals(RustedReflection.invokeInstance(reference, new String[]{"isResolved", "b"}));
    }

    private static void requireMissionEngine(Object engine) {
        requireAny(engine, MISSION_ENGINE_CLASSES, "MissionEngine");
    }

    private static void requireMissionTrigger(Object trigger) {
        requireAny(trigger, MISSION_TRIGGER_CLASSES, "MissionTrigger");
    }

    private static void requireAny(Object value, String[] classNames, String label) {
        if (value == null) {
            throw new IllegalArgumentException(label + " must not be null");
        }
        if (!RustedReflection.isAnyClass(value.getClass(), classNames)) {
            throw new IllegalArgumentException("Expected " + label + ", got " + value.getClass().getName());
        }
    }

    private static List<Object> snapshotField(Object owner, String[] fieldNames) {
        return Collections.unmodifiableList(RustedReflection.snapshotIterable(
                RustedReflection.getFieldValue(owner, fieldNames)));
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

    private static void putBooleanField(Map<String, Object> result, Object owner, String key, String[] fieldNames) {
        result.put(key, Boolean.valueOf(RustedReflection.getBooleanField(owner, fieldNames)));
    }

    private static String normalizeAlias(String name) {
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

    private static final class Alias {
        private final String name;
        private final String[] fieldNames;

        private Alias(String name, String[] fieldNames) {
            this.name = name;
            this.fieldNames = fieldNames;
        }

        private boolean matches(String normalizedName) {
            return normalizeAlias(name).equals(normalizedName);
        }
    }
}
