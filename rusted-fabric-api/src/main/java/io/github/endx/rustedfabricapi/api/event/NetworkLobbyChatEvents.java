package io.github.endx.rustedfabricapi.api.event;

public final class NetworkLobbyChatEvents {
    public static final RustedFabricEvent<ChatText> BEFORE_SEND_SYSTEM_MESSAGE = createChatTextEvent();
    public static final RustedFabricEvent<ChatText> AFTER_SEND_SYSTEM_MESSAGE = createChatTextEvent();
    public static final RustedFabricEvent<ChatText> BEFORE_SEND_QUICK_CHAT_COMMAND = createChatTextEvent();
    public static final RustedFabricEvent<ChatText> AFTER_SEND_QUICK_CHAT_COMMAND = createChatTextEvent();
    public static final RustedFabricEvent<ChatText> BEFORE_SEND_TEAM_CHAT_MESSAGE = createChatTextEvent();
    public static final RustedFabricEvent<ChatText> AFTER_SEND_TEAM_CHAT_MESSAGE = createChatTextEvent();
    public static final RustedFabricEvent<ChatText> BEFORE_SEND_CHAT_MESSAGE = createChatTextEvent();
    public static final RustedFabricEvent<ChatText> AFTER_SEND_CHAT_MESSAGE = createChatTextEvent();

    public static final RustedFabricEvent<ServerChatMessage> BEFORE_SEND_SERVER_CHAT_MESSAGE =
            createServerChatMessageEvent();
    public static final RustedFabricEvent<ServerChatMessage> AFTER_SEND_SERVER_CHAT_MESSAGE =
            createServerChatMessageEvent();

    public static final RustedFabricEvent<ReceivedChatMessage> BEFORE_RECORD_RECEIVED_CHAT_MESSAGE =
            createReceivedChatMessageEvent();
    public static final RustedFabricEvent<ReceivedChatMessage> AFTER_RECORD_RECEIVED_CHAT_MESSAGE =
            createReceivedChatMessageEvent();

    public static final RustedFabricEvent<CommandError> BEFORE_SEND_COMMAND_ERROR =
            createCommandErrorEvent();
    public static final RustedFabricEvent<CommandError> AFTER_SEND_COMMAND_ERROR =
            createCommandErrorEvent();

    public static final RustedFabricEvent<ChatCommand> BEFORE_HANDLE_CHAT_COMMAND =
            RustedFabricEvent.create(listeners -> (networkEngine, connection, team, senderName, message, commandName) -> {
                for (ChatCommand listener : listeners) {
                    listener.onEvent(networkEngine, connection, team, senderName, message, commandName);
                }
            });

    public static final RustedFabricEvent<ChatCommandResult> AFTER_HANDLE_CHAT_COMMAND =
            RustedFabricEvent.create(listeners -> (networkEngine, connection, team, senderName, message, commandName, consumed) -> {
                for (ChatCommandResult listener : listeners) {
                    listener.onEvent(networkEngine, connection, team, senderName, message, commandName, consumed);
                }
            });

    public static final RustedFabricEvent<PauseChanged> BEFORE_SET_GAME_PAUSED = createPauseChangedEvent();
    public static final RustedFabricEvent<PauseChanged> AFTER_SET_GAME_PAUSED = createPauseChangedEvent();

    public static final RustedFabricEvent<TeamOnly> BEFORE_REQUEST_KICK_TEAM_AND_PLAYER = createTeamOnlyEvent();
    public static final RustedFabricEvent<TeamOnly> AFTER_REQUEST_KICK_TEAM_AND_PLAYER = createTeamOnlyEvent();
    public static final RustedFabricEvent<TeamOnly> BEFORE_KICK_TEAM_AND_ATTACHED_PLAYER = createTeamOnlyEvent();
    public static final RustedFabricEvent<TeamOnly> AFTER_KICK_TEAM_AND_ATTACHED_PLAYER = createTeamOnlyEvent();
    public static final RustedFabricEvent<TeamOnly> BEFORE_ANNOUNCE_PLAYER_VICTORY = createTeamOnlyEvent();
    public static final RustedFabricEvent<TeamOnly> AFTER_ANNOUNCE_PLAYER_VICTORY = createTeamOnlyEvent();
    public static final RustedFabricEvent<TeamOnly> BEFORE_ANNOUNCE_PLAYER_DEFEATED = createTeamOnlyEvent();
    public static final RustedFabricEvent<TeamOnly> AFTER_ANNOUNCE_PLAYER_DEFEATED = createTeamOnlyEvent();
    public static final RustedFabricEvent<TeamOnly> BEFORE_ANNOUNCE_PLAYER_WIPED_OUT = createTeamOnlyEvent();
    public static final RustedFabricEvent<TeamOnly> AFTER_ANNOUNCE_PLAYER_WIPED_OUT = createTeamOnlyEvent();

    public static final RustedFabricEvent<TeamSlot> BEFORE_MOVE_PLAYER_TO_SLOT = createTeamSlotEvent();
    public static final RustedFabricEvent<TeamSlot> AFTER_MOVE_PLAYER_TO_SLOT = createTeamSlotEvent();
    public static final RustedFabricEvent<TeamSlot> BEFORE_APPLY_MOVE_PLAYER_TO_SLOT = createTeamSlotEvent();
    public static final RustedFabricEvent<TeamSlot> AFTER_APPLY_MOVE_PLAYER_TO_SLOT = createTeamSlotEvent();
    public static final RustedFabricEvent<TeamSlot> BEFORE_REQUEST_SET_ALLY_TEAM = createTeamSlotEvent();
    public static final RustedFabricEvent<TeamSlot> AFTER_REQUEST_SET_ALLY_TEAM = createTeamSlotEvent();

    public static final RustedFabricEvent<TeamSlotWithInteger> BEFORE_REQUEST_MOVE_PLAYER_SLOT =
            createTeamSlotWithIntegerEvent();
    public static final RustedFabricEvent<TeamSlotWithInteger> AFTER_REQUEST_MOVE_PLAYER_SLOT =
            createTeamSlotWithIntegerEvent();

    public static final RustedFabricEvent<NetworkOnly> BEFORE_ADD_AI_TO_GAME = createNetworkOnlyEvent();
    public static final RustedFabricEvent<NetworkOnly> AFTER_ADD_AI_TO_GAME = createNetworkOnlyEvent();

    public static final RustedFabricEvent<AiNamesUpdated> AFTER_UPDATE_AI_NAMES =
            RustedFabricEvent.create(listeners -> (networkEngine, changed) -> {
                for (AiNamesUpdated listener : listeners) {
                    listener.onEvent(networkEngine, changed);
                }
            });

    public static final RustedFabricEvent<TeamLayoutEvent> BEFORE_APPLY_TEAM_LAYOUT =
            createTeamLayoutEvent();
    public static final RustedFabricEvent<TeamLayoutEvent> AFTER_APPLY_TEAM_LAYOUT =
            createTeamLayoutEvent();
    public static final RustedFabricEvent<TeamLayoutEvent> BEFORE_APPLY_TEAM_LAYOUT_LOCKED =
            createTeamLayoutEvent();
    public static final RustedFabricEvent<TeamLayoutEvent> AFTER_APPLY_TEAM_LAYOUT_LOCKED =
            createTeamLayoutEvent();

    private NetworkLobbyChatEvents() {
    }

    private static RustedFabricEvent<ChatText> createChatTextEvent() {
        return RustedFabricEvent.create(listeners -> (networkEngine, text) -> {
            for (ChatText listener : listeners) {
                listener.onEvent(networkEngine, text);
            }
        });
    }

    private static RustedFabricEvent<ServerChatMessage> createServerChatMessageEvent() {
        return RustedFabricEvent.create(listeners -> (networkEngine, sourceConnection, team, senderName, message, targetConnection) -> {
            for (ServerChatMessage listener : listeners) {
                listener.onEvent(networkEngine, sourceConnection, team, senderName, message, targetConnection);
            }
        });
    }

    private static RustedFabricEvent<ReceivedChatMessage> createReceivedChatMessageEvent() {
        return RustedFabricEvent.create(listeners -> (networkEngine, connection, teamId, senderName, message) -> {
            for (ReceivedChatMessage listener : listeners) {
                listener.onEvent(networkEngine, connection, teamId, senderName, message);
            }
        });
    }

    private static RustedFabricEvent<CommandError> createCommandErrorEvent() {
        return RustedFabricEvent.create(listeners -> (networkEngine, message, targetConnection) -> {
            for (CommandError listener : listeners) {
                listener.onEvent(networkEngine, message, targetConnection);
            }
        });
    }

    private static RustedFabricEvent<PauseChanged> createPauseChangedEvent() {
        return RustedFabricEvent.create(listeners -> (networkEngine, paused) -> {
            for (PauseChanged listener : listeners) {
                listener.onEvent(networkEngine, paused);
            }
        });
    }

    private static RustedFabricEvent<TeamOnly> createTeamOnlyEvent() {
        return RustedFabricEvent.create(listeners -> (networkEngine, team) -> {
            for (TeamOnly listener : listeners) {
                listener.onEvent(networkEngine, team);
            }
        });
    }

    private static RustedFabricEvent<TeamSlot> createTeamSlotEvent() {
        return RustedFabricEvent.create(listeners -> (networkEngine, team, slot) -> {
            for (TeamSlot listener : listeners) {
                listener.onEvent(networkEngine, team, slot);
            }
        });
    }

    private static RustedFabricEvent<TeamSlotWithInteger> createTeamSlotWithIntegerEvent() {
        return RustedFabricEvent.create(listeners -> (networkEngine, team, slot, optionalValue) -> {
            for (TeamSlotWithInteger listener : listeners) {
                listener.onEvent(networkEngine, team, slot, optionalValue);
            }
        });
    }

    private static RustedFabricEvent<NetworkOnly> createNetworkOnlyEvent() {
        return RustedFabricEvent.create(listeners -> networkEngine -> {
            for (NetworkOnly listener : listeners) {
                listener.onEvent(networkEngine);
            }
        });
    }

    private static RustedFabricEvent<TeamLayoutEvent> createTeamLayoutEvent() {
        return RustedFabricEvent.create(listeners -> (networkEngine, teamLayout) -> {
            for (TeamLayoutEvent listener : listeners) {
                listener.onEvent(networkEngine, teamLayout);
            }
        });
    }

    @FunctionalInterface
    public interface ChatText {
        void onEvent(Object networkEngine, String text);
    }

    @FunctionalInterface
    public interface ServerChatMessage {
        void onEvent(Object networkEngine, Object sourceConnection, Object team,
                     String senderName, String message, Object targetConnection);
    }

    @FunctionalInterface
    public interface ReceivedChatMessage {
        void onEvent(Object networkEngine, Object connection, int teamId, String senderName, String message);
    }

    @FunctionalInterface
    public interface CommandError {
        void onEvent(Object networkEngine, String message, Object targetConnection);
    }

    @FunctionalInterface
    public interface ChatCommand {
        void onEvent(Object networkEngine, Object connection, Object team,
                     String senderName, String message, String commandName);
    }

    @FunctionalInterface
    public interface ChatCommandResult {
        void onEvent(Object networkEngine, Object connection, Object team,
                     String senderName, String message, String commandName, boolean consumed);
    }

    @FunctionalInterface
    public interface PauseChanged {
        void onEvent(Object networkEngine, boolean paused);
    }

    @FunctionalInterface
    public interface TeamOnly {
        void onEvent(Object networkEngine, Object team);
    }

    @FunctionalInterface
    public interface TeamSlot {
        void onEvent(Object networkEngine, Object team, int slot);
    }

    @FunctionalInterface
    public interface TeamSlotWithInteger {
        void onEvent(Object networkEngine, Object team, int slot, Integer optionalValue);
    }

    @FunctionalInterface
    public interface NetworkOnly {
        void onEvent(Object networkEngine);
    }

    @FunctionalInterface
    public interface AiNamesUpdated {
        void onEvent(Object networkEngine, boolean changed);
    }

    @FunctionalInterface
    public interface TeamLayoutEvent {
        void onEvent(Object networkEngine, Object teamLayout);
    }
}
