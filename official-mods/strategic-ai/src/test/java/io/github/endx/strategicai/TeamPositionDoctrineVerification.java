package io.github.endx.strategicai;

import java.util.Arrays;
import java.util.Map;

public final class TeamPositionDoctrineVerification {
    private TeamPositionDoctrineVerification() {
    }

    public static void main(String[] args) {
        Map<Integer, TeamPositionDoctrine.Role> roles = TeamPositionDoctrine.allocate(Arrays.asList(
                new TeamPositionDoctrine.Candidate(1, 1400.0F, 2.0F),
                new TeamPositionDoctrine.Candidate(2, 1500.0F, 1.7F),
                new TeamPositionDoctrine.Candidate(3, 700.0F, 1.1F),
                new TeamPositionDoctrine.Candidate(4, 1200.0F, 3.4F)));
        require(roles.get(3) == TeamPositionDoctrine.Role.FRONTLINE,
                "the position nearest the enemy was not assigned to the front");
        require(roles.get(4) == TeamPositionDoctrine.Role.ECONOMY_TECH,
                "the safe resource-rich position was not assigned to economy/tech");
        require(roles.get(1) == TeamPositionDoctrine.Role.MOBILE_SUPPORT
                        && roles.get(2) == TeamPositionDoctrine.Role.MOBILE_SUPPORT,
                "remaining positions were not reserved for mobile support");
        require(TeamPositionDoctrine.allocate(Arrays.asList(
                new TeamPositionDoctrine.Candidate(7, 900.0F, 1.0F)))
                .get(7) == TeamPositionDoctrine.Role.SOLO,
                "a solo AI was assigned a team-only position");
        System.out.println("Strategic AI team position doctrine contracts passed");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
