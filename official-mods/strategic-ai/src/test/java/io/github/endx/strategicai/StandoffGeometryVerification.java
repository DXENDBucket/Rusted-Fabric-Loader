package io.github.endx.strategicai;

public final class StandoffGeometryVerification {
    private StandoffGeometryVerification() {
    }

    public static void main(String[] args) {
        float desired = StandoffGeometry.desiredDistance(310.0F, 290.0F, 3.0F, 2.0F);
        require(desired > 290.0F && desired < 310.0F,
                "desired distance escaped the one-way fire band");
        require(Float.isNaN(StandoffGeometry.desiredDistance(296.0F, 292.0F, 3.0F, 2.0F)),
                "an unusably narrow fire band was accepted");

        StandoffGeometry.Position east = StandoffGeometry.position(
                200.0F, 100.0F, 1L, 100.0F, 100.0F, 50.0F);
        require(close(east.x, 150.0F) && close(east.y, 100.0F),
                "radial standoff position was incorrect");
        StandoffGeometry.Position overlapA = StandoffGeometry.position(
                100.0F, 100.0F, 7L, 100.0F, 100.0F, 50.0F);
        StandoffGeometry.Position overlapB = StandoffGeometry.position(
                100.0F, 100.0F, 7L, 100.0F, 100.0F, 50.0F);
        require(close(overlapA.x, overlapB.x) && close(overlapA.y, overlapB.y),
                "overlapping-unit fallback direction was not deterministic");
        System.out.println("Strategic AI standoff geometry contracts passed");
    }

    private static boolean close(float first, float second) {
        return Math.abs(first - second) < 0.001F;
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
