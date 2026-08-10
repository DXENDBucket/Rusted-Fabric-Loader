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

/** Action entrypoint for independent CustomProjectile patterns. */
public final class CustomProjectileActionFields {
    private CustomProjectileActionFields() { }

    public static void register() {
        IniActionEffects.register(IniActionEffectDefinition
                .<CustomProjectileSpawnRequest.Compiled>builder(
                        IniEssentials.MOD_ID, "spawn_custom_projectile", "spawnCustomProjectile")
                .decoder(context -> {
                    CustomProjectileSpawnRequest request =
                            CustomProjectileSpawnRequest.parse(context.rawValue());
                    CustomProjectileDefinitions.noteReference(request.reference);
                    IniEssentials.activateSynchronizedRequirement();
                    return request.compileForUnit(context.metadata());
                })
                .handler(CustomProjectileActionFields::emit)
                .documentation(new IniFieldDocumentation(
                        "namespace:path[/pattern][(name=expression,...)]",
                        "Spawns a CustomProjectile pattern with optional per-emission pattern, origin, and motion overrides.",
                        "生成 CustomProjectile 弹幕，并可为本次发射覆盖弹幕、位置与运动参数。",
                        "spawnCustomProjectile: example:plasma/main(centerDirection=self.dir+15,speed=8)",
                        IniMultiplayerImpact.GAMEPLAY_SYNCED))
                .build());
    }

    private static void emit(IniActionExecutionContext context,
                             CustomProjectileSpawnRequest.Compiled request) {
        CustomUnit actor = context.actor();
        WorldPoint point = context.actionTargetPosition().orElse(null);
        CustomProjectileEmitter.emit(request.resolve(actor, null), actor,
                ((Unit) actor).x, ((Unit) actor).y, actor.height,
                actor.direction, context.targetUnit().orElse(null), point != null,
                point != null ? point.x() : 0.0F, point != null ? point.y() : 0.0F,
                targetHeight(context), ProjectileSpawnContext.Cause.ACTION,
                context.recursionDepth());
    }

    private static float targetHeight(IniActionExecutionContext context) {
        return context.targetUnit().map(unit -> unit.height).orElse(0.0F);
    }
}
