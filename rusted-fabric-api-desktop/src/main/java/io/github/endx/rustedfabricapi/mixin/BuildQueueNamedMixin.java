package io.github.endx.rustedfabricapi.mixin;

import io.github.endx.rustedfabricapi.api.event.BuildQueueEvents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "rustedwarfare.unit.build.FactoryQueueManager", remap = false)
public abstract class BuildQueueNamedMixin {
    @Inject(
            method = "applyQueueActionWithTarget(Lrustedwarfare/unit/action/UnitAction;ZLandroid/graphics/PointF;Lrustedwarfare/unit/Unit;)Lrustedwarfare/unit/build/BuildQueueItem;",
            at = @At("HEAD"),
            cancellable = true,
            require = 1
    )
    private void rustedfabricapi$beforeQueueActionApply(@Coerce Object action, boolean front,
                                                        @Coerce Object targetPoint,
                                                        @Coerce Object targetUnit,
                                                        CallbackInfoReturnable<Object> cir) {
        boolean cancelled = BuildQueueEvents.BEFORE_QUEUE_ACTION_APPLY.invoker()
                .beforeQueueActionApply(this, action, front, targetPoint, targetUnit);
        cancelled |= io.github.endx.rustedfabricapi.api.unit.build.event.BuildQueueEvents.BEFORE_ACTION_APPLY
                .invoker().beforeActionApply(
                        (rustedwarfare.unit.build.FactoryQueueManager) (Object) this,
                        (rustedwarfare.unit.action.UnitAction) action, front,
                        targetPoint != null
                                ? io.github.endx.rustedfabricapi.api.util.RustedReflection.getFloatField(
                                        targetPoint, new String[]{"x", "a"}) : Float.NaN,
                        targetPoint != null
                                ? io.github.endx.rustedfabricapi.api.util.RustedReflection.getFloatField(
                                        targetPoint, new String[]{"y", "b"}) : Float.NaN,
                        targetPoint != null, (rustedwarfare.unit.Unit) targetUnit);
        if (cancelled) {
            cir.setReturnValue(null);
        }
    }

    @Inject(
            method = "applyQueueActionWithTarget(Lrustedwarfare/unit/action/UnitAction;ZLandroid/graphics/PointF;Lrustedwarfare/unit/Unit;)Lrustedwarfare/unit/build/BuildQueueItem;",
            at = @At("RETURN"),
            require = 1
    )
    private void rustedfabricapi$afterQueueActionApply(@Coerce Object action, boolean front,
                                                       @Coerce Object targetPoint,
                                                       @Coerce Object targetUnit,
                                                       CallbackInfoReturnable<Object> cir) {
        BuildQueueEvents.AFTER_QUEUE_ACTION_APPLY.invoker()
                .afterQueueActionApply(this, action, front, targetPoint, targetUnit, cir.getReturnValue());
        io.github.endx.rustedfabricapi.api.unit.build.event.BuildQueueEvents.AFTER_ACTION_APPLY
                .invoker().afterActionApply(
                        (rustedwarfare.unit.build.FactoryQueueManager) (Object) this,
                        (rustedwarfare.unit.action.UnitAction) action, front,
                        targetPoint != null
                                ? io.github.endx.rustedfabricapi.api.util.RustedReflection.getFloatField(
                                        targetPoint, new String[]{"x", "a"}) : Float.NaN,
                        targetPoint != null
                                ? io.github.endx.rustedfabricapi.api.util.RustedReflection.getFloatField(
                                        targetPoint, new String[]{"y", "b"}) : Float.NaN,
                        targetPoint != null, (rustedwarfare.unit.Unit) targetUnit,
                        (rustedwarfare.unit.build.BuildQueueItem) cir.getReturnValue());
    }

    @Inject(method = "setCurrentQueueItem(Lrustedwarfare/unit/build/BuildQueueItem;)V", at = @At("HEAD"), require = 1)
    private void rustedfabricapi$beforeQueueItemActivate(@Coerce Object queueItem, CallbackInfo ci) {
        BuildQueueEvents.BEFORE_QUEUE_ITEM_ACTIVATE.invoker().beforeQueueItemActivate(this, queueItem);
        io.github.endx.rustedfabricapi.api.unit.build.event.BuildQueueEvents.BEFORE_ITEM_ACTIVATE
                .invoker().onQueueItem((rustedwarfare.unit.build.FactoryQueueManager) (Object) this,
                        (rustedwarfare.unit.build.BuildQueueItem) queueItem);
    }

    @Inject(method = "setCurrentQueueItem(Lrustedwarfare/unit/build/BuildQueueItem;)V", at = @At("RETURN"), require = 1)
    private void rustedfabricapi$afterQueueItemActivate(@Coerce Object queueItem, CallbackInfo ci) {
        BuildQueueEvents.AFTER_QUEUE_ITEM_ACTIVATE.invoker().afterQueueItemActivate(this, queueItem);
        io.github.endx.rustedfabricapi.api.unit.build.event.BuildQueueEvents.AFTER_ITEM_ACTIVATE
                .invoker().onQueueItem((rustedwarfare.unit.build.FactoryQueueManager) (Object) this,
                        (rustedwarfare.unit.build.BuildQueueItem) queueItem);
    }

    @Inject(method = "clearQueueAndRefund(Z)V", at = @At("HEAD"), cancellable = true, require = 1)
    private void rustedfabricapi$beforeQueueClearAndRefund(boolean refund, CallbackInfo ci) {
        boolean cancelled = BuildQueueEvents.BEFORE_QUEUE_CLEAR_AND_REFUND.invoker()
                .beforeQueueClearAndRefund(this, refund);
        cancelled |= io.github.endx.rustedfabricapi.api.unit.build.event.BuildQueueEvents.BEFORE_CLEAR
                .invoker().beforeClear((rustedwarfare.unit.build.FactoryQueueManager) (Object) this,
                        refund);
        if (cancelled) {
            ci.cancel();
        }
    }

    @Inject(method = "clearQueueAndRefund(Z)V", at = @At("RETURN"), require = 1)
    private void rustedfabricapi$afterQueueClearAndRefund(boolean refund, CallbackInfo ci) {
        BuildQueueEvents.AFTER_QUEUE_CLEAR_AND_REFUND.invoker().afterQueueClearAndRefund(this, refund);
        io.github.endx.rustedfabricapi.api.unit.build.event.BuildQueueEvents.AFTER_CLEAR
                .invoker().afterClear((rustedwarfare.unit.build.FactoryQueueManager) (Object) this,
                        refund);
    }

    @Inject(method = "refundQueueItem(Lrustedwarfare/unit/build/BuildQueueItem;)V", at = @At("HEAD"), cancellable = true, require = 1)
    private void rustedfabricapi$beforeQueueItemRefund(@Coerce Object queueItem, CallbackInfo ci) {
        boolean cancelled = BuildQueueEvents.BEFORE_QUEUE_ITEM_REFUND.invoker()
                .beforeQueueItemRefund(this, queueItem);
        cancelled |= io.github.endx.rustedfabricapi.api.unit.build.event.BuildQueueEvents.BEFORE_ITEM_REFUND
                .invoker().beforeQueueItem(
                        (rustedwarfare.unit.build.FactoryQueueManager) (Object) this,
                        (rustedwarfare.unit.build.BuildQueueItem) queueItem);
        if (cancelled) {
            ci.cancel();
        }
    }

    @Inject(method = "refundQueueItem(Lrustedwarfare/unit/build/BuildQueueItem;)V", at = @At("RETURN"), require = 1)
    private void rustedfabricapi$afterQueueItemRefund(@Coerce Object queueItem, CallbackInfo ci) {
        BuildQueueEvents.AFTER_QUEUE_ITEM_REFUND.invoker().afterQueueItemRefund(this, queueItem);
        io.github.endx.rustedfabricapi.api.unit.build.event.BuildQueueEvents.AFTER_ITEM_REFUND
                .invoker().onQueueItem((rustedwarfare.unit.build.FactoryQueueManager) (Object) this,
                        (rustedwarfare.unit.build.BuildQueueItem) queueItem);
    }

    @Inject(
            method = "completeQueueItem(Lrustedwarfare/unit/build/BuildQueueItem;FZF)Lrustedwarfare/unit/Unit;",
            at = @At("RETURN"),
            require = 1
    )
    private void rustedfabricapi$afterQueueItemComplete(@Coerce Object queueItem, float spacing,
                                                        boolean useRallyPoint, float spawnYOffset,
                                                        CallbackInfoReturnable<Object> cir) {
        BuildQueueEvents.AFTER_QUEUE_ITEM_COMPLETE.invoker()
                .afterQueueItemComplete(this, queueItem, spacing, useRallyPoint, spawnYOffset, cir.getReturnValue());
        io.github.endx.rustedfabricapi.api.unit.build.event.BuildQueueEvents.AFTER_ITEM_COMPLETE
                .invoker().afterComplete(
                        (rustedwarfare.unit.build.FactoryQueueManager) (Object) this,
                        (rustedwarfare.unit.build.BuildQueueItem) queueItem, spacing,
                        useRallyPoint, spawnYOffset, (rustedwarfare.unit.Unit) cir.getReturnValue());
    }

    @Inject(method = "positionNewlyProducedUnit(Lrustedwarfare/unit/Unit;FZ)V", at = @At("RETURN"), require = 1)
    private void rustedfabricapi$afterNewlyProducedUnitPositioned(@Coerce Object unit, float spacing,
                                                                  boolean useRallyPoint, CallbackInfo ci) {
        BuildQueueEvents.AFTER_NEWLY_PRODUCED_UNIT_POSITIONED.invoker()
                .afterNewlyProducedUnitPositioned(this, unit, spacing, useRallyPoint);
        io.github.endx.rustedfabricapi.api.unit.build.event.BuildQueueEvents.AFTER_PRODUCED_UNIT_POSITIONED
                .invoker().afterPositioned(
                        (rustedwarfare.unit.build.FactoryQueueManager) (Object) this,
                        (rustedwarfare.unit.Unit) unit, spacing, useRallyPoint);
    }
}
