package io.github.endx.strategicai;

import io.github.endx.rustedfabricapi.api.world.WorldPoint;

import java.util.Collections;

public final class StrategicInvestmentVerification {
    private StrategicInvestmentVerification() {
    }

    public static void main(String[] args) {
        require(!EconomicInvestmentPolicy.shouldInvest(
                        EconomicInvestmentPolicy.Kind.RESOURCE_MANUFACTURER,
                        90.0D, 10000.0D, 40.0D,
                        1500.0D, 2.0D, 1.0D, 25.0D, false),
                "a resource manufacturer was built during the opening");
        require(EconomicInvestmentPolicy.shouldInvest(
                        EconomicInvestmentPolicy.Kind.RESOURCE_UPGRADE,
                        240.0D, 7000.0D, 55.0D,
                        1500.0D, 4.0D, 1.0D, 20.0D, false),
                "a normal extractor upgrade with acceptable payback was rejected");
        require(!EconomicInvestmentPolicy.shouldInvest(
                        EconomicInvestmentPolicy.Kind.RESOURCE_UPGRADE,
                        240.0D, 7000.0D, 55.0D,
                        1500.0D, 1.0D, 1.0D, 20.0D, false),
                "a very slow economy upgrade ignored its payback time");
        require(EconomicInvestmentPolicy.paybackSeconds(
                        1500.0D, 4.0D, 1.8D, 20.0D)
                        < EconomicInvestmentPolicy.paybackSeconds(
                        1500.0D, 4.0D, 1.0D, 20.0D),
                "AI and match income multipliers did not improve the payback calculation");
        require(EconomicInvestmentPolicy.manufacturerLimit(900.0D, 160.0D, true)
                        > EconomicInvestmentPolicy.manufacturerLimit(200.0D, 40.0D, false),
                "late high-income economy did not receive a larger manufacturer budget");

        require(DefensiveInvestmentPolicy.desiredForwardGround(
                        true, 100.0D, 30.0D) == 1,
                "frontline position did not retain a conservative first tower");
        require(DefensiveInvestmentPolicy.desiredForwardGround(
                        false, 100.0D, 30.0D) == 0,
                "rear position copied the frontline opening tower");
        require(DefensiveInvestmentPolicy.desiredAntiAir(
                        true, false, 180.0D, 2) == 1,
                "air disadvantage did not request base anti-air");
        require(DefensiveInvestmentPolicy.desiredAntiAir(
                        false, false, 180.0D, 2) == 0,
                "air parity incorrectly forced anti-air construction");
        require(!DefensiveInvestmentPolicy.canAfford(
                        700.0D, 45.0D, 600.0D, false, false),
                "base tower consumed the production reserve");
        WorldPoint edgeBase = new WorldPoint(80.0F, 110.0F);
        WorldPoint enemy = new WorldPoint(1800.0F, 900.0F);
        double forward = DefensiveInvestmentPolicy.placementPriority(
                edgeBase, enemy, new WorldPoint(260.0F, 180.0F), 2000.0F, 1000.0F);
        double rear = DefensiveInvestmentPolicy.placementPriority(
                edgeBase, enemy, new WorldPoint(20.0F, 40.0F), 2000.0F, 1000.0F);
        require(forward > rear,
                "map-edge defense preferred the rear edge over the enemy-facing interior");

        FactoryPortfolioPolicy.Profile direct = new FactoryPortfolioPolicy.Profile(
                4.0D, 3.0D, 0.0D, 0.7D, 0.8D, 1.0D,
                0.0D, 0.0D, true, false, false, false, false, false);
        FactoryPortfolioPolicy.Profile duplicate = new FactoryPortfolioPolicy.Profile(
                4.0D, 3.0D, 0.0D, 0.7D, 0.8D, 1.0D,
                0.0D, 0.0D, true, false, false, false, false, false);
        FactoryPortfolioPolicy.Profile crowdControl = new FactoryPortfolioPolicy.Profile(
                3.7D, 2.8D, 0.0D, 0.6D, 0.75D, 1.1D,
                2.5D, 1.25D, true, false, true, true, false, false);
        require(FactoryPortfolioPolicy.novelty(Collections.singletonList(duplicate),
                        Collections.singletonList(direct)) < 0.001D,
                "a duplicate production line received marginal portfolio value");
        require(FactoryPortfolioPolicy.novelty(Collections.singletonList(crowdControl),
                        Collections.singletonList(direct)) > 1.0D,
                "an area/flame production line was not recognized as a new tactical capability");
        System.out.println("Strategic AI economy and defense investment contracts passed");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
