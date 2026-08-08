package io.github.endx.rustedfabricapi.api.unit.attribute;

import rustedwarfare.unit.Unit;

import java.util.Objects;

/** Immutable snapshot of the common runtime values present on every native unit. */
public final class UnitVitalsSnapshot {
    private final Unit unit;
    private final float health;
    private final float maxHealth;
    private final float shield;
    private final float maxShield;
    private final float energy;

    private UnitVitalsSnapshot(Unit unit) {
        this.unit = Objects.requireNonNull(unit, "unit");
        this.health = unit.hp;
        this.maxHealth = unit.maxHp;
        this.shield = unit.shield;
        this.maxShield = unit.maxShield;
        this.energy = unit.energy;
    }

    public static UnitVitalsSnapshot capture(Unit unit) {
        return new UnitVitalsSnapshot(unit);
    }

    public Unit unit() { return unit; }
    public float health() { return health; }
    public float maxHealth() { return maxHealth; }
    public float shield() { return shield; }
    public float maxShield() { return maxShield; }
    public float energy() { return energy; }
    public float healthFraction() { return fraction(health, maxHealth); }
    public float shieldFraction() { return fraction(shield, maxShield); }

    private static float fraction(float value, float maximum) {
        return maximum > 0.0f ? value / maximum : 0.0f;
    }
}
