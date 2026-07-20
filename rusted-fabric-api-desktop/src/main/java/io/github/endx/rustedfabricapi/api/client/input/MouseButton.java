package io.github.endx.rustedfabricapi.api.client.input;

public enum MouseButton {
    NONE(-1),
    LEFT(0),
    RIGHT(1),
    MIDDLE(2),
    OTHER(-2);

    private final int desktopCode;

    MouseButton(int desktopCode) {
        this.desktopCode = desktopCode;
    }

    public int desktopCode() { return desktopCode; }

    public static MouseButton fromDesktopCode(int code) {
        if (code == 0) return LEFT;
        if (code == 1) return RIGHT;
        if (code == 2) return MIDDLE;
        return code < 0 ? NONE : OTHER;
    }
}
