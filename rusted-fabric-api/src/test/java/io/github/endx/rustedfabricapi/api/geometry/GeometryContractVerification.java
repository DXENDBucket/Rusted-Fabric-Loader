package io.github.endx.rustedfabricapi.api.geometry;

import java.util.Arrays;
import java.util.List;

public final class GeometryContractVerification {
    private GeometryContractVerification() { }

    public static void verify() {
        GeometryMask circle = GeometryMasks.circle(10.0F);
        require(circle.contains(0, 0) && circle.contains(10, 0) && !circle.contains(10.1F, 0),
                "circle containment changed");

        GeometryMask half = GeometryMasks.sector(0, 10, -90, 180);
        require(half.contains(5, 0) && !half.contains(-5, 0),
                "sector sweep containment changed");

        GeometryMask moved = GeometryMasks.transform(
                GeometryMasks.rectangle(8, 4), 20, 30, 90);
        require(moved.contains(20, 33) && !moved.contains(24, 30),
                "geometry transform changed");

        GeometryMask polygon = GeometryMasks.polygon(Arrays.asList(
                new GeometryPoint(0, 0), new GeometryPoint(10, 0),
                new GeometryPoint(5, 10)));
        require(polygon.contains(5, 4) && !polygon.contains(9, 9),
                "polygon containment changed");

        GeometryMask cut = GeometryMasks.difference(circle, GeometryMasks.circle(5));
        require(!cut.contains(0, 0) && cut.contains(8, 0),
                "boolean mask difference changed");

        List<GeometryPoint> fill = GeometrySampler.sample(
                GeometryMasks.rectangle(4, 4), 2, GeometrySampleMode.FILL, 32);
        require(!fill.isEmpty(), "fill sampler returned no points");
        expectIllegal(() -> GeometrySampler.sample(circle, 0.01F,
                GeometrySampleMode.FILL, 10));
    }

    private static void expectIllegal(Runnable action) {
        try {
            action.run();
            throw new AssertionError("invalid geometry operation was accepted");
        } catch (IllegalArgumentException expected) {
            // Expected.
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
