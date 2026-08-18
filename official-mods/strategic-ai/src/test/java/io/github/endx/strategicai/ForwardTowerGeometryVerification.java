package io.github.endx.strategicai;

import io.github.endx.rustedfabricapi.api.world.WorldPoint;

public final class ForwardTowerGeometryVerification {
    private ForwardTowerGeometryVerification() {
    }

    public static void main(String[] args) {
        WorldPoint home = new WorldPoint(0.0F, 0.0F);
        WorldPoint resource = new WorldPoint(500.0F, 0.0F);
        WorldPoint conservative = ForwardTowerGeometry.placement(
                home, resource, 165.0F, 0.0F, 0.0F);
        require(conservative.x() < resource.x(),
                "ordinary forward tower was not placed on the home side");
        require(Math.sqrt(conservative.distanceSquared(resource)) >= 230.0F,
                "ordinary forward tower was still an aggressive resource lock");
        WorldPoint fallback = ForwardTowerGeometry.safeFallback(
                home, new WorldPoint(520.0F, 0.0F), 165.0F, 45.0F);
        require(close(fallback.x(), 310.0F) && close(fallback.y(), 0.0F),
                "failed-contest fallback was not placed behind enemy range");
        System.out.println("Strategic AI forward tower geometry contracts passed");
    }

    private static boolean close(float first, float second) {
        return Math.abs(first - second) < 0.001F;
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
