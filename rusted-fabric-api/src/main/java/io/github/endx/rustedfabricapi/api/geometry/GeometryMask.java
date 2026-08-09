package io.github.endx.rustedfabricapi.api.geometry;

/** A finite, queryable two-dimensional region. Coordinates are caller-defined. */
public interface GeometryMask {
    GeometryBounds bounds();
    boolean contains(float x, float y);
}
