package io.github.endx.rustedfabricapi.impl.logic;

import io.github.endx.rustedfabricapi.api.logic.LogicUnitPropertyDefinition;
import rustedwarfare.custom.logic.LogicBoolean;
import rustedwarfare.custom.logic.LogicNumberFunction;
import rustedwarfare.custom.logic.LogicString;
import rustedwarfare.unit.OrderableUnit;

/** Creates native LogicBoolean prototypes backing public unit-property definitions. */
public final class RegisteredLogicUnitProperty {
    private RegisteredLogicUnitProperty() { }

    public static LogicBoolean create(LogicUnitPropertyDefinition definition) {
        switch (definition.valueType()) {
            case BOOLEAN: return new BooleanProperty(definition);
            case NUMBER: return new NumberProperty(definition);
            case STRING: return new StringProperty(definition);
            default: throw new IllegalArgumentException("unsupported unit property type");
        }
    }

    private static final class BooleanProperty extends LogicBoolean {
        private final LogicUnitPropertyDefinition definition;
        BooleanProperty(LogicUnitPropertyDefinition definition) { this.definition = definition; }
        @Override public boolean read(OrderableUnit unit) {
            return definition.evaluateBoolean(unit);
        }
        @Override public String getMatchFailReasonForPlayer(OrderableUnit unit) {
            return definition.name() + "=" + read(unit);
        }
    }

    private static final class NumberProperty extends LogicNumberFunction {
        private final LogicUnitPropertyDefinition definition;
        NumberProperty(LogicUnitPropertyDefinition definition) { this.definition = definition; }
        @Override public String getName() { return definition.name(); }
        @Override public float readNumber(OrderableUnit unit) {
            return definition.evaluateNumber(unit);
        }
    }

    private static final class StringProperty extends LogicString {
        private final LogicUnitPropertyDefinition definition;
        StringProperty(LogicUnitPropertyDefinition definition) { this.definition = definition; }
        @Override public String readString(OrderableUnit unit) {
            return definition.evaluateString(unit);
        }
        @Override public String getMatchFailReasonForPlayer(OrderableUnit unit) {
            return definition.name() + "=" + readString(unit);
        }
    }
}
