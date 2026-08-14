package io.github.endx.strategicai;

/** Pure deterministic geometry used by the target-specific standoff planner. */
final class StandoffGeometry {
    private StandoffGeometry() {
    }

    static float desiredDistance(float attackerRange, float returnFireRange,
            float safetyPadding, float innerPadding) {
        float lower = returnFireRange + safetyPadding;
        float upper = attackerRange - innerPadding;
        return upper > lower ? lower + (upper - lower) * 0.5F : Float.NaN;
    }

    static Position position(float attackerX, float attackerY, long attackerId,
            float targetX, float targetY, float desiredDistance) {
        float dx = attackerX - targetX;
        float dy = attackerY - targetY;
        float length = (float) Math.sqrt(dx * dx + dy * dy);
        if (length < 0.001F) {
            double angle = Math.floorMod(attackerId * 137L, 360L) * Math.PI / 180.0;
            dx = (float) Math.cos(angle);
            dy = (float) Math.sin(angle);
        } else {
            dx /= length;
            dy /= length;
        }
        return new Position(targetX + dx * desiredDistance,
                targetY + dy * desiredDistance);
    }

    static final class Position {
        final float x;
        final float y;

        Position(float x, float y) {
            this.x = x;
            this.y = y;
        }
    }
}
