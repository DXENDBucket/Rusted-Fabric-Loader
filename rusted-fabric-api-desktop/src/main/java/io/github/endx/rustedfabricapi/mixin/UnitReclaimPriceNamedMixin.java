package io.github.endx.rustedfabricapi.mixin;

import io.github.endx.rustedfabricapi.api.event.RepairReclaimEvents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "rustedwarfare.unit.Unit", remap = false)
public abstract class UnitReclaimPriceNamedMixin {
    @Inject(method = "getBaseReclaimPrice()Lrustedwarfare/custom/resource/ResourceAmount;", at = @At("RETURN"), cancellable = true, require = 1)
    private void rustedfabricapi$modifyBaseReclaimPrice(CallbackInfoReturnable<Object> cir) {
        cir.setReturnValue(RepairReclaimEvents.MODIFY_BASE_RECLAIM_PRICE.invoker()
                .modifyBaseReclaimPrice(this, cir.getReturnValue()));
    }

    @Inject(method = "getReclaimPriceOverride()Lrustedwarfare/custom/resource/ResourceAmount;", at = @At("RETURN"), cancellable = true, require = 1)
    private void rustedfabricapi$modifyReclaimPriceOverride(CallbackInfoReturnable<Object> cir) {
        cir.setReturnValue(RepairReclaimEvents.MODIFY_RECLAIM_PRICE_OVERRIDE.invoker()
                .modifyReclaimPriceOverride(this, cir.getReturnValue()));
    }

    @Inject(method = "getSimilarResourcesHaveTag()Lrustedwarfare/custom/CustomTagList;", at = @At("RETURN"), cancellable = true, require = 1)
    private void rustedfabricapi$modifySimilarResourcesTag(CallbackInfoReturnable<Object> cir) {
        cir.setReturnValue(RepairReclaimEvents.MODIFY_SIMILAR_RESOURCES_TAG.invoker()
                .modifySimilarResourcesTag(this, cir.getReturnValue()));
    }
}
