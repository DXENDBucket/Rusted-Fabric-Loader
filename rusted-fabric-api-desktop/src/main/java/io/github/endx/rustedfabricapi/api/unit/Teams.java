package io.github.endx.rustedfabricapi.api.unit;

import io.github.endx.rustedfabricapi.api.client.RustedWarfareClient;
import io.github.endx.rustedfabricapi.api.unit.event.TeamStateEvents;
import rustedwarfare.ai.AiTeam;
import rustedwarfare.game.Team;
import rustedwarfare.unit.Unit;

import java.util.Optional;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/** Strongly typed team access and relation helpers. */
public final class Teams {
    private Teams() {
    }

    public static Optional<Team> findById(int teamId) {
        return Optional.ofNullable(Team.getTeamById(teamId));
    }

    public static Optional<Team> player() {
        return Optional.ofNullable(RustedWarfareClient.getPlayerTeam());
    }

    public static Optional<Team> ownerOf(Unit unit) {
        return Optional.ofNullable(unit != null ? unit.team : null);
    }

    /** Returns active non-spectator teams as an immutable snapshot. */
    public static List<Team> snapshot() {
        return snapshot(false);
    }

    /** Returns active teams, optionally including spectators. */
    public static List<Team> snapshot(boolean includeSpectators) {
        List<?> source = Team.b(includeSpectators);
        ArrayList<Team> result = new ArrayList<Team>(source.size());
        for (Object value : source) if (value instanceof Team) result.add((Team) value);
        return Collections.unmodifiableList(result);
    }

    /** Captures identity, outcome, control, economy, and unit counters at this instant. */
    public static TeamStateSnapshot snapshotState(Team team) {
        return TeamStateSnapshot.capture(Objects.requireNonNull(team, "team"));
    }

    /** Returns immutable state snapshots for all active non-spectator teams. */
    public static List<TeamStateSnapshot> snapshotStates() {
        return snapshotStates(false);
    }

    public static List<TeamStateSnapshot> snapshotStates(boolean includeSpectators) {
        List<Team> teams = snapshot(includeSpectators);
        ArrayList<TeamStateSnapshot> result = new ArrayList<TeamStateSnapshot>(teams.size());
        for (Team team : teams) result.add(TeamStateSnapshot.capture(team));
        return Collections.unmodifiableList(result);
    }

    public static Team neutral() {
        return Team.i;
    }

    public static Team aggressive() {
        return Team.h;
    }

    public static int liveUnitCount(Team team, boolean includeIncomplete,
            boolean includeQueued) {
        return Objects.requireNonNull(team, "team")
                .getUnitCountWithOptions(includeIncomplete, includeQueued);
    }

    public static int totalUnitCountIncludingQueued(Team team) {
        return Objects.requireNonNull(team, "team").getTotalUnitCountIncludingQueued();
    }

    public static int maxUnitCount(Team team) {
        return Objects.requireNonNull(team, "team").getMaxUnitCount();
    }

    public static int incomeRate(Team team) {
        return Objects.requireNonNull(team, "team").getIncomeRate();
    }

    public static int displayIncomeRate(Team team) {
        return Objects.requireNonNull(team, "team").getDisplayIncomeRate();
    }

    public static int teamId(Team team) {
        return Objects.requireNonNull(team, "team").teamId;
    }

    public static int allianceGroup(Team team) {
        return Objects.requireNonNull(team, "team").allianceGroup;
    }

    public static Optional<String> playerName(Team team) {
        return Optional.ofNullable(Objects.requireNonNull(team, "team").playerName);
    }

    public static boolean isAi(Team team) {
        return team instanceof AiTeam;
    }

    public static boolean isLocalPlayer(Team team) {
        return team != null && player().orElse(null) == team;
    }

    public static boolean isDefeated(Team team) {
        return team != null && team.defeated;
    }

    public static boolean isVictorious(Team team) {
        return team != null && team.victorious;
    }

    public static boolean isSharingControl(Team team) {
        return team != null && team.isSharingControl();
    }

    public static double credits(Team team) {
        return Objects.requireNonNull(team, "team").credits;
    }

    /**
     * Directly assigns credits after modifier/cancellation events.
     * All peers in a lockstep match must make the same simulation mutation.
     *
     * @return true when the assignment was applied and changed the stored value
     */
    public static boolean setCredits(Team team, double credits) {
        Team checked = Objects.requireNonNull(team, "team");
        requireFinite(credits, "credits");
        double previous = checked.credits;
        double requested = TeamStateEvents.MODIFY_SET_CREDITS.invoker()
                .modify(checked, previous, credits);
        requireFinite(requested, "modified credits");
        if (TeamStateEvents.BEFORE_SET_CREDITS.invoker()
                .beforeSet(checked, previous, requested)) return false;
        if (Double.compare(previous, requested) == 0) return false;
        checked.credits = requested;
        TeamStateEvents.AFTER_CREDITS_CHANGED.invoker().afterChange(
                checked, previous, requested, TeamCreditChangeSource.API_SET);
        return true;
    }

    /**
     * Adds credits through the game's normal scaling and recorded-income path.
     * The actual applied delta can differ from {@code amount} due to native multipliers.
     */
    public static void addCreditsAndRecordIncome(Team team, float amount) {
        if (!Float.isFinite(amount)) throw new IllegalArgumentException("amount must be finite");
        Objects.requireNonNull(team, "team").addCreditsAndRecordIncome(amount);
    }

    public static boolean isSpectator(Team team) {
        return team != null && team.isSpectator();
    }

    /** Uses the game's registry-aware ownership change path. */
    public static void changeOwner(Unit unit, Team team) {
        Objects.requireNonNull(unit, "unit").changeTeam(Objects.requireNonNull(team, "team"));
    }

    public static boolean areEnemies(Team first, Team second) {
        return first != null && second != null && first.isEnemy(second);
    }

    public static boolean areAllies(Team first, Team second) {
        return first != null && second != null && first.isAlly(second);
    }

    private static void requireFinite(double value, String name) {
        if (!Double.isFinite(value)) throw new IllegalArgumentException(name + " must be finite");
    }
}
