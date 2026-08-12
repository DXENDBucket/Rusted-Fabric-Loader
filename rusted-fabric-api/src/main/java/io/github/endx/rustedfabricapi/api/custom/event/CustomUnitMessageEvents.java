package io.github.endx.rustedfabricapi.api.custom.event;

import io.github.endx.rustedfabricapi.api.event.RustedFabricEvent;

/** Intercepts native custom-unit messages after event data enrichment and before INI actions. */
public final class CustomUnitMessageEvents {
    public static final RustedFabricEvent<BeforeConfiguredActions> BEFORE_CONFIGURED_ACTIONS =
            RustedFabricEvent.create(listeners -> context -> {
                boolean consumed = false;
                for (BeforeConfiguredActions listener : listeners) {
                    consumed |= listener.beforeConfiguredActions(context);
                }
                return consumed;
            });

    private CustomUnitMessageEvents() { }

    @FunctionalInterface
    public interface BeforeConfiguredActions {
        /** Return true to consume the message and skip its configured INI event actions. */
        boolean beforeConfiguredActions(CustomUnitMessageContext context);
    }
}
