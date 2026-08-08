package io.github.endx.rustedfabricapi.api.lobby;

import rustedwarfare.network.MapType;

/** Stable API names for the game's three multiplayer map sources. */
public enum LobbyMapType {
    SKIRMISH,
    CUSTOM,
    SAVED_GAME;

    static LobbyMapType fromNative(MapType type) {
        if (type == MapType.customMap) return CUSTOM;
        if (type == MapType.savedGame) return SAVED_GAME;
        return SKIRMISH;
    }

    MapType toNative() {
        switch (this) {
            case CUSTOM: return MapType.customMap;
            case SAVED_GAME: return MapType.savedGame;
            default: return MapType.skirmishMap;
        }
    }
}
