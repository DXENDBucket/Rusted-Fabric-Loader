package io.github.endx.rustedfabricapi.api.networking.event;

import io.github.endx.rustedfabricapi.api.event.RustedFabricEvent;
import rustedwarfare.network.NetworkConnection;
import rustedwarfare.network.NetworkEngine;

/** Strongly typed multiplayer connection lifecycle boundaries. */
public final class ConnectionEvents {
    /** Client connection exists and its registration packet has been sent. */
    public static final RustedFabricEvent<ClientReady> CLIENT_CONNECTION_READY =
            RustedFabricEvent.create(listeners -> (engine, connection) -> {
                for (ClientReady listener : listeners) listener.onReady(engine, connection);
            });

    /** Host accepted and registered a player connection. */
    public static final RustedFabricEvent<PlayerRegistered> SERVER_PLAYER_REGISTERED =
            RustedFabricEvent.create(listeners -> (connection, playerName, playerIdText) -> {
                for (PlayerRegistered listener : listeners) {
                    listener.onRegistered(connection, playerName, playerIdText);
                }
            });

    /** A close with a user-visible reason is about to be requested. */
    public static final RustedFabricEvent<Closing> CONNECTION_CLOSING =
            RustedFabricEvent.create(listeners -> (connection, reason) -> {
                for (Closing listener : listeners) listener.onClosing(connection, reason);
            });

    /** Connection was removed from the engine; its peer metadata remains visible during this callback. */
    public static final RustedFabricEvent<Removed> CONNECTION_REMOVED =
            RustedFabricEvent.create(listeners -> (engine, connection) -> {
                for (Removed listener : listeners) listener.onRemoved(engine, connection);
            });

    private ConnectionEvents() {
    }

    @FunctionalInterface
    public interface ClientReady {
        void onReady(NetworkEngine engine, NetworkConnection connection);
    }

    @FunctionalInterface
    public interface PlayerRegistered {
        void onRegistered(NetworkConnection connection, String playerName, String playerIdText);
    }

    @FunctionalInterface
    public interface Closing {
        void onClosing(NetworkConnection connection, String reason);
    }

    @FunctionalInterface
    public interface Removed {
        void onRemoved(NetworkEngine engine, NetworkConnection connection);
    }
}
