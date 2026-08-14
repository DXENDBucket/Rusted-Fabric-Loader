package io.github.endx.rustedfabricapi.verification;

import io.github.endx.rustedfabricapi.api.event.RustedFabricEvent;
import io.github.endx.rustedfabricapi.api.unit.combat.event.UnitTargetEvents;

final class UnitTargetEventContractVerification {
    private UnitTargetEventContractVerification() { }

    static void verify() {
        RustedFabricEvent.Registration first = UnitTargetEvents.MODIFY_VALIDITY.subscribe(
                (attacker, target, check, turretIndex, current) -> {
                    require(check == UnitTargetEvents.Check.FIRE,
                            "target check changed between listeners");
                    require(turretIndex == 2, "turret index changed between listeners");
                    require(current, "first targeting listener received the wrong native result");
                    return Boolean.FALSE;
                });
        RustedFabricEvent.Registration second = UnitTargetEvents.MODIFY_VALIDITY.subscribe(
                (attacker, target, check, turretIndex, current) -> {
                    require(!current, "targeting listeners were not reduced in order");
                    return null;
                });
        try {
            Boolean result = UnitTargetEvents.MODIFY_VALIDITY.invoker()
                    .modify(null, null, UnitTargetEvents.Check.FIRE, 2, true);
            require(Boolean.FALSE.equals(result),
                    "null targeting result must retain the current listener result");
        } finally {
            first.close();
            second.close();
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
