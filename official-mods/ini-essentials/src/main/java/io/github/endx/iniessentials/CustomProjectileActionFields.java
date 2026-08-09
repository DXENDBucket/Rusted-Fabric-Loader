package io.github.endx.iniessentials;

import io.github.endx.rustedfabricapi.api.ini.IniFieldDocumentation;
import io.github.endx.rustedfabricapi.api.ini.IniMultiplayerImpact;
import io.github.endx.rustedfabricapi.api.ini.action.IniActionEffectDefinition;
import io.github.endx.rustedfabricapi.api.ini.action.IniActionEffects;
import io.github.endx.rustedfabricapi.api.ini.action.IniActionExecutionContext;
import io.github.endx.rustedfabricapi.api.projectile.pattern.ProjectilePatternEmitter;
import io.github.endx.rustedfabricapi.api.projectile.pattern.ProjectilePatternSpec;
import io.github.endx.rustedfabricapi.api.projectile.spawn.ProjectileSpawnContext;
import io.github.endx.rustedfabricapi.api.projectile.spawn.ProjectileSpawnSpec;
import io.github.endx.rustedfabricapi.api.world.GameWorld;
import io.github.endx.rustedfabricapi.api.world.WorldPoint;
import rustedwarfare.custom.CustomUnit;
import rustedwarfare.unit.Unit;
import rustedwarfare.util.CommonUtils;

/** Action entrypoint for independent CustomProjectile patterns. */
final class CustomProjectileActionFields {
    private CustomProjectileActionFields() { }

    static void register() {
        IniActionEffects.register(IniActionEffectDefinition
                .<CustomProjectileDefinitions.Reference>builder(
                        IniEssentials.MOD_ID, "emit_projectile_pattern",
                        "emitProjectilePattern")
                .decoder(context -> {
                    CustomProjectileDefinitions.Reference reference =
                            CustomProjectileDefinitions.Reference.parse(context.rawValue());
                    CustomProjectileDefinitions.noteReference(reference);
                    IniEssentials.activateSynchronizedRequirement();
                    return reference;
                })
                .handler(CustomProjectileActionFields::emit)
                .documentation(new IniFieldDocumentation(
                        "namespace:path/pattern",
                        "Emits one bounded CustomProjectile pattern directly, without a projectile-spawning parent projectile.",
                        "鐩存帴鍙戝皠涓€涓湁涓婇檺鐨?CustomProjectile 寮瑰箷锛屼笉鍒涘缓鐢ㄤ簬鍒峰脊鐨勬瘝寮逛綋銆?",
                        "emitProjectilePattern: example:plasma_fan/main",
                        IniMultiplayerImpact.GAMEPLAY_SYNCED))
                .build());
    }

    private static void emit(IniActionExecutionContext context,
                             CustomProjectileDefinitions.Reference reference) {
        CustomProjectileDefinitions.Definition definition = reference.definition();
        CustomProjectileDefinitions.CompiledPattern pattern = definition
                .requirePattern(reference.patternName()).compileFor(context.actor());
        CustomUnit actor = context.actor();

        float direction = pattern.centerDirection.evaluate(actor);
        float localX = pattern.originOffsetX.evaluate(actor);
        float localY = pattern.originOffsetY.evaluate(actor);
        float sin = CommonUtils.fastSin(actor.direction);
        float cos = CommonUtils.fastCos(actor.direction);
        float originX = ((Unit) actor).x + cos * localY - sin * localX;
        float originY = ((Unit) actor).y + sin * localY + cos * localX;
        float originHeight = actor.height + pattern.originOffsetHeight.evaluate(actor);

        ProjectileSpawnContext.Builder contextBuilder = ProjectileSpawnContext.builder(actor)
                .cause(ProjectileSpawnContext.Cause.ACTION)
                .recursionDepth(context.recursionDepth())
                .synchronizedTick(GameWorld.tick())
                .targetLeadRange(actor.mutableStats.maxAttackRange);
        context.targetUnit().ifPresent(contextBuilder::targetUnit);
        context.actionTargetPosition().ifPresent(point -> contextBuilder.targetPoint(
                point.x(), point.y(), targetHeight(context)));
        ProjectileSpawnContext spawnContext = contextBuilder.build();

        ProjectileSpawnSpec.Builder spec = ProjectileSpawnSpec.builder(
                        spawnContext, definition.projectile())
                .origin(originX, originY, originHeight)
                .directionDistance(pattern.directionDistance.evaluate(actor));
        switch (pattern.aimMode) {
            case DIRECTION:
                spec.directionTarget(direction);
                break;
            case POINT:
                WorldPoint point = context.actionTargetPosition().orElseThrow(() ->
                        new IllegalArgumentException("POINT CustomProjectile requires an action target point"));
                spec.pointTarget(point.x(), point.y(), targetHeight(context)).direction(direction);
                break;
            case UNIT:
                Unit target = context.targetUnit().orElseThrow(() ->
                        new IllegalArgumentException("UNIT CustomProjectile requires an action target unit"));
                spec.unitTarget(target).direction(direction);
                break;
            default:
                throw new AssertionError(pattern.aimMode);
        }
        ProjectilePatternSpec resolvedPattern = pattern.resolve(actor);
        ProjectilePatternEmitter.emit(spec.build(), resolvedPattern);
    }

    private static float targetHeight(IniActionExecutionContext context) {
        return context.targetUnit().map(unit -> unit.height).orElse(0.0F);
    }
}
