package io.github.endx.rustedfabricapi.api.unit;

import java.util.Objects;
import java.util.Optional;

import rustedwarfare.ai.AiTeam;
import rustedwarfare.game.Team;

/** Immutable view of identity, match outcome, control, economy, and unit-count state. */
public final class TeamStateSnapshot {
    private final Team team;
    private final int teamId;
    private final int allianceGroup;
    private final String playerName;
    private final boolean ai;
    private final boolean spectator;
    private final boolean localPlayer;
    private final boolean defeated;
    private final boolean victorious;
    private final boolean sharedControlEnabled;
    private final boolean sharedControlAutoEnabled;
    private final double credits;
    private final int completedUnitCount;
    private final int unitCountIncludingIncomplete;
    private final int unitCountIncludingIncompleteAndQueued;
    private final int totalUnitCountIncludingQueued;
    private final int nonBuildingUnitCountIncludingQueued;
    private final int maxUnitCount;
    private final int incomeRate;
    private final int displayIncomeRate;

    private TeamStateSnapshot(Team team) {
        this.team = Objects.requireNonNull(team, "team");
        this.teamId = team.teamId;
        this.allianceGroup = team.allianceGroup;
        this.playerName = team.playerName;
        this.ai = team instanceof AiTeam;
        this.spectator = team.isSpectator();
        this.localPlayer = Teams.player().orElse(null) == team;
        this.defeated = team.defeated;
        this.victorious = team.victorious;
        this.sharedControlEnabled = team.sharedControlEnabled;
        this.sharedControlAutoEnabled = team.sharedControlAutoEnabled;
        this.credits = team.credits;
        this.completedUnitCount = team.getUnitCountWithOptions(false, false);
        this.unitCountIncludingIncomplete = team.getUnitCountWithOptions(true, false);
        this.unitCountIncludingIncompleteAndQueued = team.getUnitCountWithOptions(true, true);
        this.totalUnitCountIncludingQueued = team.getTotalUnitCountIncludingQueued();
        this.nonBuildingUnitCountIncludingQueued = team.getNonBuildingUnitCountIncludingQueued();
        this.maxUnitCount = team.getMaxUnitCount();
        this.incomeRate = team.getIncomeRate();
        this.displayIncomeRate = team.getDisplayIncomeRate();
    }

    public static TeamStateSnapshot capture(Team team) {
        return new TeamStateSnapshot(team);
    }

    public Team team() { return team; }
    public int teamId() { return teamId; }
    public int allianceGroup() { return allianceGroup; }
    public Optional<String> playerName() { return Optional.ofNullable(playerName); }
    public boolean isAi() { return ai; }
    public boolean isSpectator() { return spectator; }
    public boolean isLocalPlayer() { return localPlayer; }
    public boolean isDefeated() { return defeated; }
    public boolean isVictorious() { return victorious; }
    public boolean isSharedControlEnabled() { return sharedControlEnabled; }
    public boolean isSharedControlAutoEnabled() { return sharedControlAutoEnabled; }
    public boolean isSharingControl() { return sharedControlEnabled || sharedControlAutoEnabled; }
    public double credits() { return credits; }
    public int completedUnitCount() { return completedUnitCount; }
    public int unitCountIncludingIncomplete() { return unitCountIncludingIncomplete; }
    public int unitCountIncludingIncompleteAndQueued() {
        return unitCountIncludingIncompleteAndQueued;
    }
    public int totalUnitCountIncludingQueued() { return totalUnitCountIncludingQueued; }
    public int nonBuildingUnitCountIncludingQueued() {
        return nonBuildingUnitCountIncludingQueued;
    }
    public int maxUnitCount() { return maxUnitCount; }
    public int incomeRate() { return incomeRate; }
    public int displayIncomeRate() { return displayIncomeRate; }

    @Override
    public String toString() {
        return "TeamStateSnapshot{teamId=" + teamId
                + ", allianceGroup=" + allianceGroup
                + ", playerName=" + playerName
                + ", ai=" + ai
                + ", spectator=" + spectator
                + ", defeated=" + defeated
                + ", victorious=" + victorious
                + ", credits=" + credits
                + ", units=" + unitCountIncludingIncompleteAndQueued
                + '}';
    }
}
