package io.github.endx.rustedfabricapi.api.custom.event;

import rustedwarfare.custom.CustomTagList;
import rustedwarfare.custom.CustomUnit;
import rustedwarfare.custom.event.CustomUnitEventType;
import rustedwarfare.unit.Unit;

import java.util.Objects;
import java.util.Optional;

/** Mutable synchronous context for a native custom-unit operation that has not happened yet. */
public final class MutableCustomUnitEventContext {
    private final CustomUnit unit;
    private final CustomUnitEventType eventType;
    private final Unit sourceUnit;
    private final CustomTagList tags;
    private final CustomUnitEventData data;
    private final String primaryValueName;
    private final double originalValue;
    private double value;
    private boolean cancelled;

    public MutableCustomUnitEventContext(CustomUnit unit, CustomUnitEventType eventType,
                                         Unit sourceUnit, CustomTagList tags,
                                         CustomUnitEventData data, String primaryValueName,
                                         double originalValue) {
        this.unit = Objects.requireNonNull(unit, "unit");
        this.eventType = Objects.requireNonNull(eventType, "eventType");
        this.sourceUnit = sourceUnit;
        this.tags = tags;
        this.data = Objects.requireNonNull(data, "data");
        this.primaryValueName = requireName(primaryValueName);
        requireFinite(originalValue);
        this.originalValue = originalValue;
        this.value = originalValue;
        data.putNumber(this.primaryValueName, originalValue);
    }

    public CustomUnit unit() { return unit; }
    public CustomUnitEventType eventType() { return eventType; }
    public Optional<Unit> sourceUnit() { return Optional.ofNullable(sourceUnit); }
    public Optional<CustomTagList> tags() { return Optional.ofNullable(tags); }
    public CustomUnitEventData data() { return data; }
    public String primaryValueName() { return primaryValueName; }
    public double originalValue() { return originalValue; }
    public double value() { return value; }

    public void setValue(double replacement) {
        requireFinite(replacement);
        value = replacement;
        data.putNumber(primaryValueName, replacement);
    }

    public void addValue(double delta) { setValue(value + delta); }
    public void multiplyValue(double factor) { setValue(value * factor); }
    public void cancel() { cancelled = true; }
    public boolean cancelled() { return cancelled; }

    private static String requireName(String value) {
        String result = Objects.requireNonNull(value, "primaryValueName").trim();
        if (result.isEmpty()) throw new IllegalArgumentException("primaryValueName must not be empty");
        return result;
    }

    private static void requireFinite(double value) {
        if (!Double.isFinite(value)) throw new IllegalArgumentException("event value must be finite");
    }
}
