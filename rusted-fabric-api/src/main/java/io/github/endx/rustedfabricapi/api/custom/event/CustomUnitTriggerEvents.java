package io.github.endx.rustedfabricapi.api.custom.event;

import io.github.endx.rustedfabricapi.api.event.RustedFabricEvent;
import rustedwarfare.custom.CustomTagList;
import rustedwarfare.custom.CustomUnit;
import rustedwarfare.custom.event.CustomUnitEventType;
import rustedwarfare.custom.logic.VariableScope;
import rustedwarfare.unit.Unit;

/** Strongly typed interception of immediate and queued configured custom-unit events. */
public final class CustomUnitTriggerEvents {
    public static final RustedFabricEvent<BeforeTrigger> BEFORE_TRIGGER =
            RustedFabricEvent.create(listeners -> (unit, eventType) -> {
                boolean cancelled = false;
                for (BeforeTrigger listener : listeners) {
                    cancelled |= listener.beforeTrigger(unit, eventType);
                }
                return cancelled;
            });
    public static final RustedFabricEvent<AfterTrigger> AFTER_TRIGGER =
            RustedFabricEvent.create(listeners -> (unit, eventType) -> {
                for (AfterTrigger listener : listeners) listener.afterTrigger(unit, eventType);
            });
    public static final RustedFabricEvent<BeforeQueue> BEFORE_QUEUE =
            RustedFabricEvent.create(listeners -> (unit, eventType, source, tags, data) -> {
                boolean cancelled = false;
                for (BeforeQueue listener : listeners) {
                    cancelled |= listener.beforeQueue(unit, eventType, source, tags, data);
                }
                return cancelled;
            });
    public static final RustedFabricEvent<AfterQueue> AFTER_QUEUE =
            RustedFabricEvent.create(listeners -> (unit, eventType, source, tags, data) -> {
                for (AfterQueue listener : listeners) {
                    listener.afterQueue(unit, eventType, source, tags, data);
                }
            });
    /** Adds typed values to the native {@code eventData(...)} scope before an event is queued. */
    public static final RustedFabricEvent<EnrichEventData> ENRICH_EVENT_DATA =
            RustedFabricEvent.create(listeners -> (unit, eventType, source, tags, data) -> {
                for (EnrichEventData listener : listeners) {
                    listener.enrich(unit, eventType, source, tags, data);
                }
            });

    private CustomUnitTriggerEvents() {
    }

    @FunctionalInterface
    public interface BeforeTrigger {
        boolean beforeTrigger(CustomUnit unit, CustomUnitEventType eventType);
    }

    @FunctionalInterface
    public interface AfterTrigger {
        void afterTrigger(CustomUnit unit, CustomUnitEventType eventType);
    }

    @FunctionalInterface
    public interface BeforeQueue {
        boolean beforeQueue(CustomUnit unit, CustomUnitEventType eventType,
                            Unit source, CustomTagList eventTags, VariableScope eventData);
    }

    @FunctionalInterface
    public interface AfterQueue {
        void afterQueue(CustomUnit unit, CustomUnitEventType eventType,
                        Unit source, CustomTagList eventTags, VariableScope eventData);
    }

    @FunctionalInterface
    public interface EnrichEventData {
        void enrich(CustomUnit unit, CustomUnitEventType eventType,
                    Unit source, CustomTagList eventTags, CustomUnitEventData eventData);
    }
}
