package io.github.endx.rustedfabricapi.mixin;

import io.github.endx.rustedfabricapi.api.event.ResourceRuntimeEvents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "com.corrodinggames.rts.game.units.custom.a.a.m", remap = false)
public abstract class TakeResourcesCollectOfficialMixin {
    @Inject(method = "a(Lcom/corrodinggames/rts/game/units/custom/j;Lcom/corrodinggames/rts/game/units/a/s;Landroid/graphics/PointF;Lcom/corrodinggames/rts/game/units/am;I)Z", at = @At("HEAD"), cancellable = true, require = 1)
    private void rustedfabricapi$beforeTakeResourcesCollect(@Coerce Object unit, @Coerce Object action, @Coerce Object targetPoint, @Coerce Object targetUnit, int recursionDepth, CallbackInfoReturnable<Boolean> cir) {
        if (ResourceRuntimeEvents.BEFORE_TAKE_RESOURCES_COLLECT.invoker().beforeTakeResourcesCollect(this, unit, action, targetPoint, targetUnit, recursionDepth)) {
            cir.setReturnValue(Boolean.FALSE);
        }
    }

    @Inject(method = "a(Lcom/corrodinggames/rts/game/units/custom/j;Lcom/corrodinggames/rts/game/units/a/s;Landroid/graphics/PointF;Lcom/corrodinggames/rts/game/units/am;I)Z", at = @At("RETURN"), require = 1)
    private void rustedfabricapi$afterTakeResourcesCollect(@Coerce Object unit, @Coerce Object action, @Coerce Object targetPoint, @Coerce Object targetUnit, int recursionDepth, CallbackInfoReturnable<Boolean> cir) {
        ResourceRuntimeEvents.AFTER_TAKE_RESOURCES_COLLECT.invoker().afterTakeResourcesCollect(this, unit, action, targetPoint, targetUnit, recursionDepth, Boolean.TRUE.equals(cir.getReturnValue()));
    }
}
