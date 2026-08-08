package io.github.endx.rustedfabricapi.api.client.input;

import java.util.Objects;
import java.util.OptionalInt;

/** Immutable keyboard callback captured after the game has updated its native input state. */
public final class KeyboardInput {
    private final KeyboardAction action;
    private final int desktopKeyCode;
    private final int gameKeyCode;
    private final char character;
    private final InputModifiers modifiers;
    private final boolean repeated;
    private final boolean userInterfaceActive;

    public KeyboardInput(KeyboardAction action, int desktopKeyCode, int gameKeyCode,
            char character, InputModifiers modifiers, boolean repeated,
            boolean userInterfaceActive) {
        this.action = Objects.requireNonNull(action, "action");
        this.desktopKeyCode = desktopKeyCode;
        this.gameKeyCode = gameKeyCode;
        this.character = character;
        this.modifiers = Objects.requireNonNull(modifiers, "modifiers");
        this.repeated = repeated;
        this.userInterfaceActive = userInterfaceActive;
    }

    public KeyboardAction action() { return action; }
    /** Slick/LWJGL-independent numeric code supplied by the desktop callback. */
    public int desktopKeyCode() { return desktopKeyCode; }
    /** Mapped Android-style game key code, or empty when the desktop code is unknown. */
    public OptionalInt gameKeyCode() {
        return gameKeyCode >= 0 ? OptionalInt.of(gameKeyCode) : OptionalInt.empty();
    }
    public char character() { return character; }
    public boolean hasPrintableCharacter() {
        return character != 0 && !Character.isISOControl(character);
    }
    public InputModifiers modifiers() { return modifiers; }
    public boolean repeated() { return repeated; }
    public boolean userInterfaceActive() { return userInterfaceActive; }

    @Override
    public String toString() {
        return "KeyboardInput{" + action + ", desktop=" + desktopKeyCode
                + ", game=" + (gameKeyCode >= 0 ? gameKeyCode : "unknown")
                + ", character=" + (int) character + ", " + modifiers
                + (repeated ? ", repeated" : "")
                + (userInterfaceActive ? ", ui" : "") + '}';
    }
}
