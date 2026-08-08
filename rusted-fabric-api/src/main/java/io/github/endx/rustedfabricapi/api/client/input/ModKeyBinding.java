package io.github.endx.rustedfabricapi.api.client.input;

import rustedwarfare.input.InputAction;

import java.util.Objects;
import java.util.Optional;

/** A custom key binding whose identity remains stable before and after engine initialization. */
public final class ModKeyBinding {
    private final String id;
    private final String displayName;
    private final String category;
    private final String defaultBinding;
    private final String nativeDisplayName;
    private volatile InputAction action;
    private boolean pollingInitialized;
    private boolean pressedLastPoll;

    ModKeyBinding(String id, String displayName, String category, String defaultBinding,
            String nativeDisplayName) {
        this.id = id;
        this.displayName = displayName;
        this.category = category;
        this.defaultBinding = defaultBinding;
        this.nativeDisplayName = nativeDisplayName;
    }

    public String id() {
        return id;
    }

    public String displayName() {
        return displayName;
    }

    public String category() {
        return category;
    }

    public String defaultBinding() {
        return defaultBinding;
    }

    public boolean isInstalled() {
        return action != null;
    }

    public Optional<InputAction> nativeAction() {
        return Optional.ofNullable(action);
    }

    public boolean isPressed() {
        InputAction current = action;
        return current != null && current.isPressed();
    }

    public boolean isJustPressed() {
        InputAction current = action;
        return current != null && current.isJustPressed();
    }

    public String primaryBindingDisplay() {
        InputAction current = action;
        return current != null ? current.getPrimaryBindingDisplay() : defaultBinding;
    }

    /** Replaces the primary keyboard binding. Use the game's syntax, for example {@code CTRL+K}. */
    public void setPrimaryBinding(String binding) {
        String checked = KeyBindings.requireText(binding, "binding");
        InputAction current = action;
        if (current == null) {
            throw new IllegalStateException("Key binding is not installed yet: " + id);
        }
        current.setKeyBinding(checked, 0);
    }

    String nativeDisplayName() {
        return nativeDisplayName;
    }

    void install(InputAction installedAction) {
        action = Objects.requireNonNull(installedAction, "installedAction");
        pollingInitialized = false;
    }

    void poll() {
        InputAction current = action;
        if (current == null) return;
        boolean pressed = current.isPressed();
        if (!pollingInitialized) {
            pollingInitialized = true;
            pressedLastPoll = pressed;
            return;
        }
        if (pressed && !pressedLastPoll) {
            KeyBindingEvents.PRESSED.invoker().onKeyBinding(this);
        } else if (!pressed && pressedLastPoll) {
            KeyBindingEvents.RELEASED.invoker().onKeyBinding(this);
        }
        pressedLastPoll = pressed;
    }

    @Override
    public String toString() {
        return "ModKeyBinding{" + id + '}';
    }
}
