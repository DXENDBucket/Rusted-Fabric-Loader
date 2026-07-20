package io.github.endx.rustedfabricapi.api.mission;

/** Immutable high-level state of the current map mission. */
public final class MissionSnapshot {
    private final boolean hasData;
    private final boolean active;
    private final boolean over;
    private final boolean hasActiveObjectives;
    private final int survivalWave;
    private final int triggerCount;
    private final boolean won;
    private final boolean lost;

    MissionSnapshot(boolean hasData, boolean active, boolean over, boolean hasActiveObjectives,
            int survivalWave, int triggerCount, boolean won, boolean lost) {
        this.hasData = hasData;
        this.active = active;
        this.over = over;
        this.hasActiveObjectives = hasActiveObjectives;
        this.survivalWave = survivalWave;
        this.triggerCount = triggerCount;
        this.won = won;
        this.lost = lost;
    }

    public boolean hasData() { return hasData; }
    public boolean isActive() { return active; }
    public boolean isOver() { return over; }
    public boolean hasActiveObjectives() { return hasActiveObjectives; }
    public int survivalWave() { return survivalWave; }
    public int triggerCount() { return triggerCount; }
    public boolean hasWon() { return won; }
    public boolean hasLost() { return lost; }

    @Override
    public String toString() {
        return "MissionSnapshot{active=" + active + ", over=" + over
                + ", wave=" + survivalWave + ", triggers=" + triggerCount
                + ", won=" + won + ", lost=" + lost + '}';
    }
}
