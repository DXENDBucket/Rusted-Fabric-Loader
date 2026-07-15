package io.github.endx.rustedfabricapi.mixin;

import io.github.endx.rustedfabricapi.api.event.CustomUnitRuntimeEvents;
import io.github.endx.rustedfabricapi.api.event.ResourceRuntimeEvents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "rustedwarfare.custom.resource.ResourceAmount", remap = false)
public abstract class ResourceAmountRuntimeNamedMixin {
    @Inject(method = "hasEnoughResources(Lrustedwarfare/unit/Unit;)Z", at = @At("HEAD"), cancellable = true, require = 1)
    private void rustedfabricapi$beforeResourceCostCheck(@Coerce Object unit, CallbackInfoReturnable<Boolean> cir) {
        if (CustomUnitRuntimeEvents.BEFORE_RESOURCE_COST_PAID.invoker().beforeResourceCostPaid(this, unit, "hasEnoughResources")) {
            cir.setReturnValue(Boolean.FALSE);
        }
    }

    @Inject(method = "hasEnoughResources(Lrustedwarfare/unit/Unit;)Z", at = @At("RETURN"), cancellable = true, require = 1)
    private void rustedfabricapi$resourceAvailabilityCheck(@Coerce Object unit, CallbackInfoReturnable<Boolean> cir) {
        boolean result = Boolean.TRUE.equals(cir.getReturnValue());
        cir.setReturnValue(Boolean.valueOf(ResourceRuntimeEvents.RESOURCE_AVAILABILITY_CHECK.invoker()
                .resourceAvailabilityCheck(this, unit, 1.0D, false, "hasEnoughResources", result)));
    }

    @Inject(method = "hasEnoughResourcesScaled(Lrustedwarfare/unit/Unit;D)Z", at = @At("RETURN"), cancellable = true, require = 1)
    private void rustedfabricapi$resourceAvailabilityCheckScaled(@Coerce Object unit, double scale, CallbackInfoReturnable<Boolean> cir) {
        boolean result = Boolean.TRUE.equals(cir.getReturnValue());
        cir.setReturnValue(Boolean.valueOf(ResourceRuntimeEvents.RESOURCE_AVAILABILITY_CHECK.invoker()
                .resourceAvailabilityCheck(this, unit, scale, true, "hasEnoughResourcesScaled", result)));
    }

    @Inject(method = "subtractFromUnit(Lrustedwarfare/unit/Unit;)V", at = @At("HEAD"), cancellable = true, require = 1)
    private void rustedfabricapi$beforeResourceAmountSubtract(@Coerce Object unit, CallbackInfo ci) {
        if (ResourceRuntimeEvents.BEFORE_RESOURCE_AMOUNT_SUBTRACT.invoker()
                .beforeResourceAmountSubtract(this, unit, 1.0D, false, "subtractFromUnit")) {
            ci.cancel();
        }
    }

    @Inject(method = "subtractFromUnit(Lrustedwarfare/unit/Unit;)V", at = @At("RETURN"), require = 1)
    private void rustedfabricapi$afterResourceAmountSubtract(@Coerce Object unit, CallbackInfo ci) {
        ResourceRuntimeEvents.AFTER_RESOURCE_AMOUNT_SUBTRACT.invoker()
                .afterResourceAmountSubtract(this, unit, 1.0D, false, "subtractFromUnit");
    }

    @Inject(method = "subtractFromUnitScaled(Lrustedwarfare/unit/Unit;D)V", at = @At("HEAD"), cancellable = true, require = 1)
    private void rustedfabricapi$beforeResourceAmountSubtractScaled(@Coerce Object unit, double scale, CallbackInfo ci) {
        if (ResourceRuntimeEvents.BEFORE_RESOURCE_AMOUNT_SUBTRACT.invoker()
                .beforeResourceAmountSubtract(this, unit, scale, true, "subtractFromUnitScaled")) {
            ci.cancel();
        }
    }

    @Inject(method = "subtractFromUnitScaled(Lrustedwarfare/unit/Unit;D)V", at = @At("RETURN"), require = 1)
    private void rustedfabricapi$afterResourceAmountSubtractScaled(@Coerce Object unit, double scale, CallbackInfo ci) {
        ResourceRuntimeEvents.AFTER_RESOURCE_AMOUNT_SUBTRACT.invoker()
                .afterResourceAmountSubtract(this, unit, scale, true, "subtractFromUnitScaled");
    }

    @Inject(method = "addToUnit(Lrustedwarfare/unit/Unit;)V", at = @At("HEAD"), cancellable = true, require = 1)
    private void rustedfabricapi$beforeResourceCostPaid(@Coerce Object unit, CallbackInfo ci) {
        if (ResourceRuntimeEvents.BEFORE_RESOURCE_AMOUNT_ADD.invoker()
                .beforeResourceAmountAdd(this, unit, 1.0D, false, "addToUnit")) {
            ci.cancel();
            return;
        }
        if (CustomUnitRuntimeEvents.BEFORE_RESOURCE_COST_PAID.invoker().beforeResourceCostPaid(this, unit, "addToUnit")) {
            ci.cancel();
        }
    }

    @Inject(method = "addToUnit(Lrustedwarfare/unit/Unit;)V", at = @At("RETURN"), require = 1)
    private void rustedfabricapi$afterResourceAmountAdd(@Coerce Object unit, CallbackInfo ci) {
        ResourceRuntimeEvents.AFTER_RESOURCE_AMOUNT_ADD.invoker()
                .afterResourceAmountAdd(this, unit, 1.0D, false, "addToUnit");
    }

    @Inject(method = "addToUnitScaled(Lrustedwarfare/unit/Unit;DZ)V", at = @At("HEAD"), cancellable = true, require = 1)
    private void rustedfabricapi$beforeResourceAmountAddScaled(@Coerce Object unit, double scale, boolean includeCredits, CallbackInfo ci) {
        if (ResourceRuntimeEvents.BEFORE_RESOURCE_AMOUNT_ADD.invoker()
                .beforeResourceAmountAdd(this, unit, scale, true, "addToUnitScaled")) {
            ci.cancel();
        }
    }

    @Inject(method = "addToUnitScaled(Lrustedwarfare/unit/Unit;DZ)V", at = @At("RETURN"), require = 1)
    private void rustedfabricapi$afterResourceAmountAddScaled(@Coerce Object unit, double scale, boolean includeCredits, CallbackInfo ci) {
        ResourceRuntimeEvents.AFTER_RESOURCE_AMOUNT_ADD.invoker()
                .afterResourceAmountAdd(this, unit, scale, true, "addToUnitScaled");
    }

    @Inject(method = "addToUnitAndRecordIncome(Lrustedwarfare/unit/Unit;)V", at = @At("HEAD"), cancellable = true, require = 1)
    private void rustedfabricapi$beforeResourceAmountAddAndRecordIncome(@Coerce Object unit, CallbackInfo ci) {
        if (ResourceRuntimeEvents.BEFORE_RESOURCE_AMOUNT_ADD.invoker()
                .beforeResourceAmountAdd(this, unit, 1.0D, false, "addToUnitAndRecordIncome")) {
            ci.cancel();
        }
    }

    @Inject(method = "addToUnitAndRecordIncome(Lrustedwarfare/unit/Unit;)V", at = @At("RETURN"), require = 1)
    private void rustedfabricapi$afterResourceAmountAddAndRecordIncome(@Coerce Object unit, CallbackInfo ci) {
        ResourceRuntimeEvents.AFTER_RESOURCE_AMOUNT_ADD.invoker()
                .afterResourceAmountAdd(this, unit, 1.0D, false, "addToUnitAndRecordIncome");
    }

    @Inject(method = "canReserveResources(Lrustedwarfare/unit/Unit;Z)Z", at = @At("RETURN"), cancellable = true, require = 1)
    private void rustedfabricapi$resourceAvailabilityCheckReserve(@Coerce Object unit, boolean lagHiding, CallbackInfoReturnable<Boolean> cir) {
        boolean result = Boolean.TRUE.equals(cir.getReturnValue());
        cir.setReturnValue(Boolean.valueOf(ResourceRuntimeEvents.RESOURCE_AVAILABILITY_CHECK.invoker()
                .resourceAvailabilityCheck(this, unit, 1.0D, false, "canReserveResources", result)));
    }

    @Inject(method = "tryReserveResources(Lrustedwarfare/unit/Unit;Z)Z", at = @At("HEAD"), cancellable = true, require = 1)
    private void rustedfabricapi$beforeResourceReserve(@Coerce Object unit, boolean lagHiding, CallbackInfoReturnable<Boolean> cir) {
        if (ResourceRuntimeEvents.BEFORE_RESOURCE_RESERVE.invoker()
                .beforeResourceReserve(this, unit, lagHiding, "tryReserveResources")) {
            cir.setReturnValue(Boolean.FALSE);
        }
    }

    @Inject(method = "tryReserveResources(Lrustedwarfare/unit/Unit;Z)Z", at = @At("RETURN"), require = 1)
    private void rustedfabricapi$afterResourceReserve(@Coerce Object unit, boolean lagHiding, CallbackInfoReturnable<Boolean> cir) {
        ResourceRuntimeEvents.AFTER_RESOURCE_RESERVE.invoker()
                .afterResourceReserve(this, unit, lagHiding, "tryReserveResources", Boolean.TRUE.equals(cir.getReturnValue()));
    }
}
