package io.github.endx.rustedfabricapi.api.client.input;

import io.github.endx.rustedfabricapi.api.event.RustedFabricEvent;

/**
 * Raw desktop input observations fired after native state and UI processing.
 *
 * <p>These events are intentionally non-cancellable so release callbacks cannot leave the game in
 * a stuck key or pointer state. Use semantic command, selection, or unit-action events to cancel
 * game behavior.</p>
 */
public final class ClientInputEvents {
    public static final RustedFabricEvent<Keyboard> KEY_PRESSED = keyboardEvent();
    public static final RustedFabricEvent<Keyboard> KEY_RELEASED = keyboardEvent();
    public static final RustedFabricEvent<Pointer> MOUSE_PRESSED = pointerEvent();
    public static final RustedFabricEvent<Pointer> MOUSE_RELEASED = pointerEvent();
    public static final RustedFabricEvent<Pointer> MOUSE_MOVED = pointerEvent();
    public static final RustedFabricEvent<Pointer> MOUSE_DRAGGED = pointerEvent();
    public static final RustedFabricEvent<Pointer> MOUSE_SCROLLED = pointerEvent();

    private ClientInputEvents() {
    }

    private static RustedFabricEvent<Keyboard> keyboardEvent() {
        return RustedFabricEvent.create(listeners -> input -> {
            for (Keyboard listener : listeners) listener.onKeyboardInput(input);
        });
    }

    private static RustedFabricEvent<Pointer> pointerEvent() {
        return RustedFabricEvent.create(listeners -> input -> {
            for (Pointer listener : listeners) listener.onPointerInput(input);
        });
    }

    @FunctionalInterface
    public interface Keyboard {
        void onKeyboardInput(KeyboardInput input);
    }

    @FunctionalInterface
    public interface Pointer {
        void onPointerInput(PointerInput input);
    }
}
