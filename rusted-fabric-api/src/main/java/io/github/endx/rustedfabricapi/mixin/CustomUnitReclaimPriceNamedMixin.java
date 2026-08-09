package io.github.endx.rustedfabricapi.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "rustedwarfare.custom.CustomUnit", remap = false)
public abstract class CustomUnitReclaimPriceNamedMixin {
    @Inject(method = "getReclaimPriceOverride()Lrustedwarfare/custom/resource/ResourceAmount;", at = @At("RETURN"), cancellable = true, require = 1)
    private void rustedfabricapi$modifyReclaimPriceOverride(CallbackInfoReturnable<Object> cir) {
        cir.setReturnValue(io.github.endx.rustedfabricapi.api.unit.repair.event.RepairReclaimEvents
                .MODIFY_RECLAIM_PRICE_OVERRIDE.invoker().modify(
                        (rustedwarfare.unit.Unit) (Object) this,
                        (rustedwarfare.custom.resource.ResourceAmount) cir.getReturnValue()));
    }

    @Inject(method = "getSimilarResourcesHaveTag()Lrustedwarfare/custom/CustomTagList;", at = @At("RETURN"), cancellable = true, require = 1)
    private void rustedfabricapi$modifySimilarResourcesTag(CallbackInfoReturnable<Object> cir) {
        cir.setReturnValue(io.github.endx.rustedfabricapi.api.unit.repair.event.RepairReclaimEvents
                .MODIFY_SIMILAR_RESOURCES_TAG.invoker().modify(
                        (rustedwarfare.unit.Unit) (Object) this,
                        (rustedwarfare.custom.CustomTagList) cir.getReturnValue()));
    }
}
