package io.github.endx.rustedfabricapi.impl.unit.build;

import io.github.endx.rustedfabricapi.api.game.Units;
import io.github.endx.rustedfabricapi.api.unit.build.event.BuildProductionEvents;
import io.github.endx.rustedfabricapi.api.unit.build.event.ProductionModifierContext;
import rustedwarfare.custom.resource.ResourceAmount;
import rustedwarfare.ui.CommandInterface;
import rustedwarfare.unit.Unit;
import rustedwarfare.unit.UnitType;
import rustedwarfare.unit.action.QueueableUnitAction;
import rustedwarfare.unit.action.UnitAction;
import rustedwarfare.util.CommonUtils;

/** Native bridge used by queue and tooltip mixins. */
public final class BuildProductionRuntime {
    private BuildProductionRuntime() { }

    public static ResourceAmount price(Unit producer, QueueableUnitAction action) {
        ResourceAmount original = action.getPrice();
        if (producer == null || action.getBuildUnitType() == null) return original;
        ProductionModifierContext context = context(producer, action, original.getCredits());
        if (context.creditCost() == original.getCredits()) return original;
        ResourceAmount changed = ResourceAmount.combine(original,
                ResourceAmount.ofCredits(context.creditCost() - original.getCredits()));
        changed.setFlagMask = original.setFlagMask;
        changed.unsetFlagMask = original.unsetFlagMask;
        changed.requiredFlagMask = original.requiredFlagMask;
        changed.missingFlagMask = original.missingFlagMask;
        return changed;
    }

    public static float buildSpeed(Unit producer, QueueableUnitAction action) {
        if (producer == null || action.getBuildUnitType() == null) return action.getBuildSpeed();
        return context(producer, action, action.getPrice().getCredits()).buildSpeedMultiplier();
    }

    public static String buildDescription(Unit producer, UnitAction action, boolean multiline) {
        String original = CommandInterface.buildActionDescriptionText(action, multiline);
        if (!(action instanceof QueueableUnitAction) || producer == null) return original;
        QueueableUnitAction queued = (QueueableUnitAction) action;
        float nativeSpeed = queued.getBuildSpeed();
        float modifiedSpeed = buildSpeed(producer, queued);
        if (nativeSpeed >= 1.0F || modifiedSpeed == nativeSpeed) return original;
        String nativeTime = CommonUtils.formatSeconds(
                1.0F / (nativeSpeed * producer.cx() * 60.0F) + 1.0E-4F);
        String modifiedTime = CommonUtils.formatSeconds(
                1.0F / (modifiedSpeed * producer.cx() * 60.0F) + 1.0E-4F);
        return original.replace(nativeTime, modifiedTime);
    }

    private static ProductionModifierContext context(Unit producer, QueueableUnitAction action,
                                                     int creditCost) {
        UnitType produced = action.getBuildUnitType();
        ProductionModifierContext context = new ProductionModifierContext(
                Units.view(producer), produced.getInternalName(), creditCost,
                action.getBuildSpeed());
        BuildProductionEvents.MODIFY.invoker().modify(context);
        return context;
    }
}
