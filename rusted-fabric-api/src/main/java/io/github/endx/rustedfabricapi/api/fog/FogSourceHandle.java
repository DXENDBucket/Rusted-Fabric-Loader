package io.github.endx.rustedfabricapi.api.fog;

import rustedwarfare.game.Team;

public interface FogSourceHandle {
    long id();
    Team team();
    FogOperation operation();
    boolean active();
    /** Remaining simulation ticks, or {@link FogSources#PERMANENT}. */
    float remainingTicks();
    /** Restarts a finite source lifetime without allocating a duplicate source. */
    boolean refresh(float durationTicks);
    boolean cancel();
}
