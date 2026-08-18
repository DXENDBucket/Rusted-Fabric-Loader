package io.github.endx.strategicai;

import io.github.endx.rustedfabricapi.api.world.WorldPoint;

/** Pure budget and quota rules for forward and base-perimeter defenses. */
final class DefensiveInvestmentPolicy {
    private DefensiveInvestmentPolicy() {
    }

    static boolean canAfford(double credits, double effectiveIncomePerSecond,
            double cost, boolean urgentAirDefense, boolean frontline) {
        if (!Double.isFinite(cost) || cost <= 0.0D) return false;
        double reserveSeconds = urgentAirDefense ? 10.0D : frontline ? 15.0D : 22.0D;
        double reserve = Math.max(650.0D,
                Math.max(0.0D, effectiveIncomePerSecond) * reserveSeconds);
        double fallbackFactor = urgentAirDefense ? 1.10D : frontline ? 1.22D : 1.38D;
        return credits >= cost + reserve || credits >= cost * fallbackFactor;
    }

    static int desiredForwardGround(boolean leadsFrontline, double gameSeconds,
            double effectiveIncomePerSecond) {
        if (!leadsFrontline) return gameSeconds >= 300.0D && effectiveIncomePerSecond >= 45.0D
                ? 1 : 0;
        if (gameSeconds < 210.0D || effectiveIncomePerSecond < 42.0D) return 1;
        return 2;
    }

    static int desiredBaseGround(double gameSeconds, int combatFactories,
            double effectiveIncomePerSecond) {
        if (gameSeconds < 75.0D || combatFactories <= 0) return 0;
        int result = 1;
        if (combatFactories >= 2 && gameSeconds >= 210.0D) result++;
        if (combatFactories >= 4 && effectiveIncomePerSecond >= 70.0D) result++;
        return Math.min(3, result);
    }

    static int desiredAntiAir(boolean airDisadvantage, boolean forward,
            double gameSeconds, int combatFactories) {
        if (!airDisadvantage || gameSeconds < 55.0D) return 0;
        if (forward) return 1;
        return combatFactories >= 3 && gameSeconds >= 300.0D ? 2 : 1;
    }

    static double placementPriority(WorldPoint center, WorldPoint threat,
            WorldPoint candidate, float worldWidth, float worldHeight) {
        float dx = threat.x() - center.x();
        float dy = threat.y() - center.y();
        float length = (float) Math.hypot(dx, dy);
        if (length < 0.001F) { dx = 1.0F; dy = 0.0F; }
        else { dx /= length; dy /= length; }
        double forward = ((candidate.x() - center.x()) * dx
                + (candidate.y() - center.y()) * dy) / 110.0D;
        float edge = Math.min(Math.min(candidate.x(), worldWidth - candidate.x()),
                Math.min(candidate.y(), worldHeight - candidate.y()));
        double edgeSafety = Math.max(-1.0D, Math.min(1.0D, edge / 150.0D));
        return forward * 1.55D + edgeSafety * 1.20D;
    }
}
