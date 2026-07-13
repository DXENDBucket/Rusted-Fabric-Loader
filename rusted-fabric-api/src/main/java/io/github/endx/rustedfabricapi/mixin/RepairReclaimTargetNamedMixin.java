package io.github.endx.rustedfabricapi.mixin;

import io.github.endx.rustedfabricapi.api.event.RepairReclaimEvents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(
        targets = {
                "rustedwarfare.custom.CustomUnit",
                "rustedwarfare.unit.OrderableUnit",
                "rustedwarfare.unit.building.RepairBayUnit",
                "rustedwarfare.unit.land.BuilderUnit",
                "rustedwarfare.unit.land.internal.Obf_g",
                "rustedwarfare.unit.special.EditorOrBuilderUnit",
                "rustedwarfare.unit.water.BuilderShipUnit"
        },
        remap = false
)
public abstract class RepairReclaimTargetNamedMixin {
    @Inject(method = "canRepairTarget(Lrustedwarfare/unit/Unit;)Z", at = @At("RETURN"), cancellable = true, require = 1)
    private void rustedfabricapi$modifyCanRepairTarget(@Coerce Object target,
                                                       CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(RepairReclaimEvents.MODIFY_CAN_REPAIR_TARGET.invoker()
                .modifyCanRepairTarget(this, target, Boolean.TRUE.equals(cir.getReturnValue())));
    }
}
