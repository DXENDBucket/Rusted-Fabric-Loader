package io.github.endx.strategicai;

import io.github.endx.rustedfabricapi.api.world.WorldPoint;

/** Pure conservative tower geometry around a contested resource point. */
final class ForwardTowerGeometry {
    private ForwardTowerGeometry() {
    }

    static WorldPoint placement(WorldPoint home, WorldPoint resource, float towerRange,
            float radialOffset, float lateralOffset) {
        float dx = resource.x() - home.x();
        float dy = resource.y() - home.y();
        float length = (float) Math.hypot(dx, dy);
        if (length < 1.0F) { dx = 1.0F; dy = 0.0F; }
        else { dx /= length; dy /= length; }
        float rightX = -dy;
        float rightY = dx;
        float baseForward = -Math.max(230.0F, towerRange + 70.0F);
        float forward = baseForward + radialOffset;
        return new WorldPoint(resource.x() + dx * forward + rightX * lateralOffset,
                resource.y() + dy * forward + rightY * lateralOffset);
    }

    static WorldPoint safeFallback(WorldPoint home, WorldPoint enemyTower,
            float enemyRange, float safetyMargin) {
        if (home == null || enemyTower == null) throw new NullPointerException("point");
        if (!Float.isFinite(enemyRange) || enemyRange < 0.0F
                || !Float.isFinite(safetyMargin) || safetyMargin < 0.0F) {
            throw new IllegalArgumentException("ranges must be finite and non-negative");
        }
        float dx = home.x() - enemyTower.x();
        float dy = home.y() - enemyTower.y();
        float length = (float) Math.hypot(dx, dy);
        if (length < 0.001F) { dx = 1.0F; dy = 0.0F; length = 1.0F; }
        float distance = enemyRange + safetyMargin;
        return new WorldPoint(enemyTower.x() + dx / length * distance,
                enemyTower.y() + dy / length * distance);
    }
}
