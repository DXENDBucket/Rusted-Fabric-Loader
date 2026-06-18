package io.github.endx.rustedfabricapi.mixin;

import io.github.endx.rustedfabricapi.api.event.RepairReclaimEvents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(
        targets = {
                "rustedwarfare.custom.CustomUnit",
                "rustedwarfare.unit.OrderableUnit"
        },
        remap = false
)
public abstract class QueuedActionResourceDeltaNamedMixin {
    @Inject(
            method = "getQueuedActionResourceDelta()Lrustedwarfare/custom/resource/ResourceAmount;",
            at = @At("RETURN"),
            cancellable = true,
            require = 1
    )
    private void rustedfabricapi$modifyQueuedActionResourceDelta(CallbackInfoReturnable<Object> cir) {
        cir.setReturnValue(RepairReclaimEvents.MODIFY_QUEUED_ACTION_RESOURCE_DELTA.invoker()
                .modifyQueuedActionResourceDelta(this, cir.getReturnValue()));
    }
}
