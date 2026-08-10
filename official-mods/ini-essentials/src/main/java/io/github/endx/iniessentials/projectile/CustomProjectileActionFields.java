package io.github.endx.iniessentials.projectile;

import io.github.endx.iniessentials.IniEssentials;
import io.github.endx.rustedfabricapi.api.ini.IniFieldDocumentation;
import io.github.endx.rustedfabricapi.api.ini.IniMultiplayerImpact;
import io.github.endx.rustedfabricapi.api.ini.action.IniActionEffectDefinition;
import io.github.endx.rustedfabricapi.api.ini.action.IniActionEffects;
import io.github.endx.rustedfabricapi.api.ini.action.IniActionExecutionContext;
import io.github.endx.rustedfabricapi.api.projectile.spawn.ProjectileSpawnContext;
import io.github.endx.rustedfabricapi.api.world.WorldPoint;
import rustedwarfare.custom.CustomUnit;
import rustedwarfare.unit.Unit;
import rustedwarfare.util.CommonUtils;

/** Action entrypoint for independent CustomProjectile patterns. */
public final class CustomProjectileActionFields {
    private CustomProjectileActionFields() { }

    public static void register() {
        registerField("spawn_custom_projectile", "spawnCustomProjectile",
                "Spawns an independent CustomProjectile directly from this action. The pattern suffix is optional and defaults to main.",
                "从该 action 直接生成独立 CustomProjectile；可省略 pattern 后缀，默认使用 main。",
                "spawnCustomProjectile: example:plasma_fan");
        registerField("emit_projectile_pattern", "emitProjectilePattern",
                "Emits one bounded CustomProjectile pattern directly, without a projectile-spawning parent projectile.",
                "直接发射一组有上限的 CustomProjectile 图案，不需要用母弹不断刷弹。",
                "emitProjectilePattern: example:plasma_fan/main");
    }

    private static void registerField(String fieldId, String key, String english,
                                      String chinese, String example) {
        IniActionEffects.register(IniActionEffectDefinition
                .<CustomProjectileDefinitions.Reference>builder(
                        IniEssentials.MOD_ID, fieldId, key)
                .exclusiveGroup("custom_projectile_spawn")
                .decoder(context -> {
                    CustomProjectileDefinitions.Reference reference =
                            CustomProjectileDefinitions.Reference.parse(context.rawValue());
                    CustomProjectileDefinitions.noteReference(reference);
                    IniEssentials.activateSynchronizedRequirement();
                    return reference;
                })
                .handler(CustomProjectileActionFields::emit)
                .documentation(new IniFieldDocumentation(
                        "namespace:path[/pattern]", english, chinese, example,
                        IniMultiplayerImpact.GAMEPLAY_SYNCED))
                .build());
    }

    private static void emit(IniActionExecutionContext context,
                             CustomProjectileDefinitions.Reference reference) {
        CustomUnit actor = context.actor();
        CustomProjectileDefinitions.CompiledPattern pattern = reference.definition()
                .requirePattern(reference.patternName()).compileFor(actor);
        float localX = pattern.originOffsetX.evaluate(actor);
        float localY = pattern.originOffsetY.evaluate(actor);
        float sin = CommonUtils.fastSin(actor.direction);
        float cos = CommonUtils.fastCos(actor.direction);
        float originX = ((Unit) actor).x + cos * localY - sin * localX;
        float originY = ((Unit) actor).y + sin * localY + cos * localX;
        float originHeight = actor.height + pattern.originOffsetHeight.evaluate(actor);

        WorldPoint point = context.actionTargetPosition().orElse(null);
        CustomProjectileEmitter.emit(reference, actor, originX, originY, originHeight,
                actor.direction, context.targetUnit().orElse(null), point != null,
                point != null ? point.x() : 0.0F, point != null ? point.y() : 0.0F,
                targetHeight(context), ProjectileSpawnContext.Cause.ACTION,
                context.recursionDepth());
    }

    private static float targetHeight(IniActionExecutionContext context) {
        return context.targetUnit().map(unit -> unit.height).orElse(0.0F);
    }
}
