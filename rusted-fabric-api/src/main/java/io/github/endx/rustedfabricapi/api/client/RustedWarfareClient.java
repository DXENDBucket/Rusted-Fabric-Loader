package io.github.endx.rustedfabricapi.api.client;

import io.github.endx.rustedfabricapi.api.thread.GameThreadScheduler;
import rustedwarfare.core.GameEngine;
import rustedwarfare.game.Team;
import rustedwarfare.unit.Unit;
import rustedwarfare.unit.special.EditorOrBuilderUnit;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Stable, named access to the current desktop game client.
 *
 * <p>The engine is not available during the earliest Fabric entrypoint phase. Register a
 * {@link io.github.endx.rustedfabricapi.api.client.event.ClientTickEvents} listener or use
 * {@link #execute(Runnable)} when game state must be accessed on the update thread.</p>
 */
public final class RustedWarfareClient {
    private RustedWarfareClient() {
    }

    /** Returns the current engine, or {@code null} before engine initialization. */
    public static GameEngine getEngine() {
        return GameEngine.getInstance();
    }

    public static Optional<GameEngine> findEngine() {
        return Optional.ofNullable(getEngine());
    }

    /** Returns the current engine or fails with a lifecycle-oriented error message. */
    public static GameEngine requireEngine() {
        GameEngine engine = getEngine();
        if (engine == null) {
            throw new IllegalStateException("Rusted Warfare's GameEngine is not initialized yet");
        }
        return engine;
    }

    /** Returns the locally controlled team, or {@code null} outside a game. */
    public static Team getPlayerTeam() {
        GameEngine engine = getEngine();
        return engine != null ? engine.playerTeam : null;
    }

    public static boolean isLevelLoaded() {
        GameEngine engine = getEngine();
        return engine != null && engine.hasLoadedLevel;
    }

    public static boolean isGameStarted() {
        GameEngine engine = getEngine();
        return engine != null && engine.isGameStarted;
    }

    /** True while the current session was launched through the sandbox editor flow. */
    public static boolean isSandboxMode() {
        if (GameEngine.isLaunchSandbox) return true;
        for (Object value : Unit.allUnits) {
            if (value instanceof EditorOrBuilderUnit && !((Unit) value).dead) return true;
        }
        return false;
    }

    /** True while the engine is simulating the animated main-menu background battle. */
    public static boolean isMenuBackgroundMap() {
        GameEngine engine = getEngine();
        return engine != null && engine.isMenuBackgroundMap;
    }

    /** Schedules state-changing work for the beginning of the next desktop update. */
    public static CompletableFuture<Void> execute(Runnable action) {
        return GameThreadScheduler.onNextUpdate(action);
    }

    public static boolean isOnUpdateThread() {
        return GameThreadScheduler.isUpdateThread();
    }
}
