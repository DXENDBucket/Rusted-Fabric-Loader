package io.github.endx.rustedfabricapi.mixin;

import io.github.endx.rustedfabricapi.api.event.ResourceRuntimeEvents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "rustedwarfare.custom.action.effect.ResourceConversionActionEffect", remap = false)
public abstract class ResourceConversionNamedMixin {
    @Inject(method = "execute(Lrustedwarfare/custom/CustomUnit;Lrustedwarfare/unit/action/UnitAction;Landroid/graphics/PointF;Lrustedwarfare/unit/Unit;I)Z", at = @At("HEAD"), cancellable = true, require = 1)
    private void rustedfabricapi$beforeResourceConversion(@Coerce Object unit, @Coerce Object action, @Coerce Object targetPoint, @Coerce Object targetUnit, int recursionDepth, CallbackInfoReturnable<Boolean> cir) {
        if (ResourceRuntimeEvents.BEFORE_RESOURCE_CONVERSION.invoker().beforeResourceConversion(this, unit, action, targetPoint, targetUnit, recursionDepth)) {
            cir.setReturnValue(Boolean.FALSE);
        }
    }

    @Inject(method = "execute(Lrustedwarfare/custom/CustomUnit;Lrustedwarfare/unit/action/UnitAction;Landroid/graphics/PointF;Lrustedwarfare/unit/Unit;I)Z", at = @At("RETURN"), require = 1)
    private void rustedfabricapi$afterResourceConversion(@Coerce Object unit, @Coerce Object action, @Coerce Object targetPoint, @Coerce Object targetUnit, int recursionDepth, CallbackInfoReturnable<Boolean> cir) {
        ResourceRuntimeEvents.AFTER_RESOURCE_CONVERSION.invoker().afterResourceConversion(this, unit, action, targetPoint, targetUnit, recursionDepth, Boolean.TRUE.equals(cir.getReturnValue()));
    }
}
