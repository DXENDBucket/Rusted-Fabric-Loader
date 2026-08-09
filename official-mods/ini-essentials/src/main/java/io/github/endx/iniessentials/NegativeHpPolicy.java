package io.github.endx.iniessentials;

import rustedwarfare.custom.CustomUnit;
import rustedwarfare.unit.Unit;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

public final class NegativeHpPolicy {
    private static final Map<Object, BooleanExpression> POLICIES =
            Collections.synchronizedMap(new WeakHashMap<Object, BooleanExpression>());

    private NegativeHpPolicy() { }

    /** Retained source-compatible static configuration bridge. */
    public static void configure(Object metadata, boolean enabled) {
        if (metadata == null) return;
        configure(metadata, enabled ? BooleanExpression.compile(metadata, "true") : null);
    }

    static void configure(Object metadata, BooleanExpression expression) {
        if (metadata == null) return;
        if (expression != null && !expression.isStaticFalse()) {
            POLICIES.put(metadata, expression);
        } else {
            POLICIES.remove(metadata);
        }
    }

    public static boolean allows(Unit unit) {
        if (!(unit instanceof CustomUnit)) return false;
        CustomUnit customUnit = (CustomUnit) unit;
        BooleanExpression expression = POLICIES.get(customUnit.unitMetadata);
        return expression != null && expression.evaluate(customUnit);
    }
}
