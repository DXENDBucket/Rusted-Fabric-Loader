package io.github.endx.rustedfabricapi.api.projectile.pattern;

import io.github.endx.rustedfabricapi.api.projectile.spawn.ProjectileAimMode;
import io.github.endx.rustedfabricapi.api.projectile.spawn.ProjectileSpawner;
import io.github.endx.rustedfabricapi.api.projectile.spawn.ProjectileSpawnSpec;
import rustedwarfare.game.Projectile;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/** Pure deterministic pattern expansion plus an optional native emission step. */
public final class ProjectilePatternEmitter {
    private ProjectilePatternEmitter() { }

    public static List<ProjectilePatternOffset> offsets(ProjectilePatternSpec pattern,
                                                        float baseDirection) {
        ProjectilePatternSpec checked = Objects.requireNonNull(pattern, "pattern");
        if (!Float.isFinite(baseDirection)) {
            throw new IllegalArgumentException("baseDirection must be finite");
        }
        ArrayList<ProjectilePatternOffset> result =
                new ArrayList<ProjectilePatternOffset>(checked.count());
        switch (checked.type()) {
            case SINGLE:
                result.add(new ProjectilePatternOffset(0, 0.0F, 0.0F, 0.0F));
                break;
            case FAN:
                for (int i = 0; i < checked.count(); i++) {
                    float angle = checked.count() == 1 ? 0.0F
                            : -checked.sweepAngle() * 0.5F
                            + checked.sweepAngle() * i / (checked.count() - 1.0F);
                    result.add(new ProjectilePatternOffset(i, angle, 0.0F, 0.0F));
                }
                break;
            case RING:
                for (int i = 0; i < checked.count(); i++) {
                    boolean fullCircle = StrictMath.abs(checked.sweepAngle()) >= 360.0F;
                    float divisor = fullCircle ? checked.count() : checked.count() - 1.0F;
                    float angle = checked.startAngle() + (checked.count() == 1 ? 0.0F
                            : checked.sweepAngle() * i / divisor);
                    result.add(new ProjectilePatternOffset(i, angle, 0.0F, 0.0F));
                }
                break;
            case LINE:
                float axis = baseDirection + checked.lineAngleOffset();
                float radians = (float) StrictMath.toRadians(axis);
                float cos = (float) StrictMath.cos(radians);
                float sin = (float) StrictMath.sin(radians);
                float center = (checked.count() - 1.0F) * 0.5F;
                for (int i = 0; i < checked.count(); i++) {
                    float distance = (i - center) * checked.originSpacing();
                    result.add(new ProjectilePatternOffset(i, 0.0F,
                            cos * distance, sin * distance));
                }
                break;
            default:
                throw new AssertionError(checked.type());
        }
        return Collections.unmodifiableList(result);
    }

    public static List<ProjectileSpawnSpec> expand(ProjectileSpawnSpec base,
                                                   ProjectilePatternSpec pattern) {
        ProjectileSpawnSpec checked = Objects.requireNonNull(base, "base");
        ProjectilePatternSpec checkedPattern = Objects.requireNonNull(pattern, "pattern");
        if ((checkedPattern.type() == ProjectilePatternType.FAN
                || checkedPattern.type() == ProjectilePatternType.RING)
                && checked.aimMode() != ProjectileAimMode.DIRECTION) {
            throw new IllegalArgumentException(checkedPattern.type()
                    + " requires a DIRECTION-aimed base projectile");
        }
        ArrayList<ProjectileSpawnSpec> result = new ArrayList<ProjectileSpawnSpec>();
        for (ProjectilePatternOffset offset : offsets(checkedPattern, checked.direction())) {
            result.add(checked.toBuilder()
                    .origin(checked.originX() + offset.originOffsetX(),
                            checked.originY() + offset.originOffsetY(),
                            checked.originHeight())
                    .direction(checked.direction() + offset.directionOffset())
                    .sequenceIndex(offset.index())
                    .build());
        }
        return Collections.unmodifiableList(result);
    }

    /** Emits every expanded projectile immediately in stable increasing-index order. */
    public static List<Projectile> emit(ProjectileSpawnSpec base,
                                        ProjectilePatternSpec pattern) {
        ArrayList<Projectile> result = new ArrayList<Projectile>();
        for (ProjectileSpawnSpec spec : expand(base, pattern)) {
            ProjectileSpawner.spawn(spec).ifPresent(result::add);
        }
        return Collections.unmodifiableList(result);
    }
}
