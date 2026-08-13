package io.github.endx.rustedfabricapi.impl.unit.build;

import io.github.endx.rustedfabricapi.api.game.Units;
import io.github.endx.rustedfabricapi.api.unit.build.event.BuildProductionEvents;
import io.github.endx.rustedfabricapi.api.unit.build.event.ProductionModifierContext;
import io.github.endx.rustedfabricapi.api.unit.build.event.ProductionCompletedContext;
import rustedwarfare.custom.resource.ResourceAmount;
import rustedwarfare.custom.resource.ResourceType;
import rustedwarfare.custom.resource.StoredResourceEntry;
import rustedwarfare.custom.resource.StoredResourceSet;
import rustedwarfare.ui.CommandInterface;
import rustedwarfare.unit.Unit;
import rustedwarfare.unit.UnitType;
import rustedwarfare.unit.action.QueueableUnitAction;
import rustedwarfare.unit.action.UnitAction;
import rustedwarfare.util.CommonUtils;

import java.util.LinkedHashMap;
import java.util.Map;

/** Native bridge used by queue and tooltip mixins. */
public final class BuildProductionRuntime {
    private BuildProductionRuntime() { }

    public static void completed(Unit producer, Unit producedUnit) {
        if (producer == null || producedUnit == null) return;
        BuildProductionEvents.AFTER_COMPLETED.invoker().completed(
                new ProductionCompletedContext(Units.view(producer), Units.view(producedUnit)));
    }

    public static ResourceAmount price(Unit producer, QueueableUnitAction action) {
        ResourceAmount original = action.getPrice();
        if (producer == null || action.getBuildUnitType() == null) return original;
        ProductionModifierContext context = context(producer, action, original);
        Map<String, Double> changedResources = context.resourceCosts();
        if (context.creditCost() == original.getCredits()
                && changedResources.equals(context.originalResourceCosts())) return original;
        ResourceAmount changed = ResourceAmount.combine(original,
                ResourceAmount.ofCredits(context.creditCost() - original.getCredits()));
        if (!changedResources.equals(context.originalResourceCosts())) {
            StoredResourceSet custom = new StoredResourceSet();
            custom.copyFrom(original.customResources);
            Map<String, ResourceType> originalTypes = customResourceTypes(original);
            for (Map.Entry<String, Double> entry : changedResources.entrySet()) {
                ResourceType type = resolveResourceType(entry.getKey(), originalTypes);
                custom.setAmount(type, entry.getValue().doubleValue());
            }
            changed.customResources = custom;
        }
        changed.setFlagMask = original.setFlagMask;
        changed.unsetFlagMask = original.unsetFlagMask;
        changed.requiredFlagMask = original.requiredFlagMask;
        changed.missingFlagMask = original.missingFlagMask;
        return changed;
    }

    public static float buildSpeed(Unit producer, QueueableUnitAction action) {
        if (producer == null || action.getBuildUnitType() == null) return action.getBuildSpeed();
        return context(producer, action, action.getPrice()).buildSpeedMultiplier();
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
                                                     ResourceAmount price) {
        UnitType produced = action.getBuildUnitType();
        ProductionModifierContext context = new ProductionModifierContext(
                Units.view(producer), produced.getInternalName(), price.getCredits(),
                action.getBuildSpeed(), customResourceCosts(price));
        BuildProductionEvents.MODIFY.invoker().modify(context);
        return context;
    }

    private static Map<String, Double> customResourceCosts(ResourceAmount price) {
        LinkedHashMap<String, Double> result = new LinkedHashMap<String, Double>();
        for (Object value : price.customResources.entries) {
            StoredResourceEntry entry = (StoredResourceEntry) value;
            result.put(entry.resourceType.getInternalName(), Double.valueOf(entry.amount));
        }
        return result;
    }

    private static Map<String, ResourceType> customResourceTypes(ResourceAmount price) {
        LinkedHashMap<String, ResourceType> result = new LinkedHashMap<String, ResourceType>();
        for (Object value : price.customResources.entries) {
            StoredResourceEntry entry = (StoredResourceEntry) value;
            result.put(entry.resourceType.getInternalName(), entry.resourceType);
        }
        return result;
    }

    private static ResourceType resolveResourceType(String name,
                                                    Map<String, ResourceType> originalTypes) {
        ResourceType type = originalTypes.get(name);
        if (type == null) type = ResourceType.getAnyResourceTypeByName(name);
        if (type == null && !name.startsWith("l_") && !name.startsWith("g_")) {
            type = ResourceType.getAnyResourceTypeByName("l_" + name);
            if (type == null) type = ResourceType.getAnyResourceTypeByName("g_" + name);
        }
        if (type == null) throw new IllegalArgumentException("Unknown custom resource: " + name);
        return type;
    }
}
