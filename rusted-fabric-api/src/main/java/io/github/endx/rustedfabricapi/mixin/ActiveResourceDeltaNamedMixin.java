package io.github.endx.rustedfabricapi.mixin;

import io.github.endx.rustedfabricapi.api.event.RepairReclaimEvents;
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
        RepairReclaimEvents.BEFORE_ACTIVE_RESOURCE_DELTA_REFRESH.invoker()
                .beforeActiveResourceDeltaRefresh(this);
    }

    @Inject(method = "refreshActiveResourceDelta()V", at = @At("RETURN"), require = 1)
    private void rustedfabricapi$afterActiveResourceDeltaRefresh(CallbackInfo ci) {
        RepairReclaimEvents.AFTER_ACTIVE_RESOURCE_DELTA_REFRESH.invoker()
                .afterActiveResourceDeltaRefresh(this);
    }
}
