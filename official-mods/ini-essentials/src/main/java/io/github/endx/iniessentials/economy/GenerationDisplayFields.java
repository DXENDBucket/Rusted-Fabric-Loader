package io.github.endx.iniessentials.economy;

import io.github.endx.iniessentials.BooleanExpression;
import io.github.endx.iniessentials.IniEssentials;
import io.github.endx.iniessentials.NumericExpression;
import io.github.endx.rustedfabricapi.api.custom.event.CustomUnitEconomyEvents;
import io.github.endx.rustedfabricapi.api.custom.event.PeriodicGenerationDisplayContext;
import io.github.endx.rustedfabricapi.api.ini.IniApplicationPhase;
import io.github.endx.rustedfabricapi.api.ini.IniExtensions;
import io.github.endx.rustedfabricapi.api.ini.IniFieldDefinition;
import io.github.endx.rustedfabricapi.api.ini.IniFieldDocumentation;
import io.github.endx.rustedfabricapi.api.ini.IniMultiplayerImpact;
import io.github.endx.rustedfabricapi.api.ini.IniSectionSelector;
import rustedwarfare.custom.CustomUnit;
import rustedwarfare.custom.CustomUnitMetadata;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.WeakHashMap;

/** Dynamic overrides for native periodic-generation queries and HUD economy totals. */
public final class GenerationDisplayFields {
    private static final String RESOURCE_PREFIX = "generationDisplayResource_";
    private static final Map<Object, Definition> BY_METADATA =
            Collections.synchronizedMap(new WeakHashMap<Object, Definition>());

    private GenerationDisplayFields() { }

    public static void register() {
        registerWhen();
        registerCredits();
        registerResources();
        CustomUnitEconomyEvents.MODIFY_PERIODIC_GENERATION_DISPLAY.register(
                GenerationDisplayFields::modifyDisplay);
    }

    private static void registerWhen() {
        IniExtensions.register(IniFieldDefinition
                .<String>builder(IniEssentials.MOD_ID, "generation_display_when",
                        IniSectionSelector.exact("core"), "generationDisplayWhen")
                .applicationPhase(IniApplicationPhase.AFTER_METADATA_PARSED)
                .decoder(context -> context.rawValue().trim())
                .applier(field -> {
                    definition(field.metadata()).when = BooleanExpression.compile(
                            field.metadata(), field.value());
                    IniEssentials.activateSynchronizedRequirement();
                })
                .documentation(new IniFieldDocumentation(
                        "runtime LogicBoolean",
                        "Enables the generation-display overrides for this unit instance when true.",
                        "为 true 时，对此单位实例启用产出显示覆盖。",
                        "generationDisplayWhen: self.resource(type='nutrient_mode', greaterThan=0)",
                        IniMultiplayerImpact.GAMEPLAY_SYNCED))
                .build());
    }

    private static void registerCredits() {
        IniExtensions.register(IniFieldDefinition
                .<String>builder(IniEssentials.MOD_ID, "generation_display_credits",
                        IniSectionSelector.exact("core"), "generationDisplayCredits")
                .applicationPhase(IniApplicationPhase.AFTER_METADATA_PARSED)
                .decoder(context -> context.rawValue().trim())
                .applier(field -> {
                    definition(field.metadata()).credits = NumericExpression.compile(
                            field.metadata(), field.value());
                    IniEssentials.activateSynchronizedRequirement();
                })
                .documentation(new IniFieldDocumentation(
                        "runtime number (credits/second)",
                        "Overrides the credits-per-second value queried by the HUD and team economy totals; it does not settle resources.",
                        "覆盖 HUD 与队伍经济统计查询的每秒资金值；不会据此结算资源。",
                        "generationDisplayCredits: 0",
                        IniMultiplayerImpact.GAMEPLAY_SYNCED))
                .build());
    }

    private static void registerResources() {
        IniExtensions.register(IniFieldDefinition
                .<String>builder(IniEssentials.MOD_ID, "generation_display_resource",
                        IniSectionSelector.exact("core"), RESOURCE_PREFIX)
                .matchKeyPrefix()
                .applicationPhase(IniApplicationPhase.AFTER_METADATA_PARSED)
                .decoder(context -> context.rawValue().trim())
                .applier(field -> {
                    String resourceName = field.source().key().substring(RESOURCE_PREFIX.length()).trim();
                    if (resourceName.isEmpty()) {
                        throw new IllegalArgumentException(RESOURCE_PREFIX
                                + " must be followed by a resource name");
                    }
                    CustomUnitMetadata metadata = (CustomUnitMetadata) field.metadata();
                    if (metadata.getResourceTypeByNameOrBuiltin(resourceName) == null) {
                        throw new IllegalArgumentException("Unknown generation display resource '"
                                + resourceName + "' for " + metadata.getInternalName());
                    }
                    definition(field.metadata()).resources.put(resourceName,
                            NumericExpression.compile(field.metadata(), field.value()));
                    IniEssentials.activateSynchronizedRequirement();
                })
                .documentation(new IniFieldDocumentation(
                        "runtime number (resource/second)",
                        "Overrides the queried per-second display rate for the named built-in or local resource; it does not settle resources.",
                        "覆盖指定内置或单位局部资源所查询的每秒显示量；不会据此结算资源。",
                        "generationDisplayResource_nutrient: 2.8*incomeMultiplier()",
                        IniMultiplayerImpact.GAMEPLAY_SYNCED))
                .build());
    }

    private static Definition definition(Object metadata) {
        Definition existing = BY_METADATA.get(metadata);
        if (existing != null) return existing;
        Definition created = new Definition();
        BY_METADATA.put(metadata, created);
        return created;
    }

    private static void modifyDisplay(PeriodicGenerationDisplayContext context) {
        Object identity = context.unit().identity();
        if (!(identity instanceof CustomUnit)) return;
        CustomUnit unit = (CustomUnit) identity;
        Definition definition = BY_METADATA.get(unit.unitMetadata);
        if (definition == null || (definition.when != null && !definition.when.evaluate(unit))) {
            return;
        }
        if (definition.credits != null) {
            context.setCreditRatePerSecond(definition.credits.evaluate(unit));
        }
        for (Map.Entry<String, NumericExpression> entry : definition.resources.entrySet()) {
            context.setResourceRatePerSecond(entry.getKey(), entry.getValue().evaluate(unit));
        }
    }

    private static final class Definition {
        private BooleanExpression when;
        private NumericExpression credits;
        private final LinkedHashMap<String, NumericExpression> resources =
                new LinkedHashMap<String, NumericExpression>();
    }
}
