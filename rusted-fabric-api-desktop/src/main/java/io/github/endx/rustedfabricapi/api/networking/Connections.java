package io.github.endx.rustedfabricapi.api.networking;

import io.github.endx.rustedfabricapi.api.multiplayer.MultiplayerManifest;
import io.github.endx.rustedfabricapi.desktop.DesktopMultiplayerTransport;
import rustedwarfare.game.Team;
import rustedwarfare.network.NetworkConnection;
import rustedwarfare.network.NetworkEngine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Typed snapshots and common operations for the game's active network connections. */
public final class Connections {
    private Connections() {
    }

    public static List<NetworkConnection> snapshot(NetworkEngine engine) {
        Objects.requireNonNull(engine, "engine");
        ArrayList<NetworkConnection> result = new ArrayList<NetworkConnection>();
        for (Object value : engine.connections) {
            if (value instanceof NetworkConnection) result.add((NetworkConnection) value);
        }
        return Collections.unmodifiableList(result);
    }

    public static List<NetworkConnection> validated(NetworkEngine engine) {
        ArrayList<NetworkConnection> result = new ArrayList<NetworkConnection>();
        for (NetworkConnection connection : snapshot(engine)) {
            if (connection.validated && connection.isOpen()) result.add(connection);
        }
        return Collections.unmodifiableList(result);
    }

    public static List<NetworkConnection> loaderPeers(NetworkEngine engine) {
        ArrayList<NetworkConnection> result = new ArrayList<NetworkConnection>();
        for (NetworkConnection connection : validated(engine)) {
            if (isLoaderPeer(connection)) result.add(connection);
        }
        return Collections.unmodifiableList(result);
    }

    public static Optional<NetworkConnection> findById(NetworkEngine engine, int connectionId) {
        for (NetworkConnection connection : snapshot(engine)) {
            if (connection.connectionId == connectionId) return Optional.of(connection);
        }
        return Optional.empty();
    }

    public static Optional<Team> player(NetworkConnection connection) {
        return Optional.ofNullable(Objects.requireNonNull(connection, "connection").player);
    }

    public static boolean isLoaderPeer(NetworkConnection connection) {
        return connection != null && DesktopMultiplayerTransport.isLoaderPeer(connection);
    }

    public static Optional<MultiplayerManifest> manifest(NetworkConnection connection) {
        return connection == null ? Optional.empty()
                : DesktopMultiplayerTransport.peerManifest(connection);
    }

    /** Sends the game's normal disconnect reason and closes this connection. */
    public static void disconnect(NetworkConnection connection, String reason) {
        Objects.requireNonNull(connection, "connection")
                .sendDisconnectReasonAndClose(Objects.requireNonNull(reason, "reason"));
    }
}
