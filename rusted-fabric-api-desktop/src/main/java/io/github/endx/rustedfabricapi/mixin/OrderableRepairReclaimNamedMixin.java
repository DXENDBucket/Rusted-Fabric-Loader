package io.github.endx.rustedfabricapi.mixin;

import io.github.endx.rustedfabricapi.api.event.RepairReclaimEvents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "rustedwarfare.unit.OrderableUnit", remap = false)
public abstract class OrderableRepairReclaimNamedMixin {
    @Inject(
            method = "updateRepairReclaimOrder(FLrustedwarfare/unit/UnitOrder;Lrustedwarfare/unit/path/WaypointUpdateState;)V",
            at = @At("HEAD"),
            cancellable = true,
            require = 1
    )
    private void rustedfabricapi$beforeRepairReclaimOrderUpdate(float delta,
                                                                @Coerce Object waypoint,
                                                                @Coerce Object waypointState,
                                                                CallbackInfo ci) {
        if (RepairReclaimEvents.BEFORE_REPAIR_RECLAIM_ORDER_UPDATE.invoker()
                .beforeRepairReclaimOrderUpdate(this, delta, waypoint, waypointState)) {
            ci.cancel();
        }
    }

    @Inject(
            method = "updateRepairReclaimOrder(FLrustedwarfare/unit/UnitOrder;Lrustedwarfare/unit/path/WaypointUpdateState;)V",
            at = @At("RETURN"),
            require = 1
    )
    private void rustedfabricapi$afterRepairReclaimOrderUpdate(float delta,
                                                               @Coerce Object waypoint,
                                                               @Coerce Object waypointState,
                                                               CallbackInfo ci) {
        RepairReclaimEvents.AFTER_REPAIR_RECLAIM_ORDER_UPDATE.invoker()
                .afterRepairReclaimOrderUpdate(this, delta, waypoint, waypointState);
    }

    @Inject(method = "getBuildProgressSpeedForTarget(Lrustedwarfare/unit/Unit;)F", at = @At("RETURN"), cancellable = true, require = 1)
    private void rustedfabricapi$modifyBuildProgressSpeed(@Coerce Object target,
                                                          CallbackInfoReturnable<Float> cir) {
        Float current = cir.getReturnValue();
        cir.setReturnValue(RepairReclaimEvents.MODIFY_BUILD_PROGRESS_SPEED.invoker()
                .modifyBuildProgressSpeed(this, target, current != null ? current.floatValue() : 0.0F));
    }

    @Inject(method = "getBuildPriceForTarget(Lrustedwarfare/unit/Unit;)Lrustedwarfare/custom/resource/ResourceAmount;", at = @At("RETURN"), cancellable = true, require = 1)
    private void rustedfabricapi$modifyBuildPriceForTarget(@Coerce Object target,
                                                           CallbackInfoReturnable<Object> cir) {
        cir.setReturnValue(RepairReclaimEvents.MODIFY_BUILD_PRICE_FOR_TARGET.invoker()
                .modifyBuildPriceForTarget(this, target, cir.getReturnValue()));
    }

    @Inject(method = "getRepairReclaimResourceDelta()Lrustedwarfare/custom/resource/ResourceAmount;", at = @At("RETURN"), cancellable = true, require = 1)
    private void rustedfabricapi$modifyRepairReclaimResourceDelta(CallbackInfoReturnable<Object> cir) {
        cir.setReturnValue(RepairReclaimEvents.MODIFY_REPAIR_RECLAIM_RESOURCE_DELTA.invoker()
                .modifyRepairReclaimResourceDelta(this, cir.getReturnValue()));
    }

    @Inject(
            method = "findNearestReclaimResourceTarget(Lrustedwarfare/unit/OrderableUnit;FFFLrustedwarfare/custom/CustomTagList;)Lrustedwarfare/unit/Unit;",
            at = @At("RETURN"),
            cancellable = true,
            require = 1
    )
    private static void rustedfabricapi$modifyNearestReclaimResourceTarget(@Coerce Object searcher,
                                                                          float x, float y, float range,
                                                                          @Coerce Object requiredTags,
                                                                          CallbackInfoReturnable<Object> cir) {
        cir.setReturnValue(RepairReclaimEvents.MODIFY_NEAREST_RECLAIM_RESOURCE_TARGET.invoker()
                .modifyNearestReclaimResourceTarget(searcher, x, y, range, requiredTags, cir.getReturnValue()));
    }
}
