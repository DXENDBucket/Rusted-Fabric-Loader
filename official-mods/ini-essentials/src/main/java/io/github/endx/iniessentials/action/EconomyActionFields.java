package io.github.endx.iniessentials.action;

import io.github.endx.iniessentials.IniEssentials;
import io.github.endx.rustedfabricapi.api.custom.CustomUnitHandle;
import io.github.endx.rustedfabricapi.api.ini.IniFieldDocumentation;
import io.github.endx.rustedfabricapi.api.ini.IniMultiplayerImpact;
import io.github.endx.rustedfabricapi.api.ini.action.IniActionEffectDefinition;
import io.github.endx.rustedfabricapi.api.ini.action.IniActionEffects;

import java.util.Locale;

/** Economy-cache maintenance for actions that switch custom production modes. */
public final class EconomyActionFields {
    private static final String FIELD = "refreshTeamEconomyStats";

    private EconomyActionFields() { }

    public static void register() {
        IniActionEffects.register(IniActionEffectDefinition
                .<Boolean>builder(IniEssentials.MOD_ID,
                        "refresh_team_economy_stats", FIELD)
                .decoder(context -> {
                    IniEssentials.activateSynchronizedRequirement();
                    String value = context.rawValue().trim().toLowerCase(Locale.ROOT);
                    if ("true".equals(value)) return Boolean.TRUE;
                    if ("false".equals(value)) return Boolean.FALSE;
                    throw new IllegalArgumentException(FIELD + " expects true or false");
                })
                .handler((context, enabled) -> {
                    if (enabled.booleanValue()) {
                        CustomUnitHandle.of(context.actor()).refreshTeamEconomyStats();
                    }
                })
                .documentation(new IniFieldDocumentation(
                        "boolean",
                        "Rebuilds the acting unit team's cached economy after this action changes a custom production mode.",
                        "当此 Action 改变自定义产出模式后，重建执行单位所属队伍的经济统计缓存。",
                        "refreshTeamEconomyStats: true",
                        IniMultiplayerImpact.GAMEPLAY_SYNCED))
                .build());
    }
}
