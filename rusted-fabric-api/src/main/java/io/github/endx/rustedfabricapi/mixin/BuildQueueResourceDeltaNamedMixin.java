package io.github.endx.rustedfabricapi.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(
        targets = {
                "rustedwarfare.custom.CustomUnit",
                "rustedwarfare.unit.OrderableUnit",
                "rustedwarfare.unit.building.ProductionBuildingUnitBase"
        },
        remap = false
)
public abstract class BuildQueueResourceDeltaNamedMixin {
    @Inject(method = "getBuildQueueResourceDelta()Lrustedwarfare/custom/resource/ResourceAmount;", at = @At("RETURN"), cancellable = true, require = 1)
    private void rustedfabricapi$modifyBuildQueueResourceDelta(CallbackInfoReturnable<Object> cir) {
        cir.setReturnValue(io.github.endx.rustedfabricapi.api.unit.repair.event.RepairReclaimEvents
                .MODIFY_BUILD_QUEUE_RESOURCE_DELTA.invoker().modify(
                        (rustedwarfare.unit.Unit) (Object) this,
                        (rustedwarfare.custom.resource.ResourceAmount) cir.getReturnValue()));
    }
}
