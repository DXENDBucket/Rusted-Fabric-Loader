package io.github.endx.rustedfabricapi.api.projectile.pattern;

import rustedwarfare.custom.CustomProjectileTemplate;
import rustedwarfare.custom.CustomUnit;
import rustedwarfare.unit.Unit;

import java.util.Objects;
import java.util.Optional;

/** Mutable request fired after the native turret projectile template has been selected. */
public final class TurretProjectilePatternRequest {
    private final CustomUnit shooter;
    private final Unit targetUnit;
    private final float targetX;
    private final float targetY;
    private final int turretIndex;
    private final CustomProjectileTemplate nativeTemplate;
    private final int projectileCount;
    private TurretProjectilePatternPlan replacement;

    public TurretProjectilePatternRequest(CustomUnit shooter, Unit targetUnit,
                                          float targetX, float targetY, int turretIndex,
                                          CustomProjectileTemplate nativeTemplate,
                                          int projectileCount) {
        this.shooter = Objects.requireNonNull(shooter, "shooter");
        this.targetUnit = targetUnit;
        this.targetX = finite(targetX, "targetX");
        this.targetY = finite(targetY, "targetY");
        if (turretIndex < 0) throw new IllegalArgumentException("turretIndex must be >= 0");
        this.turretIndex = turretIndex;
        this.nativeTemplate = Objects.requireNonNull(nativeTemplate, "nativeTemplate");
        if (projectileCount < 0) {
            throw new IllegalArgumentException("projectileCount must be >= 0");
        }
        this.projectileCount = projectileCount;
    }

    public CustomUnit shooter() { return shooter; }
    public Optional<Unit> targetUnit() { return Optional.ofNullable(targetUnit); }
    public float targetX() { return targetX; }
    public float targetY() { return targetY; }
    public int turretIndex() { return turretIndex; }
    public CustomProjectileTemplate nativeTemplate() { return nativeTemplate; }
    public int projectileCount() { return projectileCount; }
    public Optional<TurretProjectilePatternPlan> replacement() {
        return Optional.ofNullable(replacement);
    }

    /** Replaces the one native projectile while keeping the surrounding native firing method. */
    public void replace(TurretProjectilePatternPlan value) {
        replacement = Objects.requireNonNull(value, "replacement");
    }

    private static float finite(float value, String name) {
        if (!Float.isFinite(value)) throw new IllegalArgumentException(name + " must be finite");
        return value;
    }
}
