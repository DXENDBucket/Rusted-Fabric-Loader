package io.github.endx.rustedfabricapi.api.fog;

/** Semantic states over the native 0..10 per-team fog level. */
public enum FogState {
    VISIBLE(0),
    EXPLORED(5),
    UNEXPLORED(10);

    private final int nativeLevel;
    FogState(int nativeLevel) { this.nativeLevel = nativeLevel; }
    public int nativeLevel() { return nativeLevel; }

    public static FogState classify(int nativeLevel) {
        if (nativeLevel < 0 || nativeLevel > 10) {
            throw new IllegalArgumentException("native fog level must be between 0 and 10");
        }
        if (nativeLevel < 5) return VISIBLE;
        if (nativeLevel < 10) return EXPLORED;
        return UNEXPLORED;
    }
}
