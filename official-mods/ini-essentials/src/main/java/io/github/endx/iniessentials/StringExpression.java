package io.github.endx.iniessentials;

import io.github.endx.rustedfabricapi.api.logic.LogicBooleanApi;
import rustedwarfare.unit.OrderableUnit;

/** A native LogicBoolean string expression retained for runtime evaluation. */
public final class StringExpression {
    private final String constant;
    private final Object compiled;

    private StringExpression(String constant, Object compiled) {
        this.constant = constant;
        this.compiled = compiled;
    }

    public static StringExpression constant(String value) {
        String checked = value != null ? value.trim() : "";
        if (checked.isEmpty()) throw new IllegalArgumentException("string value must not be empty");
        return new StringExpression(checked, null);
    }

    public static StringExpression compile(Object metadata, String source) {
        String checked = source != null ? source.trim() : "";
        if (checked.isEmpty()) throw new IllegalArgumentException("string expression must not be empty");
        Object compiled = LogicBooleanApi.create(metadata, checked);
        String returnType = LogicBooleanApi.getReturnTypeName(compiled);
        if (returnType == null || !"string".equalsIgnoreCase(returnType)) {
            throw new IllegalArgumentException("expression must return a string, got " + returnType);
        }
        return new StringExpression(null, compiled);
    }

    public String evaluate(OrderableUnit unit) {
        String result = compiled != null ? LogicBooleanApi.readString(compiled, unit) : constant;
        if (result == null || result.trim().isEmpty()) {
            throw new IllegalArgumentException("string expression produced an empty value");
        }
        return result.trim();
    }
}
