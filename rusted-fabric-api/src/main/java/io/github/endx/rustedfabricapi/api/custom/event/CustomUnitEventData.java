package io.github.endx.rustedfabricapi.api.custom.event;

import rustedwarfare.custom.logic.VariableScope;
import rustedwarfare.custom.logic.VariableScope$VariableDataBoolean;
import rustedwarfare.custom.logic.VariableScope$VariableDataNumber;
import rustedwarfare.custom.logic.VariableScope$VariableDataString;
import rustedwarfare.custom.logic.VariableScope$VariableDataUnit;
import rustedwarfare.custom.logic.VariableScope$VariableName;
import rustedwarfare.unit.Unit;

import java.util.Locale;
import java.util.Objects;

/** Mutable typed view of the native data attached to a queued custom-unit event. */
public final class CustomUnitEventData {
    private final VariableScope scope;

    private CustomUnitEventData(VariableScope scope) {
        this.scope = Objects.requireNonNull(scope, "scope");
    }

    public static CustomUnitEventData create() {
        return new CustomUnitEventData(new VariableScope());
    }

    public static CustomUnitEventData wrap(VariableScope scope) {
        return new CustomUnitEventData(scope);
    }

    public VariableScope nativeScope() { return scope; }

    public boolean contains(String name) {
        return scope.getDataObjectRaw(variableName(name)) != null;
    }

    public CustomUnitEventData putNumber(String name, double value) {
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException("value must be finite");
        }
        scope.setDataRaw(variableName(name), new VariableScope$VariableDataNumber(value));
        return this;
    }

    public CustomUnitEventData putBoolean(String name, boolean value) {
        scope.setDataRaw(variableName(name), new VariableScope$VariableDataBoolean(value));
        return this;
    }

    public CustomUnitEventData putString(String name, String value) {
        scope.setDataRaw(variableName(name),
                new VariableScope$VariableDataString(Objects.requireNonNull(value, "value")));
        return this;
    }

    public CustomUnitEventData putUnit(String name, Unit value) {
        scope.setDataRaw(variableName(name), new VariableScope$VariableDataUnit(value));
        return this;
    }

    private static VariableScope$VariableName variableName(String name) {
        String normalized = Objects.requireNonNull(name, "name").trim()
                .toLowerCase(Locale.ROOT);
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("name must not be empty");
        }
        return VariableScope$VariableName.get(normalized);
    }
}
