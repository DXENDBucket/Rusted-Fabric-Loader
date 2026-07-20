package io.github.endx.rustedfabricapi.api.client.input;

/** Immutable modifier-key state using Rusted Warfare's stable bit layout. */
public final class InputModifiers {
    public static final int CONTROL_MASK = 1;
    public static final int SHIFT_MASK = 2;
    public static final int ALT_MASK = 4;
    public static final InputModifiers NONE = new InputModifiers(0);

    private final int mask;

    private InputModifiers(int mask) {
        this.mask = mask & (CONTROL_MASK | SHIFT_MASK | ALT_MASK);
    }

    public static InputModifiers fromMask(int mask) {
        int normalized = mask & (CONTROL_MASK | SHIFT_MASK | ALT_MASK);
        return normalized == 0 ? NONE : new InputModifiers(normalized);
    }

    public int mask() { return mask; }
    public boolean control() { return (mask & CONTROL_MASK) != 0; }
    public boolean shift() { return (mask & SHIFT_MASK) != 0; }
    public boolean alt() { return (mask & ALT_MASK) != 0; }
    public boolean any() { return mask != 0; }

    public boolean contains(InputModifiers required) {
        if (required == null) throw new NullPointerException("required");
        return (mask & required.mask) == required.mask;
    }

    @Override
    public boolean equals(Object other) {
        return this == other || other instanceof InputModifiers
                && mask == ((InputModifiers) other).mask;
    }

    @Override
    public int hashCode() { return mask; }

    @Override
    public String toString() {
        if (mask == 0) return "InputModifiers{none}";
        StringBuilder result = new StringBuilder("InputModifiers{");
        if (control()) result.append("control,");
        if (shift()) result.append("shift,");
        if (alt()) result.append("alt,");
        result.setCharAt(result.length() - 1, '}');
        return result.toString();
    }
}
