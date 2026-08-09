package io.github.endx.rustedfabricapi.mixin;

import android.graphics.PointF;
import io.github.endx.rustedfabricapi.impl.custom.QueuedEventActionRuntime;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import rustedwarfare.custom.CustomUnit;
import rustedwarfare.unit.Unit;
import rustedwarfare.unit.action.UnitAction;

@Mixin(value = CustomUnit.class, remap = false)
public abstract class QueuedEventActionNamedMixin {
    @Redirect(
            method = "processQueuedCustomEvents(FI)V",
            at = @At(value = "INVOKE",
                    target = "Lrustedwarfare/custom/CustomUnit;executeActionWithContext(Lrustedwarfare/unit/action/UnitAction;Landroid/graphics/PointF;Lrustedwarfare/unit/Unit;II)Z"),
            require = 1)
    private static boolean rustedfabricapi$executeQueuedEventAction(
            CustomUnit actor, UnitAction action, PointF targetPoint, Unit targetUnit,
            int recursionDepth, int extraDepth) {
        return QueuedEventActionRuntime.execute(
                actor, action, targetPoint, targetUnit, recursionDepth, extraDepth);
    }
}
