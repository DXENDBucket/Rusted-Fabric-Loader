package io.github.endx.rustedfabricapi.api.custom;

import rustedwarfare.custom.CustomTagList;
import rustedwarfare.custom.CustomUnit;
import rustedwarfare.custom.event.CustomUnitEventType;
import rustedwarfare.custom.logic.VariableScope;
import rustedwarfare.unit.Unit;

import java.util.Objects;

/** Immediate and queued execution helpers for configured custom-unit events. */
public final class CustomUnitTriggers {
    private CustomUnitTriggers() {
    }

    public static void trigger(CustomUnit unit, CustomUnitEventType eventType) {
        Objects.requireNonNull(unit, "unit");
        unit.triggerCustomEvent(Objects.requireNonNull(eventType, "eventType"));
    }

    public static void queue(CustomUnit unit, CustomUnitEventType eventType) {
        queue(unit, eventType, null, null, null);
    }

    public static void queue(CustomUnit unit, CustomUnitEventType eventType,
                             Unit source, CustomTagList eventTags, VariableScope eventData) {
        Objects.requireNonNull(unit, "unit");
        unit.queueCustomEventWithContext(
                Objects.requireNonNull(eventType, "eventType"), source, eventTags, eventData);
    }
}
