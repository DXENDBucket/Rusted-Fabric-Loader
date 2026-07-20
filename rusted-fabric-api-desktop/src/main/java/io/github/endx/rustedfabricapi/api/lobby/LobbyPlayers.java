package io.github.endx.rustedfabricapi.api.lobby;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import io.github.endx.rustedfabricapi.api.client.RustedWarfareClient;
import io.github.endx.rustedfabricapi.api.networking.Connections;
import io.github.endx.rustedfabricapi.api.unit.Teams;
import rustedwarfare.ai.AiTeam;
import rustedwarfare.game.Team;
import rustedwarfare.network.NetworkConnection;
import rustedwarfare.network.NetworkEngine;

/** Native-synchronization-preserving lobby player and match administration helpers. */
public final class LobbyPlayers {
    /** Native sentinel accepted by the move command for a spectator slot. */
    public static final int SPECTATOR_SLOT = -3;

    private LobbyPlayers() {
    }

    public static List<Team> snapshot() {
        return snapshot(true);
    }

    /** Returns active teams, optionally excluding spectators. */
    public static List<Team> snapshot(boolean includeSpectators) {
        if (includeSpectators) return Teams.snapshot(true);
        NetworkEngine network = network();
        ArrayList<Team> result = new ArrayList<Team>();
        for (Object value : network.getTeamListSnapshot()) {
            if (value instanceof Team) result.add((Team) value);
        }
        return Collections.unmodifiableList(result);
    }

    public static Optional<NetworkConnection> connection(Team team) {
        Team checked = Objects.requireNonNull(team, "team");
        NetworkEngine network = network();
        for (NetworkConnection connection : Connections.validated(network)) {
            if (connection.player == checked) return Optional.of(connection);
        }
        return Optional.empty();
    }

    /** Adds an AI through the native host path and returns the newly registered team when visible. */
    public static Optional<Team> addAi() {
        NetworkEngine network = requireServer();
        requirePreGame(network);
        Set<Team> previous = Collections.newSetFromMap(new IdentityHashMap<Team, Boolean>());
        previous.addAll(snapshot());
        network.addAIToGame();
        for (Team team : snapshot()) {
            if (team instanceof AiTeam && !previous.contains(team)) return Optional.of(team);
        }
        return Optional.empty();
    }

    /** Works for a host or a native proxy controller and keeps the game's normal kick/ban policy. */
    public static void requestKick(Team team) {
        NetworkEngine network = requireController();
        network.requestKickTeamAndPlayer(Objects.requireNonNull(team, "team"));
    }

    /** Requests a zero-based player slot and optional zero-based ally-team override. */
    public static void requestMoveToSlot(Team team, int zeroBasedSlot, Integer allyTeamOverride) {
        NetworkEngine network = requireController();
        requirePreGame(network);
        if (zeroBasedSlot != SPECTATOR_SLOT) checkIndex(zeroBasedSlot, "slot");
        if (allyTeamOverride != null && allyTeamOverride.intValue() != -1) {
            checkIndex(allyTeamOverride.intValue(), "allyTeamOverride");
        }
        network.requestMovePlayerSlot(Objects.requireNonNull(team, "team"),
                zeroBasedSlot, allyTeamOverride);
    }

    /** Uses -1 for automatic and otherwise a zero-based ally-team index. */
    public static void requestAllyTeam(Team team, int zeroBasedAllyTeam) {
        NetworkEngine network = requireController();
        requirePreGame(network);
        if (zeroBasedAllyTeam != -1) checkIndex(zeroBasedAllyTeam, "allyTeam");
        network.requestSetAllyTeam(Objects.requireNonNull(team, "team"), zeroBasedAllyTeam);
    }

    public static void applyLayout(LobbyTeamLayout layout) {
        NetworkEngine network = requireServer();
        requirePreGame(network);
        network.applyTeamLayoutLocked(Objects.requireNonNull(layout, "layout").toNative());
    }

    public static void setPaused(boolean paused) {
        NetworkEngine network = requireServer();
        if (!network.hasGameBeenStarted()) {
            throw new IllegalStateException("Pause state requires a started game");
        }
        network.setGamePaused(paused);
    }

    private static NetworkEngine network() {
        NetworkEngine network = RustedWarfareClient.requireEngine().networkEngine;
        if (network == null) throw new IllegalStateException("Network engine is not initialized");
        return network;
    }

    private static NetworkEngine requireController() {
        NetworkEngine network = network();
        if (!network.isServerOrProxyController()) {
            throw new IllegalStateException("Operation requires host or proxy control");
        }
        return network;
    }

    private static NetworkEngine requireServer() {
        NetworkEngine network = network();
        if (!network.isServer) throw new IllegalStateException("Operation requires the host");
        return network;
    }

    private static void requirePreGame(NetworkEngine network) {
        if (network.hasGameBeenStarted() || network.isGameStarting()) {
            throw new IllegalStateException("Lobby player operation cannot run after game start begins");
        }
    }

    private static void checkIndex(int value, String name) {
        if (value < 0 || value > 255) {
            throw new IllegalArgumentException(name + " must be between 0 and 255");
        }
    }
}
