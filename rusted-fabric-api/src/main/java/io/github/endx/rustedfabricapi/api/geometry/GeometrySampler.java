package io.github.endx.rustedfabricapi.api.geometry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/** Deterministic grid sampler for masks, effects, fog and later gameplay consumers. */
public final class GeometrySampler {
    public static final int DEFAULT_MAX_POINTS = 4096;

    private GeometrySampler() { }

    public static List<GeometryPoint> sample(GeometryMask mask, float spacing,
                                             GeometrySampleMode mode) {
        return sample(mask, spacing, mode, DEFAULT_MAX_POINTS);
    }

    public static List<GeometryPoint> sample(GeometryMask mask, float spacing,
                                             GeometrySampleMode mode, int maxPoints) {
        Objects.requireNonNull(mask, "mask");
        Objects.requireNonNull(mode, "mode");
        if (!Float.isFinite(spacing) || spacing <= 0.0F) {
            throw new IllegalArgumentException("spacing must be positive and finite");
        }
        if (maxPoints <= 0) throw new IllegalArgumentException("maxPoints must be positive");
        GeometryBounds bounds = mask.bounds();
        ArrayList<GeometryPoint> result = new ArrayList<GeometryPoint>();
        float half = spacing * 0.5F;
        int columns = checkedSteps(bounds.width(), spacing);
        int rows = checkedSteps(bounds.height(), spacing);
        for (int row = 0; row <= rows; row++) {
            float y = bounds.minY() + Math.min(bounds.height(), row * spacing);
            for (int column = 0; column <= columns; column++) {
                float x = bounds.minX() + Math.min(bounds.width(), column * spacing);
                if (!mask.contains(x, y)) continue;
                if (mode == GeometrySampleMode.OUTLINE
                        && mask.contains(x - half, y) && mask.contains(x + half, y)
                        && mask.contains(x, y - half) && mask.contains(x, y + half)) {
                    continue;
                }
                if (result.size() >= maxPoints) {
                    throw new IllegalArgumentException("geometry sample exceeds maxPoints=" + maxPoints);
                }
                result.add(new GeometryPoint(x, y));
            }
        }
        return Collections.unmodifiableList(result);
    }

    private static int checkedSteps(float size, float spacing) {
        double steps = Math.ceil(size / spacing);
        if (steps > Integer.MAX_VALUE) throw new IllegalArgumentException("geometry sample is too large");
        return (int) steps;
    }
}
