package io.github.endx.rustedfabricapi.api.client.minimap;

/** Immutable integer point in window/screen coordinates. */
public final class ScreenPoint {
    private final int x;
    private final int y;

    public ScreenPoint(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public int getX() { return x; }

    public int getY() { return y; }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof ScreenPoint)) return false;
        ScreenPoint point = (ScreenPoint) other;
        return x == point.x && y == point.y;
    }

    @Override
    public int hashCode() { return 31 * x + y; }

    @Override
    public String toString() { return "ScreenPoint{" + x + ", " + y + '}'; }
}
