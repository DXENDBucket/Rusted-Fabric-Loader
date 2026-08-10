package io.github.endx.iniessentials.projectile;

import io.github.endx.rustedfabricapi.api.projectile.pattern.ProjectilePatternEmitter;
import io.github.endx.rustedfabricapi.api.projectile.pattern.ProjectilePatternSpec;
import io.github.endx.rustedfabricapi.api.projectile.spawn.ProjectileSpawnContext;
import io.github.endx.rustedfabricapi.api.projectile.spawn.ProjectileSpawnSpec;
import io.github.endx.rustedfabricapi.api.world.GameWorld;
import rustedwarfare.custom.CustomUnit;
import rustedwarfare.unit.Unit;
import rustedwarfare.util.CommonUtils;

/** Shared emission path for unit actions and projectile lifecycle actions. */
final class CustomProjectileEmitter {
    private static final int MAX_RECURSION = 10;

    private CustomProjectileEmitter() { }

    static void emit(CustomProjectileSpawnRequest.Resolved request, CustomUnit actor,
                     float baseOriginX, float baseOriginY, float baseOriginHeight, float direction,
                     Unit targetUnit, boolean hasTargetPoint,
                     float targetX, float targetY, float targetHeight,
                     ProjectileSpawnContext.Cause cause, int recursionDepth) {
        if (recursionDepth > MAX_RECURSION) {
            throw new IllegalArgumentException("CustomProjectile recursion exceeds " + MAX_RECURSION);
        }
        CustomProjectileDefinitions.Definition definition = request.reference.definition();
        CustomProjectileDefinitions.CompiledPattern pattern = definition
                .requirePattern(request.reference.patternName()).compileFor(actor);
        float resolvedDirection = request.valueOr("centerDirection",
                pattern.centerDirection(actor, direction));
        float localX = request.valueOr("originOffsetX", pattern.originOffsetX.evaluate(actor));
        float localY = request.valueOr("originOffsetY", pattern.originOffsetY.evaluate(actor));
        float sin = CommonUtils.fastSin(direction);
        float cos = CommonUtils.fastCos(direction);
        float originX = baseOriginX + cos * localY - sin * localX;
        float originY = baseOriginY + sin * localY + cos * localX;
        float originHeight = baseOriginHeight + request.valueOr("originOffsetHeight",
                pattern.originOffsetHeight.evaluate(actor));

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
                .directionDistance(request.valueOr("directionDistance",
                        pattern.directionDistance.evaluate(actor)));
        io.github.endx.rustedfabricapi.api.projectile.spawn.ProjectileAimMode aimMode =
                request.aimMode != null ? request.aimMode : pattern.aimMode;
        switch (aimMode) {
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
        ProjectilePatternSpec resolvedPattern = ProjectilePatternSpec.builder(pattern.type)
                .count(Math.round(request.valueOr("count", pattern.count.evaluate(actor))))
                .startAngle(request.valueOr("startAngle", pattern.startAngle.evaluate(actor)))
                .sweepAngle(request.valueOr("sweepAngle", pattern.sweepAngle.evaluate(actor)))
                .originSpacing(request.valueOr("originSpacing",
                        pattern.originSpacing.evaluate(actor)))
                .lineAngleOffset(request.valueOr("lineAngleOffset",
                        pattern.lineAngleOffset.evaluate(actor)))
                .build();
        ProjectileSpawnSpec resolvedSpec = spec.build();
        ProjectileSpawnContext spawnContext = resolvedSpec.context();
        CustomProjectileRuntime.beginSpawnOverrides(spawnContext, request);
        try {
            ProjectilePatternEmitter.emit(resolvedSpec, resolvedPattern);
        } finally {
            CustomProjectileRuntime.endSpawnOverrides(spawnContext);
        }
    }
}
