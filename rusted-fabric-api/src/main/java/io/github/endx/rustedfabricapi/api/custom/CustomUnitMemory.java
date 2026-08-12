package io.github.endx.rustedfabricapi.api.custom;

import rustedwarfare.custom.CustomUnit;
import rustedwarfare.custom.logic.LogicBoolean$ReturnType;
import rustedwarfare.custom.logic.VariableScope;
import rustedwarfare.custom.logic.VariableScope$VariableData;
import rustedwarfare.custom.logic.VariableScope$VariableDataBoolean;
import rustedwarfare.custom.logic.VariableScope$VariableDataNumber;
import rustedwarfare.custom.logic.VariableScope$VariableDataString;
import rustedwarfare.custom.logic.VariableScope$VariableDataUnit;
import rustedwarfare.custom.logic.VariableScope$VariableName;
import rustedwarfare.unit.Unit;

import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalDouble;

/** Typed access to the native {@code defineUnitMemory} state stored on a custom-unit instance. */
public final class CustomUnitMemory {
    private CustomUnitMemory() { }

    public static boolean contains(CustomUnit unit, String name) {
        return data(unit, name) != null;
    }

    /** Returns whether the current unit metadata declares this name in {@code defineUnitMemory}. */
    public static boolean isDefined(CustomUnit unit, String name) {
        CustomUnit checked = Objects.requireNonNull(unit, "unit");
        return checked.getUnitMetadata() != null
                && checked.getUnitMetadata().definedMemoryVariables != null
                && checked.getUnitMetadata().definedMemoryVariables.get(variableName(name)) != null;
    }

    public static OptionalDouble findNumber(CustomUnit unit, String name) {
        VariableScope$VariableData value = data(unit, name);
        return value != null && value.getReturnType() == LogicBoolean$ReturnType.number
                ? OptionalDouble.of(value.readNumber(unit)) : OptionalDouble.empty();
    }

    public static double number(CustomUnit unit, String name, double fallback) {
        OptionalDouble value = findNumber(unit, name);
        return value.isPresent() ? value.getAsDouble() : fallback;
    }

    public static Optional<Boolean> findBoolean(CustomUnit unit, String name) {
        VariableScope$VariableData value = data(unit, name);
        return value != null && value.getReturnType() == LogicBoolean$ReturnType.bool
                ? Optional.of(Boolean.valueOf(value.read(unit))) : Optional.empty();
    }

    public static boolean bool(CustomUnit unit, String name, boolean fallback) {
        return findBoolean(unit, name).orElse(Boolean.valueOf(fallback)).booleanValue();
    }

    public static Optional<String> findString(CustomUnit unit, String name) {
        VariableScope$VariableData value = data(unit, name);
        if (value == null || value.getReturnType() != LogicBoolean$ReturnType.string) {
            return Optional.empty();
        }
        return Optional.ofNullable(value.readString(unit));
    }

    public static String string(CustomUnit unit, String name, String fallback) {
        return findString(unit, name).orElse(fallback);
    }

    public static Optional<Unit> findUnit(CustomUnit unit, String name) {
        VariableScope$VariableData value = data(unit, name);
        if (value == null || value.getReturnType() != LogicBoolean$ReturnType.unit) {
            return Optional.empty();
        }
        return Optional.ofNullable(value.readUnit(unit));
    }

    public static void setNumber(CustomUnit unit, String name, double value) {
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException("memory number must be finite");
        }
        mutableScope(unit).setDataRaw(variableName(name),
                new VariableScope$VariableDataNumber(value));
    }

    public static void setBoolean(CustomUnit unit, String name, boolean value) {
        mutableScope(unit).setDataRaw(variableName(name),
                new VariableScope$VariableDataBoolean(value));
    }

    public static void setString(CustomUnit unit, String name, String value) {
        mutableScope(unit).setDataRaw(variableName(name),
                new VariableScope$VariableDataString(Objects.requireNonNull(value, "value")));
    }

    public static void setUnit(CustomUnit unit, String name, Unit value) {
        mutableScope(unit).setDataRaw(variableName(name),
                new VariableScope$VariableDataUnit(value));
    }

    private static VariableScope$VariableData data(CustomUnit unit, String name) {
        CustomUnit checked = Objects.requireNonNull(unit, "unit");
        VariableScope scope = checked.memory;
        return scope != null ? scope.getDataObjectRaw(variableName(name)) : null;
    }

    private static VariableScope mutableScope(CustomUnit unit) {
        CustomUnit checked = Objects.requireNonNull(unit, "unit");
        if (checked.memory == null || checked.memory == VariableScope.emptyVariableScope) {
            checked.memory = new VariableScope();
        }
        return checked.memory;
    }

    private static VariableScope$VariableName variableName(String name) {
        String checked = Objects.requireNonNull(name, "name").trim()
                .toLowerCase(Locale.ROOT);
        if (!checked.matches("[a-z_][a-z0-9_]*")) {
            throw new IllegalArgumentException("invalid custom-unit memory name: " + name);
        }
        return VariableScope$VariableName.get(checked);
    }
}
