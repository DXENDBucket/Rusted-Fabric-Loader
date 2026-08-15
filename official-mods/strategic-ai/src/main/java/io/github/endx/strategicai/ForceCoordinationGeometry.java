package io.github.endx.strategicai;

import io.github.endx.rustedfabricapi.api.world.WorldPoint;

/** Pure deterministic geometry for staging and reinforcement orders. */
final class ForceCoordinationGeometry {
    private ForceCoordinationGeometry() {
    }

    static WorldPoint advance(WorldPoint origin, WorldPoint objective, float maximumDistance) {
        if (origin == null || objective == null) throw new NullPointerException("point");
        if (!Float.isFinite(maximumDistance) || maximumDistance < 0.0F) {
            throw new IllegalArgumentException("maximumDistance must be finite and non-negative");
        }
        float dx = objective.x() - origin.x();
        float dy = objective.y() - origin.y();
        float distance = (float) Math.sqrt(dx * dx + dy * dy);
        if (distance <= maximumDistance || distance <= 0.0001F) return objective;
        float scale = maximumDistance / distance;
        return new WorldPoint(origin.x() + dx * scale, origin.y() + dy * scale);
    }

    static boolean arrived(WorldPoint position, WorldPoint destination, float radius) {
        if (position == null || destination == null) throw new NullPointerException("point");
        if (!Float.isFinite(radius) || radius < 0.0F) {
            throw new IllegalArgumentException("radius must be finite and non-negative");
        }
        return position.distanceSquared(destination) <= radius * radius;
    }
}
