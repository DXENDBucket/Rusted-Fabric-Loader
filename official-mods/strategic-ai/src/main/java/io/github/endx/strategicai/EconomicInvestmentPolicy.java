package io.github.endx.strategicai;

/** Pure timing, reserve, and payback rules for economy construction. */
final class EconomicInvestmentPolicy {
    enum Kind {
        RESOURCE_UPGRADE,
        RESOURCE_MANUFACTURER
    }

    private EconomicInvestmentPolicy() {
    }

    static boolean shouldInvest(Kind kind, double gameSeconds,
            double credits, double effectiveIncomePerSecond,
            double cost, double nominalIncomeDelta,
            double effectiveIncomeMultiplier, double constructionSeconds,
            boolean economyRole) {
        if (!finitePositive(cost) || !finitePositive(nominalIncomeDelta)
                || !finitePositive(effectiveIncomeMultiplier)) return false;
        double effectiveDelta = nominalIncomeDelta * effectiveIncomeMultiplier;
        double payback = Math.max(0.0D, constructionSeconds) + cost / effectiveDelta;
        double minimumAge = kind == Kind.RESOURCE_MANUFACTURER ? 165.0D : 38.0D;
        if (gameSeconds < minimumAge) return false;

        double maximumPayback;
        if (kind == Kind.RESOURCE_MANUFACTURER) {
            maximumPayback = gameSeconds < 420.0D ? 480.0D
                    : gameSeconds < 900.0D ? 720.0D : 900.0D;
        } else {
            maximumPayback = gameSeconds < 180.0D ? 360.0D
                    : gameSeconds < 720.0D ? 540.0D : 450.0D;
        }
        if (economyRole) maximumPayback *= 1.16D;
        if (payback > maximumPayback) return false;

        double reserveSeconds = kind == Kind.RESOURCE_MANUFACTURER ? 24.0D : 18.0D;
        double reserve = Math.max(850.0D,
                Math.max(0.0D, effectiveIncomePerSecond) * reserveSeconds);
        double minimumBankFactor = economyRole ? 1.16D : 1.32D;
        return credits >= cost + reserve || credits >= cost * minimumBankFactor + reserve * 0.35D;
    }

    static int manufacturerLimit(double gameSeconds, double effectiveIncomePerSecond,
            boolean economyRole) {
        if (gameSeconds < 165.0D) return 0;
        int result = gameSeconds < 420.0D ? 1 : gameSeconds < 780.0D ? 2 : 3;
        if (effectiveIncomePerSecond >= 80.0D) result++;
        if (effectiveIncomePerSecond >= 145.0D) result++;
        if (economyRole && gameSeconds >= 300.0D) result++;
        return Math.min(6, result);
    }

    static double paybackSeconds(double cost, double nominalIncomeDelta,
            double effectiveIncomeMultiplier, double constructionSeconds) {
        double delta = nominalIncomeDelta * effectiveIncomeMultiplier;
        return finitePositive(cost) && finitePositive(delta)
                ? Math.max(0.0D, constructionSeconds) + cost / delta
                : Double.POSITIVE_INFINITY;
    }

    private static boolean finitePositive(double value) {
        return Double.isFinite(value) && value > 0.0D;
    }
}
