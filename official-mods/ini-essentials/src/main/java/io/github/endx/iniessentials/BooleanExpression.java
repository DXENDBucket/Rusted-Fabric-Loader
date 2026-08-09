package io.github.endx.iniessentials;

import io.github.endx.rustedfabricapi.api.logic.LogicBooleanApi;
import rustedwarfare.unit.OrderableUnit;

/** A native LogicBoolean expression retained for evaluation against a runtime unit. */
public final class BooleanExpression {
    private final Object compiled;

    private BooleanExpression(Object compiled) {
        this.compiled = compiled;
    }

    public static BooleanExpression compile(Object metadata, String source) {
        String checked = source != null ? source.trim() : "";
        if (checked.isEmpty()) {
            throw new IllegalArgumentException("boolean expression must not be empty");
        }
        return new BooleanExpression(
                LogicBooleanApi.parseBooleanBlock(metadata, checked, false));
    }

    public static BooleanExpression compile(Object metadata, String source, String fallback) {
        return compile(metadata, source != null ? source : fallback);
    }

    public boolean evaluate(OrderableUnit unit) {
        return LogicBooleanApi.readBoolean(compiled, unit);
    }

    public boolean isStaticFalse() {
        return LogicBooleanApi.isStaticFalse(compiled);
    }
}
