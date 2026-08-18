package io.github.endx.rustedfabricapi.api.game;

import io.github.endx.rustedfabricapi.api.util.RustedReflection;

import java.util.Optional;

/** A live, namespace-neutral view of a Rusted Warfare team. */
public final class TeamView {
    private final Object team;

    TeamView(Object team) {
        this.team = team;
    }

    /** Returns the underlying mapped team for an API not covered by this view yet. */
    public Object raw() {
        return team;
    }

    public int id() {
        Object value = RustedReflection.getFieldValue(team, new String[]{"teamId", "k"});
        return value instanceof Number ? ((Number) value).intValue() : -1;
    }

    public int allianceGroup() {
        Object value = RustedReflection.getFieldValue(team,
                new String[]{"allianceGroup", "r"});
        return value instanceof Number ? ((Number) value).intValue() : -1;
    }

    public Optional<String> playerName() {
        Object value = RustedReflection.getFieldValue(team, new String[]{"playerName", "v"});
        return value instanceof String && !((String) value).isEmpty()
                ? Optional.of((String) value) : Optional.empty();
    }

    public boolean aiControlled() {
        Class<?> type = team.getClass();
        while (type != null) {
            String name = type.getName();
            if ("rustedwarfare.ai.AiTeam".equals(name)
                    || "com.corrodinggames.rts.game.a.a".equals(name)) return true;
            type = type.getSuperclass();
        }
        return false;
    }

    public double credits() {
        Object value = RustedReflection.getFieldValue(team, new String[]{"credits", "o"});
        return value instanceof Number ? ((Number) value).doubleValue() : 0.0D;
    }

    public boolean spectator() {
        return Boolean.TRUE.equals(RustedReflection.invokeInstance(team,
                new String[]{"isSpectator", "b"}));
    }

    public int totalUnitCountIncludingQueued() {
        return intResult(new String[]{"getTotalUnitCountIncludingQueued", "s"});
    }

    public int nonBuildingUnitCountIncludingQueued() {
        return intResult(new String[]{"getNonBuildingUnitCountIncludingQueued", "w"});
    }

    public int maxUnitCount() {
        return intResult(new String[]{"getMaxUnitCount", "x"});
    }

    public int incomeRate() {
        return intResult(new String[]{"getIncomeRate", "u"});
    }

    public int displayIncomeRate() {
        return intResult(new String[]{"getDisplayIncomeRate", "v"});
    }

    /** Match-wide economy multiplier selected by the game setup. */
    public float gameIncomeMultiplier() {
        return positiveFloatResult(new String[]{"getGameIncomeMultiplier", "D"});
    }

    /** Per-team AI income multiplier derived from the effective AI difficulty. */
    public float aiIncomeMultiplier() {
        return positiveFloatResult(new String[]{"getAiIncomeMultiplier", "E"});
    }

    /** Multiplier applied to a native credit-generation rate for this team. */
    public float effectiveIncomeMultiplier() {
        return gameIncomeMultiplier() * aiIncomeMultiplier();
    }

    public boolean enemyOf(TeamView other) {
        requireOther(other);
        return Boolean.TRUE.equals(RustedReflection.invokeInstance(team,
                new String[]{"isEnemy", "c"}, other.team));
    }

    public boolean alliedWith(TeamView other) {
        requireOther(other);
        return Boolean.TRUE.equals(RustedReflection.invokeInstance(team,
                new String[]{"isAlly", "d"}, other.team));
    }

    public boolean sameTeam(TeamView other) {
        return other != null && other.team == team;
    }

    private int intResult(String[] names) {
        Object value = RustedReflection.invokeInstance(team, names);
        return value instanceof Number ? ((Number) value).intValue() : 0;
    }

    private float positiveFloatResult(String[] names) {
        Object value = RustedReflection.invokeInstance(team, names);
        float result = value instanceof Number ? ((Number) value).floatValue() : 1.0F;
        return Float.isFinite(result) && result > 0.0F ? result : 1.0F;
    }

    private static void requireOther(TeamView other) {
        if (other == null) throw new IllegalArgumentException("other team must not be null");
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof TeamView && ((TeamView) other).team == team;
    }

    @Override
    public int hashCode() {
        return System.identityHashCode(team);
    }

    @Override
    public String toString() {
        return "TeamView{" + team.getClass().getName() + "@"
                + Integer.toHexString(System.identityHashCode(team)) + "}";
    }
}
