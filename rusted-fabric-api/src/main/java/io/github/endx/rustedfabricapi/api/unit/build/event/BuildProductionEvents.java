package io.github.endx.rustedfabricapi.api.unit.build.event;

import io.github.endx.rustedfabricapi.api.event.RustedFabricEvent;

/** Dynamic unit-production price and duration modifiers. */
public final class BuildProductionEvents {
    public static final RustedFabricEvent<Modify> MODIFY = RustedFabricEvent.create(listeners ->
            context -> {
                for (Modify listener : listeners) listener.modify(context);
            });

    private BuildProductionEvents() { }

    @FunctionalInterface
    public interface Modify {
        void modify(ProductionModifierContext context);
    }
}
