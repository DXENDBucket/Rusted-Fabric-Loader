package io.github.endx.rustedfabricapi.api.stats;

import rustedwarfare.game.Team;
import rustedwarfare.stats.TeamStats;

/** Immutable public subset of the native per-team match statistics. */
public final class TeamStatisticsSnapshot {
    private final Team team;
    private final int unitsKilled;
    private final int buildingsKilled;
    private final int experimentalsKilled;
    private final int unitsLost;
    private final int buildingsLost;
    private final int experimentalsLost;
    private final boolean hasHistory;

    TeamStatisticsSnapshot(Team team, TeamStats stats) {
        this.team = team;
        this.unitsKilled = stats.unitsKilled;
        this.buildingsKilled = stats.buildingsKilled;
        this.experimentalsKilled = stats.experimentalsKilled;
        this.unitsLost = stats.unitsLost;
        this.buildingsLost = stats.buildingsLost;
        this.experimentalsLost = stats.experimentalsLost;
        this.hasHistory = stats.history != null && stats.history.hasHistory();
    }

    /** Team represented by the snapshot; {@code null} denotes neutral/invalid-team stats. */
    public Team getTeam() { return team; }

    public int getUnitsKilled() { return unitsKilled; }

    public int getBuildingsKilled() { return buildingsKilled; }

    public int getExperimentalsKilled() { return experimentalsKilled; }

    public int getUnitsLost() { return unitsLost; }

    public int getBuildingsLost() { return buildingsLost; }

    public int getExperimentalsLost() { return experimentalsLost; }

    public int getTotalKills() { return unitsKilled + buildingsKilled + experimentalsKilled; }

    public int getTotalLosses() { return unitsLost + buildingsLost + experimentalsLost; }

    public boolean hasHistory() { return hasHistory; }

    @Override
    public String toString() {
        return "TeamStatisticsSnapshot{kills=" + getTotalKills()
                + ", losses=" + getTotalLosses() + ", hasHistory=" + hasHistory + '}';
    }
}
