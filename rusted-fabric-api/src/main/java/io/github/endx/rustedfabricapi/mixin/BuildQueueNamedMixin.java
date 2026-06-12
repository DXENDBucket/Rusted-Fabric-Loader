package io.github.endx.rustedfabricapi.mixin;

import io.github.endx.rustedfabricapi.api.event.BuildQueueEvents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "rustedwarfare.unit.build.BuildQueue", remap = false)
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
        if (BuildQueueEvents.BEFORE_QUEUE_ACTION_APPLY.invoker()
                .beforeQueueActionApply(this, action, front, targetPoint, targetUnit)) {
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
    }

    @Inject(method = "setCurrentQueueItem(Lrustedwarfare/unit/build/BuildQueueItem;)V", at = @At("HEAD"), require = 1)
    private void rustedfabricapi$beforeQueueItemActivate(@Coerce Object queueItem, CallbackInfo ci) {
        BuildQueueEvents.BEFORE_QUEUE_ITEM_ACTIVATE.invoker().beforeQueueItemActivate(this, queueItem);
    }

    @Inject(method = "setCurrentQueueItem(Lrustedwarfare/unit/build/BuildQueueItem;)V", at = @At("RETURN"), require = 1)
    private void rustedfabricapi$afterQueueItemActivate(@Coerce Object queueItem, CallbackInfo ci) {
        BuildQueueEvents.AFTER_QUEUE_ITEM_ACTIVATE.invoker().afterQueueItemActivate(this, queueItem);
    }

    @Inject(method = "clearQueueAndRefund(Z)V", at = @At("HEAD"), cancellable = true, require = 1)
    private void rustedfabricapi$beforeQueueClearAndRefund(boolean refund, CallbackInfo ci) {
        if (BuildQueueEvents.BEFORE_QUEUE_CLEAR_AND_REFUND.invoker().beforeQueueClearAndRefund(this, refund)) {
            ci.cancel();
        }
    }

    @Inject(method = "clearQueueAndRefund(Z)V", at = @At("RETURN"), require = 1)
    private void rustedfabricapi$afterQueueClearAndRefund(boolean refund, CallbackInfo ci) {
        BuildQueueEvents.AFTER_QUEUE_CLEAR_AND_REFUND.invoker().afterQueueClearAndRefund(this, refund);
    }

    @Inject(method = "refundQueueItem(Lrustedwarfare/unit/build/BuildQueueItem;)V", at = @At("HEAD"), cancellable = true, require = 1)
    private void rustedfabricapi$beforeQueueItemRefund(@Coerce Object queueItem, CallbackInfo ci) {
        if (BuildQueueEvents.BEFORE_QUEUE_ITEM_REFUND.invoker().beforeQueueItemRefund(this, queueItem)) {
            ci.cancel();
        }
    }

    @Inject(method = "refundQueueItem(Lrustedwarfare/unit/build/BuildQueueItem;)V", at = @At("RETURN"), require = 1)
    private void rustedfabricapi$afterQueueItemRefund(@Coerce Object queueItem, CallbackInfo ci) {
        BuildQueueEvents.AFTER_QUEUE_ITEM_REFUND.invoker().afterQueueItemRefund(this, queueItem);
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
    }

    @Inject(method = "positionNewlyProducedUnit(Lrustedwarfare/unit/Unit;FZ)V", at = @At("RETURN"), require = 1)
    private void rustedfabricapi$afterNewlyProducedUnitPositioned(@Coerce Object unit, float spacing,
                                                                  boolean useRallyPoint, CallbackInfo ci) {
        BuildQueueEvents.AFTER_NEWLY_PRODUCED_UNIT_POSITIONED.invoker()
                .afterNewlyProducedUnitPositioned(this, unit, spacing, useRallyPoint);
    }
}
