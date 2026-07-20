package io.github.endx.rustedfabricapi.api.custom.action;

import android.graphics.PointF;
import rustedwarfare.custom.CustomUnit;
import rustedwarfare.custom.action.effect.CustomActionEffect;
import rustedwarfare.unit.Unit;
import rustedwarfare.unit.action.UnitAction;

import java.util.Objects;

/** Execution helpers for mapped custom-unit action effects. */
public final class CustomActionEffects {
    private CustomActionEffects() {
    }

    public static boolean execute(CustomActionEffect effect, CustomUnit actor,
                                  UnitAction action, int recursionDepth) {
        return executeInternal(effect, actor, action, null, null, recursionDepth);
    }

    public static boolean executeAt(CustomActionEffect effect, CustomUnit actor,
                                    UnitAction action, float x, float y,
                                    Unit target, int recursionDepth) {
        if (!Float.isFinite(x) || !Float.isFinite(y)) {
            throw new IllegalArgumentException("target coordinates must be finite");
        }
        return executeInternal(effect, actor, action, new PointF(x, y), target, recursionDepth);
    }

    public static boolean executeOn(CustomActionEffect effect, CustomUnit actor,
                                    UnitAction action, Unit target, int recursionDepth) {
        return executeInternal(effect, actor, action, null,
                Objects.requireNonNull(target, "target"), recursionDepth);
    }

    private static boolean executeInternal(CustomActionEffect effect, CustomUnit actor,
                                           UnitAction action, PointF targetPoint,
                                           Unit target, int recursionDepth) {
        Objects.requireNonNull(effect, "effect");
        Objects.requireNonNull(actor, "actor");
        Objects.requireNonNull(action, "action");
        if (recursionDepth < 0) {
            throw new IllegalArgumentException("recursionDepth must be non-negative");
        }
        return effect.execute(actor, action, targetPoint, target, recursionDepth);
    }
}
