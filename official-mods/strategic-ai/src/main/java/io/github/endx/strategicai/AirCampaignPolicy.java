package io.github.endx.strategicai;

/** Pure mode selection for the allied air campaign. */
final class AirCampaignPolicy {
    private AirCampaignPolicy() {
    }

    static StrategicAirPlan.Mode select(boolean assembled,
            float friendlyAirToAir, float enemyAirToAir,
            boolean strikeAvailable) {
        if (enemyAirToAir > 0.01F) {
            if (!assembled && friendlyAirToAir < enemyAirToAir * 1.15F) {
                return StrategicAirPlan.Mode.REGROUP;
            }
            return friendlyAirToAir >= enemyAirToAir * 0.76F
                    ? StrategicAirPlan.Mode.INTERCEPT
                    : StrategicAirPlan.Mode.REGROUP;
        }
        if (!assembled) return StrategicAirPlan.Mode.REGROUP;
        return strikeAvailable ? StrategicAirPlan.Mode.STRIKE
                : StrategicAirPlan.Mode.PATROL;
    }
}
