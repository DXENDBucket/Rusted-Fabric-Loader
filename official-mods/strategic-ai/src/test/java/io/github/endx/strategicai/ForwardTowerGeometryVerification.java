package io.github.endx.strategicai;

import io.github.endx.rustedfabricapi.api.world.WorldPoint;

public final class ForwardTowerGeometryVerification {
    private ForwardTowerGeometryVerification() {
    }

    public static void main(String[] args) {
        WorldPoint home = new WorldPoint(0.0F, 0.0F);
        WorldPoint resource = new WorldPoint(500.0F, 0.0F);
        WorldPoint contest = ForwardTowerGeometry.placement(
                home, resource, 165.0F, false, 0.0F, 0.0F);
        WorldPoint lock = ForwardTowerGeometry.placement(
                home, resource, 165.0F, true, 0.0F, 0.0F);
        require(contest.x() > resource.x(), "contest tower was not placed enemy-side");
        require(lock.x() < resource.x(), "lock tower was not placed home-side");
        require(Math.sqrt(lock.distanceSquared(resource)) < 165.0F - 18.0F,
                "lock tower did not keep the resource inside effective range");
        System.out.println("Strategic AI forward tower geometry contracts passed");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
