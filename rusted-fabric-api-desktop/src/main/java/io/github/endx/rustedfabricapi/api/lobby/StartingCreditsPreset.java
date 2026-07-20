package io.github.endx.rustedfabricapi.api.lobby;

/** Credit presets understood by Rusted Warfare's native lobby protocol. */
public enum StartingCreditsPreset {
    DEFAULT(0, 4_000),
    NONE(1, 0),
    LOW_1000(2, 1_000),
    LOW_2000(3, 2_000),
    MEDIUM_5000(4, 5_000),
    HIGH_10000(5, 10_000),
    HIGH_50000(6, 50_000),
    HIGH_100000(7, 100_000),
    HIGH_200000(8, 200_000),
    UNKNOWN(-1, 999);

    private final int nativeId;
    private final int credits;

    StartingCreditsPreset(int nativeId, int credits) {
        this.nativeId = nativeId;
        this.credits = credits;
    }

    public int nativeId() { return nativeId; }

    public int credits() { return credits; }

    static StartingCreditsPreset fromNative(int value) {
        for (StartingCreditsPreset preset : values()) if (preset.nativeId == value) return preset;
        return UNKNOWN;
    }
}
