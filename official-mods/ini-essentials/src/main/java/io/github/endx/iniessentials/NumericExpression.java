package io.github.endx.iniessentials;

import io.github.endx.rustedfabricapi.api.logic.LogicBooleanApi;
import rustedwarfare.unit.OrderableUnit;

public final class NumericExpression {
    private final String source;
    private final Object compiled;

    private NumericExpression(String source, Object compiled) {
        this.source = source;
        this.compiled = compiled;
    }

    public static NumericExpression compile(Object metadata, String source) {
        String checked = source != null ? source.trim() : "";
        if (checked.isEmpty()) throw new IllegalArgumentException("numeric expression must not be empty");
        return new NumericExpression(checked, LogicBooleanApi.parseNumberBlock(metadata, checked));
    }

    public static NumericExpression compile(Object metadata, String source, String fallback) {
        return compile(metadata, source != null ? source : fallback);
    }

    public float evaluate(OrderableUnit unit) {
        float value = LogicBooleanApi.readNumber(compiled, unit);
        if (!Float.isFinite(value)) {
            throw new IllegalArgumentException("expression produced a non-finite value: " + source);
        }
        return value;
    }
}
