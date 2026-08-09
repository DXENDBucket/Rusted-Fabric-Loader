package io.github.endx.rustedfabricapi.api.projectile.spawn;

import rustedwarfare.custom.CustomProjectileTemplate;
import rustedwarfare.custom.CustomUnit;
import rustedwarfare.game.Projectile;
import rustedwarfare.unit.Unit;
import rustedwarfare.util.CommonUtils;

import java.util.Objects;
import java.util.Optional;

/** Creates normal native projectiles from explicit point, unit, or direction specs. */
public final class ProjectileSpawner {
    private ProjectileSpawner() { }

    /**
     * Creates and fully initializes one native projectile, unless a before-spawn listener cancels.
     * Must run on the deterministic game update thread.
     */
    public static Optional<Projectile> spawn(ProjectileSpawnSpec spec) {
        ProjectileSpawnRequest request = new ProjectileSpawnRequest(
                Objects.requireNonNull(spec, "spec"));
        ProjectileSpawnEvents.BEFORE_SPAWN.invoker().beforeSpawn(request);
        if (request.cancelled()) return Optional.empty();

        ProjectileSpawnSpec current = Objects.requireNonNull(request.spec(), "final spec");
        ProjectileSpawnContext context = current.context();
        Unit source = context.source();
        CustomProjectileTemplate template = current.template();

        Projectile projectile = CustomUnit.createProjectileFromTemplate(
                source, context.turretIndex(), template,
                current.originX(), current.originY(), current.originHeight(),
                current.direction());

        Unit target = null;
        float targetX;
        float targetY;
        float targetHeight;
        switch (current.aimMode()) {
            case UNIT:
                target = current.targetUnit().orElseThrow(() ->
                        new IllegalArgumentException("UNIT aim requires targetUnit"));
                targetX = target.x;
                targetY = target.y;
                targetHeight = target.height;
                break;
            case POINT:
                targetX = current.targetX();
                targetY = current.targetY();
                targetHeight = current.targetHeight();
                break;
            case DIRECTION:
                float cos = CommonUtils.fastCos(current.direction());
                float sin = CommonUtils.fastSin(current.direction());
                targetX = current.originX() + cos * current.directionDistance();
                targetY = current.originY() + sin * current.directionDistance();
                targetHeight = current.originHeight();
                break;
            default:
                throw new AssertionError(current.aimMode());
        }

        template.applyOnProjectileCreatedEffects(source, projectile, target,
                targetX, targetY, context.targetLeadRange());
        if (target == null) projectile.targetHeight = targetHeight;
        ProjectileSpawnEvents.AFTER_SPAWN.invoker().afterSpawn(projectile, current);
        return Optional.of(projectile);
    }
}
