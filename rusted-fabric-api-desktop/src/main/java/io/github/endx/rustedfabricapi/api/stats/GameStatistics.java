package io.github.endx.rustedfabricapi.api.stats;

import io.github.endx.rustedfabricapi.api.client.RustedWarfareClient;
import io.github.endx.rustedfabricapi.api.unit.Teams;
import rustedwarfare.game.Team;
import rustedwarfare.stats.StatsEngine;
import rustedwarfare.stats.TeamStats;
import rustedwarfare.unit.Unit;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/** Read-only match statistics and history access. */
public final class GameStatistics {
    private GameStatistics() {
    }

    public static StatsEngine manager() {
        StatsEngine manager = RustedWarfareClient.requireEngine().gameStatistics;
        if (manager == null) throw new IllegalStateException("Statistics engine is not initialized");
        return manager;
    }

    public static boolean isEnabled() {
        return StatsEngine.enabled;
    }

    public static TeamStatisticsSnapshot snapshot(Team team) {
        Team checked = Objects.requireNonNull(team, "team");
        return new TeamStatisticsSnapshot(checked, manager().getStatsForTeam(checked));
    }

    public static TeamStatisticsSnapshot snapshot(Unit unit) {
        Unit checked = Objects.requireNonNull(unit, "unit");
        return new TeamStatisticsSnapshot(checked.team, manager().getStatsForUnit(checked));
    }

    /** Returns an immutable snapshot for every currently active team. */
    public static List<TeamStatisticsSnapshot> snapshotAll() {
        List<Team> teams = Teams.snapshot(true);
        ArrayList<TeamStatisticsSnapshot> result =
                new ArrayList<TeamStatisticsSnapshot>(teams.size());
        StatsEngine manager = manager();
        for (Team team : teams) {
            result.add(new TeamStatisticsSnapshot(team, manager.getStatsForTeam(team)));
        }
        return Collections.unmodifiableList(result);
    }

    /** Reads the graph value at a simulation time in milliseconds. */
    public static int historyValue(Team team, StatisticMetric metric, int gameTimeMillis) {
        if (gameTimeMillis < 0) throw new IllegalArgumentException("gameTimeMillis must be non-negative");
        TeamStats stats = manager().getStatsForTeam(Objects.requireNonNull(team, "team"));
        if (stats.history == null) return 0;
        return stats.history.getValueAtFrame(
                Objects.requireNonNull(metric, "metric").nativeMetric(), gameTimeMillis);
    }
}
