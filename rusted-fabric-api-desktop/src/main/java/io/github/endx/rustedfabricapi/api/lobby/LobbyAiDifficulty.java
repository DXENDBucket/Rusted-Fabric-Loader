package io.github.endx.rustedfabricapi.api.lobby;

/** Native lobby-wide AI difficulty levels. Individual AI overrides remain game-owned. */
public enum LobbyAiDifficulty {
    VERY_EASY(-2),
    EASY(-1),
    MEDIUM(0),
    HARD(1),
    VERY_HARD(2),
    IMPOSSIBLE(3),
    UNKNOWN(Integer.MIN_VALUE);

    private final int nativeId;

    LobbyAiDifficulty(int nativeId) {
        this.nativeId = nativeId;
    }

    public int nativeId() { return nativeId; }

    static LobbyAiDifficulty fromNative(int value) {
        for (LobbyAiDifficulty difficulty : values()) {
            if (difficulty.nativeId == value) return difficulty;
        }
        return UNKNOWN;
    }
}
