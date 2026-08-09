package io.github.endx.rustedfabricapi.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(
        targets = {
                "rustedwarfare.custom.CustomUnit",
                "rustedwarfare.unit.building.ProductionBuildingUnitBase"
        },
        remap = false
)
public abstract class BuildQueueHostNamedMixin {
    @Inject(method = "completeBuildQueueItem(Lrustedwarfare/unit/build/BuildQueueItem;)V", at = @At("HEAD"), cancellable = true, require = 1)
    private void rustedfabricapi$beforeHostBuildQueueItemComplete(@Coerce Object queueItem, CallbackInfo ci) {
        boolean cancelled = io.github.endx.rustedfabricapi.api.unit.build.event.BuildQueueEvents.BEFORE_HOST_ITEM_COMPLETE
                .invoker().beforeHostItem((rustedwarfare.unit.build.BuildQueueHost) (Object) this,
                        (rustedwarfare.unit.build.BuildQueueItem) queueItem);
        if (cancelled) {
            ci.cancel();
        }
    }

    @Inject(method = "completeBuildQueueItem(Lrustedwarfare/unit/build/BuildQueueItem;)V", at = @At("RETURN"), require = 1)
    private void rustedfabricapi$afterHostBuildQueueItemComplete(@Coerce Object queueItem, CallbackInfo ci) {
        io.github.endx.rustedfabricapi.api.unit.build.event.BuildQueueEvents.AFTER_HOST_ITEM_COMPLETE
                .invoker().onHostItem((rustedwarfare.unit.build.BuildQueueHost) (Object) this,
                        (rustedwarfare.unit.build.BuildQueueItem) queueItem);
    }

    @Inject(method = "triggerWhenBuildingAction(Lrustedwarfare/unit/build/BuildQueueItem;)V", at = @At("RETURN"), require = 1)
    private void rustedfabricapi$afterHostBuildQueueItemActivate(@Coerce Object queueItem, CallbackInfo ci) {
        io.github.endx.rustedfabricapi.api.unit.build.event.BuildQueueEvents.AFTER_HOST_ITEM_ACTIVATE
                .invoker().onHostItem((rustedwarfare.unit.build.BuildQueueHost) (Object) this,
                        (rustedwarfare.unit.build.BuildQueueItem) queueItem);
    }

    @Inject(method = "canRefundBuildQueueItem(Lrustedwarfare/unit/build/BuildQueueItem;)Z", at = @At("RETURN"), cancellable = true, require = 1)
    private void rustedfabricapi$modifyHostBuildQueueItemRefundable(@Coerce Object queueItem,
                                                                    CallbackInfoReturnable<Boolean> cir) {
        Boolean result = io.github.endx.rustedfabricapi.api.unit.build.event.BuildQueueEvents.MODIFY_HOST_ITEM_REFUNDABLE
                .invoker().modify((rustedwarfare.unit.build.BuildQueueHost) (Object) this,
                        (rustedwarfare.unit.build.BuildQueueItem) queueItem,
                        Boolean.TRUE.equals(cir.getReturnValue()));
        cir.setReturnValue(result);
    }
}
