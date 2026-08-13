package io.github.endx.rustedfabricapi.api.unit.build.event;

import io.github.endx.rustedfabricapi.api.event.RustedFabricEvent;

/** Dynamic unit-production price and duration modifiers. */
public final class BuildProductionEvents {
    public static final RustedFabricEvent<Modify> MODIFY = RustedFabricEvent.create(listeners ->
            context -> {
                for (Modify listener : listeners) listener.modify(context);
            });

    /** Fires once the native queue has actually created the produced unit. */
    public static final RustedFabricEvent<Completed> AFTER_COMPLETED =
            RustedFabricEvent.create(listeners -> context -> {
                for (Completed listener : listeners) listener.completed(context);
            });

    private BuildProductionEvents() { }

    @FunctionalInterface
    public interface Modify {
        void modify(ProductionModifierContext context);
    }

    @FunctionalInterface
    public interface Completed {
        void completed(ProductionCompletedContext context);
    }
}
