package io.github.endx.rustedfabricapi.api.client.render;

/** Immutable position in render-target pixels relative to the world viewport. */
public final class ScreenPosition {
    private final float x;
    private final float y;

    public ScreenPosition(float x, float y) {
        if (!Float.isFinite(x) || !Float.isFinite(y)) {
            throw new IllegalArgumentException("screen coordinates must be finite");
        }
        this.x = x;
        this.y = y;
    }

    public float x() { return x; }
    public float y() { return y; }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof ScreenPosition)) return false;
        ScreenPosition position = (ScreenPosition) other;
        return Float.compare(x, position.x) == 0 && Float.compare(y, position.y) == 0;
    }

    @Override
    public int hashCode() {
        return 31 * Float.hashCode(x) + Float.hashCode(y);
    }

    @Override
    public String toString() {
        return "ScreenPosition{" + x + ", " + y + '}';
    }
}
