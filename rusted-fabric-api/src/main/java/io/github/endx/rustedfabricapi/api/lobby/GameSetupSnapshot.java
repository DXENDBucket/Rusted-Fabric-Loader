package io.github.endx.rustedfabricapi.api.lobby;

import java.util.Objects;

import rustedwarfare.network.GameSetup;
import rustedwarfare.network.NetworkEngine;

/** Immutable, mapped-field-only view of multiplayer game rules. */
public final class GameSetupSnapshot {
    private final LobbyMapType mapType;
    private final String mapPath;
    private final StartingCreditsPreset startingCredits;
    private final LobbyFogMode fogMode;
    private final boolean revealedMap;
    private final LobbyAiDifficulty aiDifficulty;
    private final int startingUnitsId;
    private final String startingUnitsDisplayName;
    private final float incomeMultiplier;
    private final boolean noNukes;
    private final boolean sharedControl;
    private final boolean teamsLocked;
    private final int randomSeed;

    GameSetupSnapshot(NetworkEngine network, GameSetup setup) {
        mapType = LobbyMapType.fromNative(setup.mapType);
        mapPath = setup.mapPath;
        startingCredits = StartingCreditsPreset.fromNative(setup.startingCredits);
        fogMode = LobbyFogMode.fromNative(setup.fogMode);
        revealedMap = setup.revealedMap;
        aiDifficulty = LobbyAiDifficulty.fromNative(setup.aiDifficulty);
        startingUnitsId = setup.startingUnits;
        startingUnitsDisplayName = network.getStartingUnitsDisplayName(setup.startingUnits);
        incomeMultiplier = setup.incomeMultiplier;
        noNukes = setup.noNukes;
        sharedControl = setup.sharedControl;
        teamsLocked = setup.teamsLocked;
        randomSeed = setup.randomSeed;
    }

    public LobbyMapType mapType() { return mapType; }

    public String mapPath() { return mapPath; }

    public StartingCreditsPreset startingCredits() { return startingCredits; }

    public int resolvedStartingCredits() { return startingCredits.credits(); }

    public LobbyFogMode fogMode() { return fogMode; }

    public boolean revealedMap() { return revealedMap; }

    public LobbyAiDifficulty aiDifficulty() { return aiDifficulty; }

    public int startingUnitsId() { return startingUnitsId; }

    public String startingUnitsDisplayName() { return startingUnitsDisplayName; }

    public float incomeMultiplier() { return incomeMultiplier; }

    public boolean noNukes() { return noNukes; }

    public boolean sharedControl() { return sharedControl; }

    public boolean teamsLocked() { return teamsLocked; }

    public int randomSeed() { return randomSeed; }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof GameSetupSnapshot)) return false;
        GameSetupSnapshot that = (GameSetupSnapshot) other;
        return revealedMap == that.revealedMap
                && startingUnitsId == that.startingUnitsId
                && Float.compare(incomeMultiplier, that.incomeMultiplier) == 0
                && noNukes == that.noNukes && sharedControl == that.sharedControl
                && teamsLocked == that.teamsLocked && randomSeed == that.randomSeed
                && mapType == that.mapType && Objects.equals(mapPath, that.mapPath)
                && startingCredits == that.startingCredits && fogMode == that.fogMode
                && aiDifficulty == that.aiDifficulty;
    }

    @Override
    public int hashCode() {
        return Objects.hash(mapType, mapPath, startingCredits, fogMode, revealedMap,
                aiDifficulty, startingUnitsId, incomeMultiplier, noNukes,
                sharedControl, teamsLocked, randomSeed);
    }

    @Override
    public String toString() {
        return "GameSetupSnapshot{" + mapType + ", map='" + mapPath + '\''
                + ", credits=" + startingCredits + ", fog=" + fogMode
                + ", ai=" + aiDifficulty + ", startingUnits=" + startingUnitsId
                + ", income=" + incomeMultiplier + '}';
    }
}
