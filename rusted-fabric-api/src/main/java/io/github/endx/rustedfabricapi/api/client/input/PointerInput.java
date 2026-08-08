package io.github.endx.rustedfabricapi.api.client.input;

import java.util.Objects;
import java.util.Optional;

import io.github.endx.rustedfabricapi.api.world.WorldPoint;

/** Immutable mouse/pointer callback with physical, logical, and optional world coordinates. */
public final class PointerInput {
    private final PointerAction action;
    private final MouseButton button;
    private final int desktopButtonCode;
    private final int rawX;
    private final int rawY;
    private final float screenX;
    private final float screenY;
    private final float deltaX;
    private final float deltaY;
    private final int wheelDelta;
    private final InputModifiers modifiers;
    private final boolean userInterfaceActive;
    private final WorldPoint worldPosition;
    private final boolean insideWorldViewport;

    public PointerInput(PointerAction action, MouseButton button, int desktopButtonCode,
            int rawX, int rawY, float screenX, float screenY, float deltaX, float deltaY,
            int wheelDelta, InputModifiers modifiers, boolean userInterfaceActive,
            WorldPoint worldPosition, boolean insideWorldViewport) {
        this.action = Objects.requireNonNull(action, "action");
        this.button = Objects.requireNonNull(button, "button");
        requireFinite(screenX, "screenX");
        requireFinite(screenY, "screenY");
        requireFinite(deltaX, "deltaX");
        requireFinite(deltaY, "deltaY");
        if (worldPosition == null && insideWorldViewport) {
            throw new IllegalArgumentException("insideWorldViewport requires a world position");
        }
        this.desktopButtonCode = desktopButtonCode;
        this.rawX = rawX;
        this.rawY = rawY;
        this.screenX = screenX;
        this.screenY = screenY;
        this.deltaX = deltaX;
        this.deltaY = deltaY;
        this.wheelDelta = wheelDelta;
        this.modifiers = Objects.requireNonNull(modifiers, "modifiers");
        this.userInterfaceActive = userInterfaceActive;
        this.worldPosition = worldPosition;
        this.insideWorldViewport = insideWorldViewport;
    }

    public PointerAction action() { return action; }
    public MouseButton button() { return button; }
    public int desktopButtonCode() { return desktopButtonCode; }
    /** Raw desktop callback value before render scaling; captured-pointer mode may report motion. */
    public int rawX() { return rawX; }
    public int rawY() { return rawY; }
    /** Logical coordinate matching HUD drawing and the native touch state. */
    public float screenX() { return screenX; }
    public float screenY() { return screenY; }
    public float deltaX() { return deltaX; }
    public float deltaY() { return deltaY; }
    public int wheelDelta() { return wheelDelta; }
    public InputModifiers modifiers() { return modifiers; }
    public boolean userInterfaceActive() { return userInterfaceActive; }
    public Optional<WorldPoint> worldPosition() { return Optional.ofNullable(worldPosition); }
    public boolean insideWorldViewport() { return insideWorldViewport; }

    private static void requireFinite(float value, String name) {
        if (!Float.isFinite(value)) throw new IllegalArgumentException(name + " must be finite");
    }

    @Override
    public String toString() {
        return "PointerInput{" + action + ", button=" + button + '/' + desktopButtonCode
                + ", screen=" + screenX + ',' + screenY
                + ", delta=" + deltaX + ',' + deltaY + ", wheel=" + wheelDelta
                + (insideWorldViewport ? ", world=" + worldPosition : "")
                + (userInterfaceActive ? ", ui" : "") + '}';
    }
}
