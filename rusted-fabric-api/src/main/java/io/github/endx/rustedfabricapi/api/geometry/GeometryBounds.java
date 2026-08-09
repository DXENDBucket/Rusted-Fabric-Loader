package io.github.endx.rustedfabricapi.api.geometry;

/** Immutable axis-aligned bounds for a two-dimensional geometry mask. */
public final class GeometryBounds {
    private final float minX;
    private final float minY;
    private final float maxX;
    private final float maxY;

    public GeometryBounds(float minX, float minY, float maxX, float maxY) {
        requireFinite(minX, "minX");
        requireFinite(minY, "minY");
        requireFinite(maxX, "maxX");
        requireFinite(maxY, "maxY");
        if (minX > maxX || minY > maxY) {
            throw new IllegalArgumentException("minimum bounds must not exceed maximum bounds");
        }
        this.minX = minX;
        this.minY = minY;
        this.maxX = maxX;
        this.maxY = maxY;
    }

    public float minX() { return minX; }
    public float minY() { return minY; }
    public float maxX() { return maxX; }
    public float maxY() { return maxY; }
    public float width() { return maxX - minX; }
    public float height() { return maxY - minY; }

    public GeometryBounds union(GeometryBounds other) {
        if (other == null) throw new NullPointerException("other");
        return new GeometryBounds(Math.min(minX, other.minX), Math.min(minY, other.minY),
                Math.max(maxX, other.maxX), Math.max(maxY, other.maxY));
    }

    public GeometryBounds intersect(GeometryBounds other) {
        if (other == null) throw new NullPointerException("other");
        float left = Math.max(minX, other.minX);
        float top = Math.max(minY, other.minY);
        float right = Math.min(maxX, other.maxX);
        float bottom = Math.min(maxY, other.maxY);
        if (left > right || top > bottom) return new GeometryBounds(0, 0, 0, 0);
        return new GeometryBounds(left, top, right, bottom);
    }

    public GeometryBounds expand(float amount) {
        requireFinite(amount, "amount");
        if (amount < 0.0F && (-amount * 2.0F > width() || -amount * 2.0F > height())) {
            throw new IllegalArgumentException("negative expansion collapses the bounds");
        }
        return new GeometryBounds(minX - amount, minY - amount,
                maxX + amount, maxY + amount);
    }

    private static void requireFinite(float value, String name) {
        if (!Float.isFinite(value)) throw new IllegalArgumentException(name + " must be finite");
    }
}
