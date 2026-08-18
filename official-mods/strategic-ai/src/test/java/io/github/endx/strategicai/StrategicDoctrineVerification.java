package io.github.endx.strategicai;

import io.github.endx.rustedfabricapi.api.ai.AiForceRole;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public final class StrategicDoctrineVerification {
    private StrategicDoctrineVerification() {
    }

    public static void main(String[] args) {
        require(StrategicPosture.select(2, 1, true) == StrategicPosture.FORTIFY,
                "a threatened small force did not fortify");
        require(StrategicPosture.select(4, 0, true) == StrategicPosture.EXPAND,
                "an uncontested early force did not expand");
        require(StrategicPosture.select(8, 0, true) == StrategicPosture.PRESSURE,
                "a developed force did not apply pressure");

        List<ForceRoleAllocator.Candidate> force = Arrays.asList(
                new ForceRoleAllocator.Candidate(1L, 0.8F, 900.0F),
                new ForceRoleAllocator.Candidate(2L, 2.5F, 220.0F),
                new ForceRoleAllocator.Candidate(3L, 1.0F, 500.0F),
                new ForceRoleAllocator.Candidate(4L, 1.2F, 450.0F),
                new ForceRoleAllocator.Candidate(5L, 1.1F, 400.0F));
        Map<Long, AiForceRole> expand = ForceRoleAllocator.allocate(
                force, StrategicPosture.EXPAND, 0);
        require(count(expand, AiForceRole.STATIC_DEFENSE) == 0,
                "an unthreatened force still left a permanent unit at home");
        require(expand.get(2L) == AiForceRole.RAIDER,
                "the fastest unit was not assigned to raiding");
        require(expand.get(1L) == AiForceRole.FRONTLINE
                        && expand.get(3L) == AiForceRole.FRONTLINE,
                "the remaining force was not assigned to the main group");

        Map<Long, AiForceRole> fortify = ForceRoleAllocator.allocate(
                force, StrategicPosture.FORTIFY, 2);
        require(count(fortify, AiForceRole.STATIC_DEFENSE) == 3,
                "fortify posture did not scale defenders with the threat");
        require(count(fortify, AiForceRole.RAIDER) == 0,
                "fortify posture still detached a raiding group");
        double fastInterceptor = ProductionValuePolicy.airSuperiorityInvestmentValue(
                900.0D, 9.0D, 190.0D, 2.4D, 1200.0D, 0.001D, 2);
        double slowPaperWinner = ProductionValuePolicy.airSuperiorityInvestmentValue(
                1050.0D, 10.0D, 220.0D, 1.15D, 1200.0D, 0.001D, 2);
        require(fastInterceptor > slowPaperWinner,
                "air-superiority scoring ignored pursuit speed");
        require(!ProductionValuePolicy.exactTypeSaturated(2, 8)
                        && ProductionValuePolicy.exactTypeSaturated(4, 8),
                "land production concentration threshold is incorrect");
        require(ProductionValuePolicy.exactTypeSaturationPenalty(6, 10)
                        > ProductionValuePolicy.exactTypeSaturationPenalty(5, 10),
                "land production monopoly penalty does not grow");
        double efficientT1 = ProductionValuePolicy.balancedCombatValue(
                Math.sqrt(500.0D * 0.76666665D),
                Math.sqrt(500.0D * 0.76666665D) * 100.0D / 600.0D);
        double strongerT2 = ProductionValuePolicy.balancedCombatValue(
                Math.sqrt(650.0D * 1.1111112D),
                Math.sqrt(650.0D * 1.1111112D) * 100.0D / 1000.0D);
        require(Math.abs(efficientT1 - strongerT2) < 1.0D,
                "a modest T1 efficiency lead still overwhelms absolute combat quality");
        double basicMechPower = Math.sqrt(500.0D * 0.76666665D);
        double minigunMechPower = 61.8D;
        double basicMech = ProductionValuePolicy.balancedCombatValue(
                basicMechPower, basicMechPower * 100.0D / 600.0D,
                ProductionValuePolicy.productionThroughput(basicMechPower, 0.0012D));
        double minigunMech = ProductionValuePolicy.balancedCombatValue(
                minigunMechPower, minigunMechPower * 100.0D / 5000.0D,
                ProductionValuePolicy.productionThroughput(minigunMechPower, 0.0006D));
        require(minigunMech > basicMech,
                "factory-slot throughput did not justify a stronger slow-built unit");
        double basicBurn = ProductionValuePolicy.creditBurnPerSecond(600.0D, 0.0012D);
        double minigunBurn = ProductionValuePolicy.creditBurnPerSecond(5000.0D, 0.0006D);
        require(Math.abs(basicBurn - 43.2D) < 0.01D
                        && Math.abs(minigunBurn - 180.0D) < 0.01D,
                "production credit burn does not match native queue timing");
        require(ProductionValuePolicy.sustainableProducerTarget(
                        45.0D, 3200.0D, 1200.0D,
                        basicBurn, 40.0D, 4) == 2,
                "a banked low-tier production rhythm did not add its second factory");
        require(ProductionValuePolicy.sustainableProducerTarget(
                        45.0D, 10000.0D, 10000.0D,
                        minigunBurn, 40.0D, 4) == 1,
                "a temporary bank incorrectly justified unsustainable high-tier factories");
        require(ProductionValuePolicy.sustainableProducerTarget(
                        360.0D, 10000.0D, 10000.0D,
                        minigunBurn, 40.0D, 4) == 2,
                "sustained income did not fund two high-tier factories");
        System.out.println("Strategic AI doctrine contracts passed");
    }

    private static int count(Map<Long, AiForceRole> roles, AiForceRole role) {
        int count = 0;
        for (AiForceRole value : roles.values()) if (value == role) count++;
        return count;
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
