package io.github.endx.rustedfabricapi.mixin;

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
        boolean cancelled = io.github.endx.rustedfabricapi.api.unit.repair.event.RepairReclaimEvents
                .BEFORE_ORDER_UPDATE.invoker().beforeUpdate(
                        (rustedwarfare.unit.OrderableUnit) (Object) this, delta,
                        (rustedwarfare.unit.UnitOrder) waypoint);
        if (cancelled) {
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
        io.github.endx.rustedfabricapi.api.unit.repair.event.RepairReclaimEvents
                .AFTER_ORDER_UPDATE.invoker().afterUpdate(
                        (rustedwarfare.unit.OrderableUnit) (Object) this, delta,
                        (rustedwarfare.unit.UnitOrder) waypoint);
    }

    @Inject(method = "getBuildProgressSpeedForTarget(Lrustedwarfare/unit/Unit;)F", at = @At("RETURN"), cancellable = true, require = 1)
    private void rustedfabricapi$modifyBuildProgressSpeed(@Coerce Object target,
                                                          CallbackInfoReturnable<Float> cir) {
        Float current = cir.getReturnValue();
        cir.setReturnValue(io.github.endx.rustedfabricapi.api.unit.repair.event.RepairReclaimEvents
                .MODIFY_BUILD_PROGRESS_SPEED.invoker().modify(
                        (rustedwarfare.unit.OrderableUnit) (Object) this,
                        (rustedwarfare.unit.Unit) target,
                        current != null ? current.floatValue() : 0.0F));
    }

    @Inject(method = "getBuildPriceForTarget(Lrustedwarfare/unit/Unit;)Lrustedwarfare/custom/resource/ResourceAmount;", at = @At("RETURN"), cancellable = true, require = 1)
    private void rustedfabricapi$modifyBuildPriceForTarget(@Coerce Object target,
                                                           CallbackInfoReturnable<Object> cir) {
        cir.setReturnValue(io.github.endx.rustedfabricapi.api.unit.repair.event.RepairReclaimEvents
                .MODIFY_BUILD_PRICE.invoker().modify(
                        (rustedwarfare.unit.OrderableUnit) (Object) this,
                        (rustedwarfare.unit.Unit) target,
                        (rustedwarfare.custom.resource.ResourceAmount) cir.getReturnValue()));
    }

    @Inject(method = "getRepairReclaimResourceDelta()Lrustedwarfare/custom/resource/ResourceAmount;", at = @At("RETURN"), cancellable = true, require = 1)
    private void rustedfabricapi$modifyRepairReclaimResourceDelta(CallbackInfoReturnable<Object> cir) {
        cir.setReturnValue(io.github.endx.rustedfabricapi.api.unit.repair.event.RepairReclaimEvents
                .MODIFY_REPAIR_RECLAIM_RESOURCE_DELTA.invoker().modify(
                        (rustedwarfare.unit.Unit) (Object) this,
                        (rustedwarfare.custom.resource.ResourceAmount) cir.getReturnValue()));
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
        cir.setReturnValue(io.github.endx.rustedfabricapi.api.unit.repair.event.RepairReclaimEvents
                .MODIFY_NEAREST_RESOURCE_TARGET.invoker().modify(
                        (rustedwarfare.unit.OrderableUnit) searcher, x, y, range,
                        (rustedwarfare.custom.CustomTagList) requiredTags,
                        (rustedwarfare.unit.Unit) cir.getReturnValue()));
    }
}
