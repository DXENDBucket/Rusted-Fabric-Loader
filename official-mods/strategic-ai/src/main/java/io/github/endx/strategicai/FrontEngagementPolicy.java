package io.github.endx.strategicai;

/** Pure transition thresholds for tower-line combat. */
final class FrontEngagementPolicy {
    private FrontEngagementPolicy() {
    }

    static StrategicFrontState.Mode select(float exchangeRatio,
            int friendlyCount, int enemyDefenses) {
        if (enemyDefenses <= 0) return StrategicFrontState.Mode.OPEN;
        if (exchangeRatio >= 1.35F && friendlyCount >= 6) {
            return StrategicFrontState.Mode.ASSAULT;
        }
        if (exchangeRatio >= 0.72F && friendlyCount >= 4) {
            return StrategicFrontState.Mode.MUSTER;
        }
        return StrategicFrontState.Mode.ATTRITION;
    }
}
