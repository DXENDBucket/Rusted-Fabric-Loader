package io.github.endx.iniessentials;

import io.github.endx.rustedfabricapi.api.ini.IniFieldDefinition;
import io.github.endx.rustedfabricapi.api.ini.IniFieldDocumentation;
import io.github.endx.rustedfabricapi.api.ini.IniExtensions;
import io.github.endx.rustedfabricapi.api.ini.IniMultiplayerImpact;
import io.github.endx.rustedfabricapi.api.ini.IniSectionSelector;
import io.github.endx.rustedfabricapi.api.custom.event.DamageEventData;
import io.github.endx.rustedfabricapi.api.custom.event.NativeEventData;
import io.github.endx.rustedfabricapi.api.multiplayer.MultiplayerMod;
import io.github.endx.rustedfabricapi.api.multiplayer.MultiplayerRequirements;
import io.github.endx.rustedfabricapi.api.unit.event.UnitDamageEvents;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;

import java.util.concurrent.atomic.AtomicBoolean;

public final class IniEssentials implements ModInitializer {
    public static final String MOD_ID = "ini_essentials";
    private static final AtomicBoolean SYNC_REQUIREMENT_ACTIVE = new AtomicBoolean();

    @Override
    public void onInitialize() {
        ExtendedMathFunctions.register();
        EventRuleDefinitions.register();
        GeometryDefinitions.register();
        FogActionFields.register();
        CameraActionFields.register();
        DamageEventData.enable(IniEssentials::activateSynchronizedRequirement);
        NativeEventData.enable(IniEssentials::activateSynchronizedRequirement);
        IniExtensions.register(IniFieldDefinition
                .<String>builder(MOD_ID, "allow_negative_hp",
                        IniSectionSelector.exact("core"), "allowNegativeHp")
                .decoder(context -> context.rawValue().trim())
                .applier(field -> {
                    BooleanExpression expression = BooleanExpression.compile(
                            field.metadata(), field.value());
                    NegativeHpPolicy.configure(field.metadata(), expression);
                    if (!expression.isStaticFalse()) activateSynchronizedRequirement();
                })
                .documentation(new IniFieldDocumentation(
                        "runtime LogicBoolean",
                        "Allows overkill damage to leave this custom unit with HP below zero instead of clamping it to zero.",
                        "允许过量伤害使此自定义单位的生命值降到零以下，而不是夹到零。",
                        "allowNegativeHp: memory.berserk or self.resource.rage > 0",
                        IniMultiplayerImpact.GAMEPLAY_SYNCED))
                .build());
        UnitDamageEvents.MODIFY_LETHAL_HEALTH.register((unit, attacker, requestedAmount,
                                                        projectile, nativeValue,
                                                        unclampedValue, currentValue) ->
                NegativeHpPolicy.allows(unit) ? Float.valueOf(unclampedValue) : null);
        System.out.println("[INI Essentials] Registered opt-in INI fields and event data");
    }

    static void activateSynchronizedRequirement() {
        if (!SYNC_REQUIREMENT_ACTIVE.compareAndSet(false, true)) return;
        String version = FabricLoader.getInstance().getModContainer(MOD_ID)
                .map(container -> container.getMetadata().getVersion().getFriendlyString())
                .orElse("0.1.0");
        MultiplayerRequirements.activate(MultiplayerMod.required(
                MOD_ID, version, "ini_essentials_v9",
                "d19abaa0f6819f95b03bd45f6416d345f72e9a7b650d984086fefa3a3c8dc04f"));
    }
}
