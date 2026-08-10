package io.github.endx.iniessentials.projectile;

import io.github.endx.rustedfabricapi.api.projectile.pattern.ProjectilePatternEmitter;
import io.github.endx.rustedfabricapi.api.projectile.pattern.ProjectilePatternSpec;
import io.github.endx.rustedfabricapi.api.projectile.spawn.ProjectileSpawnContext;
import io.github.endx.rustedfabricapi.api.projectile.spawn.ProjectileSpawnSpec;
import io.github.endx.rustedfabricapi.api.world.GameWorld;
import rustedwarfare.custom.CustomUnit;
import rustedwarfare.unit.Unit;

/** Shared emission path for unit actions and projectile lifecycle actions. */
final class CustomProjectileEmitter {
    private static final int MAX_RECURSION = 10;

    private CustomProjectileEmitter() { }

    static void emit(CustomProjectileDefinitions.Reference reference, CustomUnit actor,
                     float originX, float originY, float originHeight, float direction,
                     Unit targetUnit, boolean hasTargetPoint,
                     float targetX, float targetY, float targetHeight,
                     ProjectileSpawnContext.Cause cause, int recursionDepth) {
        if (recursionDepth > MAX_RECURSION) {
            throw new IllegalArgumentException("CustomProjectile recursion exceeds " + MAX_RECURSION);
        }
        CustomProjectileDefinitions.Definition definition = reference.definition();
        CustomProjectileDefinitions.CompiledPattern pattern = definition
                .requirePattern(reference.patternName()).compileFor(actor);
        float resolvedDirection = pattern.centerDirection(actor, direction);

        ProjectileSpawnContext.Builder context = ProjectileSpawnContext.builder(actor)
                .cause(cause)
                .recursionDepth(recursionDepth)
                .synchronizedTick(GameWorld.tick())
                .targetLeadRange(actor.mutableStats.maxAttackRange);
        if (targetUnit != null) context.targetUnit(targetUnit);
        if (hasTargetPoint) context.targetPoint(targetX, targetY, targetHeight);

        ProjectileSpawnSpec.Builder spec = ProjectileSpawnSpec.builder(
                        context.build(), definition.projectile())
                .origin(originX, originY, originHeight)
                .collision(definition.collision().compileFor(actor).resolve(actor))
                .directionDistance(pattern.directionDistance.evaluate(actor));
        switch (pattern.aimMode) {
            case DIRECTION:
                spec.directionTarget(resolvedDirection);
                break;
            case POINT:
                if (!hasTargetPoint) {
                    throw new IllegalArgumentException(
                            "POINT CustomProjectile requires a target point");
                }
                spec.pointTarget(targetX, targetY, targetHeight).direction(resolvedDirection);
                break;
            case UNIT:
                if (targetUnit == null) {
                    throw new IllegalArgumentException(
                            "UNIT CustomProjectile requires a target unit");
                }
                spec.unitTarget(targetUnit).direction(resolvedDirection);
                break;
            default:
                throw new AssertionError(pattern.aimMode);
        }
        ProjectilePatternSpec resolvedPattern = pattern.resolve(actor);
        ProjectilePatternEmitter.emit(spec.build(), resolvedPattern);
    }
}
