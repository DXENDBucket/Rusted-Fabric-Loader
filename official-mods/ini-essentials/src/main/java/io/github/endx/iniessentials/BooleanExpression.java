package io.github.endx.iniessentials;

import io.github.endx.rustedfabricapi.api.logic.LogicBooleanApi;
import rustedwarfare.unit.OrderableUnit;

/** A native LogicBoolean expression retained for evaluation against a runtime unit. */
final class BooleanExpression {
    private final Object compiled;

    private BooleanExpression(Object compiled) {
        this.compiled = compiled;
    }

    static BooleanExpression compile(Object metadata, String source) {
        String checked = source != null ? source.trim() : "";
        if (checked.isEmpty()) {
            throw new IllegalArgumentException("boolean expression must not be empty");
        }
        return new BooleanExpression(
                LogicBooleanApi.parseBooleanBlock(metadata, checked, false));
    }

    boolean evaluate(OrderableUnit unit) {
        return LogicBooleanApi.readBoolean(compiled, unit);
    }

    boolean isStaticFalse() {
        return LogicBooleanApi.isStaticFalse(compiled);
    }
}
