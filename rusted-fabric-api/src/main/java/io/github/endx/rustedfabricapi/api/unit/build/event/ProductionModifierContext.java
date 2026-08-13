package io.github.endx.rustedfabricapi.api.unit.build.event;

import io.github.endx.rustedfabricapi.api.game.UnitView;

import java.util.Objects;

/** Mutable price and build-time view for one unit-production action. */
public final class ProductionModifierContext {
    private final UnitView producer;
    private final String producedUnitType;
    private final int originalCreditCost;
    private final float originalBuildSpeedMultiplier;
    private int creditCost;
    private float buildSpeedMultiplier;

    public ProductionModifierContext(UnitView producer, String producedUnitType,
                                     int creditCost, float buildSpeedMultiplier) {
        this.producer = Objects.requireNonNull(producer, "producer");
        this.producedUnitType = Objects.requireNonNull(producedUnitType, "producedUnitType");
        if (creditCost < 0) throw new IllegalArgumentException("creditCost must not be negative");
        requirePositiveFinite(buildSpeedMultiplier, "buildSpeedMultiplier");
        this.originalCreditCost = creditCost;
        this.originalBuildSpeedMultiplier = buildSpeedMultiplier;
        this.creditCost = creditCost;
        this.buildSpeedMultiplier = buildSpeedMultiplier;
    }

    public UnitView producer() { return producer; }
    public String producedUnitType() { return producedUnitType; }
    public int originalCreditCost() { return originalCreditCost; }
    public float originalBuildSpeedMultiplier() { return originalBuildSpeedMultiplier; }
    public int creditCost() { return creditCost; }
    public float buildSpeedMultiplier() { return buildSpeedMultiplier; }

    public void setCreditCost(int value) {
        if (value < 0) throw new IllegalArgumentException("credit cost must not be negative");
        creditCost = value;
    }

    public void addCreditCost(int delta) {
        setCreditCost(Math.max(0, Math.addExact(creditCost, delta)));
    }

    public void setBuildSpeedMultiplier(float value) {
        requirePositiveFinite(value, "build speed multiplier");
        buildSpeedMultiplier = value;
    }

    /** Multiplies build duration; values below one make production faster. */
    public void multiplyBuildTime(double factor) {
        if (!Double.isFinite(factor) || factor <= 0.0D) {
            throw new IllegalArgumentException("build time factor must be finite and positive");
        }
        double changed = buildSpeedMultiplier / factor;
        if (!Double.isFinite(changed) || changed > Float.MAX_VALUE) {
            throw new IllegalArgumentException("resulting build speed is out of range");
        }
        buildSpeedMultiplier = (float) changed;
    }

    private static void requirePositiveFinite(float value, String name) {
        if (!Float.isFinite(value) || value <= 0.0F) {
            throw new IllegalArgumentException(name + " must be finite and positive");
        }
    }
}
