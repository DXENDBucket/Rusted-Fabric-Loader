package io.github.endx.strategicai;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Pure positional doctrine used to give allied AIs distinct jobs at game start. */
final class TeamPositionDoctrine {
    enum Role {
        SOLO,
        FRONTLINE,
        ECONOMY_TECH,
        MOBILE_SUPPORT
    }

    private TeamPositionDoctrine() {
    }

    static Map<Integer, Role> allocate(List<Candidate> source) {
        if (source.isEmpty()) return Collections.emptyMap();
        ArrayList<Candidate> candidates = new ArrayList<Candidate>(source);
        candidates.sort(Comparator.comparingInt(value -> value.teamId));
        LinkedHashMap<Integer, Role> result = new LinkedHashMap<Integer, Role>();
        if (candidates.size() == 1) {
            result.put(candidates.get(0).teamId, Role.SOLO);
            return result;
        }

        Candidate frontline = Collections.min(candidates, Comparator
                .comparingInt((Candidate value) -> value.operational ? 0 : 1)
                .thenComparingDouble(value -> value.frontAccessCost)
                .thenComparingInt(value -> value.teamId));
        result.put(frontline.teamId, Role.FRONTLINE);

        Candidate economy = null;
        float bestEconomyScore = Float.NEGATIVE_INFINITY;
        for (Candidate candidate : candidates) {
            if (candidate == frontline || !candidate.operational) continue;
            float safety = candidate.frontAccessCost
                    / Math.max(1.0F, frontline.frontAccessCost);
            float score = candidate.safeResourcePotential * 0.7F
                    + Math.min(2.0F, safety) * 0.3F;
            if (score > bestEconomyScore || score == bestEconomyScore
                    && (economy == null || candidate.teamId < economy.teamId)) {
                economy = candidate;
                bestEconomyScore = score;
            }
        }
        if (economy != null) result.put(economy.teamId, Role.ECONOMY_TECH);
        for (Candidate candidate : candidates) {
            result.putIfAbsent(candidate.teamId, Role.MOBILE_SUPPORT);
        }
        return Collections.unmodifiableMap(result);
    }

    static final class Candidate {
        final int teamId;
        final float frontAccessCost;
        final float safeResourcePotential;
        final boolean operational;

        Candidate(int teamId, float frontAccessCost, float safeResourcePotential) {
            this(teamId, frontAccessCost, safeResourcePotential, true);
        }

        Candidate(int teamId, float frontAccessCost, float safeResourcePotential,
                boolean operational) {
            this.teamId = teamId;
            this.frontAccessCost = frontAccessCost;
            this.safeResourcePotential = safeResourcePotential;
            this.operational = operational;
        }
    }
}
