package io.github.endx.rustedfabricapi.api.logic;

import rustedwarfare.unit.OrderableUnit;
import rustedwarfare.unit.Unit;

import java.util.Objects;
import java.util.Optional;

/** A compiled native LogicBoolean expression whose return type is a unit reference. */
public final class UnitReferenceExpression {
    private final String source;
    private final Object compiled;

    private UnitReferenceExpression(String source, Object compiled) {
        this.source = source;
        this.compiled = compiled;
    }

    /**
     * Compiles any native expression returning a unit, including chained references such as
     * {@code self.customTarget1} and marker-producing references such as
     * {@code self.getOffsetRelative(y=100)}.
     */
    public static UnitReferenceExpression compile(Object metadata, String expression) {
        String checked = Objects.requireNonNull(expression, "expression").trim();
        if (checked.isEmpty()) {
            throw new IllegalArgumentException("unit reference expression must not be empty");
        }
        Object compiled = LogicBooleanApi.parseBooleanBlock(metadata, checked, true);
        String returnType = LogicBooleanApi.getReturnTypeName(compiled);
        if (!"unit".equalsIgnoreCase(returnType)) {
            throw new IllegalArgumentException("expression must return a unit, got " + returnType
                    + ": " + checked);
        }
        return new UnitReferenceExpression(checked, compiled);
    }

    /** Evaluates against {@code self}; null native references become an empty optional. */
    public Optional<Unit> evaluate(OrderableUnit self) {
        Object result = LogicBooleanApi.readUnit(compiled,
                Objects.requireNonNull(self, "self"));
        if (result == null) return Optional.empty();
        if (!(result instanceof Unit)) {
            throw new IllegalStateException("native unit reference returned "
                    + result.getClass().getName());
        }
        return Optional.of((Unit) result);
    }

    public Unit evaluateOrNull(OrderableUnit self) {
        return evaluate(self).orElse(null);
    }

    public String source() {
        return source;
    }

    /** Raw compiled LogicBoolean for APIs not yet represented by a typed facade. */
    public Object compiled() {
        return compiled;
    }

    @Override
    public String toString() {
        return "UnitReferenceExpression{" + source + '}';
    }
}
