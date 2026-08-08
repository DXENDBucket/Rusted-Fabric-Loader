package io.github.endx.rustedfabricapi.mixin;

import io.github.endx.rustedfabricapi.api.unit.combat.event.CombatEvents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import rustedwarfare.unit.OrderableUnit;
import rustedwarfare.unit.Unit;

/** Covers the two mapped overrides that bypass OrderableUnit's base target decision. */
@Mixin(targets = {
        "rustedwarfare.custom.CustomUnit",
        "rustedwarfare.unit.land.ExperimentalTankUnit"
}, remap = false)
public abstract class CombatTurretOverrideNamedMixin {
    @Inject(method = "canTurretAttackTarget(ILrustedwarfare/unit/Unit;ZZ)Z",
            at = @At("RETURN"), cancellable = true, require = 1)
    private void rustedfabricapi$modifyCanTurretAttackTarget(
            int turretIndex, Unit target, boolean ignoreRange, boolean requireRange,
            CallbackInfoReturnable<Boolean> cir) {
        Boolean result = CombatEvents.MODIFY_CAN_TURRET_ATTACK.invoker().modify(
                (OrderableUnit) (Object) this, turretIndex, target, ignoreRange, requireRange,
                Boolean.TRUE.equals(cir.getReturnValue()));
        if (result != null) cir.setReturnValue(result);
    }
}
