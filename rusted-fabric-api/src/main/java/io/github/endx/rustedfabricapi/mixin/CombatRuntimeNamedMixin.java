package io.github.endx.rustedfabricapi.mixin;

import io.github.endx.rustedfabricapi.api.unit.combat.event.CombatEvents;
import io.github.endx.rustedfabricapi.api.unit.combat.event.UnitTargetEvents;
import io.github.endx.rustedfabricapi.api.game.Units;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import rustedwarfare.unit.OrderableUnit;
import rustedwarfare.unit.Unit;

@Mixin(targets = "rustedwarfare.unit.OrderableUnit", remap = false)
public abstract class CombatRuntimeNamedMixin {
    @Inject(method = "tryFireTurretAtTarget(FLrustedwarfare/unit/Unit;I)Z",
            at = @At("HEAD"), cancellable = true, require = 1)
    private void rustedfabricapi$beforeTryFireTurretAtTarget(float delta, Unit target,
                                                             int turretIndex,
                                                             CallbackInfoReturnable<Boolean> cir) {
        if (CombatEvents.BEFORE_TRY_FIRE.invoker().beforeTryFire(
                (OrderableUnit) (Object) this, delta, target, turretIndex)) {
            cir.setReturnValue(Boolean.FALSE);
            return;
        }
        Boolean portable = UnitTargetEvents.MODIFY_VALIDITY.invoker().modify(
                Units.view(this), Units.view(target), UnitTargetEvents.Check.FIRE,
                turretIndex, true);
        if (Boolean.FALSE.equals(portable)) {
            cir.setReturnValue(Boolean.FALSE);
        }
    }

    @Inject(method = "tryFireTurretAtTarget(FLrustedwarfare/unit/Unit;I)Z",
            at = @At("RETURN"), require = 1)
    private void rustedfabricapi$afterTryFireTurretAtTarget(float delta, Unit target,
                                                            int turretIndex,
                                                            CallbackInfoReturnable<Boolean> cir) {
        CombatEvents.AFTER_TRY_FIRE.invoker().afterTryFire(
                (OrderableUnit) (Object) this, delta, target, turretIndex,
                Boolean.TRUE.equals(cir.getReturnValue()));
    }

    @Inject(method = "isTargetWithinAttackRange(Lrustedwarfare/unit/Unit;)Z",
            at = @At("RETURN"), cancellable = true, require = 1)
    private void rustedfabricapi$modifyTargetWithinAttackRange(Unit target,
                                                               CallbackInfoReturnable<Boolean> cir) {
        Boolean result = CombatEvents.MODIFY_TARGET_IN_RANGE.invoker().modify(
                (OrderableUnit) (Object) this, target,
                Boolean.TRUE.equals(cir.getReturnValue()));
        boolean current = result != null ? result.booleanValue()
                : Boolean.TRUE.equals(cir.getReturnValue());
        Boolean portable = UnitTargetEvents.MODIFY_VALIDITY.invoker().modify(
                Units.view(this), Units.view(target), UnitTargetEvents.Check.ATTACK_RANGE,
                -1, current);
        if (portable != null) cir.setReturnValue(portable);
    }

    @Inject(method = "canAutoAttackTarget(Lrustedwarfare/unit/Unit;Z)Z",
            at = @At("RETURN"), cancellable = true, require = 1)
    private void rustedfabricapi$modifyCanAutoAttackTarget(Unit target, boolean checkSearchRange,
                                                           CallbackInfoReturnable<Boolean> cir) {
        Boolean result = CombatEvents.MODIFY_CAN_AUTO_ATTACK.invoker().modify(
                (OrderableUnit) (Object) this, target, checkSearchRange,
                Boolean.TRUE.equals(cir.getReturnValue()));
        boolean current = result != null ? result.booleanValue()
                : Boolean.TRUE.equals(cir.getReturnValue());
        Boolean portable = UnitTargetEvents.MODIFY_VALIDITY.invoker().modify(
                Units.view(this), Units.view(target), UnitTargetEvents.Check.AUTO_ATTACK,
                -1, current);
        if (portable != null) cir.setReturnValue(portable);
    }

    @Inject(method = "canAutoAttackVisibleTarget(Lrustedwarfare/unit/Unit;Z)Z",
            at = @At("RETURN"), cancellable = true, require = 1)
    private void rustedfabricapi$modifyCanAutoAttackVisibleTarget(
            Unit target, boolean checkSearchRange, CallbackInfoReturnable<Boolean> cir) {
        Boolean result = CombatEvents.MODIFY_CAN_AUTO_ATTACK_VISIBLE.invoker().modify(
                (OrderableUnit) (Object) this, target, checkSearchRange,
                Boolean.TRUE.equals(cir.getReturnValue()));
        boolean current = result != null ? result.booleanValue()
                : Boolean.TRUE.equals(cir.getReturnValue());
        Boolean portable = UnitTargetEvents.MODIFY_VALIDITY.invoker().modify(
                Units.view(this), Units.view(target),
                UnitTargetEvents.Check.AUTO_ATTACK_VISIBLE, -1, current);
        if (portable != null) cir.setReturnValue(portable);
    }

    @Inject(method = "canTurretAttackTarget(ILrustedwarfare/unit/Unit;ZZ)Z",
            at = @At("RETURN"), cancellable = true, require = 1)
    private void rustedfabricapi$modifyCanTurretAttackTarget(
            int turretIndex, Unit target, boolean ignoreRange, boolean requireRange,
            CallbackInfoReturnable<Boolean> cir) {
        Boolean result = CombatEvents.MODIFY_CAN_TURRET_ATTACK.invoker().modify(
                (OrderableUnit) (Object) this, turretIndex, target, ignoreRange, requireRange,
                Boolean.TRUE.equals(cir.getReturnValue()));
        boolean current = result != null ? result.booleanValue()
                : Boolean.TRUE.equals(cir.getReturnValue());
        Boolean portable = UnitTargetEvents.MODIFY_VALIDITY.invoker().modify(
                Units.view(this), Units.view(target), UnitTargetEvents.Check.TURRET_ATTACK,
                turretIndex, current);
        if (portable != null) cir.setReturnValue(portable);
    }
}
