package io.github.endx.strategicai;

import io.github.endx.rustedfabricapi.api.world.WorldPoint;

public final class ForceCoordinationGeometryVerification {
    private ForceCoordinationGeometryVerification() {
    }

    public static void main(String[] args) {
        WorldPoint staged = ForceCoordinationGeometry.advance(
                new WorldPoint(0.0F, 0.0F), new WorldPoint(500.0F, 0.0F), 260.0F);
        require(close(staged.x(), 260.0F) && close(staged.y(), 0.0F),
                "staging point did not respect its forward limit");
        WorldPoint nearby = ForceCoordinationGeometry.advance(
                new WorldPoint(10.0F, 10.0F), new WorldPoint(40.0F, 50.0F), 100.0F);
        require(nearby.equals(new WorldPoint(40.0F, 50.0F)),
                "nearby objective should be used directly");
        require(ForceCoordinationGeometry.arrived(
                        new WorldPoint(0.0F, 0.0F), new WorldPoint(30.0F, 40.0F), 50.0F),
                "arrival radius rejected its boundary");
        require(!ForceCoordinationGeometry.arrived(
                        new WorldPoint(0.0F, 0.0F), new WorldPoint(31.0F, 40.0F), 50.0F),
                "arrival radius accepted an outside point");
        System.out.println("Strategic AI force coordination geometry contracts passed");
    }

    private static boolean close(float first, float second) {
        return Math.abs(first - second) < 0.001F;
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
