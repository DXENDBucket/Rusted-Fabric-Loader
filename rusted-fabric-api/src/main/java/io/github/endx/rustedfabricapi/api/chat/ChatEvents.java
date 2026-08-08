package io.github.endx.rustedfabricapi.api.chat;

import io.github.endx.rustedfabricapi.api.event.RustedFabricEvent;
import rustedwarfare.game.Team;
import rustedwarfare.network.NetworkConnection;
import rustedwarfare.network.NetworkEngine;

/** Typed chat transport and local-receipt boundaries. */
public final class ChatEvents {
    public static final RustedFabricEvent<Outgoing> BEFORE_OUTGOING =
            RustedFabricEvent.create(listeners -> (network, message) -> {
                boolean cancelled = false;
                for (Outgoing listener : listeners) cancelled |= listener.onOutgoing(network, message);
                return cancelled;
            });
    public static final RustedFabricEvent<OutgoingSent> AFTER_OUTGOING =
            RustedFabricEvent.create(listeners -> (network, message) -> {
                for (OutgoingSent listener : listeners) listener.onOutgoing(network, message);
            });
    public static final RustedFabricEvent<BeforeServerMessage> BEFORE_SERVER_MESSAGE =
            RustedFabricEvent.create(listeners -> (network, source, team, sender, message, target) -> {
                boolean cancelled = false;
                for (BeforeServerMessage listener : listeners) {
                    cancelled |= listener.beforeServerMessage(network, source, team, sender, message, target);
                }
                return cancelled;
            });
    public static final RustedFabricEvent<AfterServerMessage> AFTER_SERVER_MESSAGE =
            RustedFabricEvent.create(listeners -> (network, source, team, sender, message, target) -> {
                for (AfterServerMessage listener : listeners) {
                    listener.afterServerMessage(network, source, team, sender, message, target);
                }
            });
    public static final RustedFabricEvent<BeforeReceived> BEFORE_RECEIVED =
            RustedFabricEvent.create(listeners -> (network, connection, teamId, sender, message) -> {
                boolean cancelled = false;
                for (BeforeReceived listener : listeners) {
                    cancelled |= listener.beforeReceived(network, connection, teamId, sender, message);
                }
                return cancelled;
            });
    public static final RustedFabricEvent<AfterReceived> AFTER_RECEIVED =
            RustedFabricEvent.create(listeners -> (network, connection, teamId, sender, message) -> {
                for (AfterReceived listener : listeners) {
                    listener.afterReceived(network, connection, teamId, sender, message);
                }
            });

    private ChatEvents() {
    }

    @FunctionalInterface
    public interface Outgoing {
        boolean onOutgoing(NetworkEngine network, String message);
    }

    @FunctionalInterface
    public interface OutgoingSent {
        void onOutgoing(NetworkEngine network, String message);
    }

    @FunctionalInterface
    public interface BeforeServerMessage {
        boolean beforeServerMessage(NetworkEngine network, NetworkConnection source, Team team,
                String sender, String message, NetworkConnection target);
    }

    @FunctionalInterface
    public interface AfterServerMessage {
        void afterServerMessage(NetworkEngine network, NetworkConnection source, Team team,
                String sender, String message, NetworkConnection target);
    }

    @FunctionalInterface
    public interface BeforeReceived {
        boolean beforeReceived(NetworkEngine network, NetworkConnection connection, int teamId,
                String sender, String message);
    }

    @FunctionalInterface
    public interface AfterReceived {
        void afterReceived(NetworkEngine network, NetworkConnection connection, int teamId,
                String sender, String message);
    }
}
