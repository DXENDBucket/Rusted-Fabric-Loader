package io.github.endx.rustedfabricapi.api.unit.combat.event;

import io.github.endx.rustedfabricapi.api.event.RustedFabricEvent;
import io.github.endx.rustedfabricapi.api.game.UnitView;

/** Namespace-neutral targeting decisions usable by portable Java mods. */
public final class UnitTargetEvents {
    public static final RustedFabricEvent<ModifyValidity> MODIFY_VALIDITY =
            RustedFabricEvent.create(listeners ->
                    (attacker, target, check, turretIndex, current) -> {
                        Boolean result = Boolean.valueOf(current);
                        for (ModifyValidity listener : listeners) {
                            Boolean replacement = listener.modify(attacker, target, check,
                                    turretIndex, result.booleanValue());
                            if (replacement != null) result = replacement;
                        }
                        return result;
                    });

    private UnitTargetEvents() { }

    public enum Check {
        ATTACK_RANGE,
        AUTO_ATTACK,
        AUTO_ATTACK_VISIBLE,
        TURRET_ATTACK,
        FIRE
    }

    @FunctionalInterface
    public interface ModifyValidity {
        /** Return {@code null} to retain {@code current}. Turret index is {@code -1} when absent. */
        Boolean modify(UnitView attacker, UnitView target, Check check,
                       int turretIndex, boolean current);
    }
}
