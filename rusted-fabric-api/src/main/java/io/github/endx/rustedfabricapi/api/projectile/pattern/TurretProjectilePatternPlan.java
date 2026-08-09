package io.github.endx.rustedfabricapi.api.projectile.pattern;

import io.github.endx.rustedfabricapi.api.projectile.spawn.ProjectileAimMode;
import io.github.endx.rustedfabricapi.api.projectile.spawn.ProjectileCollisionSpec;
import rustedwarfare.custom.CustomProjectileTemplate;

import java.util.Objects;

/** Immutable replacement plan for one native turret shot. */
public final class TurretProjectilePatternPlan {
    private final CustomProjectileTemplate template;
    private final ProjectilePatternSpec pattern;
    private final ProjectileAimMode aimMode;
    private final float centerDirection;
    private final float originOffsetX;
    private final float originOffsetY;
    private final float originOffsetHeight;
    private final float directionDistance;
    private final ProjectileCollisionSpec collision;

    private TurretProjectilePatternPlan(Builder builder) {
        template = Objects.requireNonNull(builder.template, "template");
        pattern = Objects.requireNonNull(builder.pattern, "pattern");
        aimMode = Objects.requireNonNull(builder.aimMode, "aimMode");
        centerDirection = finite(builder.centerDirection, "centerDirection");
        originOffsetX = finite(builder.originOffsetX, "originOffsetX");
        originOffsetY = finite(builder.originOffsetY, "originOffsetY");
        originOffsetHeight = finite(builder.originOffsetHeight, "originOffsetHeight");
        directionDistance = finite(builder.directionDistance, "directionDistance");
        collision = Objects.requireNonNull(builder.collision, "collision");
        if (directionDistance <= 0.0F) {
            throw new IllegalArgumentException("directionDistance must be > 0");
        }
        if ((pattern.type() == ProjectilePatternType.FAN
                || pattern.type() == ProjectilePatternType.RING)
                && aimMode != ProjectileAimMode.DIRECTION) {
            throw new IllegalArgumentException(pattern.type()
                    + " turret pattern requires DIRECTION aim");
        }
    }

    public static Builder builder(CustomProjectileTemplate template,
                                  ProjectilePatternSpec pattern) {
        return new Builder(template, pattern);
    }

    public CustomProjectileTemplate template() { return template; }
    public ProjectilePatternSpec pattern() { return pattern; }
    public ProjectileAimMode aimMode() { return aimMode; }
    public float centerDirection() { return centerDirection; }
    /** World-axis X offset applied to the native turret muzzle. */
    public float originOffsetX() { return originOffsetX; }
    /** World-axis Y offset applied to the native turret muzzle. */
    public float originOffsetY() { return originOffsetY; }
    public float originOffsetHeight() { return originOffsetHeight; }
    public float directionDistance() { return directionDistance; }
    public ProjectileCollisionSpec collision() { return collision; }

    private static float finite(float value, String name) {
        if (!Float.isFinite(value)) throw new IllegalArgumentException(name + " must be finite");
        return value;
    }

    public static final class Builder {
        private final CustomProjectileTemplate template;
        private final ProjectilePatternSpec pattern;
        private ProjectileAimMode aimMode = ProjectileAimMode.DIRECTION;
        private float centerDirection;
        private float originOffsetX;
        private float originOffsetY;
        private float originOffsetHeight;
        private float directionDistance = 100000.0F;
        private ProjectileCollisionSpec collision = ProjectileCollisionSpec.none();

        private Builder(CustomProjectileTemplate template, ProjectilePatternSpec pattern) {
            this.template = Objects.requireNonNull(template, "template");
            this.pattern = Objects.requireNonNull(pattern, "pattern");
        }

        public Builder aimMode(ProjectileAimMode value) { aimMode = value; return this; }
        public Builder centerDirection(float value) { centerDirection = value; return this; }
        public Builder originOffset(float x, float y, float height) {
            originOffsetX = x; originOffsetY = y; originOffsetHeight = height; return this;
        }
        public Builder directionDistance(float value) { directionDistance = value; return this; }
        public Builder collision(ProjectileCollisionSpec value) {
            collision = Objects.requireNonNull(value, "collision"); return this;
        }
        public TurretProjectilePatternPlan build() {
            return new TurretProjectilePatternPlan(this);
        }
    }
}
