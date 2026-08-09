package io.github.endx.rustedfabricapi.impl.projectile;

import io.github.endx.rustedfabricapi.api.projectile.pattern.ProjectilePatternEmitter;
import io.github.endx.rustedfabricapi.api.projectile.pattern.ProjectilePatternOffset;
import io.github.endx.rustedfabricapi.api.projectile.pattern.TurretProjectilePatternEvents;
import io.github.endx.rustedfabricapi.api.projectile.pattern.TurretProjectilePatternPlan;
import io.github.endx.rustedfabricapi.api.projectile.pattern.TurretProjectilePatternRequest;
import io.github.endx.rustedfabricapi.api.projectile.spawn.ProjectileAimMode;
import io.github.endx.rustedfabricapi.api.projectile.spawn.ProjectileCollisions;
import io.github.endx.rustedfabricapi.api.projectile.spawn.ProjectileSpawnContext;
import io.github.endx.rustedfabricapi.api.projectile.spawn.ProjectileSpawnSpec;
import io.github.endx.rustedfabricapi.api.projectile.spawn.ProjectileSpawner;
import io.github.endx.rustedfabricapi.api.world.GameWorld;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;
import rustedwarfare.custom.CustomProjectileTemplate;
import rustedwarfare.custom.CustomUnit;
import rustedwarfare.game.Projectile;
import rustedwarfare.unit.Unit;
import rustedwarfare.util.CommonUtils;

import java.util.List;

/** Internal state bridge for the tightly scoped native turret firing weave. */
public final class TurretProjectilePatternRuntime {
    private static final ThreadLocal<Frame> ACTIVE = new ThreadLocal<Frame>();

    private TurretProjectilePatternRuntime() { }

    public static CustomProjectileTemplate selectTemplate(
            CustomUnit shooter, Unit targetUnit, float targetX, float targetY,
            int turretIndex, CustomProjectileTemplate nativeTemplate, int projectileCount) {
        ACTIVE.remove();
        TurretProjectilePatternRequest request = new TurretProjectilePatternRequest(
                shooter, targetUnit, targetX, targetY, turretIndex,
                nativeTemplate, projectileCount);
        TurretProjectilePatternEvents.PLAN.invoker().plan(request);
        TurretProjectilePatternPlan plan = request.replacement().orElse(null);
        if (plan == null) return nativeTemplate;
        ACTIVE.set(new Frame(request, plan));
        return plan.template();
    }

    /** Adjusts the raw native projectile allocation to the first pattern origin. */
    public static void modifyCreateArguments(Args args) {
        Frame frame = ACTIVE.get();
        if (frame == null) return;
        frame.baseOriginX = ((Float) args.get(1)).floatValue() + frame.plan.originOffsetX();
        frame.baseOriginY = ((Float) args.get(2)).floatValue() + frame.plan.originOffsetY();
        frame.baseOriginHeight = ((Float) args.get(3)).floatValue()
                + frame.plan.originOffsetHeight();
        ProjectilePatternOffset first = frame.offsets.get(0);
        args.set(1, Float.valueOf(frame.baseOriginX + first.originOffsetX()));
        args.set(2, Float.valueOf(frame.baseOriginY + first.originOffsetY()));
        args.set(3, Float.valueOf(frame.baseOriginHeight));
    }

    /** Applies the first pattern direction and replacement template to native initialization. */
    public static void modifyTemplateArguments(Args args) {
        Frame frame = ACTIVE.get();
        if (frame == null) return;
        boolean reusedNativeProjectile = !hasOrigin(frame);
        if (reusedNativeProjectile) {
            // instantReuseLast can skip createProjectileWithHeightAndTurret entirely.
            // The apply call still carries the native muzzle, so use it as the same
            // stable anchor rather than rejecting an otherwise valid native path.
            frame.baseOriginX = ((Float) args.get(4)).floatValue()
                    + frame.plan.originOffsetX();
            frame.baseOriginY = ((Float) args.get(5)).floatValue()
                    + frame.plan.originOffsetY();
            frame.baseOriginHeight = ((Float) args.get(6)).floatValue()
                    + frame.plan.originOffsetHeight();
        }
        ProjectilePatternOffset first = frame.offsets.get(0);
        float firstX = frame.baseOriginX + first.originOffsetX();
        float firstY = frame.baseOriginY + first.originOffsetY();
        if (reusedNativeProjectile) {
            ((Projectile) args.get(0)).setSourceAndPosition(
                    (Unit) args.get(1), firstX, firstY, frame.baseOriginHeight);
        }
        ProjectileCollisions.apply((Projectile) args.get(0), frame.plan.collision());
        args.set(3, frame.plan.template());
        args.set(4, Float.valueOf(firstX));
        args.set(5, Float.valueOf(firstY));
        args.set(6, Float.valueOf(frame.baseOriginHeight));
        args.set(7, Float.valueOf(frame.plan.centerDirection()
                + first.directionOffset()));
    }

    /** Initializes the native primary projectile, then emits only pattern entries after index zero. */
    public static void applyCreatedEffects(CustomProjectileTemplate template, Unit source,
                                           Projectile projectile, Unit nativeTarget,
                                           float nativeTargetX, float nativeTargetY,
                                           float targetLeadRange) {
        Frame frame = ACTIVE.get();
        if (frame == null) {
            template.applyOnProjectileCreatedEffects(source, projectile, nativeTarget,
                    nativeTargetX, nativeTargetY, targetLeadRange);
            return;
        }
        requireOrigin(frame);
        ProjectilePatternOffset first = frame.offsets.get(0);
        float primaryDirection = frame.plan.centerDirection() + first.directionOffset();
        Unit effectiveTarget = nativeTarget;
        float effectiveTargetX = nativeTargetX;
        float effectiveTargetY = nativeTargetY;
        switch (frame.plan.aimMode()) {
            case DIRECTION:
                effectiveTarget = null;
                effectiveTargetX = frame.baseOriginX + first.originOffsetX()
                        + CommonUtils.fastCos(primaryDirection)
                        * frame.plan.directionDistance();
                effectiveTargetY = frame.baseOriginY + first.originOffsetY()
                        + CommonUtils.fastSin(primaryDirection)
                        * frame.plan.directionDistance();
                break;
            case POINT:
                effectiveTarget = null;
                break;
            case UNIT:
                if (effectiveTarget == null) {
                    throw new IllegalArgumentException(
                            "UNIT turret CustomProjectile requires a target unit");
                }
                break;
            default:
                throw new AssertionError(frame.plan.aimMode());
        }
        frame.plan.template().applyOnProjectileCreatedEffects(source, projectile,
                effectiveTarget, effectiveTargetX, effectiveTargetY, targetLeadRange);
        emitRemaining(frame, source, projectile.targetHeight, targetLeadRange);
    }

    public static void clear() { ACTIVE.remove(); }

    private static void emitRemaining(Frame frame, Unit source, float targetHeight,
                                      float targetLeadRange) {
        if (frame.offsets.size() <= 1) return;
        ProjectileSpawnContext.Builder context = ProjectileSpawnContext.builder(source)
                .cause(ProjectileSpawnContext.Cause.TURRET)
                .turretIndex(frame.request.turretIndex())
                .recursionDepth(frame.request.projectileCount())
                .synchronizedTick(GameWorld.tick())
                .targetLeadRange(targetLeadRange);
        frame.request.targetUnit().ifPresent(context::targetUnit);
        if (frame.plan.aimMode() == ProjectileAimMode.POINT) {
            context.targetPoint(frame.request.targetX(), frame.request.targetY(), targetHeight);
        }

        ProjectileSpawnSpec.Builder base = ProjectileSpawnSpec.builder(
                        context.build(), frame.plan.template())
                .origin(frame.baseOriginX, frame.baseOriginY, frame.baseOriginHeight)
                .direction(frame.plan.centerDirection())
                .collision(frame.plan.collision())
                .directionDistance(frame.plan.directionDistance());
        switch (frame.plan.aimMode()) {
            case DIRECTION:
                base.directionTarget(frame.plan.centerDirection());
                break;
            case POINT:
                base.pointTarget(frame.request.targetX(), frame.request.targetY(), targetHeight);
                break;
            case UNIT:
                base.unitTarget(frame.request.targetUnit().orElseThrow(() ->
                        new IllegalArgumentException(
                                "UNIT turret CustomProjectile requires a target unit")));
                break;
            default:
                throw new AssertionError(frame.plan.aimMode());
        }
        List<ProjectileSpawnSpec> expanded = ProjectilePatternEmitter.expand(
                base.build(), frame.plan.pattern());
        for (int i = 1; i < expanded.size(); i++) {
            ProjectileSpawner.spawn(expanded.get(i));
        }
    }

    private static void requireOrigin(Frame frame) {
        if (!hasOrigin(frame)) {
            throw new IllegalStateException(
                    "turret projectile pattern create hook did not capture the native muzzle");
        }
    }

    private static boolean hasOrigin(Frame frame) {
        return Float.isFinite(frame.baseOriginX) && Float.isFinite(frame.baseOriginY)
                && Float.isFinite(frame.baseOriginHeight);
    }

    private static final class Frame {
        final TurretProjectilePatternRequest request;
        final TurretProjectilePatternPlan plan;
        final List<ProjectilePatternOffset> offsets;
        float baseOriginX = Float.NaN;
        float baseOriginY = Float.NaN;
        float baseOriginHeight = Float.NaN;

        Frame(TurretProjectilePatternRequest request, TurretProjectilePatternPlan plan) {
            this.request = request;
            this.plan = plan;
            offsets = ProjectilePatternEmitter.offsets(
                    plan.pattern(), plan.centerDirection());
        }
    }
}
