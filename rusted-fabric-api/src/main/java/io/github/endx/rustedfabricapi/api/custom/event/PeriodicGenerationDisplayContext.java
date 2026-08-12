package io.github.endx.rustedfabricapi.api.custom.event;

import io.github.endx.rustedfabricapi.api.custom.CustomUnitHandle;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/** Mutable, query-only view of the rates shown for one custom unit's periodic generation. */
public final class PeriodicGenerationDisplayContext {
    private final CustomUnitHandle unit;
    private final double originalCreditRatePerSecond;
    private double creditRatePerSecond;
    private final LinkedHashMap<String, Double> resourceRateOverrides =
            new LinkedHashMap<String, Double>();

    public PeriodicGenerationDisplayContext(CustomUnitHandle unit,
                                             double creditRatePerSecond) {
        this.unit = Objects.requireNonNull(unit, "unit");
        requireFinite(creditRatePerSecond, "creditRatePerSecond");
        this.originalCreditRatePerSecond = creditRatePerSecond;
        this.creditRatePerSecond = creditRatePerSecond;
    }

    public CustomUnitHandle unit() { return unit; }
    public double originalCreditRatePerSecond() { return originalCreditRatePerSecond; }
    public double creditRatePerSecond() { return creditRatePerSecond; }

    public void setCreditRatePerSecond(double value) {
        requireFinite(value, "value");
        creditRatePerSecond = value;
    }

    /** Replaces the displayed per-second rate for a built-in or unit-local INI resource. */
    public void setResourceRatePerSecond(String resourceName, double value) {
        String checked = Objects.requireNonNull(resourceName, "resourceName").trim();
        if (checked.isEmpty()) throw new IllegalArgumentException("resourceName must not be blank");
        requireFinite(value, "value");
        resourceRateOverrides.put(checked, Double.valueOf(value));
    }

    public Map<String, Double> resourceRateOverridesPerSecond() {
        return Collections.unmodifiableMap(resourceRateOverrides);
    }

    public boolean modified() {
        return Double.compare(originalCreditRatePerSecond, creditRatePerSecond) != 0
                || !resourceRateOverrides.isEmpty();
    }

    private static void requireFinite(double value, String name) {
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException(name + " must be finite");
        }
    }
}
