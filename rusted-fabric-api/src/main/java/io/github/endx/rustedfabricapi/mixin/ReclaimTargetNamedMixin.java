package io.github.endx.rustedfabricapi.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(
        targets = {
                "rustedwarfare.custom.CustomUnit",
                "rustedwarfare.unit.OrderableUnit"
        },
        remap = false
)
public abstract class ReclaimTargetNamedMixin {
    @Inject(method = "canReclaimUnitTarget(Lrustedwarfare/unit/Unit;)Z", at = @At("RETURN"), cancellable = true, require = 1)
    private void rustedfabricapi$modifyCanReclaimUnitTarget(@Coerce Object target,
                                                            CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(io.github.endx.rustedfabricapi.api.unit.repair.event.RepairReclaimEvents
                .MODIFY_CAN_RECLAIM.invoker().modify(
                        (rustedwarfare.unit.OrderableUnit) (Object) this,
                        (rustedwarfare.unit.Unit) target,
                        Boolean.TRUE.equals(cir.getReturnValue())));
    }

    @Inject(method = "getUnbuildSpeedForTarget(Lrustedwarfare/unit/Unit;)F", at = @At("RETURN"), cancellable = true, require = 1)
    private void rustedfabricapi$modifyUnbuildSpeed(@Coerce Object target, CallbackInfoReturnable<Float> cir) {
        Float current = cir.getReturnValue();
        cir.setReturnValue(io.github.endx.rustedfabricapi.api.unit.repair.event.RepairReclaimEvents
                .MODIFY_UNBUILD_SPEED.invoker().modify(
                        (rustedwarfare.unit.OrderableUnit) (Object) this,
                        (rustedwarfare.unit.Unit) target,
                        current != null ? current.floatValue() : 0.0F));
    }
}
