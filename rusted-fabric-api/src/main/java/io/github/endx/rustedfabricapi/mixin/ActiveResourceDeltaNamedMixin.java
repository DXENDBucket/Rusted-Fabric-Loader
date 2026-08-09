package io.github.endx.rustedfabricapi.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(
        targets = {
                "rustedwarfare.unit.Unit",
                "rustedwarfare.unit.OrderableUnit"
        },
        remap = false
)
public abstract class ActiveResourceDeltaNamedMixin {
    @Inject(method = "refreshActiveResourceDelta()V", at = @At("HEAD"), require = 1)
    private void rustedfabricapi$beforeActiveResourceDeltaRefresh(CallbackInfo ci) {
        io.github.endx.rustedfabricapi.api.unit.repair.event.RepairReclaimEvents
                .BEFORE_ACTIVE_RESOURCE_DELTA_REFRESH.invoker()
                .onUnit((rustedwarfare.unit.Unit) (Object) this);
    }

    @Inject(method = "refreshActiveResourceDelta()V", at = @At("RETURN"), require = 1)
    private void rustedfabricapi$afterActiveResourceDeltaRefresh(CallbackInfo ci) {
        io.github.endx.rustedfabricapi.api.unit.repair.event.RepairReclaimEvents
                .AFTER_ACTIVE_RESOURCE_DELTA_REFRESH.invoker()
                .onUnit((rustedwarfare.unit.Unit) (Object) this);
    }
}
