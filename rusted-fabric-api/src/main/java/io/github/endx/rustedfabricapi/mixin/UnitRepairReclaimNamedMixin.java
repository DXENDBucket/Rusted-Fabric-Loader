package io.github.endx.rustedfabricapi.mixin;

import io.github.endx.rustedfabricapi.api.event.RepairReclaimEvents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "rustedwarfare.unit.Unit", remap = false)
public abstract class UnitRepairReclaimNamedMixin {
    @Inject(method = "setConstructionProgress(F)V", at = @At("HEAD"), cancellable = true, require = 1)
    private void rustedfabricapi$beforeConstructionProgressSet(float progress, CallbackInfo ci) {
        if (RepairReclaimEvents.BEFORE_CONSTRUCTION_PROGRESS_SET.invoker()
                .beforeConstructionProgressSet(this, progress)) {
            ci.cancel();
        }
    }

    @Inject(method = "setConstructionProgress(F)V", at = @At("RETURN"), require = 1)
    private void rustedfabricapi$afterConstructionProgressSet(float progress, CallbackInfo ci) {
        RepairReclaimEvents.AFTER_CONSTRUCTION_PROGRESS_SET.invoker()
                .afterConstructionProgressSet(this, progress);
    }
}
