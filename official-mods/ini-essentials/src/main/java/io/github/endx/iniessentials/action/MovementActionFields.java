package io.github.endx.iniessentials.action;

import io.github.endx.iniessentials.IniEssentials;
import io.github.endx.iniessentials.StringExpression;
import io.github.endx.rustedfabricapi.api.ini.IniFieldDocumentation;
import io.github.endx.rustedfabricapi.api.ini.IniMultiplayerImpact;
import io.github.endx.rustedfabricapi.api.ini.action.IniActionEffectDefinition;
import io.github.endx.rustedfabricapi.api.ini.action.IniActionEffects;
import io.github.endx.rustedfabricapi.api.unit.movement.UnitMovementMode;
import io.github.endx.rustedfabricapi.api.unit.movement.UnitMovementOverrides;

/** Runtime movement-domain changes for ordinary and hidden actions. */
public final class MovementActionFields {
    private static final String FIELD = "setUnitMovementType";

    private MovementActionFields() { }

    public static void register() {
        IniActionEffects.register(IniActionEffectDefinition
                .<StringExpression>builder(IniEssentials.MOD_ID,
                        "set_unit_movement_type", FIELD)
                .decoder(context -> {
                    IniEssentials.activateSynchronizedRequirement();
                    String raw = context.rawValue().trim();
                    try {
                        UnitMovementMode.parse(raw);
                        return StringExpression.constant(raw);
                    } catch (IllegalArgumentException notAConstant) {
                        return StringExpression.compile(context.metadata(), raw);
                    }
                })
                .handler((context, expression) -> UnitMovementOverrides.set(
                        context.actor(), UnitMovementMode.parse(
                                expression.evaluate(context.actor()))))
                .documentation(new IniFieldDocumentation(
                        "native|land|air|water|hover|building|overCliff|overCliffWater|none or runtime string",
                        "Changes only this unit instance's movement/pathing type and building classification; native clears the override.",
                        "仅修改当前单位实例的移动/寻路类型及建筑分类；native 会清除覆盖。",
                        "setUnitMovementType: water",
                        IniMultiplayerImpact.GAMEPLAY_SYNCED))
                .build());
    }
}
