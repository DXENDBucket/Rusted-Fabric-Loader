package io.github.endx.rustedfabricapi.api.lobby;

import java.util.Objects;

import rustedwarfare.network.GameSetup;

/** Mutable, validated edit request created from the current native lobby setup. */
public final class GameSetupEditor {
    private LobbyMapType mapType;
    private String mapPath;
    private int startingCreditsId;
    private int fogModeId;
    private boolean revealedMap;
    private int aiDifficultyId;
    private int startingUnitsId;
    private float incomeMultiplier;
    private boolean noNukes;
    private boolean sharedControl;
    private boolean teamsLocked;
    private int randomSeed;

    GameSetupEditor(GameSetup setup) {
        mapType = LobbyMapType.fromNative(setup.mapType);
        mapPath = setup.mapPath;
        startingCreditsId = setup.startingCredits;
        fogModeId = setup.fogMode;
        revealedMap = setup.revealedMap;
        aiDifficultyId = setup.aiDifficulty;
        startingUnitsId = setup.startingUnits;
        incomeMultiplier = setup.incomeMultiplier;
        noNukes = setup.noNukes;
        sharedControl = setup.sharedControl;
        teamsLocked = setup.teamsLocked;
        randomSeed = setup.randomSeed;
    }

    public LobbyMapType mapType() { return mapType; }

    public String mapPath() { return mapPath; }

    public StartingCreditsPreset startingCredits() {
        return StartingCreditsPreset.fromNative(startingCreditsId);
    }

    public LobbyFogMode fogMode() { return LobbyFogMode.fromNative(fogModeId); }

    public boolean revealedMap() { return revealedMap; }

    public LobbyAiDifficulty aiDifficulty() {
        return LobbyAiDifficulty.fromNative(aiDifficultyId);
    }

    public int startingUnitsId() { return startingUnitsId; }

    public float incomeMultiplier() { return incomeMultiplier; }

    public boolean noNukes() { return noNukes; }

    public boolean sharedControl() { return sharedControl; }

    public boolean teamsLocked() { return teamsLocked; }

    public int randomSeed() { return randomSeed; }

    public GameSetupEditor map(LobbyMapType type, String path) {
        mapType = Objects.requireNonNull(type, "type");
        if (path == null) throw new NullPointerException("path");
        String value = path.trim();
        if (value.isEmpty() || value.length() > 1024) {
            throw new IllegalArgumentException("Map path must contain 1 to 1024 characters");
        }
        for (int i = 0; i < value.length(); i++) {
            if (Character.isISOControl(value.charAt(i))) {
                throw new IllegalArgumentException("Map path contains a control character");
            }
        }
        mapPath = value;
        return this;
    }

    public GameSetupEditor startingCredits(StartingCreditsPreset preset) {
        startingCreditsId = requireKnown(Objects.requireNonNull(preset, "preset")).nativeId();
        return this;
    }

    public GameSetupEditor fogMode(LobbyFogMode mode) {
        fogModeId = requireKnown(Objects.requireNonNull(mode, "mode")).nativeId();
        return this;
    }

    public GameSetupEditor revealedMap(boolean value) {
        revealedMap = value;
        return this;
    }

    public GameSetupEditor aiDifficulty(LobbyAiDifficulty difficulty) {
        aiDifficultyId = requireKnown(Objects.requireNonNull(difficulty, "difficulty")).nativeId();
        return this;
    }

    /** Accepts a built-in ID or a custom starting-unit option reported by the current game. */
    public GameSetupEditor startingUnits(int optionId) {
        startingUnitsId = optionId;
        return this;
    }

    public GameSetupEditor incomeMultiplier(float value) {
        if (!Float.isFinite(value) || value <= 0.0F || value > 100.0F) {
            throw new IllegalArgumentException("Income multiplier must be finite and in (0, 100]");
        }
        incomeMultiplier = value;
        return this;
    }

    public GameSetupEditor noNukes(boolean value) {
        noNukes = value;
        return this;
    }

    public GameSetupEditor sharedControl(boolean value) {
        sharedControl = value;
        return this;
    }

    public GameSetupEditor teamsLocked(boolean value) {
        teamsLocked = value;
        return this;
    }

    public GameSetupEditor randomSeed(int value) {
        randomSeed = value;
        return this;
    }

    void applyTo(GameSetup setup) {
        if (mapType == null || mapPath == null) {
            throw new IllegalStateException("Lobby map selection is incomplete");
        }
        setup.mapType = mapType.toNative();
        setup.mapPath = mapPath;
        setup.startingCredits = startingCreditsId;
        setup.fogMode = fogModeId;
        setup.revealedMap = revealedMap;
        setup.aiDifficulty = aiDifficultyId;
        setup.startingUnits = startingUnitsId;
        setup.incomeMultiplier = incomeMultiplier;
        setup.noNukes = noNukes;
        setup.sharedControl = sharedControl;
        setup.teamsLocked = teamsLocked;
        setup.randomSeed = randomSeed;
    }

    private static StartingCreditsPreset requireKnown(StartingCreditsPreset value) {
        if (value == StartingCreditsPreset.UNKNOWN) {
            throw new IllegalArgumentException("UNKNOWN is read-only");
        }
        return value;
    }

    private static LobbyFogMode requireKnown(LobbyFogMode value) {
        if (value == LobbyFogMode.UNKNOWN) throw new IllegalArgumentException("UNKNOWN is read-only");
        return value;
    }

    private static LobbyAiDifficulty requireKnown(LobbyAiDifficulty value) {
        if (value == LobbyAiDifficulty.UNKNOWN) {
            throw new IllegalArgumentException("UNKNOWN is read-only");
        }
        return value;
    }
}
