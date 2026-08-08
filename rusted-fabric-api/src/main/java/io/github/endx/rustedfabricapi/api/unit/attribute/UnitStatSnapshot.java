package io.github.endx.rustedfabricapi.api.unit.attribute;

import rustedwarfare.custom.CustomUnit;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

/** Immutable point-in-time view of every mapped custom-unit stat. */
public final class UnitStatSnapshot {
    private final CustomUnit unit;
    private final Map<UnitStat, Double> values;

    UnitStatSnapshot(CustomUnit unit, Map<UnitStat, Double> values) {
        this.unit = Objects.requireNonNull(unit, "unit");
        this.values = Collections.unmodifiableMap(new EnumMap<UnitStat, Double>(values));
    }

    public CustomUnit unit() { return unit; }
    public double get(UnitStat stat) {
        Double value = values.get(Objects.requireNonNull(stat, "stat"));
        if (value == null) throw new IllegalArgumentException("Stat is absent: " + stat);
        return value;
    }
    public Map<UnitStat, Double> values() { return values; }
}
