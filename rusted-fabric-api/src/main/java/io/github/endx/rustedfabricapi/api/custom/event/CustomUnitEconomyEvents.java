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

    private CustomUnitEconomyEvents() { }

    @FunctionalInterface
    public interface BeforePeriodicGeneration {
        boolean beforeGeneration(PeriodicGenerationContext context);
    }
}
