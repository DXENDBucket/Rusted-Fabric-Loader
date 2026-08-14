package rustedwarfare.game;

import java.util.HashMap;
import java.util.Map;

public class Team {
    private static final Map<Integer, Team> BY_ID = new HashMap<Integer, Team>();

    public final int id;
    public final int teamId;
    public double credits;
    public boolean spectator;
    public int totalUnits;
    public int nonBuildings;
    public int maximumUnits;
    public int income;
    public Team enemy;

    public Team(int id) {
        this.id = id;
        this.teamId = id;
        BY_ID.put(Integer.valueOf(id), this);
    }

    public static Team getTeamById(int id) { return BY_ID.get(Integer.valueOf(id)); }
    public boolean isSpectator() { return spectator; }
    public int getTotalUnitCountIncludingQueued() { return totalUnits; }
    public int getNonBuildingUnitCountIncludingQueued() { return nonBuildings; }
    public int getMaxUnitCount() { return maximumUnits; }
    public int getIncomeRate() { return income; }
    public int getDisplayIncomeRate() { return income; }
    public boolean isEnemy(Team other) { return other == enemy; }
    public boolean isAlly(Team other) { return other == this; }
}
