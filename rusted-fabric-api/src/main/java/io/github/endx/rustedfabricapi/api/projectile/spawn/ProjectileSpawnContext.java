package io.github.endx.rustedfabricapi.api.projectile.spawn;

import rustedwarfare.unit.Unit;

import java.util.Objects;
import java.util.Optional;

/** Immutable source and target context shared by one projectile or an expanded pattern. */
public final class ProjectileSpawnContext {
    public enum Cause { TURRET, ACTION, PROJECTILE, JAVA }

    private final Unit source;
    private final Unit targetUnit;
    private final boolean hasTargetPoint;
    private final float targetX;
    private final float targetY;
    private final float targetHeight;
    private final int turretIndex;
    private final int actionIndex;
    private final int recursionDepth;
    private final int synchronizedTick;
    private final float targetLeadRange;
    private final Cause cause;

    private ProjectileSpawnContext(Builder builder) {
        source = Objects.requireNonNull(builder.source, "source");
        targetUnit = builder.targetUnit;
        hasTargetPoint = builder.hasTargetPoint;
        targetX = finite(builder.targetX, "targetX");
        targetY = finite(builder.targetY, "targetY");
        targetHeight = finite(builder.targetHeight, "targetHeight");
        if (builder.turretIndex < -1) throw new IllegalArgumentException("turretIndex must be >= -1");
        if (builder.actionIndex < -1) throw new IllegalArgumentException("actionIndex must be >= -1");
        if (builder.recursionDepth < 0) throw new IllegalArgumentException("recursionDepth must be >= 0");
        turretIndex = builder.turretIndex;
        actionIndex = builder.actionIndex;
        recursionDepth = builder.recursionDepth;
        synchronizedTick = builder.synchronizedTick;
        targetLeadRange = finite(builder.targetLeadRange, "targetLeadRange");
        cause = Objects.requireNonNull(builder.cause, "cause");
    }

    public static Builder builder(Unit source) { return new Builder(source); }

    public Unit source() { return source; }
    public Optional<Unit> targetUnit() { return Optional.ofNullable(targetUnit); }
    public boolean hasTargetPoint() { return hasTargetPoint; }
    public float targetX() { return targetX; }
    public float targetY() { return targetY; }
    public float targetHeight() { return targetHeight; }
    public int turretIndex() { return turretIndex; }
    public int actionIndex() { return actionIndex; }
    public int recursionDepth() { return recursionDepth; }
    public int synchronizedTick() { return synchronizedTick; }
    public float targetLeadRange() { return targetLeadRange; }
    public Cause cause() { return cause; }

    private static float finite(float value, String name) {
        if (!Float.isFinite(value)) throw new IllegalArgumentException(name + " must be finite");
        return value;
    }

    public static final class Builder {
        private final Unit source;
        private Unit targetUnit;
        private boolean hasTargetPoint;
        private float targetX;
        private float targetY;
        private float targetHeight;
        private int turretIndex = -1;
        private int actionIndex = -1;
        private int recursionDepth;
        private int synchronizedTick = -1;
        private float targetLeadRange;
        private Cause cause = Cause.JAVA;

        private Builder(Unit source) { this.source = Objects.requireNonNull(source, "source"); }

        public Builder targetUnit(Unit value) { targetUnit = value; return this; }
        public Builder targetPoint(float x, float y) { return targetPoint(x, y, 0.0F); }
        public Builder targetPoint(float x, float y, float height) {
            hasTargetPoint = true;
            targetX = x;
            targetY = y;
            targetHeight = height;
            return this;
        }
        public Builder noTargetPoint() { hasTargetPoint = false; return this; }
        public Builder turretIndex(int value) { turretIndex = value; return this; }
        public Builder actionIndex(int value) { actionIndex = value; return this; }
        public Builder recursionDepth(int value) { recursionDepth = value; return this; }
        public Builder synchronizedTick(int value) { synchronizedTick = value; return this; }
        public Builder targetLeadRange(float value) { targetLeadRange = value; return this; }
        public Builder cause(Cause value) { cause = value; return this; }
        public ProjectileSpawnContext build() { return new ProjectileSpawnContext(this); }
    }
}
