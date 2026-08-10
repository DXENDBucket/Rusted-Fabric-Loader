package io.github.endx.iniessentials.action;

import io.github.endx.iniessentials.IniEssentials;
import io.github.endx.rustedfabricapi.api.custom.PerActionAutoTriggerCooldowns;
import io.github.endx.rustedfabricapi.api.ini.IniFieldDefinition;
import io.github.endx.rustedfabricapi.api.ini.IniFieldDocumentation;
import io.github.endx.rustedfabricapi.api.ini.IniMultiplayerImpact;
import io.github.endx.rustedfabricapi.api.ini.IniSectionSelector;
import io.github.endx.rustedfabricapi.api.ini.IniExtensions;
import rustedwarfare.custom.CustomUnitMetadata;
import rustedwarfare.custom.action.CustomActionConfig;
import rustedwarfare.util.UnitConfig;

/** Per-action form of the native unit-wide automatic-trigger cooldown. */
public final class PerActionAutoTriggerCooldownFields {
    private static final String COOLDOWN = "autoTriggerCooldownTime";
    private static final String DANGEROUS = "autoTriggerCooldownTime_allowDangerousHighCPU";
    private static final float MAX_FRAMES = 120.0F;
    private static final float SAFE_MIN_FRAMES = 5.0F;

    private PerActionAutoTriggerCooldownFields() { }

    public static void register() {
        IniExtensions.register(IniFieldDefinition
                .<String>builder(IniEssentials.MOD_ID, "per_action_auto_trigger_cooldown",
                        IniSectionSelector.any(), COOLDOWN)
                .activatesWhen(context -> isActionSection(context.section()))
                .decoder(context -> context.rawValue().trim())
                .validator((context, value) -> {
                    if (value.isEmpty()) throw new IllegalArgumentException(COOLDOWN + " is empty");
                })
                .applier(field -> configure((CustomUnitMetadata) field.metadata(),
                        (UnitConfig) field.unitConfig(), field.source().section()))
                .documentation(new IniFieldDocumentation(
                        "time (0s to 2s)",
                        "Gives this autoTrigger its own per-unit cooldown instead of sharing the core cooldown latch with every action.",
                        "为此 autoTrigger 设置独立的单位实例计时，不再与该单位的所有 Action 共用 core 冷却锁。",
                        "autoTriggerCooldownTime: 0.5s",
                        IniMultiplayerImpact.GAMEPLAY_SYNCED))
                .build());
        IniExtensions.register(IniFieldDefinition
                .<String>builder(IniEssentials.MOD_ID, "per_action_auto_trigger_dangerous_cpu",
                        IniSectionSelector.any(), DANGEROUS)
                .activatesWhen(context -> isActionSection(context.section()))
                .decoder(context -> context.rawValue().trim())
                .validator((context, value) -> UnitConfig.parseBoolean(
                        context.section(), DANGEROUS, value))
                .applier(field -> validateDangerousPair((UnitConfig) field.unitConfig(),
                        field.source().section()))
                .documentation(new IniFieldDocumentation(
                        "boolean",
                        "Allows this action's autoTrigger cooldown to be shorter than the native safe minimum.",
                        "允许此 Action 的 autoTrigger 冷却低于原版安全下限。",
                        "autoTriggerCooldownTime_allowDangerousHighCPU: true",
                        IniMultiplayerImpact.GAMEPLAY_SYNCED))
                .build());
    }

    private static void configure(CustomUnitMetadata metadata, UnitConfig config, String section) {
        requireAutoTrigger(config, section);
        float frames = config.getTimeInFrames(section, COOLDOWN, null);
        boolean dangerous = config.getBoolean(section, DANGEROUS, Boolean.FALSE);
        validateDuration(section, frames, dangerous);
        CustomActionConfig actionConfig = metadata.getActionConfigByName(actionName(section));
        if (actionConfig == null) {
            throw new IllegalArgumentException("[" + section
                    + "] could not resolve its native custom action config");
        }
        PerActionAutoTriggerCooldowns.configureSeconds(metadata, actionConfig, frames / 60.0F);
        IniEssentials.activateSynchronizedRequirement();
    }

    private static void validateDangerousPair(UnitConfig config, String section) {
        if (!config.hasKey(section, COOLDOWN)) {
            throw new IllegalArgumentException("[" + section + "] " + DANGEROUS
                    + " requires " + COOLDOWN + " in the same action section");
        }
        requireAutoTrigger(config, section);
    }

    private static void validateDuration(String section, float frames, boolean dangerous) {
        if (!Float.isFinite(frames) || frames < 0.0F) {
            throw new IllegalArgumentException("[" + section + "] " + COOLDOWN
                    + " must be finite and cannot be below zero");
        }
        if (frames > MAX_FRAMES) {
            throw new IllegalArgumentException("[" + section + "] " + COOLDOWN
                    + " cannot be more than 2 seconds");
        }
        if (!dangerous && frames < SAFE_MIN_FRAMES) {
            throw new IllegalArgumentException("[" + section + "] " + COOLDOWN
                    + " is below the native safe minimum; set " + DANGEROUS + ": true");
        }
    }

    private static void requireAutoTrigger(UnitConfig config, String section) {
        if (!config.hasKey(section, "autoTrigger")) {
            throw new IllegalArgumentException("[" + section + "] " + COOLDOWN
                    + " only applies to autoTrigger and requires autoTrigger in the same section");
        }
    }

    private static boolean isActionSection(String section) {
        return section.startsWith("action_") || section.startsWith("hiddenAction_");
    }

    private static String actionName(String section) {
        String prefix = section.startsWith("hiddenAction_") ? "hiddenAction_" : "action_";
        return section.substring(prefix.length());
    }
}
