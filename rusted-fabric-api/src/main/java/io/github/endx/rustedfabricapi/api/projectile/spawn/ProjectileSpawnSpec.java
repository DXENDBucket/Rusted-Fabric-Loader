package io.github.endx.rustedfabricapi.api.projectile.spawn;

import rustedwarfare.custom.CustomProjectileTemplate;
import rustedwarfare.unit.Unit;

import java.util.Objects;
import java.util.Optional;

/** Immutable description of one real native projectile to create. */
public final class ProjectileSpawnSpec {
    public static final float DEFAULT_DIRECTION_DISTANCE = 100000.0F;

    private final ProjectileSpawnContext context;
    private final CustomProjectileTemplate template;
    private final ProjectileAimMode aimMode;
    private final float originX;
    private final float originY;
    private final float originHeight;
    private final float direction;
    private final Unit targetUnit;
    private final float targetX;
    private final float targetY;
    private final float targetHeight;
    private final float directionDistance;
    private final int sequenceIndex;

    private ProjectileSpawnSpec(Builder builder) {
        context = Objects.requireNonNull(builder.context, "context");
        template = Objects.requireNonNull(builder.template, "template");
        aimMode = Objects.requireNonNull(builder.aimMode, "aimMode");
        originX = finite(builder.originX, "originX");
        originY = finite(builder.originY, "originY");
        originHeight = finite(builder.originHeight, "originHeight");
        direction = finite(builder.direction, "direction");
        targetUnit = builder.targetUnit;
        targetX = finite(builder.targetX, "targetX");
        targetY = finite(builder.targetY, "targetY");
        targetHeight = finite(builder.targetHeight, "targetHeight");
        directionDistance = finite(builder.directionDistance, "directionDistance");
        if (directionDistance <= 0.0F) {
            throw new IllegalArgumentException("directionDistance must be > 0");
        }
        if (builder.sequenceIndex < 0) throw new IllegalArgumentException("sequenceIndex must be >= 0");
        sequenceIndex = builder.sequenceIndex;
        if (aimMode == ProjectileAimMode.UNIT && targetUnit == null) {
            throw new IllegalArgumentException("UNIT aim requires targetUnit");
        }
    }

    public static Builder builder(ProjectileSpawnContext context,
                                  CustomProjectileTemplate template) {
        return new Builder(context, template);
    }

    public static ProjectileSpawnSpec point(ProjectileSpawnContext context,
                                            CustomProjectileTemplate template,
                                            float originX, float originY, float originHeight,
                                            float targetX, float targetY, float targetHeight) {
        return builder(context, template)
                .origin(originX, originY, originHeight)
                .pointTarget(targetX, targetY, targetHeight)
                .direction(directionTo(originX, originY, targetX, targetY))
                .build();
    }

    public static ProjectileSpawnSpec direction(ProjectileSpawnContext context,
                                                CustomProjectileTemplate template,
                                                float originX, float originY, float originHeight,
                                                float direction) {
        return builder(context, template)
                .origin(originX, originY, originHeight)
                .directionTarget(direction)
                .build();
    }

    public static ProjectileSpawnSpec unit(ProjectileSpawnContext context,
                                           CustomProjectileTemplate template,
                                           float originX, float originY, float originHeight,
                                           Unit targetUnit, float direction) {
        return builder(context, template)
                .origin(originX, originY, originHeight)
                .unitTarget(targetUnit)
                .direction(direction)
                .build();
    }

    public Builder toBuilder() { return new Builder(this); }

    public ProjectileSpawnContext context() { return context; }
    public CustomProjectileTemplate template() { return template; }
    public ProjectileAimMode aimMode() { return aimMode; }
    public float originX() { return originX; }
    public float originY() { return originY; }
    public float originHeight() { return originHeight; }
    public float direction() { return direction; }
    public Optional<Unit> targetUnit() { return Optional.ofNullable(targetUnit); }
    public float targetX() { return targetX; }
    public float targetY() { return targetY; }
    public float targetHeight() { return targetHeight; }
    public float directionDistance() { return directionDistance; }
    public int sequenceIndex() { return sequenceIndex; }

    private static float finite(float value, String name) {
        if (!Float.isFinite(value)) throw new IllegalArgumentException(name + " must be finite");
        return value;
    }

    private static float directionTo(float x, float y, float targetX, float targetY) {
        return (float) StrictMath.toDegrees(StrictMath.atan2(targetY - y, targetX - x));
    }

    public static final class Builder {
        private final ProjectileSpawnContext context;
        private final CustomProjectileTemplate template;
        private ProjectileAimMode aimMode;
        private float originX;
        private float originY;
        private float originHeight;
        private float direction;
        private Unit targetUnit;
        private float targetX;
        private float targetY;
        private float targetHeight;
        private float directionDistance = DEFAULT_DIRECTION_DISTANCE;
        private int sequenceIndex;

        private Builder(ProjectileSpawnContext context, CustomProjectileTemplate template) {
            this.context = Objects.requireNonNull(context, "context");
            this.template = Objects.requireNonNull(template, "template");
            Unit source = context.source();
            originX = source.x;
            originY = source.y;
            originHeight = source.height;
            direction = source.direction;
            targetUnit = context.targetUnit().orElse(null);
            targetX = context.targetX();
            targetY = context.targetY();
            targetHeight = context.targetHeight();
            aimMode = targetUnit != null ? ProjectileAimMode.UNIT
                    : context.hasTargetPoint() ? ProjectileAimMode.POINT
                    : ProjectileAimMode.DIRECTION;
        }

        private Builder(ProjectileSpawnSpec spec) {
            context = spec.context;
            template = spec.template;
            aimMode = spec.aimMode;
            originX = spec.originX;
            originY = spec.originY;
            originHeight = spec.originHeight;
            direction = spec.direction;
            targetUnit = spec.targetUnit;
            targetX = spec.targetX;
            targetY = spec.targetY;
            targetHeight = spec.targetHeight;
            directionDistance = spec.directionDistance;
            sequenceIndex = spec.sequenceIndex;
        }

        public Builder origin(float x, float y, float height) {
            originX = x; originY = y; originHeight = height; return this;
        }
        public Builder direction(float value) { direction = value; return this; }
        public Builder unitTarget(Unit value) {
            aimMode = ProjectileAimMode.UNIT;
            targetUnit = Objects.requireNonNull(value, "targetUnit");
            return this;
        }
        public Builder pointTarget(float x, float y, float height) {
            aimMode = ProjectileAimMode.POINT;
            targetUnit = null;
            targetX = x; targetY = y; targetHeight = height;
            return this;
        }
        public Builder directionTarget(float value) {
            aimMode = ProjectileAimMode.DIRECTION;
            targetUnit = null;
            direction = value;
            return this;
        }
        public Builder directionDistance(float value) { directionDistance = value; return this; }
        public Builder sequenceIndex(int value) { sequenceIndex = value; return this; }
        public ProjectileSpawnSpec build() { return new ProjectileSpawnSpec(this); }
    }
}
