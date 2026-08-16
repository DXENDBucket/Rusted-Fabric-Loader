package io.github.endx.strategicai;

/** Pure hysteresis rules that keep dynamic team assignments from oscillating. */
final class StrategicReplanPolicy {
    private static final float MATERIAL_ADVANTAGE_RATIO = 0.82F;
    private static final float MATERIAL_ADVANTAGE_ABSOLUTE = 90.0F;

    private StrategicReplanPolicy() {
    }

    static boolean acceptFrontlineSwitch(float currentCost, float proposedCost,
            boolean currentOperational, boolean teamSetChanged) {
        if (teamSetChanged || !currentOperational || !Float.isFinite(currentCost)) return true;
        if (!Float.isFinite(proposedCost)) return false;
        return proposedCost + MATERIAL_ADVANTAGE_ABSOLUTE < currentCost
                && proposedCost < currentCost * MATERIAL_ADVANTAGE_RATIO;
    }
}
