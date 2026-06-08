package io.github.endx.rustedfabricapi.mixin;

import io.github.endx.rustedfabricapi.api.event.CustomUnitRuntimeEvents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "com.corrodinggames.rts.game.units.custom.d.b", remap = false)
public abstract class ResourceAmountRuntimeOfficialMixin {
    @Inject(method = "b(Lcom/corrodinggames/rts/game/units/am;)Z", at = @At("HEAD"), cancellable = true, require = 1)
    private void rustedfabricapi$beforeResourceCostCheck(@Coerce Object unit, CallbackInfoReturnable<Boolean> cir) {
        if (CustomUnitRuntimeEvents.BEFORE_RESOURCE_COST_PAID.invoker().beforeResourceCostPaid(this, unit, "hasEnoughResources")) {
            cir.setReturnValue(Boolean.FALSE);
        }
    }

    @Inject(method = "h(Lcom/corrodinggames/rts/game/units/am;)V", at = @At("HEAD"), cancellable = true, require = 1)
    private void rustedfabricapi$beforeResourceCostPaid(@Coerce Object unit, CallbackInfo ci) {
        if (CustomUnitRuntimeEvents.BEFORE_RESOURCE_COST_PAID.invoker().beforeResourceCostPaid(this, unit, "addToUnit")) {
            ci.cancel();
        }
    }
}
