package io.github.endx.rustedfabricapi.mixin;

import android.graphics.PointF;
import io.github.endx.rustedfabricapi.internal.unit.action.JavaUnitActionRuntime;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import rustedwarfare.unit.Unit;
import rustedwarfare.unit.action.UnitAction;

@Mixin(targets = {
        "rustedwarfare.unit.Unit",
        "rustedwarfare.custom.CustomUnit"
}, remap = false)
public abstract class UnitJavaActionExecutionNamedMixin {
    @Inject(method = "queueAction(Lrustedwarfare/unit/action/UnitAction;ZLandroid/graphics/PointF;Lrustedwarfare/unit/Unit;)V",
            at = @At("HEAD"), cancellable = true, require = 1)
    private void rustedfabricapi$executeJavaAction(UnitAction action, boolean queued,
            PointF targetPoint, Unit targetUnit, CallbackInfo ci) {
        if (JavaUnitActionRuntime.execute((Unit) (Object) this, action, queued,
                targetPoint, targetUnit)) {
            ci.cancel();
        }
    }
}
