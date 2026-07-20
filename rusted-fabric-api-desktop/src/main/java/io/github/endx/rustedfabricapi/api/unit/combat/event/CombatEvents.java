package io.github.endx.rustedfabricapi.api.unit.combat.event;

import io.github.endx.rustedfabricapi.api.event.RustedFabricEvent;
import rustedwarfare.unit.OrderableUnit;
import rustedwarfare.unit.Unit;

/** Strongly typed targeting decisions and normal turret-fire lifecycle events. */
public final class CombatEvents {
    public static final RustedFabricEvent<BeforeTryFire> BEFORE_TRY_FIRE =
            RustedFabricEvent.create(listeners -> (attacker, delta, target, turretIndex) -> {
                boolean cancelled = false;
                for (BeforeTryFire listener : listeners) {
                    cancelled |= listener.beforeTryFire(attacker, delta, target, turretIndex);
                }
                return cancelled;
            });
    public static final RustedFabricEvent<AfterTryFire> AFTER_TRY_FIRE =
            RustedFabricEvent.create(listeners -> (attacker, delta, target, turretIndex, fired) -> {
                for (AfterTryFire listener : listeners) {
                    listener.afterTryFire(attacker, delta, target, turretIndex, fired);
                }
            });
    public static final RustedFabricEvent<ModifyTargetDecision> MODIFY_TARGET_IN_RANGE =
            targetDecisionEvent();
    public static final RustedFabricEvent<ModifyTargetDecisionWithFlag> MODIFY_CAN_AUTO_ATTACK =
            targetDecisionWithFlagEvent();
    public static final RustedFabricEvent<ModifyTargetDecisionWithFlag> MODIFY_CAN_AUTO_ATTACK_VISIBLE =
            targetDecisionWithFlagEvent();
    public static final RustedFabricEvent<ModifyTurretTargetDecision> MODIFY_CAN_TURRET_ATTACK =
            RustedFabricEvent.create(listeners ->
                    (attacker, turretIndex, target, ignoreRange, requireRange, current) -> {
                Boolean result = Boolean.valueOf(current);
                for (ModifyTurretTargetDecision listener : listeners) {
                    Boolean replacement = listener.modify(attacker, turretIndex, target,
                            ignoreRange, requireRange, result.booleanValue());
                    if (replacement != null) result = replacement;
                }
                return result;
            });

    private CombatEvents() {
    }

    private static RustedFabricEvent<ModifyTargetDecision> targetDecisionEvent() {
        return RustedFabricEvent.create(listeners -> (attacker, target, current) -> {
            Boolean result = Boolean.valueOf(current);
            for (ModifyTargetDecision listener : listeners) {
                Boolean replacement = listener.modify(attacker, target, result.booleanValue());
                if (replacement != null) result = replacement;
            }
            return result;
        });
    }

    private static RustedFabricEvent<ModifyTargetDecisionWithFlag> targetDecisionWithFlagEvent() {
        return RustedFabricEvent.create(listeners ->
                (attacker, target, checkSearchRange, current) -> {
            Boolean result = Boolean.valueOf(current);
            for (ModifyTargetDecisionWithFlag listener : listeners) {
                Boolean replacement = listener.modify(attacker, target, checkSearchRange,
                        result.booleanValue());
                if (replacement != null) result = replacement;
            }
            return result;
        });
    }

    @FunctionalInterface
    public interface BeforeTryFire {
        boolean beforeTryFire(OrderableUnit attacker, float delta, Unit target, int turretIndex);
    }

    @FunctionalInterface
    public interface AfterTryFire {
        void afterTryFire(OrderableUnit attacker, float delta, Unit target,
                          int turretIndex, boolean fired);
    }

    @FunctionalInterface
    public interface ModifyTargetDecision {
        /** Return {@code null} to retain {@code currentResult}. */
        Boolean modify(OrderableUnit attacker, Unit target, boolean currentResult);
    }

    @FunctionalInterface
    public interface ModifyTargetDecisionWithFlag {
        /** Return {@code null} to retain {@code currentResult}. */
        Boolean modify(OrderableUnit attacker, Unit target, boolean checkSearchRange,
                       boolean currentResult);
    }

    @FunctionalInterface
    public interface ModifyTurretTargetDecision {
        /** Return {@code null} to retain {@code currentResult}. */
        Boolean modify(OrderableUnit attacker, int turretIndex, Unit target,
                       boolean ignoreRange, boolean requireRange, boolean currentResult);
    }
}
