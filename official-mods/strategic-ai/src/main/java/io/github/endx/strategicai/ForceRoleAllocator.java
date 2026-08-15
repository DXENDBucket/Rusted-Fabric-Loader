package io.github.endx.strategicai;

import io.github.endx.rustedfabricapi.api.ai.AiForceRole;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Deterministically divides a force into defenders, raiders, and the main battle group. */
final class ForceRoleAllocator {
    private ForceRoleAllocator() {
    }

    static Map<Long, AiForceRole> allocate(List<Candidate> source,
            StrategicPosture posture, int homeThreats) {
        ArrayList<Candidate> remaining = new ArrayList<Candidate>(source);
        LinkedHashMap<Long, AiForceRole> result = new LinkedHashMap<Long, AiForceRole>();
        int defenders = defenderQuota(remaining.size(), posture, homeThreats);
        remaining.sort(Comparator.comparingDouble((Candidate value) -> -value.maxHealth)
                .thenComparingDouble(value -> value.speed)
                .thenComparingLong(value -> value.id));
        take(remaining, result, defenders, AiForceRole.STATIC_DEFENSE);

        int raiders = raiderQuota(source.size(), posture);
        remaining.sort(Comparator.comparingDouble((Candidate value) -> -value.speed)
                .thenComparingDouble(value -> value.maxHealth)
                .thenComparingLong(value -> value.id));
        take(remaining, result, raiders, AiForceRole.RAIDER);

        remaining.sort(Comparator.comparingLong(value -> value.id));
        for (Candidate candidate : remaining) result.put(candidate.id, AiForceRole.FRONTLINE);
        return result;
    }

    private static int defenderQuota(int size, StrategicPosture posture, int homeThreats) {
        if (size == 0) return 0;
        if (posture == StrategicPosture.FORTIFY) {
            return Math.min(size, Math.max(homeThreats + 1, (size + 1) / 2));
        }
        return size >= (posture == StrategicPosture.EXPAND ? 4 : 5) ? 1 : 0;
    }

    private static int raiderQuota(int size, StrategicPosture posture) {
        if (posture == StrategicPosture.FORTIFY || size < 2) return 0;
        return Math.max(1, size / (posture == StrategicPosture.EXPAND ? 4 : 5));
    }

    private static void take(List<Candidate> remaining, Map<Long, AiForceRole> result,
            int count, AiForceRole role) {
        int actual = Math.min(count, remaining.size());
        for (int index = 0; index < actual; index++) {
            result.put(remaining.get(index).id, role);
        }
        if (actual > 0) remaining.subList(0, actual).clear();
    }

    static final class Candidate {
        final long id;
        final float speed;
        final float maxHealth;

        Candidate(long id, float speed, float maxHealth) {
            this.id = id;
            this.speed = speed;
            this.maxHealth = maxHealth;
        }
    }
}
