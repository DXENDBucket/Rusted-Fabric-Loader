package io.github.endx.rustedfabricapi.mixin;

import android.graphics.Paint;
import io.github.endx.rustedfabricapi.api.client.event.ProductionActionNameColorEvents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;
import rustedwarfare.unit.UnitType;
import rustedwarfare.unit.action.UnitAction;

/** Applies client color overrides at the native queue-unit label color branch. */
@Mixin(targets = "rustedwarfare.ui.CommandInterface", remap = false)
public abstract class CommandInterfaceProductionNameColorNamedMixin {
    private static final int NATIVE_DISABLED_TIER_ONE_COLOR = 0xff006400;
    private static final int NATIVE_DISABLED_HIGH_TIER_COLOR = 0xff75780f;

    @Unique private UnitAction rustedfabricapi$productionNameAction;
    @Unique private UnitType rustedfabricapi$productionNameType;

    @Redirect(
            method = "drawActionButtonsAndHandleInput(F)I",
            at = @At(value = "INVOKE",
                    target = "Lrustedwarfare/unit/action/UnitAction;getDisplayTextWithCount()Ljava/lang/String;"),
            require = 1)
    private String rustedfabricapi$captureProductionNameAction(UnitAction action) {
        rustedfabricapi$productionNameAction = action;
        rustedfabricapi$productionNameType = action.getDisplayType()
                == rustedwarfare.unit.action.ActionDisplayType.queueUnit
                ? action.getBuildUnitType() : null;
        return action.getDisplayTextWithCount();
    }

    @ModifyArg(
            method = "drawActionButtonsAndHandleInput(F)I",
            at = @At(value = "INVOKE",
                    target = "Lrustedwarfare/render/GraphicsEngine;drawText(Ljava/lang/String;FFLandroid/graphics/Paint;)V",
                    ordinal = 1),
            index = 3,
            require = 1)
    private Paint rustedfabricapi$resolveProductionNameColor(Paint paint) {
        UnitType producedType = rustedfabricapi$productionNameType;
        if (producedType == null) return paint;
        Integer override = ProductionActionNameColorEvents.RESOLVE.invoker().resolve(
                rustedfabricapi$productionNameAction, producedType, null);
        if (override == null) return paint;

        int color = override.intValue();
        int nativeColor = paint.e();
        if (nativeColor == NATIVE_DISABLED_TIER_ONE_COLOR
                || nativeColor == NATIVE_DISABLED_HIGH_TIER_COLOR) color = dim(color);
        paint.b(color);
        return paint;
    }

    @Unique
    private static int dim(int color) {
        int alpha = color >>> 24;
        int red = (color >>> 16 & 0xff) / 2;
        int green = (color >>> 8 & 0xff) / 2;
        int blue = (color & 0xff) / 2;
        return alpha << 24 | red << 16 | green << 8 | blue;
    }
}
