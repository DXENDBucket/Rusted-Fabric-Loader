package io.github.endx.rustedfabricapi.api.lobby;

/** Multiplayer fog modes encoded by the native game setup packet. */
public enum LobbyFogMode {
    OFF(0),
    BASIC(1),
    LINE_OF_SIGHT(2),
    UNKNOWN(-1);

    private final int nativeId;

    LobbyFogMode(int nativeId) {
        this.nativeId = nativeId;
    }

    public int nativeId() { return nativeId; }

    static LobbyFogMode fromNative(int value) {
        for (LobbyFogMode mode : values()) if (mode.nativeId == value) return mode;
        return UNKNOWN;
    }
}
