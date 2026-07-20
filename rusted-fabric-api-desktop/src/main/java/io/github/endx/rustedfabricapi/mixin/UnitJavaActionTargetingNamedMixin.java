package io.github.endx.rustedfabricapi.mixin;

import io.github.endx.rustedfabricapi.internal.unit.action.JavaUnitActionRuntime;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import rustedwarfare.unit.Unit;
import rustedwarfare.unit.action.UnitAction;

@Mixin(targets = {
        "rustedwarfare.unit.OrderableUnit",
        "rustedwarfare.custom.CustomUnit"
}, remap = false)
public abstract class UnitJavaActionTargetingNamedMixin {
    @Inject(method = "checkTargetedActionOrder(Lrustedwarfare/unit/action/UnitAction;FF)Z",
            at = @At("HEAD"), cancellable = true, require = 1)
    private void rustedfabricapi$validateJavaActionTarget(UnitAction action, float x, float y,
            CallbackInfoReturnable<Boolean> cir) {
        Boolean allowed = JavaUnitActionRuntime.targetedActionAllowed(
                (Unit) (Object) this, action, x, y);
        if (allowed != null) cir.setReturnValue(allowed.booleanValue());
    }
}
