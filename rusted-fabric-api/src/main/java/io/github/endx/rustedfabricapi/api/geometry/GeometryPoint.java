package io.github.endx.rustedfabricapi.api.geometry;

/** Immutable local or world-space point produced by a geometry sampler. */
public final class GeometryPoint {
    private final float x;
    private final float y;

    public GeometryPoint(float x, float y) {
        if (!Float.isFinite(x) || !Float.isFinite(y)) {
            throw new IllegalArgumentException("geometry coordinates must be finite");
        }
        this.x = x;
        this.y = y;
    }

    public float x() { return x; }
    public float y() { return y; }

    @Override public String toString() { return "GeometryPoint{" + x + ", " + y + '}'; }
}
