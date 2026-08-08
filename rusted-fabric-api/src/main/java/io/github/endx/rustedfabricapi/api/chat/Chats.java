package io.github.endx.rustedfabricapi.api.chat;

import io.github.endx.rustedfabricapi.api.client.RustedWarfareClient;
import rustedwarfare.game.Team;
import rustedwarfare.network.NetworkConnection;
import rustedwarfare.network.NetworkEngine;

/** High-level access to the game's normal chat transport and local chat display. */
public final class Chats {
    private static final int MAX_MESSAGE_LENGTH = 4096;

    private Chats() {
    }

    public static NetworkEngine network() {
        NetworkEngine network = RustedWarfareClient.requireEngine().networkEngine;
        if (network == null) throw new IllegalStateException("Network engine is not initialized");
        return network;
    }

    /** Sends as the local player, or records locally when no network session is active. */
    public static void send(String message) {
        network().sendChatMessage(requireMessage(message));
    }

    public static void sendToTeam(String message) {
        network().sendTeamChatMessage(requireMessage(message));
    }

    /** Broadcasts a sender-less server message. */
    public static void broadcastSystem(String message) {
        NetworkEngine network = requireServer();
        network.sendSystemMessage(requireMessage(message));
    }

    /** Sends a server-authored line to all eligible players. */
    public static void broadcast(String sender, String message) {
        NetworkEngine network = requireServer();
        network.sendChatMessageFromServer(null, null, optionalSender(sender), requireMessage(message));
    }

    /** Sends a server-authored line to a single validated connection. */
    public static void sendTo(NetworkConnection target, String sender, String message) {
        if (target == null) throw new NullPointerException("target");
        NetworkEngine network = requireServer();
        network.sendChatMessageFromServerToTarget(
                null, null, optionalSender(sender), requireMessage(message), target);
    }

    /** Sends through the native team-only routing path. */
    public static void sendToTeam(Team team, String sender, String message) {
        if (team == null) throw new NullPointerException("team");
        NetworkEngine network = requireServer();
        network.sendChatMessageFromServer(null, team, optionalSender(sender),
                "-t " + requireMessage(message));
    }

    /** Adds a sender-less message only to this process's chat history/UI. */
    public static void showLocalSystem(String message) {
        network().addLocalSystemMessage(requireMessage(message));
    }

    private static NetworkEngine requireServer() {
        NetworkEngine network = network();
        if (!network.isServer) throw new IllegalStateException("Operation requires the server/host");
        return network;
    }

    static String requireMessage(String message) {
        if (message == null) throw new NullPointerException("message");
        String value = message.trim();
        if (value.isEmpty()) throw new IllegalArgumentException("Chat message must not be blank");
        if (value.length() > MAX_MESSAGE_LENGTH) throw new IllegalArgumentException("Chat message is too long");
        return value;
    }

    private static String optionalSender(String sender) {
        if (sender == null) return null;
        String value = sender.trim();
        return value.isEmpty() ? null : value;
    }
}
