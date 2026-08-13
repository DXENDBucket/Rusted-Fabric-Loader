package io.github.endx.rustedfabricapi.api.unit.build.event;

import io.github.endx.rustedfabricapi.api.game.UnitView;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/** Mutable price and build-time view for one unit-production action. */
public final class ProductionModifierContext {
    private final UnitView producer;
    private final String producedUnitType;
    private final int originalCreditCost;
    private final float originalBuildSpeedMultiplier;
    private final Map<String, Double> originalResourceCosts;
    private final Map<String, Double> resourceCosts;
    private int creditCost;
    private float buildSpeedMultiplier;

    public ProductionModifierContext(UnitView producer, String producedUnitType,
                                     int creditCost, float buildSpeedMultiplier) {
        this(producer, producedUnitType, creditCost, buildSpeedMultiplier,
                Collections.emptyMap());
    }

    public ProductionModifierContext(UnitView producer, String producedUnitType,
                                     int creditCost, float buildSpeedMultiplier,
                                     Map<String, Double> resourceCosts) {
        this.producer = Objects.requireNonNull(producer, "producer");
        this.producedUnitType = Objects.requireNonNull(producedUnitType, "producedUnitType");
        if (creditCost < 0) throw new IllegalArgumentException("creditCost must not be negative");
        requirePositiveFinite(buildSpeedMultiplier, "buildSpeedMultiplier");
        LinkedHashMap<String, Double> checkedResources = checkedResources(resourceCosts);
        this.originalCreditCost = creditCost;
        this.originalBuildSpeedMultiplier = buildSpeedMultiplier;
        this.originalResourceCosts = Collections.unmodifiableMap(
                new LinkedHashMap<String, Double>(checkedResources));
        this.resourceCosts = checkedResources;
        this.creditCost = creditCost;
        this.buildSpeedMultiplier = buildSpeedMultiplier;
    }

    public UnitView producer() { return producer; }
    public String producedUnitType() { return producedUnitType; }
    public int originalCreditCost() { return originalCreditCost; }
    public float originalBuildSpeedMultiplier() { return originalBuildSpeedMultiplier; }
    public int creditCost() { return creditCost; }
    public float buildSpeedMultiplier() { return buildSpeedMultiplier; }

    /** Immutable original custom-resource costs keyed by native internal resource name. */
    public Map<String, Double> originalResourceCosts() { return originalResourceCosts; }

    /** Immutable current custom-resource costs keyed by native internal resource name. */
    public Map<String, Double> resourceCosts() {
        return Collections.unmodifiableMap(new LinkedHashMap<String, Double>(resourceCosts));
    }

    /**
     * Reads a custom-resource cost. Configuration names also match native {@code l_}/{@code g_}
     * local/global prefixes, so {@code resourceCost("扩张警戒")} can read
     * {@code l_扩张警戒}.
     */
    public double resourceCost(String resourceName) {
        String key = matchingResourceKey(resourceName, resourceCosts);
        return key == null ? 0.0D : resourceCosts.get(key).doubleValue();
    }

    public double originalResourceCost(String resourceName) {
        String key = matchingResourceKey(resourceName, originalResourceCosts);
        return key == null ? 0.0D : originalResourceCosts.get(key).doubleValue();
    }

    public void setResourceCost(String resourceName, double value) {
        requireFinite(value, "resource cost");
        String checkedName = requireResourceName(resourceName);
        String existing = matchingResourceKey(checkedName, resourceCosts);
        resourceCosts.put(existing == null ? checkedName : existing, Double.valueOf(value));
    }

    public void addResourceCost(String resourceName, double delta) {
        if (!Double.isFinite(delta)) {
            throw new IllegalArgumentException("resource cost delta must be finite");
        }
        setResourceCost(resourceName, resourceCost(resourceName) + delta);
    }

    /** Multiplies one custom-resource cost without adding it when the action has no such cost. */
    public void multiplyResourceCost(String resourceName, double factor) {
        requireNonNegativeFinite(factor, "resource cost factor");
        String existing = matchingResourceKey(resourceName, resourceCosts);
        if (existing == null) return;
        setResourceCost(existing, resourceCosts.get(existing).doubleValue() * factor);
    }

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

    private static LinkedHashMap<String, Double> checkedResources(Map<String, Double> values) {
        LinkedHashMap<String, Double> checked = new LinkedHashMap<String, Double>();
        if (values == null) return checked;
        for (Map.Entry<String, Double> entry : values.entrySet()) {
            String name = requireResourceName(entry.getKey());
            if (entry.getValue() == null) {
                throw new IllegalArgumentException("resource cost must not be null");
            }
            double amount = entry.getValue().doubleValue();
            requireFinite(amount, "resource cost");
            checked.put(name, Double.valueOf(amount));
        }
        return checked;
    }

    private static String matchingResourceKey(String resourceName, Map<String, Double> values) {
        String checked = requireResourceName(resourceName);
        if (values.containsKey(checked)) return checked;
        if (!checked.startsWith("l_") && !checked.startsWith("g_")) {
            if (values.containsKey("l_" + checked)) return "l_" + checked;
            if (values.containsKey("g_" + checked)) return "g_" + checked;
        }
        return null;
    }

    private static String requireResourceName(String value) {
        String checked = value == null ? "" : value.trim();
        if (checked.isEmpty()) throw new IllegalArgumentException("resource name must not be blank");
        return checked;
    }

    private static void requireNonNegativeFinite(double value, String name) {
        if (!Double.isFinite(value) || value < 0.0D) {
            throw new IllegalArgumentException(name + " must be finite and non-negative");
        }
    }

    private static void requireFinite(double value, String name) {
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException(name + " must be finite");
        }
    }
}
