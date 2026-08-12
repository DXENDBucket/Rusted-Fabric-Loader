package io.github.endx.rustedfabricapi.api.custom.event;

import io.github.endx.rustedfabricapi.api.event.RustedFabricEvent;

/** Sparse settlement events for native custom-unit periodic resource generation. */
public final class CustomUnitEconomyEvents {
    /**
     * Fires only when an active {@code generation_delay} interval is ready to settle.
     * Return {@code true} after replacing the complete native generation amount.
     */
    public static final RustedFabricEvent<BeforePeriodicGeneration> BEFORE_PERIODIC_GENERATION =
            RustedFabricEvent.create(listeners -> context -> {
                boolean cancelled = false;
                for (BeforePeriodicGeneration listener : listeners) {
                    cancelled |= listener.beforeGeneration(context);
                }
                return cancelled;
            });

    /**
     * Modifies query-only generation rates used by team income totals and selected-unit details.
     * Listeners must be side-effect free: the game can query these values multiple times per frame.
     */
    public static final RustedFabricEvent<ModifyPeriodicGenerationDisplay>
            MODIFY_PERIODIC_GENERATION_DISPLAY =
            RustedFabricEvent.create(listeners -> context -> {
                for (ModifyPeriodicGenerationDisplay listener : listeners) {
                    listener.modifyDisplay(context);
                }
            });

    private CustomUnitEconomyEvents() { }

    @FunctionalInterface
    public interface BeforePeriodicGeneration {
        boolean beforeGeneration(PeriodicGenerationContext context);
    }

    @FunctionalInterface
    public interface ModifyPeriodicGenerationDisplay {
        void modifyDisplay(PeriodicGenerationDisplayContext context);
    }
}
