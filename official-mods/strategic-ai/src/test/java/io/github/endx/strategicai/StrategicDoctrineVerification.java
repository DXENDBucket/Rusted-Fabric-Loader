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
        require(expand.get(1L) == AiForceRole.STATIC_DEFENSE,
                "the durable slow unit was not reserved for defense");
        require(expand.get(2L) == AiForceRole.RAIDER,
                "the fastest unit was not assigned to raiding");
        require(expand.get(3L) == AiForceRole.FRONTLINE,
                "the remaining force was not assigned to the main group");

        Map<Long, AiForceRole> fortify = ForceRoleAllocator.allocate(
                force, StrategicPosture.FORTIFY, 2);
        require(count(fortify, AiForceRole.STATIC_DEFENSE) == 3,
                "fortify posture did not scale defenders with the threat");
        require(count(fortify, AiForceRole.RAIDER) == 0,
                "fortify posture still detached a raiding group");
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
