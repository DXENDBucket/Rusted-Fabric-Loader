package io.github.endx.rustedfabricapi.api.fog;

import rustedwarfare.game.Team;

public interface FogSourceHandle {
    long id();
    Team team();
    FogOperation operation();
    boolean active();
    boolean cancel();
}
