package io.github.endx.rustedfabricapi.mixin;

import io.github.endx.rustedfabricapi.impl.unit.build.BuildProductionRuntime;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import rustedwarfare.custom.resource.ResourceAmount;
import rustedwarfare.ui.text.StyledTextBlock;
import rustedwarfare.unit.Unit;
import rustedwarfare.unit.action.QueueableUnitAction;
import rustedwarfare.unit.action.UnitAction;

@Mixin(targets = "rustedwarfare.unit.action.UnitAction", remap = false)
public abstract class UnitActionProductionModifierNamedMixin {
    @Redirect(method = "isActiveAndQueueAllowed(Lrustedwarfare/unit/Unit;Z)Z",
            at = @At(value = "INVOKE",
                    target = "Lrustedwarfare/unit/action/UnitAction;getPrice()Lrustedwarfare/custom/resource/ResourceAmount;"),
            require = 2)
    private ResourceAmount rustedfabricapi$productionPriceForAvailability(
            UnitAction action, Unit producer, boolean predicted) {
        return action instanceof QueueableUnitAction
                ? BuildProductionRuntime.price(producer, (QueueableUnitAction) action)
                : action.getPrice();
    }

    @Redirect(method = "appendPriceAndTextToTooltip(Lrustedwarfare/unit/Unit;Lrustedwarfare/ui/text/StyledTextBlock;Landroid/graphics/Paint;Landroid/graphics/Paint;)V",
            at = @At(value = "INVOKE",
                    target = "Lrustedwarfare/unit/action/UnitAction;getPrice()Lrustedwarfare/custom/resource/ResourceAmount;"),
            require = 1)
    private ResourceAmount rustedfabricapi$productionPriceForTooltip(
            UnitAction action, Unit producer, StyledTextBlock text,
            android.graphics.Paint titlePaint, android.graphics.Paint resourcePaint) {
        return action instanceof QueueableUnitAction
                ? BuildProductionRuntime.price(producer, (QueueableUnitAction) action)
                : action.getPrice();
    }

    @Redirect(method = "appendDescriptionToTooltip(Lrustedwarfare/unit/Unit;Lrustedwarfare/ui/text/StyledTextBlock;)V",
            at = @At(value = "INVOKE",
                    target = "Lrustedwarfare/ui/CommandInterface;buildActionDescriptionText(Lrustedwarfare/unit/action/UnitAction;Z)Ljava/lang/String;"),
            require = 1)
    private String rustedfabricapi$productionTimeForTooltip(
            UnitAction action, boolean multiline, Unit producer, StyledTextBlock text) {
        return BuildProductionRuntime.buildDescription(producer, action, multiline);
    }
}
