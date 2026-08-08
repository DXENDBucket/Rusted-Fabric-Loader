package io.github.endx.rustedfabricapi.api.lobby;

import rustedwarfare.network.TeamLayout;

/** Host-side automatic team layouts exposed without leaking native enum names. */
public enum LobbyTeamLayout {
    TWO_SIDES,
    THREE_SIDES,
    FREE_FOR_ALL,
    SPECTATORS;

    TeamLayout toNative() {
        switch (this) {
            case THREE_SIDES: return TeamLayout.layout3Sides;
            case FREE_FOR_ALL: return TeamLayout.layoutFfa;
            case SPECTATORS: return TeamLayout.layoutSpectators;
            default: return TeamLayout.layout2Sides;
        }
    }
}
