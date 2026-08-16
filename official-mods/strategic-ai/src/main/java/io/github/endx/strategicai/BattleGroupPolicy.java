package io.github.endx.strategicai;

/** Pure cohesion rules for releasing a land battle group from its rally line. */
final class BattleGroupPolicy {
    private static final float OPEN_READY_FRACTION = 0.50F;
    private static final float ASSAULT_READY_FRACTION = 0.60F;

    private BattleGroupPolicy() {
    }

    static boolean shouldCommit(StrategicFrontState.Mode mode,
            int totalUnits, int readyUnits, int minimumGroup) {
        if (mode == null || totalUnits <= 0 || readyUnits <= 0) return false;
        if (minimumGroup <= 0) throw new IllegalArgumentException(
                "minimumGroup must be positive");
        if (mode != StrategicFrontState.Mode.OPEN
                && mode != StrategicFrontState.Mode.ASSAULT) return false;
        int absoluteMinimum = Math.min(totalUnits, minimumGroup);
        float fraction = mode == StrategicFrontState.Mode.ASSAULT
                ? ASSAULT_READY_FRACTION : OPEN_READY_FRACTION;
        int cohesiveMinimum = Math.max(absoluteMinimum,
                (int) Math.ceil(totalUnits * fraction));
        return readyUnits >= cohesiveMinimum;
    }

    static boolean readyForFront(float distanceToRally,
            float distanceToFront, float rallyToFront) {
        if (!Float.isFinite(distanceToRally) || !Float.isFinite(distanceToFront)
                || !Float.isFinite(rallyToFront)) return false;
        // Units already beyond the rally line are part of the vanguard and must not be
        // recalled merely because the remainder of the group is still assembling.
        return distanceToRally <= 145.0F
                || distanceToFront <= rallyToFront + 90.0F;
    }
}
