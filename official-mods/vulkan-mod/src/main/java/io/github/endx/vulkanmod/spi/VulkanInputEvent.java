package io.github.endx.vulkanmod.spi;

/** Immutable platform input event emitted by a driver-owned native window. */
public final class VulkanInputEvent {
    public enum Type {
        POINTER_MOVE,
        BUTTON_DOWN,
        BUTTON_UP,
        WHEEL,
        KEY_DOWN,
        KEY_UP,
        CHARACTER,
        FOCUS_LOST
    }

    private final Type type;
    private final int x;
    private final int y;
    private final int code;
    private final int value;
    private final char character;

    private VulkanInputEvent(Type type, int x, int y, int code, int value, char character) {
        if (type == null) throw new NullPointerException("type");
        this.type = type;
        this.x = x;
        this.y = y;
        this.code = code;
        this.value = value;
        this.character = character;
    }

    public static VulkanInputEvent pointer(Type type, int x, int y, int button) {
        if (type != Type.POINTER_MOVE && type != Type.BUTTON_DOWN
                && type != Type.BUTTON_UP) {
            throw new IllegalArgumentException("not a pointer event: " + type);
        }
        return new VulkanInputEvent(type, x, y, button, 0, '\0');
    }

    public static VulkanInputEvent wheel(int x, int y, int delta) {
        return new VulkanInputEvent(Type.WHEEL, x, y, 0, delta, '\0');
    }

    public static VulkanInputEvent key(Type type, int virtualKey, int repeatCount) {
        if (type != Type.KEY_DOWN && type != Type.KEY_UP) {
            throw new IllegalArgumentException("not a key event: " + type);
        }
        return new VulkanInputEvent(type, 0, 0, virtualKey, repeatCount, '\0');
    }

    public static VulkanInputEvent character(char character) {
        return new VulkanInputEvent(Type.CHARACTER, 0, 0, 0, 0, character);
    }

    public static VulkanInputEvent focusLost() {
        return new VulkanInputEvent(Type.FOCUS_LOST, 0, 0, 0, 0, '\0');
    }

    public Type type() { return type; }
    public int x() { return x; }
    public int y() { return y; }
    /** Mouse button for button events, or Win32 virtual-key code for key events. */
    public int code() { return code; }
    /** Wheel delta or native repeat count, depending on event type. */
    public int value() { return value; }
    public char character() { return character; }
}
