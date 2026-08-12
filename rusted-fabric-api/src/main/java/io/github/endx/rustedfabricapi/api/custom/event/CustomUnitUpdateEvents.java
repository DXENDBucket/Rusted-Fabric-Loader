package io.github.endx.rustedfabricapi.api.custom.event;

import io.github.endx.rustedfabricapi.api.event.RustedFabricEvent;

/** Per-instance simulation update events for native custom units. */
public final class CustomUnitUpdateEvents {
    public static final RustedFabricEvent<Update> BEFORE_UPDATE = updateEvent();
    public static final RustedFabricEvent<Update> AFTER_UPDATE = updateEvent();

    private CustomUnitUpdateEvents() { }

    private static RustedFabricEvent<Update> updateEvent() {
        return RustedFabricEvent.create(listeners -> context -> {
            for (Update listener : listeners) listener.update(context);
        });
    }

    @FunctionalInterface
    public interface Update {
        void update(CustomUnitUpdateContext context);
    }
}
