package io.github.endx.rustedfabricapi.api.custom.event;

import io.github.endx.rustedfabricapi.api.event.RustedFabricEvent;

/** Synchronous mutable custom-unit operations, distinct from later queued action notifications. */
public final class CustomUnitOperationEvents {
    public static final RustedFabricEvent<BeforeEvent> BEFORE_EVENT =
            RustedFabricEvent.create(listeners -> context -> {
                for (BeforeEvent listener : listeners) listener.beforeEvent(context);
            });

    private CustomUnitOperationEvents() { }

    @FunctionalInterface
    public interface BeforeEvent {
        void beforeEvent(MutableCustomUnitEventContext context);
    }
}
