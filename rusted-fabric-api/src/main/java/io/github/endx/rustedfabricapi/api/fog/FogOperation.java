package io.github.endx.rustedfabricapi.api.fog;

/** High-level merge operations for per-team fog levels. */
public enum FogOperation {
    /** Makes matching tiles currently visible. */
    REVEAL,
    /** Removes unexplored shroud without hiding tiles that are already visible. */
    EXPLORE,
    /** Restores LOS cover while preserving completely unexplored shroud. */
    CONCEAL,
    /** Restores completely unexplored shroud. */
    SHROUD;

    int apply(int current) {
        switch (this) {
            case REVEAL: return 0;
            case EXPLORE: return Math.min(current, 5);
            case CONCEAL: return Math.max(current, 5);
            case SHROUD: return 10;
            default: throw new AssertionError(this);
        }
    }
}
