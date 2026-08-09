package io.github.endx.rustedfabricapi.api.projectile.pattern;

import java.util.Objects;

/** Immutable parameters for one bounded, same-tick projectile pattern. */
public final class ProjectilePatternSpec {
    public static final int MAX_PROJECTILES = 1024;

    private final ProjectilePatternType type;
    private final int count;
    private final float startAngle;
    private final float sweepAngle;
    private final float originSpacing;
    private final float lineAngleOffset;

    private ProjectilePatternSpec(Builder builder) {
        type = Objects.requireNonNull(builder.type, "type");
        if (builder.count < 1 || builder.count > MAX_PROJECTILES) {
            throw new IllegalArgumentException("count must be in 1.." + MAX_PROJECTILES);
        }
        count = builder.count;
        startAngle = finite(builder.startAngle, "startAngle");
        sweepAngle = finite(builder.sweepAngle, "sweepAngle");
        originSpacing = finite(builder.originSpacing, "originSpacing");
        lineAngleOffset = finite(builder.lineAngleOffset, "lineAngleOffset");
        if (originSpacing < 0.0F) throw new IllegalArgumentException("originSpacing must be >= 0");
        if (type == ProjectilePatternType.SINGLE && count != 1) {
            throw new IllegalArgumentException("SINGLE pattern count must be 1");
        }
    }

    public static ProjectilePatternSpec single() {
        return builder(ProjectilePatternType.SINGLE).build();
    }

    public static ProjectilePatternSpec fan(int count, float sweepAngle) {
        return builder(ProjectilePatternType.FAN).count(count).sweepAngle(sweepAngle).build();
    }

    public static ProjectilePatternSpec ring(int count, float startAngle) {
        return ring(count, startAngle, 360.0F);
    }

    public static ProjectilePatternSpec ring(int count, float startAngle, float sweepAngle) {
        return builder(ProjectilePatternType.RING).count(count).startAngle(startAngle)
                .sweepAngle(sweepAngle).build();
    }

    public static ProjectilePatternSpec line(int count, float originSpacing) {
        return builder(ProjectilePatternType.LINE).count(count).originSpacing(originSpacing).build();
    }

    public static Builder builder(ProjectilePatternType type) { return new Builder(type); }

    public ProjectilePatternType type() { return type; }
    public int count() { return count; }
    public float startAngle() { return startAngle; }
    public float sweepAngle() { return sweepAngle; }
    public float originSpacing() { return originSpacing; }
    public float lineAngleOffset() { return lineAngleOffset; }

    private static float finite(float value, String name) {
        if (!Float.isFinite(value)) throw new IllegalArgumentException(name + " must be finite");
        return value;
    }

    public static final class Builder {
        private final ProjectilePatternType type;
        private int count = 1;
        private float startAngle;
        private float sweepAngle;
        private float originSpacing;
        private float lineAngleOffset = 90.0F;

        private Builder(ProjectilePatternType type) {
            this.type = Objects.requireNonNull(type, "type");
            if (type == ProjectilePatternType.RING) sweepAngle = 360.0F;
        }

        public Builder count(int value) { count = value; return this; }
        /** Ring offset from the base direction. */
        public Builder startAngle(float value) { startAngle = value; return this; }
        /** Total fan angle, centered on the base direction. */
        public Builder sweepAngle(float value) { sweepAngle = value; return this; }
        public Builder originSpacing(float value) { originSpacing = value; return this; }
        /** Direction of the line axis relative to the base direction; 90 is unit-local right. */
        public Builder lineAngleOffset(float value) { lineAngleOffset = value; return this; }
        public ProjectilePatternSpec build() { return new ProjectilePatternSpec(this); }
    }
}
