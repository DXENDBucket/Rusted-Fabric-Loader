package io.github.endx.rustedfabricapi.api.unit.action;

import java.util.Locale;
import java.util.Objects;

/** Immutable attachment of one Java action to a unit type and tech-level interval. */
public final class JavaUnitActionBinding {
    private final String unitTypeName;
    private final String normalizedUnitTypeName;
    private final int minimumTechLevel;
    private final int maximumTechLevel;
    private final JavaUnitAction action;

    public JavaUnitActionBinding(String unitTypeName, int minimumTechLevel,
            int maximumTechLevel, JavaUnitAction action) {
        Objects.requireNonNull(unitTypeName, "unitTypeName");
        String checked = unitTypeName.trim();
        if (checked.isEmpty()) throw new IllegalArgumentException("unitTypeName must not be blank");
        if (minimumTechLevel < 0 || maximumTechLevel < minimumTechLevel) {
            throw new IllegalArgumentException("invalid tech-level interval");
        }
        this.unitTypeName = checked;
        this.normalizedUnitTypeName = checked.toLowerCase(Locale.ROOT);
        this.minimumTechLevel = minimumTechLevel;
        this.maximumTechLevel = maximumTechLevel;
        this.action = Objects.requireNonNull(action, "action");
    }

    public String unitTypeName() { return unitTypeName; }
    public int minimumTechLevel() { return minimumTechLevel; }
    public int maximumTechLevel() { return maximumTechLevel; }
    public JavaUnitAction action() { return action; }
    public boolean matches(String typeName, int techLevel) {
        return typeName != null && normalizedUnitTypeName.equals(typeName.toLowerCase(Locale.ROOT))
                && techLevel >= minimumTechLevel && techLevel <= maximumTechLevel;
    }

    @Override
    public String toString() {
        return "JavaUnitActionBinding{" + unitTypeName + '[' + minimumTechLevel + ','
                + maximumTechLevel + "] -> " + action.id() + '}';
    }
}
