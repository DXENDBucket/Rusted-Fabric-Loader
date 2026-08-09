package io.github.endx.rustedfabricapi.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "rustedwarfare.unit.Unit", remap = false)
public abstract class UnitRepairReclaimNamedMixin {
    @Inject(method = "setConstructionProgress(F)V", at = @At("HEAD"), cancellable = true, require = 1)
    private void rustedfabricapi$beforeConstructionProgressSet(float progress, CallbackInfo ci) {
        boolean cancelled = io.github.endx.rustedfabricapi.api.unit.repair.event.RepairReclaimEvents
                .BEFORE_CONSTRUCTION_PROGRESS_SET.invoker()
                .beforeSet((rustedwarfare.unit.Unit) (Object) this, progress);
        if (cancelled) {
            ci.cancel();
        }
    }

    @Inject(method = "setConstructionProgress(F)V", at = @At("RETURN"), require = 1)
    private void rustedfabricapi$afterConstructionProgressSet(float progress, CallbackInfo ci) {
        io.github.endx.rustedfabricapi.api.unit.repair.event.RepairReclaimEvents
                .AFTER_CONSTRUCTION_PROGRESS_SET.invoker()
                .afterSet((rustedwarfare.unit.Unit) (Object) this, progress);
    }
}
