package io.github.endx.rustedfabricapi.api.ini;

/** Documents whether a field can affect deterministic multiplayer state. */
public enum IniMultiplayerImpact {
    CLIENT_ONLY,
    SERVER_AUTHORITY,
    GAMEPLAY_SYNCED
}
