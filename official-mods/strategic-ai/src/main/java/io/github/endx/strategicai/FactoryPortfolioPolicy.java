package io.github.endx.strategicai;

import io.github.endx.rustedfabricapi.api.ai.AiUnitTypeCapabilities;

import java.util.List;

/** Generic marginal-value scoring for a new production family. */
final class FactoryPortfolioPolicy {
    private FactoryPortfolioPolicy() {
    }

    static Profile profile(AiUnitTypeCapabilities unit) {
        return new Profile(log(unit.maximumHealth() + unit.maximumShield()),
                log(unit.estimatedGroundDps()), log(unit.estimatedAirDps()),
                unit.maximumAttackRange() / 260.0D,
                unit.movementSpeed() / 2.4D, log(unit.buildSpeed() * 1000.0D),
                log(unit.estimatedAreaDps()),
                unit.maximumAreaDamageRadius() / 90.0D,
                unit.canAttackGround(), unit.canAttackAir(),
                unit.areaWeapon(), unit.flameWeapon(),
                unit.maximumWarmupTime() > 0.01F,
                unit.retainsWarmupAfterFiring());
    }

    static double novelty(List<Profile> candidate, List<Profile> existing) {
        if (candidate.isEmpty() || existing.isEmpty()) return 0.0D;
        double first = 0.0D;
        double second = 0.0D;
        for (Profile product : candidate) {
            double nearest = Double.POSITIVE_INFINITY;
            for (Profile present : existing) {
                nearest = Math.min(nearest, distance(product, present));
            }
            if (nearest > first) {
                second = first;
                first = nearest;
            } else if (nearest > second) {
                second = nearest;
            }
        }
        return first + second * 0.55D;
    }

    static double distance(Profile first, Profile second) {
        double sum = square(first.durability - second.durability) * 0.75D
                + square(first.groundDps - second.groundDps)
                + square(first.airDps - second.airDps)
                + square(first.range - second.range) * 1.25D
                + square(first.speed - second.speed) * 0.70D
                + square(first.throughput - second.throughput) * 0.65D
                + square(first.areaDps - second.areaDps) * 0.75D
                + square(first.areaRadius - second.areaRadius) * 0.90D;
        if (first.attacksGround != second.attacksGround) sum += 0.65D;
        if (first.attacksAir != second.attacksAir) sum += 0.80D;
        if (first.areaWeapon != second.areaWeapon) sum += 0.80D;
        if (first.flameWeapon != second.flameWeapon) sum += 0.25D;
        if (first.warmup != second.warmup) sum += 0.30D;
        if (first.retainsWarmup != second.retainsWarmup) sum += 0.25D;
        return Math.sqrt(sum);
    }

    private static double log(double value) {
        return Math.log1p(Math.max(0.0D, value));
    }

    private static double square(double value) { return value * value; }

    static final class Profile {
        final double durability;
        final double groundDps;
        final double airDps;
        final double range;
        final double speed;
        final double throughput;
        final double areaDps;
        final double areaRadius;
        final boolean attacksGround;
        final boolean attacksAir;
        final boolean areaWeapon;
        final boolean flameWeapon;
        final boolean warmup;
        final boolean retainsWarmup;

        Profile(double durability, double groundDps, double airDps,
                double range, double speed, double throughput,
                double areaDps, double areaRadius,
                boolean attacksGround, boolean attacksAir,
                boolean areaWeapon, boolean flameWeapon,
                boolean warmup, boolean retainsWarmup) {
            this.durability = durability;
            this.groundDps = groundDps;
            this.airDps = airDps;
            this.range = range;
            this.speed = speed;
            this.throughput = throughput;
            this.areaDps = areaDps;
            this.areaRadius = areaRadius;
            this.attacksGround = attacksGround;
            this.attacksAir = attacksAir;
            this.areaWeapon = areaWeapon;
            this.flameWeapon = flameWeapon;
            this.warmup = warmup;
            this.retainsWarmup = retainsWarmup;
        }
    }
}
