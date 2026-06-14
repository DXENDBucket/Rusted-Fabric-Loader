package io.github.endx.rustedfabricapi.api.event;

public final class NetworkCallbackEvents {
    public static final RustedFabricEvent<ClientChatMessage> BEFORE_ALLOW_CLIENT_CHAT_MESSAGE =
            createClientChatMessageEvent();
    public static final RustedFabricEvent<ClientChatMessageResult> AFTER_ALLOW_CLIENT_CHAT_MESSAGE =
            createClientChatMessageResultEvent();
    public static final RustedFabricEvent<ServerChatMessage> BEFORE_ALLOW_SERVER_CHAT_MESSAGE =
            createServerChatMessageEvent();
    public static final RustedFabricEvent<ServerChatMessageResult> AFTER_ALLOW_SERVER_CHAT_MESSAGE =
            createServerChatMessageResultEvent();
    public static final RustedFabricEvent<ClientChatMessage> BEFORE_ON_CLIENT_CHAT_MESSAGE_ACCEPTED =
            createClientChatMessageEvent();
    public static final RustedFabricEvent<ClientChatMessage> AFTER_ON_CLIENT_CHAT_MESSAGE_ACCEPTED =
            createClientChatMessageEvent();

    public static final RustedFabricEvent<NewPlayerJoin> BEFORE_VALIDATE_NEW_PLAYER_JOIN =
            createNewPlayerJoinEvent();
    public static final RustedFabricEvent<NewPlayerJoinResult> AFTER_VALIDATE_NEW_PLAYER_JOIN =
            createNewPlayerJoinResultEvent();
    public static final RustedFabricEvent<PlayerSlotJoin> BEFORE_VALIDATE_PLAYER_SLOT_JOIN =
            createPlayerSlotJoinEvent();
    public static final RustedFabricEvent<PlayerSlotJoinResult> AFTER_VALIDATE_PLAYER_SLOT_JOIN =
            createPlayerSlotJoinResultEvent();
    public static final RustedFabricEvent<PlayerRegistered> BEFORE_ON_PLAYER_REGISTERED =
            createPlayerRegisteredEvent();
    public static final RustedFabricEvent<PlayerRegistered> AFTER_ON_PLAYER_REGISTERED =
            createPlayerRegisteredEvent();
    public static final RustedFabricEvent<PlayerAdded> BEFORE_ON_PLAYER_ADDED =
            createPlayerAddedEvent();
    public static final RustedFabricEvent<PlayerAdded> AFTER_ON_PLAYER_ADDED =
            createPlayerAddedEvent();
    public static final RustedFabricEvent<CallbacksOnly> BEFORE_ON_ALL_PLAYERS_READY =
            createCallbacksOnlyEvent();
    public static final RustedFabricEvent<CallbacksOnly> AFTER_ON_ALL_PLAYERS_READY =
            createCallbacksOnlyEvent();

    public static final RustedFabricEvent<ConnectionBooleanResult> AFTER_CAN_GRANT_SERVER_CONTROL =
            createConnectionBooleanResultEvent();
    public static final RustedFabricEvent<ConnectionBooleanResult> AFTER_IS_PROXY_CONTROLLER_CONNECTION =
            createConnectionBooleanResultEvent();
    public static final RustedFabricEvent<CallbacksOnly> BEFORE_ON_BATTLEROOM_CLOSED =
            createCallbacksOnlyEvent();
    public static final RustedFabricEvent<CallbacksOnly> AFTER_ON_BATTLEROOM_CLOSED =
            createCallbacksOnlyEvent();
    public static final RustedFabricEvent<CallbacksBooleanResult> AFTER_IS_GAME_STARTING =
            createCallbacksBooleanResultEvent();

    private NetworkCallbackEvents() {
    }

    private static RustedFabricEvent<ClientChatMessage> createClientChatMessageEvent() {
        return RustedFabricEvent.create(listeners -> (callbacks, connection, senderName, message) -> {
            for (ClientChatMessage listener : listeners) {
                listener.onEvent(callbacks, connection, senderName, message);
            }
        });
    }

    private static RustedFabricEvent<ClientChatMessageResult> createClientChatMessageResultEvent() {
        return RustedFabricEvent.create(listeners -> (callbacks, connection, senderName, message, allowed) -> {
            for (ClientChatMessageResult listener : listeners) {
                listener.onEvent(callbacks, connection, senderName, message, allowed);
            }
        });
    }

    private static RustedFabricEvent<ServerChatMessage> createServerChatMessageEvent() {
        return RustedFabricEvent.create(listeners -> (callbacks, connection, team, message, teamOnly) -> {
            for (ServerChatMessage listener : listeners) {
                listener.onEvent(callbacks, connection, team, message, teamOnly);
            }
        });
    }

    private static RustedFabricEvent<ServerChatMessageResult> createServerChatMessageResultEvent() {
        return RustedFabricEvent.create(listeners -> (callbacks, connection, team, message, teamOnly, allowed) -> {
            for (ServerChatMessageResult listener : listeners) {
                listener.onEvent(callbacks, connection, team, message, teamOnly, allowed);
            }
        });
    }

    private static RustedFabricEvent<NewPlayerJoin> createNewPlayerJoinEvent() {
        return RustedFabricEvent.create(listeners ->
                (callbacks, connection, playerName, networkVersion, appVersion, packageName, playerColor) -> {
                    for (NewPlayerJoin listener : listeners) {
                        listener.onEvent(callbacks, connection, playerName, networkVersion,
                                appVersion, packageName, playerColor);
                    }
                });
    }

    private static RustedFabricEvent<NewPlayerJoinResult> createNewPlayerJoinResultEvent() {
        return RustedFabricEvent.create(listeners ->
                (callbacks, connection, playerName, networkVersion, appVersion, packageName, playerColor, rejectionReason) -> {
                    for (NewPlayerJoinResult listener : listeners) {
                        listener.onEvent(callbacks, connection, playerName, networkVersion,
                                appVersion, packageName, playerColor, rejectionReason);
                    }
                });
    }

    private static RustedFabricEvent<PlayerSlotJoin> createPlayerSlotJoinEvent() {
        return RustedFabricEvent.create(listeners -> (callbacks, connection, playerName) -> {
            for (PlayerSlotJoin listener : listeners) {
                listener.onEvent(callbacks, connection, playerName);
            }
        });
    }

    private static RustedFabricEvent<PlayerSlotJoinResult> createPlayerSlotJoinResultEvent() {
        return RustedFabricEvent.create(listeners -> (callbacks, connection, playerName, rejectionReason) -> {
            for (PlayerSlotJoinResult listener : listeners) {
                listener.onEvent(callbacks, connection, playerName, rejectionReason);
            }
        });
    }

    private static RustedFabricEvent<PlayerRegistered> createPlayerRegisteredEvent() {
        return RustedFabricEvent.create(listeners -> (callbacks, connection, playerName, playerIdText) -> {
            for (PlayerRegistered listener : listeners) {
                listener.onEvent(callbacks, connection, playerName, playerIdText);
            }
        });
    }

    private static RustedFabricEvent<PlayerAdded> createPlayerAddedEvent() {
        return RustedFabricEvent.create(listeners -> (callbacks, team) -> {
            for (PlayerAdded listener : listeners) {
                listener.onEvent(callbacks, team);
            }
        });
    }

    private static RustedFabricEvent<CallbacksOnly> createCallbacksOnlyEvent() {
        return RustedFabricEvent.create(listeners -> callbacks -> {
            for (CallbacksOnly listener : listeners) {
                listener.onEvent(callbacks);
            }
        });
    }

    private static RustedFabricEvent<ConnectionBooleanResult> createConnectionBooleanResultEvent() {
        return RustedFabricEvent.create(listeners -> (callbacks, connection, result) -> {
            for (ConnectionBooleanResult listener : listeners) {
                listener.onEvent(callbacks, connection, result);
            }
        });
    }

    private static RustedFabricEvent<CallbacksBooleanResult> createCallbacksBooleanResultEvent() {
        return RustedFabricEvent.create(listeners -> (callbacks, result) -> {
            for (CallbacksBooleanResult listener : listeners) {
                listener.onEvent(callbacks, result);
            }
        });
    }

    @FunctionalInterface
    public interface ClientChatMessage {
        void onEvent(Object callbacks, Object connection, String senderName, String message);
    }

    @FunctionalInterface
    public interface ClientChatMessageResult {
        void onEvent(Object callbacks, Object connection, String senderName, String message, boolean allowed);
    }

    @FunctionalInterface
    public interface ServerChatMessage {
        void onEvent(Object callbacks, Object connection, Object team, String message, boolean teamOnly);
    }

    @FunctionalInterface
    public interface ServerChatMessageResult {
        void onEvent(Object callbacks, Object connection, Object team,
                     String message, boolean teamOnly, boolean allowed);
    }

    @FunctionalInterface
    public interface NewPlayerJoin {
        void onEvent(Object callbacks, Object connection, String playerName, int networkVersion,
                     int appVersion, String packageName, Object playerColor);
    }

    @FunctionalInterface
    public interface NewPlayerJoinResult {
        void onEvent(Object callbacks, Object connection, String playerName, int networkVersion,
                     int appVersion, String packageName, Object playerColor, String rejectionReason);
    }

    @FunctionalInterface
    public interface PlayerSlotJoin {
        void onEvent(Object callbacks, Object connection, String playerName);
    }

    @FunctionalInterface
    public interface PlayerSlotJoinResult {
        void onEvent(Object callbacks, Object connection, String playerName, String rejectionReason);
    }

    @FunctionalInterface
    public interface PlayerRegistered {
        void onEvent(Object callbacks, Object connection, String playerName, String playerIdText);
    }

    @FunctionalInterface
    public interface PlayerAdded {
        void onEvent(Object callbacks, Object team);
    }

    @FunctionalInterface
    public interface CallbacksOnly {
        void onEvent(Object callbacks);
    }

    @FunctionalInterface
    public interface ConnectionBooleanResult {
        void onEvent(Object callbacks, Object connection, boolean result);
    }

    @FunctionalInterface
    public interface CallbacksBooleanResult {
        void onEvent(Object callbacks, boolean result);
    }
}
