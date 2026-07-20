package io.github.endx.rustedfabricapi.api.client.input;

import java.util.Objects;
import java.util.OptionalInt;

import io.github.endx.rustedfabricapi.api.client.RustedWarfareClient;
import rustedwarfare.core.GameEngine;
import rustedwarfare.input.SlickToAndroidKeycodes;

/** Stable game-key translation and current-state helpers for desktop input callbacks. */
public final class InputKeys {
    private InputKeys() {
    }

    public static int gameKeyCode(String name) {
        String checked = Objects.requireNonNull(name, "name").trim();
        if (checked.isEmpty()) throw new IllegalArgumentException("name must not be blank");
        return SlickToAndroidKeycodes.getAndroidKeyCode(checked);
    }

    public static String gameKeyName(int gameKeyCode) {
        return SlickToAndroidKeycodes.getAndroidKeyName(gameKeyCode);
    }

    public static OptionalInt toGameKeyCode(int desktopKeyCode) {
        try {
            int code = SlickToAndroidKeycodes.slickToAndroidKeyCode(desktopKeyCode);
            return code > 0 ? OptionalInt.of(code) : OptionalInt.empty();
        } catch (RuntimeException ignored) {
            return OptionalInt.empty();
        }
    }

    public static boolean isDown(int gameKeyCode) {
        GameEngine engine = RustedWarfareClient.getEngine();
        return engine != null && engine.isKeyDown(gameKeyCode);
    }

    public static InputModifiers modifiers() {
        GameEngine engine = RustedWarfareClient.getEngine();
        return engine != null ? InputModifiers.fromMask(engine.getModifierKeyMask())
                : InputModifiers.NONE;
    }
}
