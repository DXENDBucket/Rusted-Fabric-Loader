package io.github.endx.rustedfabricapi.api.world;

import io.github.endx.rustedfabricapi.api.client.RustedWarfareClient;
import io.github.endx.rustedfabricapi.api.map.Maps;
import rustedwarfare.core.GameEngine;

/** Read-only snapshot-style access to the single active game world and simulation state. */
public final class GameWorld {
    private GameWorld() {
    }

    public static boolean isLoaded() {
        return RustedWarfareClient.isLevelLoaded() && Maps.isLoaded();
    }

    public static boolean isStarted() {
        return RustedWarfareClient.isGameStarted();
    }

    public static int tick() {
        return engine().currentTick;
    }

    public static int gameTimeMillis() {
        return engine().gameTimeMillis;
    }

    public static float gameTimer() {
        return engine().gameTimer;
    }

    public static float speed() {
        return engine().gameSpeed;
    }

    public static float speedMultiplier() {
        return engine().gameSpeedMultiplier;
    }

    public static boolean isPaused() {
        return engine().isGamePaused();
    }

    public static boolean isSaving() {
        return engine().isSaving;
    }

    public static boolean hasWon() {
        return engine().hasWonGame;
    }

    public static boolean hasLost() {
        return engine().hasLostGame;
    }

    public static boolean isMultiplayer() {
        GameEngine engine = RustedWarfareClient.getEngine();
        return engine != null && engine.networkEngine != null
                && engine.networkEngine.networkingStarted;
    }

    public static boolean isHost() {
        GameEngine engine = RustedWarfareClient.getEngine();
        return engine != null && engine.networkEngine != null
                && engine.networkEngine.networkingStarted && engine.networkEngine.isServer;
    }

    public static boolean isRemoteClient() {
        GameEngine engine = RustedWarfareClient.getEngine();
        return engine != null && engine.networkEngine != null
                && engine.networkEngine.networkingStarted && !engine.networkEngine.isServer;
    }

    public static boolean contains(float x, float y) {
        if (!Float.isFinite(x) || !Float.isFinite(y) || !Maps.isLoaded()) return false;
        return x >= 0.0F && y >= 0.0F
                && x < Maps.widthInWorldUnits() && y < Maps.heightInWorldUnits();
    }

    private static GameEngine engine() {
        return RustedWarfareClient.requireEngine();
    }
}
