package io.github.endx.rustedfabricapi.api.chat.command;

import rustedwarfare.game.Team;
import rustedwarfare.network.NetworkConnection;
import rustedwarfare.network.NetworkEngine;

import java.util.Collections;
import java.util.ArrayList;
import java.util.List;

/** Immutable invocation data for a registered namespaced chat command. */
public final class ChatCommandContext {
    private final NetworkEngine network;
    private final NetworkConnection connection;
    private final Team team;
    private final String senderName;
    private final String rawMessage;
    private final String command;
    private final String rawArguments;
    private final List<String> arguments;

    ChatCommandContext(NetworkEngine network, NetworkConnection connection, Team team,
            String senderName, String rawMessage, ChatCommands.ParsedCommand parsed) {
        this.network = network;
        this.connection = connection;
        this.team = team;
        this.senderName = senderName;
        this.rawMessage = rawMessage;
        this.command = parsed.name;
        this.rawArguments = parsed.rawArguments;
        this.arguments = Collections.unmodifiableList(new ArrayList<String>(parsed.arguments));
    }

    public NetworkEngine network() { return network; }

    /** Null for commands issued directly by the local host. */
    public NetworkConnection connection() { return connection; }

    public Team team() { return team; }

    public String senderName() { return senderName; }

    public String rawMessage() { return rawMessage; }

    public String command() { return command; }

    public String rawArguments() { return rawArguments; }

    public List<String> arguments() { return arguments; }

    public boolean isRemote() { return connection != null; }

    public boolean isLocalHost() { return connection == null && network.isServer; }

    /** Replies only to the invoking player, or locally for the host console/player. */
    public void reply(String message) {
        String checked = requireReply(message);
        if (connection == null) {
            network.addLocalSystemMessage(checked);
        } else {
            network.sendChatMessageFromServerToTarget(null, null, null, checked, connection);
        }
    }

    public void error(String message) {
        network.sendCommandError(requireReply(message), connection);
    }

    private static String requireReply(String message) {
        if (message == null) throw new NullPointerException("message");
        String value = message.trim();
        if (value.isEmpty()) throw new IllegalArgumentException("Reply must not be blank");
        return value;
    }
}
