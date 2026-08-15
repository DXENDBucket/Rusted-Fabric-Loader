package io.github.endx.strategicai;

import io.github.endx.rustedfabricapi.api.world.WorldPoint;

import java.util.List;

public final class BaseLayoutGeometryVerification {
    private BaseLayoutGeometryVerification() {
    }

    public static void main(String[] args) {
        WorldPoint anchor = new WorldPoint(500.0F, 500.0F);
        List<WorldPoint> production = BaseLayoutGeometry.slots(anchor,
                new WorldPoint(1000.0F, 500.0F), BaseLayoutGeometry.District.PRODUCTION);
        List<WorldPoint> defense = BaseLayoutGeometry.slots(anchor,
                new WorldPoint(1000.0F, 500.0F), BaseLayoutGeometry.District.DEFENSE);
        require(production.get(0).x() < anchor.x(),
                "production district was not placed behind the base");
        require(defense.get(0).x() > anchor.x(),
                "defense district was not placed toward the front");
        require(close(production.get(0).x(), production.get(1).x())
                        && close(production.get(0).y() + production.get(1).y(), 1000.0F),
                "paired production slots were not symmetric");

        List<WorldPoint> northProduction = BaseLayoutGeometry.slots(anchor,
                new WorldPoint(500.0F, 1000.0F), BaseLayoutGeometry.District.PRODUCTION);
        require(northProduction.get(0).y() < anchor.y(),
                "base districts did not rotate with the front direction");
        System.out.println("Strategic AI base layout geometry contracts passed");
    }

    private static boolean close(float first, float second) {
        return Math.abs(first - second) < 0.001F;
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
