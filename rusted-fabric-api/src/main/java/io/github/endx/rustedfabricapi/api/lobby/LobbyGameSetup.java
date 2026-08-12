package io.github.endx.rustedfabricapi.api.lobby;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

import io.github.endx.rustedfabricapi.api.client.RustedWarfareClient;
import rustedwarfare.network.GameSetup;
import rustedwarfare.network.NetworkEngine;

/** Snapshot and controller-safe mutation access to native multiplayer game rules. */
public final class LobbyGameSetup {
    private LobbyGameSetup() {
    }

    public static GameSetupSnapshot snapshot() {
        return snapshot(network());
    }

    public static GameSetupSnapshot snapshot(NetworkEngine network) {
        Objects.requireNonNull(network, "network");
        GameSetup setup = requireSetup(network);
        return new GameSetupSnapshot(network, setup);
    }

    public static GameSetupSnapshot snapshot(NetworkEngine network, GameSetup setup) {
        return new GameSetupSnapshot(Objects.requireNonNull(network, "network"),
                Objects.requireNonNull(setup, "setup"));
    }

    /**
     * Returns the live income multiplier without allocating a complete lobby snapshot.
     * This is suitable for deterministic simulation code that evaluates income frequently.
     */
    public static float currentIncomeMultiplier() {
        return requireSetup(network()).incomeMultiplier;
    }

    public static boolean canUpdate() {
        NetworkEngine network = network();
        return network.isServerOrProxyController() && !network.hasGameBeenStarted()
                && !network.isGameStarting();
    }

    /**
     * Applies one validated transaction. The host uses direct native mutation and synchronization;
     * a proxy controller uses the game's normal command translation.
     */
    public static boolean update(Consumer<GameSetupEditor> changes) {
        Objects.requireNonNull(changes, "changes");
        NetworkEngine network = network();
        if (!network.isServerOrProxyController()) {
            throw new IllegalStateException("Lobby setup updates require host or proxy control");
        }
        if (network.hasGameBeenStarted() || network.isGameStarting()) {
            throw new IllegalStateException("Lobby setup cannot change after game start begins");
        }
        synchronized (network) {
            GameSetup currentSetup = requireSetup(network);
            GameSetupSnapshot previous = new GameSetupSnapshot(network, currentSetup);
            GameSetupEditor editor = new GameSetupEditor(currentSetup);
            changes.accept(editor);
            validateStartingUnits(network, editor.startingUnitsId());

            GameSetup candidate = currentSetup.copy();
            editor.applyTo(candidate);
            GameSetupSnapshot requested = new GameSetupSnapshot(network, candidate);
            if (previous.equals(requested)) return false;
            if (LobbyGameSetupEvents.BEFORE_UPDATE.invoker()
                    .beforeUpdate(network, previous, requested)) return false;

            boolean success = false;
            try {
                GameSetup nativeTarget = network.getGameSetup();
                if (nativeTarget == null) {
                    throw new IllegalStateException("Native lobby setup is not writable");
                }
                editor.applyTo(nativeTarget);
                network.applyGameSetup(nativeTarget);
                success = true;
                return true;
            } finally {
                LobbyGameSetupEvents.AFTER_UPDATE.invoker()
                        .afterUpdate(network, previous, requested, success);
            }
        }
    }

    public static List<Integer> startingUnitOptions() {
        NetworkEngine network = network();
        ArrayList<Integer> result = new ArrayList<Integer>();
        for (Object option : network.getStartingUnitOptions()) {
            if (option instanceof Integer) result.add((Integer) option);
        }
        return Collections.unmodifiableList(result);
    }

    public static String startingUnitsDisplayName(int optionId) {
        return network().getStartingUnitsDisplayName(optionId);
    }

    public static String aiDifficultyDisplayName(LobbyAiDifficulty difficulty) {
        LobbyAiDifficulty checked = Objects.requireNonNull(difficulty, "difficulty");
        if (checked == LobbyAiDifficulty.UNKNOWN) return "Unknown";
        return network().getAiDifficultyDisplayName(checked.nativeId());
    }

    private static NetworkEngine network() {
        NetworkEngine network = RustedWarfareClient.requireEngine().networkEngine;
        if (network == null) throw new IllegalStateException("Network engine is not initialized");
        return network;
    }

    private static GameSetup requireSetup(NetworkEngine network) {
        GameSetup setup = network.gameSetup;
        if (setup == null) throw new IllegalStateException("Lobby setup is not initialized");
        return setup;
    }

    private static void validateStartingUnits(NetworkEngine network, int optionId) {
        for (Object option : network.getStartingUnitOptions()) {
            if (option instanceof Integer && ((Integer) option).intValue() == optionId) return;
        }
        throw new IllegalArgumentException("Unknown starting-units option: " + optionId);
    }
}
