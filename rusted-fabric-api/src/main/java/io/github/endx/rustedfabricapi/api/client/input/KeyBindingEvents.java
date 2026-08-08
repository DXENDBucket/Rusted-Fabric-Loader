package io.github.endx.rustedfabricapi.api.client.input;

import io.github.endx.rustedfabricapi.api.event.RustedFabricEvent;

/** Edge-triggered events for key bindings registered through {@link KeyBindings}. */
public final class KeyBindingEvents {
    public static final RustedFabricEvent<Listener> PRESSED = event();
    public static final RustedFabricEvent<Listener> RELEASED = event();

    private KeyBindingEvents() {
    }

    private static RustedFabricEvent<Listener> event() {
        return RustedFabricEvent.create(listeners -> binding -> {
            for (Listener listener : listeners) listener.onKeyBinding(binding);
        });
    }

    @FunctionalInterface
    public interface Listener {
        void onKeyBinding(ModKeyBinding binding);
    }
}
