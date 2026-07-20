package io.github.endx.rustedfabricapi.api.world;

/** Immutable two-dimensional position in Rusted Warfare world units. */
public final class WorldPoint {
    private final float x;
    private final float y;

    public WorldPoint(float x, float y) {
        if (!Float.isFinite(x) || !Float.isFinite(y)) {
            throw new IllegalArgumentException("world coordinates must be finite");
        }
        this.x = x;
        this.y = y;
    }

    public float x() {
        return x;
    }

    public float y() {
        return y;
    }

    public float distanceSquared(WorldPoint other) {
        if (other == null) throw new NullPointerException("other");
        float dx = x - other.x;
        float dy = y - other.y;
        return dx * dx + dy * dy;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof WorldPoint)) return false;
        WorldPoint point = (WorldPoint) other;
        return Float.compare(x, point.x) == 0 && Float.compare(y, point.y) == 0;
    }

    @Override
    public int hashCode() {
        return 31 * Float.hashCode(x) + Float.hashCode(y);
    }

    @Override
    public String toString() {
        return "WorldPoint{" + x + ", " + y + '}';
    }
}
