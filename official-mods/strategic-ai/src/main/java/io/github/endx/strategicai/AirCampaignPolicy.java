package io.github.endx.strategicai;

/** Pure mode selection for the allied air campaign. */
final class AirCampaignPolicy {
    private AirCampaignPolicy() {
    }

    static StrategicAirPlan.Mode select(boolean assembled,
            float friendlyAirToAir, float enemyAirToAir,
            boolean strikeAvailable) {
        if (!assembled) return StrategicAirPlan.Mode.REGROUP;
        if (enemyAirToAir > 0.01F) {
            return friendlyAirToAir >= enemyAirToAir * 0.76F
                    ? StrategicAirPlan.Mode.INTERCEPT
                    : StrategicAirPlan.Mode.REGROUP;
        }
        return strikeAvailable ? StrategicAirPlan.Mode.STRIKE
                : StrategicAirPlan.Mode.PATROL;
    }
}
