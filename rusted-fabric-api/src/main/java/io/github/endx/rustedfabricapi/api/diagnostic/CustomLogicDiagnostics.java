package io.github.endx.rustedfabricapi.api.diagnostic;

import io.github.endx.rustedfabricapi.api.util.RustedReflection;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class CustomLogicDiagnostics {
    private static final String[] LOGIC_BOOLEAN_CLASSES = {
            "rustedwarfare.custom.logic.LogicBoolean",
            "com.corrodinggames.rts.game.units.custom.logicBooleans.LogicBoolean"
    };
    private static final String[] MUTABLE_UNIT_STATS_CLASSES = {
            "rustedwarfare.custom.MutableUnitStats",
            "com.corrodinggames.rts.game.units.custom.as"
    };
    private static final String[] CUSTOM_UNIT_CLASSES = {
            "rustedwarfare.custom.CustomUnit",
            "com.corrodinggames.rts.game.units.custom.j"
    };
    private static final String[] MUTABLE_STAT_ACCESSOR_CLASSES = {
            "rustedwarfare.custom.stats.MutableStatAccessor",
            "com.corrodinggames.rts.game.units.custom.at"
    };
    private static final String[] METADATA_MUTABLE_STAT_ACCESSOR_CLASSES = {
            "rustedwarfare.custom.stats.MetadataMutableStatAccessor",
            "com.corrodinggames.rts.game.units.custom.aw"
    };
    private static final String[] RUNTIME_MUTABLE_STAT_ACCESSOR_CLASSES = {
            "rustedwarfare.custom.stats.RuntimeMutableStatAccessor",
            "com.corrodinggames.rts.game.units.custom.ax"
    };
    private static final String[] MUTABLE_STAT_WRITER_FACTORY_CLASSES = {
            "rustedwarfare.custom.stats.MutableStatWriterFactory",
            "com.corrodinggames.rts.game.units.custom.au"
    };
    private static final String[] MUTABLE_STAT_CACHED_WRITER_ELEMENT_CLASSES = {
            "rustedwarfare.custom.stats.MutableStatCachedWriterElement",
            "com.corrodinggames.rts.game.units.custom.av"
    };
    private static final String[] RANDOM_MOVEMENT_BEHAVIOR_CLASSES = {
            "rustedwarfare.custom.runtime.RandomMovementBehavior",
            "com.corrodinggames.rts.game.units.custom.b.l"
    };
    private static final String[] REPEL_FROM_UNITS_BEHAVIOR_CLASSES = {
            "rustedwarfare.custom.runtime.RepelFromUnitsBehavior",
            "com.corrodinggames.rts.game.units.custom.b.j"
    };

    private CustomLogicDiagnostics() {
    }

    public static Map<String, Object> describeLogicBoolean(Object logicBoolean) {
        requireAny(logicBoolean, LOGIC_BOOLEAN_CLASSES, "LogicBoolean");
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("className", logicBoolean.getClass().getName());
        putOptionalInvoke(result, logicBoolean, "name", new String[]{"getName"});
        putOptionalInvoke(result, logicBoolean, "classDebugName", new String[]{"getClassDebugName"});
        putOptionalInvoke(result, logicBoolean, "returnType", new String[]{"getReturnType"});
        putOptionalField(result, logicBoolean, "x1", new String[]{"x1"});
        putOptionalField(result, logicBoolean, "y1", new String[]{"y1"});
        putOptionalField(result, logicBoolean, "x2", new String[]{"x2"});
        putOptionalField(result, logicBoolean, "y2", new String[]{"y2"});
        putOptionalField(result, logicBoolean, "firstOperand", new String[]{"firstOperand", "a"});
        putOptionalField(result, logicBoolean, "secondOperand", new String[]{"secondOperand", "b"});
        putOptionalField(result, logicBoolean, "stringExpression", new String[]{"stringExpression", "a"});
        putOptionalField(result, logicBoolean, "xExpression", new String[]{"xExpression", "x"});
        putOptionalField(result, logicBoolean, "yExpression", new String[]{"yExpression", "y"});
        putOptionalField(result, logicBoolean, "xOffsetExpression", new String[]{"xOffsetExpression", "x"});
        putOptionalField(result, logicBoolean, "yOffsetExpression", new String[]{"yOffsetExpression", "y"});
        putOptionalField(result, logicBoolean, "xOffset", new String[]{"xOffset", "x"});
        putOptionalField(result, logicBoolean, "yOffset", new String[]{"yOffset", "y"});
        putOptionalField(result, logicBoolean, "dir", new String[]{"dir"});
        putOptionalField(result, logicBoolean, "dirOffset", new String[]{"dirOffset"});
        putOptionalField(result, logicBoolean, "height", new String[]{"height"});
        putOptionalField(result, logicBoolean, "teamId", new String[]{"teamId"});
        putOptionalField(result, logicBoolean, "flagIdExpression", new String[]{"flagIdExpression", "id"});
        return Collections.unmodifiableMap(result);
    }

    public static Map<String, Object> describeMutableStatAccessor(Object accessor) {
        requireAny(accessor, MUTABLE_STAT_ACCESSOR_CLASSES, "MutableStatAccessor");
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("className", accessor.getClass().getName());
        putOptionalField(result, accessor, "id", new String[]{"id", "a"});
        putOptionalField(result, accessor, "name", new String[]{"name", "b"});
        putOptionalInvoke(result, accessor, "returnType", new String[]{"getReturnType", "a"});
        putOptionalInvoke(result, accessor, "runtimeField", new String[]{"isRuntimeField", "b"});
        result.put("metadataAccessor", Boolean.valueOf(
                RustedReflection.isAnyClass(accessor.getClass(), METADATA_MUTABLE_STAT_ACCESSOR_CLASSES)));
        result.put("runtimeAccessor", Boolean.valueOf(
                RustedReflection.isAnyClass(accessor.getClass(), RUNTIME_MUTABLE_STAT_ACCESSOR_CLASSES)));
        return Collections.unmodifiableMap(result);
    }

    public static double readMutableStat(Object accessor, Object customUnit, Object metadataStats) {
        requireAny(accessor, MUTABLE_STAT_ACCESSOR_CLASSES, "MutableStatAccessor");
        requireAny(customUnit, CUSTOM_UNIT_CLASSES, "CustomUnit");
        requireAny(metadataStats, MUTABLE_UNIT_STATS_CLASSES, "MutableUnitStats");
        Object value = RustedReflection.invokeInstance(accessor, new String[]{"read", "a"}, customUnit, metadataStats);
        return value instanceof Number ? ((Number) value).doubleValue() : 0.0D;
    }

    public static double readMetadataMutableStat(Object accessor, Object metadataStats) {
        requireAny(accessor, METADATA_MUTABLE_STAT_ACCESSOR_CLASSES, "MetadataMutableStatAccessor");
        requireAny(metadataStats, MUTABLE_UNIT_STATS_CLASSES, "MutableUnitStats");
        Object value = RustedReflection.invokeInstance(accessor, new String[]{"readMetadataValue", "a"}, metadataStats);
        return value instanceof Number ? ((Number) value).doubleValue() : 0.0D;
    }

    public static double readRuntimeMutableStat(Object accessor, Object customUnit) {
        requireAny(accessor, RUNTIME_MUTABLE_STAT_ACCESSOR_CLASSES, "RuntimeMutableStatAccessor");
        requireAny(customUnit, CUSTOM_UNIT_CLASSES, "CustomUnit");
        Object value = RustedReflection.invokeInstance(accessor, new String[]{"readRuntime", "a"}, customUnit);
        return value instanceof Number ? ((Number) value).doubleValue() : 0.0D;
    }

    public static Map<String, Object> describeMutableStatWriterFactory(Object writerFactory) {
        requireAny(writerFactory, MUTABLE_STAT_WRITER_FACTORY_CLASSES, "MutableStatWriterFactory");
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("className", writerFactory.getClass().getName());
        putOptionalField(result, writerFactory, "metadata", new String[]{"metadata", "a"});
        return Collections.unmodifiableMap(result);
    }

    public static Object createMutableStatWriterElement(Object writerFactory, String section, String key,
                                                        String operator, String valueExpression) {
        requireAny(writerFactory, MUTABLE_STAT_WRITER_FACTORY_CLASSES, "MutableStatWriterFactory");
        return RustedReflection.invokeInstance(writerFactory, new String[]{"createWriterElement"},
                section, key, operator, valueExpression);
    }

    public static Map<String, Object> describeMutableStatWriterElement(Object writerElement) {
        requireAny(writerElement, MUTABLE_STAT_CACHED_WRITER_ELEMENT_CLASSES, "MutableStatCachedWriterElement");
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("className", writerElement.getClass().getName());
        putOptionalField(result, writerElement, "statAccessor", new String[]{"statAccessor", "a"});
        putOptionalField(result, writerElement, "valueExpression", new String[]{"valueExpression", "b"});
        putOptionalField(result, writerElement, "operator", new String[]{"operator", "c"});
        return Collections.unmodifiableMap(result);
    }

    public static Map<String, Object> describeRandomMovementBehavior(Object behavior) {
        requireAny(behavior, RANDOM_MOVEMENT_BEHAVIOR_CLASSES, "RandomMovementBehavior");
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("className", behavior.getClass().getName());
        putOptionalField(result, behavior, "enabledCondition", new String[]{"enabledCondition", "a"});
        putOptionalField(result, behavior, "speed", new String[]{"speed", "b"});
        putOptionalField(result, behavior, "maxSpeed", new String[]{"maxSpeed", "c"});
        putOptionalField(result, behavior, "awayFromEdge", new String[]{"awayFromEdge", "d"});
        return Collections.unmodifiableMap(result);
    }

    public static Map<String, Object> describeRepelFromUnitsBehavior(Object behavior) {
        requireAny(behavior, REPEL_FROM_UNITS_BEHAVIOR_CLASSES, "RepelFromUnitsBehavior");
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("className", behavior.getClass().getName());
        putOptionalField(result, behavior, "enabledCondition", new String[]{"enabledCondition", "a"});
        putOptionalField(result, behavior, "speed", new String[]{"speed", "b"});
        putOptionalField(result, behavior, "maxSpeed", new String[]{"maxSpeed", "c"});
        putOptionalField(result, behavior, "otherUnitHasTags", new String[]{"otherUnitHasTags", "d"});
        putOptionalField(result, behavior, "onlySameTeam", new String[]{"onlySameTeam", "e"});
        return Collections.unmodifiableMap(result);
    }

    private static void putOptionalField(Map<String, Object> result, Object owner, String key, String[] fieldNames) {
        try {
            Object value = RustedReflection.getFieldValue(owner, fieldNames);
            result.put(key, value);
        } catch (RuntimeException ignored) {
        }
    }

    private static void putOptionalInvoke(Map<String, Object> result, Object owner, String key, String[] methodNames) {
        try {
            Object value = RustedReflection.invokeInstance(owner, methodNames);
            result.put(key, value != null ? value.toString() : null);
        } catch (RuntimeException ignored) {
        }
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
