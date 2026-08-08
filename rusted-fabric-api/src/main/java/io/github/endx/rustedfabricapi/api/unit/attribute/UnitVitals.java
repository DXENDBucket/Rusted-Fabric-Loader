package io.github.endx.rustedfabricapi.api.unit.attribute;

import rustedwarfare.unit.Unit;

import java.util.Objects;

/** Common runtime-value helpers that work for built-in and custom units. */
public final class UnitVitals {
    private UnitVitals() {
    }

    public static UnitVitalsSnapshot snapshot(Unit unit) {
        return UnitVitalsSnapshot.capture(unit);
    }

    /** Uses the game's health setter so native health bookkeeping remains active. */
    public static void setHealth(Unit unit, float health) {
        requireFinite(health, "health");
        Objects.requireNonNull(unit, "unit").setHp(health);
    }

    public static void setShield(Unit unit, float shield) {
        requireFinite(shield, "shield");
        Objects.requireNonNull(unit, "unit").shield = shield;
    }

    public static void setEnergy(Unit unit, float energy) {
        requireFinite(energy, "energy");
        Objects.requireNonNull(unit, "unit").energy = energy;
    }

    private static void requireFinite(float value, String name) {
        if (!Float.isFinite(value)) {
            throw new IllegalArgumentException(name + " must be finite");
        }
    }
}
