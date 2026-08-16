package io.github.endx.strategicai;

/** Pure per-unit combat policy shared by live micro control and verification. */
final class UnitMicroPolicy {
    enum Decision {
        RETREAT,
        HOLD_FIRE_WINDOW,
        STANDOFF,
        EDGE_CONTROL,
        RUSH,
        DISENGAGE,
        ENGAGE,
        SUPPORT
    }

    private UnitMicroPolicy() {
    }

    static Decision select(float healthFraction, boolean recentlyDamaged,
            boolean underThreat, boolean canEngage, boolean safeStandoff,
            boolean attackerWithinRange, boolean defenderWithinRange) {
        float health = Float.isFinite(healthFraction)
                ? Math.max(0.0F, Math.min(1.0F, healthFraction)) : 0.0F;
        if (health <= 0.10F || underThreat && (health <= 0.22F
                || recentlyDamaged && health <= 0.36F && defenderWithinRange)) {
            return Decision.RETREAT;
        }
        if (safeStandoff) {
            return attackerWithinRange && !defenderWithinRange
                    ? Decision.HOLD_FIRE_WINDOW : Decision.STANDOFF;
        }
        return canEngage ? Decision.ENGAGE : Decision.SUPPORT;
    }

    static boolean recovered(float healthFraction) {
        return Float.isFinite(healthFraction) && healthFraction >= 0.58F;
    }

    static Decision selectLive(float healthFraction, boolean recentlyDamaged,
            boolean canEngage, boolean canReturnFire,
            float attackerRange, float returnFireRange, float centerDistance,
            float attackerSpeed, float targetSpeed, float localStrengthRatio) {
        float health = finite(healthFraction, 0.0F, 1.0F);
        float ownRange = finite(attackerRange, 0.0F, Float.MAX_VALUE);
        float enemyRange = finite(returnFireRange, 0.0F, Float.MAX_VALUE);
        float distance = finite(centerDistance, 0.0F, Float.MAX_VALUE);
        float ownSpeed = finite(attackerSpeed, 0.0F, Float.MAX_VALUE);
        float enemySpeed = finite(targetSpeed, 0.0F, Float.MAX_VALUE);
        float strength = finite(localStrengthRatio, 0.0F, 20.0F);
        boolean inEnemyReach = canReturnFire && distance <= enemyRange + 36.0F;
        if (health <= 0.10F || recentlyDamaged && inEnemyReach && health <= 0.30F) {
            return Decision.RETREAT;
        }
        if (!canEngage) return inEnemyReach ? Decision.DISENGAGE : Decision.SUPPORT;

        // Equal displayed ranges still benefit from edge control: the faster responder can
        // repeatedly cross its own firing boundary instead of sitting in the middle of return fire.
        if (!canReturnFire || ownRange + 3.0F >= enemyRange) {
            float edge = desiredEdgeDistance(ownRange);
            return Math.abs(distance - edge) <= 7.0F
                    ? Decision.HOLD_FIRE_WINDOW : Decision.EDGE_CONTROL;
        }

        boolean meaningfullyFaster = ownSpeed >= enemySpeed * 1.08F + 0.03F;
        if (strength >= 1.35F || meaningfullyFaster && strength >= 0.92F) {
            return Decision.RUSH;
        }
        if (inEnemyReach && strength < 0.92F) return Decision.DISENGAGE;
        return Decision.ENGAGE;
    }

    static float desiredEdgeDistance(float attackerRange) {
        return Math.max(8.0F, attackerRange - 3.0F);
    }

    private static float finite(float value, float minimum, float maximum) {
        if (!Float.isFinite(value)) return minimum;
        return Math.max(minimum, Math.min(maximum, value));
    }
}
