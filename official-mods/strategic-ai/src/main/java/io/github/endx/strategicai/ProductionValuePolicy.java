package io.github.endx.strategicai;

/** Pure scoring helpers for production choices that need deterministic verification. */
final class ProductionValuePolicy {
    private ProductionValuePolicy() {
    }

    static double airSuperiorityPower(double durability, double airDps,
            double attackRange, double movementSpeed) {
        double directPower = Math.sqrt(Math.max(1.0D, durability)
                * Math.max(0.04D, airDps));
        double normalizedSpeed = clamp(movementSpeed / 2.0D, 0.30D, 1.45D);
        // Air-to-air range only matters if the aircraft can establish and preserve the firing
        // distance. Slow interceptors must not win on a paper range/DPS comparison alone.
        double mobility = 0.30D + 0.70D * Math.pow(normalizedSpeed, 1.35D);
        double range = 1.0D + Math.min(0.32D, Math.max(0.0D, attackRange) / 1100.0D);
        return directPower * mobility * range;
    }

    static double balancedCombatValue(double combatPower, double costEfficiency) {
        return balancedCombatValue(combatPower, costEfficiency, 0.0D);
    }

    static double balancedCombatValue(double combatPower, double costEfficiency,
            double productionThroughput) {
        // Both absolute quality and efficiency matter, but neither may grow linearly forever.
        // The previous linear efficiency multiplier let a ~20% efficiency lead outweigh every
        // range, tech, composition, and battlefield-role signal combined.
        return Math.log1p(Math.max(0.0D, combatPower)) * 3.0D
                + Math.log1p(Math.max(0.0D, costEfficiency)) * 6.0D
                + Math.log1p(Math.max(0.0D, productionThroughput)) * 5.0D;
    }

    static double productionThroughput(double combatPower, double buildSpeed) {
        // Multiplication by 1000 only gives normal game build speeds a human-sized scale.
        // The ratio remains combat power produced by one factory slot per unit time.
        return Math.max(0.0D, combatPower) * Math.max(0.0D, buildSpeed) * 1000.0D;
    }

    static double creditBurnPerSecond(double creditCost, double buildSpeed) {
        return Math.max(0.0D, creditCost) * Math.max(0.0D, buildSpeed) * 60.0D;
    }

    static int sustainableProducerTarget(double incomePerSecond, double bankedCredits,
            double reserveCredits, double creditBurnPerSecond,
            double burstHorizonSeconds, int maximum) {
        if (maximum <= 1 || creditBurnPerSecond <= 0.0D) return 1;
        double spendableBank = Math.max(0.0D, bankedCredits - reserveCredits);
        double sustainableBudget = Math.max(0.0D, incomePerSecond)
                + spendableBank / Math.max(1.0D, burstHorizonSeconds);
        int target = (int) Math.floor(sustainableBudget / creditBurnPerSecond + 0.08D);
        return Math.max(1, Math.min(maximum, target));
    }

    static double airSuperiorityInvestmentValue(double durability, double airDps,
            double attackRange, double movementSpeed, double creditCost,
            double buildSpeed, int techLevel) {
        double power = airSuperiorityPower(
                durability, airDps, attackRange, movementSpeed);
        double efficiency = power * 1000.0D / Math.max(1.0D, creditCost);
        // Efficiency represents an equal-credit squad fight. A small individual-quality term
        // prevents a barely more efficient disposable aircraft from always displacing a much
        // stronger interceptor, while tech level is only a tie-break-sized signal.
        double throughput = productionThroughput(power, buildSpeed);
        return efficiency + Math.log1p(power) * 0.18D
                + Math.log1p(throughput) * 0.35D
                + Math.max(0, techLevel) * 0.06D;
    }

    static boolean exactTypeSaturated(int sameType, int groundCombatTotal) {
        if (groundCombatTotal < 6) return false;
        return sameType >= concentrationLimit(groundCombatTotal);
    }

    static double exactTypeSaturationPenalty(int sameType, int groundCombatTotal) {
        if (!exactTypeSaturated(sameType, groundCombatTotal)) return 0.0D;
        int excess = sameType - concentrationLimit(groundCombatTotal) + 1;
        double share = sameType / (double) Math.max(1, groundCombatTotal);
        return excess * 2.4D + Math.max(0.0D, share - 0.50D) * 12.0D;
    }

    private static int concentrationLimit(int total) {
        return Math.max(3, (int) Math.ceil(total * 0.45D));
    }

    private static double clamp(double value, double minimum, double maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }
}
