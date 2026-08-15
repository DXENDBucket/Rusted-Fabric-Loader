package io.github.endx.strategicai;

import io.github.endx.rustedfabricapi.api.world.WorldPoint;

/** Pure contest/lock tower geometry around a denied resource point. */
final class ForwardTowerGeometry {
    private ForwardTowerGeometry() {
    }

    static WorldPoint placement(WorldPoint home, WorldPoint resource, float towerRange,
            boolean lockMode, float radialOffset, float lateralOffset) {
        float dx = resource.x() - home.x();
        float dy = resource.y() - home.y();
        float length = (float) Math.hypot(dx, dy);
        if (length < 1.0F) { dx = 1.0F; dy = 0.0F; }
        else { dx /= length; dy /= length; }
        float rightX = -dy;
        float rightY = dx;
        float baseForward = lockMode
                ? -Math.min(towerRange - 28.0F, towerRange * 0.72F) : 55.0F;
        float forward = baseForward + radialOffset;
        return new WorldPoint(resource.x() + dx * forward + rightX * lateralOffset,
                resource.y() + dy * forward + rightY * lateralOffset);
    }
}
