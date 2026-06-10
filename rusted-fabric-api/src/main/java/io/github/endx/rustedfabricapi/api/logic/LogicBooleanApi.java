package io.github.endx.rustedfabricapi.api.logic;

import io.github.endx.rustedfabricapi.api.util.RustedReflection;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class LogicBooleanApi {
    private static final String[] LOGIC_BOOLEAN_CLASSES = {
            "rustedwarfare.custom.logic.LogicBoolean",
            "com.corrodinggames.rts.game.units.custom.logicBooleans.LogicBoolean"
    };
    private static final String[] LOGIC_BOOLEAN_LOADER_CLASSES = {
            "rustedwarfare.custom.logic.LogicBooleanLoader",
            "com.corrodinggames.rts.game.units.custom.logicBooleans.LogicBooleanLoader"
    };
    private static final String[] RETURN_TYPE_CLASSES = {
            "rustedwarfare.custom.logic.LogicBoolean$ReturnType",
            "com.corrodinggames.rts.game.units.custom.logicBooleans.LogicBoolean$ReturnType"
    };

    private LogicBooleanApi() {
    }

    public static Object create(Object metadata, String expression) {
        return RustedReflection.invokeStatic(LOGIC_BOOLEAN_CLASSES, new String[]{"create"}, metadata, expression);
    }

    public static Object create(Object metadata, String expression, Object defaultValue) {
        return RustedReflection.invokeStatic(LOGIC_BOOLEAN_CLASSES, new String[]{"create"},
                metadata, expression, defaultValue);
    }

    public static Object parseNumberBlock(Object metadata, String expression) {
        return RustedReflection.invokeStatic(LOGIC_BOOLEAN_LOADER_CLASSES, new String[]{"parseNumberBlock", "a"},
                metadata, expression);
    }

    public static Object parseBooleanBlock(Object metadata, String expression, boolean allowUnits) {
        return RustedReflection.invokeStatic(LOGIC_BOOLEAN_LOADER_CLASSES, new String[]{"parseBooleanBlock", "a"},
                metadata, expression, Boolean.valueOf(allowUnits));
    }

    public static Object with(Object logicBoolean, Object metadata, String section, String key) {
        requireLogicBoolean(logicBoolean);
        return RustedReflection.invokeInstance(logicBoolean, new String[]{"with"}, metadata, section, key);
    }

    public static String getReturnTypeName(Object logicBoolean) {
        Object returnType = getReturnType(logicBoolean);
        return returnType != null ? returnType.toString() : null;
    }

    public static Object getReturnType(Object logicBoolean) {
        requireLogicBoolean(logicBoolean);
        return RustedReflection.invokeInstance(logicBoolean, new String[]{"getReturnType"});
    }

    public static boolean readBoolean(Object logicBoolean, Object orderableUnit) {
        requireLogicBoolean(logicBoolean);
        Object value = RustedReflection.invokeInstance(logicBoolean, new String[]{"read"}, orderableUnit);
        return Boolean.TRUE.equals(value);
    }

    public static float readNumber(Object logicBoolean, Object orderableUnit) {
        requireLogicBoolean(logicBoolean);
        Object value = RustedReflection.invokeInstance(logicBoolean, new String[]{"readNumber"}, orderableUnit);
        return value instanceof Number ? ((Number) value).floatValue() : 0.0F;
    }

    public static String readString(Object logicBoolean, Object orderableUnit) {
        requireLogicBoolean(logicBoolean);
        Object value = RustedReflection.invokeInstance(logicBoolean, new String[]{"readString"}, orderableUnit);
        return value != null ? value.toString() : null;
    }

    public static Object readUnit(Object logicBoolean, Object orderableUnit) {
        requireLogicBoolean(logicBoolean);
        return RustedReflection.invokeInstance(logicBoolean, new String[]{"readUnit"}, orderableUnit);
    }

    public static int getArraySize(Object logicBoolean, Object orderableUnit) {
        requireLogicBoolean(logicBoolean);
        Object value = RustedReflection.invokeInstance(logicBoolean, new String[]{"getArraySize"}, orderableUnit);
        return value instanceof Number ? ((Number) value).intValue() : 0;
    }

    public static Object readArrayElement(Object logicBoolean, Object orderableUnit, int index) {
        requireLogicBoolean(logicBoolean);
        return RustedReflection.invokeInstance(logicBoolean, new String[]{"readArrayElement"},
                orderableUnit, Integer.valueOf(index));
    }

    public static String valueToStringDebug(Object logicBoolean, Object orderableUnit) {
        requireLogicBoolean(logicBoolean);
        Object value = RustedReflection.invokeInstance(logicBoolean, new String[]{"valueToStringDebug"}, orderableUnit);
        return value != null ? value.toString() : null;
    }

    public static String getMatchFailReasonForPlayer(Object logicBoolean, Object orderableUnit) {
        requireLogicBoolean(logicBoolean);
        Object value = RustedReflection.invokeInstance(logicBoolean, new String[]{"getMatchFailReasonForPlayer"},
                orderableUnit);
        return value != null ? value.toString() : null;
    }

    public static boolean isStaticTrue(Object logicBoolean) {
        return Boolean.TRUE.equals(RustedReflection.invokeStatic(LOGIC_BOOLEAN_CLASSES,
                new String[]{"isStaticTrue"}, logicBoolean));
    }

    public static boolean isStaticFalse(Object logicBoolean) {
        return Boolean.TRUE.equals(RustedReflection.invokeStatic(LOGIC_BOOLEAN_CLASSES,
                new String[]{"isStaticFalse"}, logicBoolean));
    }

    public static boolean isStaticNull(Object logicBoolean) {
        return Boolean.TRUE.equals(RustedReflection.invokeStatic(LOGIC_BOOLEAN_CLASSES,
                new String[]{"isStaticNull"}, logicBoolean));
    }

    public static boolean isStaticNumber(Object logicBoolean) {
        return Boolean.TRUE.equals(RustedReflection.invokeStatic(LOGIC_BOOLEAN_CLASSES,
                new String[]{"isStaticNumber"}, logicBoolean));
    }

    public static Float getStaticNumber(Object logicBoolean) {
        Object value = RustedReflection.invokeStatic(LOGIC_BOOLEAN_CLASSES, new String[]{"getStaticNumber"},
                logicBoolean);
        return value instanceof Number ? Float.valueOf(((Number) value).floatValue()) : null;
    }

    public static String returnTypeToUserString(String returnTypeName) {
        Object returnType = returnTypeValue(returnTypeName);
        Object value = RustedReflection.invokeStatic(RETURN_TYPE_CLASSES, new String[]{"toUserString"}, returnType);
        return value != null ? value.toString() : null;
    }

    public static boolean returnTypeCanBeNull(String returnTypeName) {
        Object returnType = returnTypeValue(returnTypeName);
        return Boolean.TRUE.equals(RustedReflection.invokeStatic(RETURN_TYPE_CLASSES,
                new String[]{"canBeNull"}, returnType));
    }

    public static boolean returnTypeIsArray(String returnTypeName) {
        Object returnType = returnTypeValue(returnTypeName);
        return Boolean.TRUE.equals(RustedReflection.invokeStatic(RETURN_TYPE_CLASSES,
                new String[]{"isArrayType"}, returnType));
    }

    public static String getArrayBaseReturnType(String returnTypeName) {
        Object returnType = returnTypeValue(returnTypeName);
        Object value = RustedReflection.invokeStatic(RETURN_TYPE_CLASSES, new String[]{"getArrayBaseType"}, returnType);
        return value != null ? value.toString() : null;
    }

    public static String getArrayReturnTypeFromBase(String returnTypeName) {
        Object returnType = returnTypeValue(returnTypeName);
        Object value = RustedReflection.invokeStatic(RETURN_TYPE_CLASSES,
                new String[]{"getArrayTypeFromBase"}, returnType);
        return value != null ? value.toString() : null;
    }

    public static List<String> returnTypeNames() {
        Object values = RustedReflection.invokeStatic(RETURN_TYPE_CLASSES, new String[]{"values"});
        List<Object> rawValues = RustedReflection.snapshotIterable(values);
        List<String> result = new ArrayList<String>(rawValues.size());
        for (Object value : rawValues) {
            result.add(value.toString());
        }
        return Collections.unmodifiableList(result);
    }

    private static Object returnTypeValue(String returnTypeName) {
        if (returnTypeName == null || returnTypeName.trim().isEmpty()) {
            throw new IllegalArgumentException("returnTypeName must not be empty");
        }
        return RustedReflection.invokeStatic(RETURN_TYPE_CLASSES, new String[]{"valueOf"}, returnTypeName);
    }

    private static void requireLogicBoolean(Object logicBoolean) {
        if (logicBoolean == null) {
            throw new IllegalArgumentException("logicBoolean must not be null");
        }
        if (!RustedReflection.isAnyClass(logicBoolean.getClass(), LOGIC_BOOLEAN_CLASSES)) {
            throw new IllegalArgumentException("Expected LogicBoolean, got " + logicBoolean.getClass().getName());
        }
    }
}
