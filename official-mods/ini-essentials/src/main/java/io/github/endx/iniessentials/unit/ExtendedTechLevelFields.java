package io.github.endx.iniessentials.unit;

import io.github.endx.iniessentials.IniEssentials;
import io.github.endx.rustedfabricapi.api.ini.IniApplicationPhase;
import io.github.endx.rustedfabricapi.api.ini.IniExtensionKind;
import io.github.endx.rustedfabricapi.api.ini.IniExtensions;
import io.github.endx.rustedfabricapi.api.ini.IniFieldDefinition;
import io.github.endx.rustedfabricapi.api.ini.IniFieldDocumentation;
import io.github.endx.rustedfabricapi.api.ini.IniMultiplayerImpact;
import io.github.endx.rustedfabricapi.api.ini.IniSectionSelector;
import rustedwarfare.custom.CustomUnitMetadata;

/** Extends the native custom-unit techLevel range while retaining T3 native action semantics. */
public final class ExtendedTechLevelFields {
    public static final int NATIVE_MAX_TECH_LEVEL = 3;
    public static final int MAX_TECH_LEVEL = 255;

    private ExtendedTechLevelFields() { }

    public static void register() {
        IniExtensions.register(IniFieldDefinition
                .<Integer>builder(IniEssentials.MOD_ID, "extended_tech_level",
                        IniSectionSelector.exact("core"), "techLevel")
                .kind(IniExtensionKind.EXTENDED_VALUE)
                .applicationPhase(IniApplicationPhase.AFTER_METADATA_PARSED)
                .activatesWhen(context -> isExtended(context.rawValue()))
                .decoder(context -> Integer.valueOf(parse(context.rawValue())))
                .validator((context, value) -> {
                    if (value.intValue() > MAX_TECH_LEVEL) throw new IllegalArgumentException(
                            "techLevel must be at most " + MAX_TECH_LEVEL);
                })
                // The native parser allocates three action tables. Let it build the highest
                // native table, then restore the actual extended level on parsed metadata.
                .nativeFallback((context, value) ->
                        Integer.toString(NATIVE_MAX_TECH_LEVEL))
                .applier(field -> {
                    if (!(field.metadata() instanceof CustomUnitMetadata)) {
                        throw new IllegalArgumentException("extended techLevel requires custom unit metadata");
                    }
                    ((CustomUnitMetadata) field.metadata()).techLevel = field.value().intValue();
                    IniEssentials.activateSynchronizedRequirement();
                })
                .documentation(new IniFieldDocumentation(
                        "integer 1..255 (native: 1..3)",
                        "Allows custom units to use techLevel above 3; higher levels inherit native T3 action-list behavior while preserving their actual level for UI and mod logic.",
                        "允许自定义单位填写大于 3 的 techLevel；更高等级沿用原版 T3 action 列表行为，同时为 UI 与模组逻辑保留真实等级。",
                        "techLevel: 4",
                        IniMultiplayerImpact.GAMEPLAY_SYNCED))
                .build());
    }

    private static int parse(String source) {
        try {
            return Integer.parseInt(source.trim());
        } catch (NumberFormatException failure) {
            throw new IllegalArgumentException("techLevel requires an integer: " + source, failure);
        }
    }

    private static boolean isExtended(String source) {
        try {
            return Integer.parseInt(source.trim()) > NATIVE_MAX_TECH_LEVEL;
        } catch (NumberFormatException ignored) {
            return false;
        }
    }
}
