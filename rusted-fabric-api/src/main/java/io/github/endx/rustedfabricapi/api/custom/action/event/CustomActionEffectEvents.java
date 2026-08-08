package io.github.endx.rustedfabricapi.api.custom.action.event;

import io.github.endx.rustedfabricapi.api.event.RustedFabricEvent;
import rustedwarfare.custom.CustomUnit;
import rustedwarfare.custom.action.effect.CustomActionEffect;
import rustedwarfare.unit.Unit;
import rustedwarfare.unit.action.UnitAction;

/** Strongly typed interception around each custom action-effect execution. */
public final class CustomActionEffectEvents {
    public static final RustedFabricEvent<BeforeExecute> BEFORE_EXECUTE =
            RustedFabricEvent.create(listeners ->
                    (effect, actor, action, targetX, targetY, hasTargetPoint, target, recursionDepth) -> {
                boolean cancelled = false;
                for (BeforeExecute listener : listeners) {
                    cancelled |= listener.beforeExecute(effect, actor, action,
                            targetX, targetY, hasTargetPoint, target, recursionDepth);
                }
                return cancelled;
            });
    public static final RustedFabricEvent<AfterExecute> AFTER_EXECUTE =
            RustedFabricEvent.create(listeners ->
                    (effect, actor, action, targetX, targetY, hasTargetPoint,
                     target, recursionDepth, result) -> {
                for (AfterExecute listener : listeners) {
                    listener.afterExecute(effect, actor, action, targetX, targetY,
                            hasTargetPoint, target, recursionDepth, result);
                }
            });

    private CustomActionEffectEvents() {
    }

    @FunctionalInterface
    public interface BeforeExecute {
        boolean beforeExecute(CustomActionEffect effect, CustomUnit actor, UnitAction action,
                              float targetX, float targetY, boolean hasTargetPoint,
                              Unit target, int recursionDepth);
    }

    @FunctionalInterface
    public interface AfterExecute {
        void afterExecute(CustomActionEffect effect, CustomUnit actor, UnitAction action,
                          float targetX, float targetY, boolean hasTargetPoint,
                          Unit target, int recursionDepth, boolean result);
    }
}
