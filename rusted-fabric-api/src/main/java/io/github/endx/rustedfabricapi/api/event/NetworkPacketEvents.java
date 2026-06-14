package io.github.endx.rustedfabricapi.api.event;

public final class NetworkPacketEvents {
    public static final RustedFabricEvent<NetworkBoolean> BEFORE_RESET_NETWORK_STATE =
            createNetworkBooleanEvent();
    public static final RustedFabricEvent<NetworkBoolean> AFTER_RESET_NETWORK_STATE =
            createNetworkBooleanEvent();
    public static final RustedFabricEvent<NetworkString> BEFORE_DISCONNECT_WITH_REASON =
            createNetworkStringEvent();
    public static final RustedFabricEvent<NetworkString> AFTER_DISCONNECT_WITH_REASON =
            createNetworkStringEvent();

    public static final RustedFabricEvent<PacketEvent> BEFORE_SEND_PACKET_TO_VALIDATED_CONNECTIONS =
            createPacketEvent();
    public static final RustedFabricEvent<PacketEvent> AFTER_SEND_PACKET_TO_VALIDATED_CONNECTIONS =
            createPacketEvent();
    public static final RustedFabricEvent<PacketEvent> BEFORE_SEND_PACKET_TO_ALL_INCLUDING_RELAY =
            createPacketEvent();
    public static final RustedFabricEvent<PacketEvent> AFTER_SEND_PACKET_TO_ALL_INCLUDING_RELAY =
            createPacketEvent();
    public static final RustedFabricEvent<PacketEvent> BEFORE_SEND_PACKET_TO_SERVER =
            createPacketEvent();
    public static final RustedFabricEvent<PacketEvent> AFTER_SEND_PACKET_TO_SERVER =
            createPacketEvent();
    public static final RustedFabricEvent<PacketEvent> BEFORE_SEND_PACKET_TO_CLIENTS_INCLUDING_RELAY =
            createPacketEvent();
    public static final RustedFabricEvent<PacketEvent> AFTER_SEND_PACKET_TO_CLIENTS_INCLUDING_RELAY =
            createPacketEvent();
    public static final RustedFabricEvent<PacketEvent> BEFORE_SEND_PACKET_TO_CLIENTS =
            createPacketEvent();
    public static final RustedFabricEvent<PacketEvent> AFTER_SEND_PACKET_TO_CLIENTS =
            createPacketEvent();

    public static final RustedFabricEvent<NetworkOnly> BEFORE_SEND_REGISTER_CONNECTION_TO_ALL =
            createNetworkOnlyEvent();
    public static final RustedFabricEvent<NetworkOnly> AFTER_SEND_REGISTER_CONNECTION_TO_ALL =
            createNetworkOnlyEvent();
    public static final RustedFabricEvent<NetworkOnly> BEFORE_RESET_NETWORK_CLIENT_ID =
            createNetworkOnlyEvent();
    public static final RustedFabricEvent<NetworkOnly> AFTER_RESET_NETWORK_CLIENT_ID =
            createNetworkOnlyEvent();
    public static final RustedFabricEvent<NetworkOnly> BEFORE_GENERATE_NEW_SERVER_ID =
            createNetworkOnlyEvent();
    public static final RustedFabricEvent<NetworkOnly> AFTER_GENERATE_NEW_SERVER_ID =
            createNetworkOnlyEvent();
    public static final RustedFabricEvent<NetworkOnly> BEFORE_UPDATE_SHARED_CONTROL_DUE_TO_DISCONNECTS =
            createNetworkOnlyEvent();
    public static final RustedFabricEvent<NetworkOnly> AFTER_UPDATE_SHARED_CONTROL_DUE_TO_DISCONNECTS =
            createNetworkOnlyEvent();
    public static final RustedFabricEvent<NetworkOnly> BEFORE_RESET_CLIENT_READY_FLAGS =
            createNetworkOnlyEvent();
    public static final RustedFabricEvent<NetworkOnly> AFTER_RESET_CLIENT_READY_FLAGS =
            createNetworkOnlyEvent();

    public static final RustedFabricEvent<ReadyCheck> BEFORE_ARE_ALL_CLIENTS_READY =
            RustedFabricEvent.create(listeners -> (networkEngine, includeSpectators, minimumPlayerCount) -> {
                for (ReadyCheck listener : listeners) {
                    listener.onEvent(networkEngine, includeSpectators, minimumPlayerCount);
                }
            });
    public static final RustedFabricEvent<ReadyCheckResult> AFTER_ARE_ALL_CLIENTS_READY =
            RustedFabricEvent.create(listeners -> (networkEngine, includeSpectators, minimumPlayerCount, ready) -> {
                for (ReadyCheckResult listener : listeners) {
                    listener.onEvent(networkEngine, includeSpectators, minimumPlayerCount, ready);
                }
            });

    public static final RustedFabricEvent<LocalPlayerName> BEFORE_SET_LOCAL_PLAYER_NAME =
            RustedFabricEvent.create(listeners -> (networkEngine, playerName) -> {
                for (LocalPlayerName listener : listeners) {
                    listener.onEvent(networkEngine, playerName);
                }
            });
    public static final RustedFabricEvent<LocalPlayerNameResult> AFTER_SET_LOCAL_PLAYER_NAME =
            RustedFabricEvent.create(listeners -> (networkEngine, playerName, resultName) -> {
                for (LocalPlayerNameResult listener : listeners) {
                    listener.onEvent(networkEngine, playerName, resultName);
                }
            });

    public static final RustedFabricEvent<NetworkOnly> BEFORE_UPDATE_AI_DIFFICULTY =
            createNetworkOnlyEvent();
    public static final RustedFabricEvent<NetworkOnly> AFTER_UPDATE_AI_DIFFICULTY =
            createNetworkOnlyEvent();
    public static final RustedFabricEvent<TeamEvent> BEFORE_APPLY_AI_DIFFICULTY_FOR_TEAM =
            createTeamEvent();
    public static final RustedFabricEvent<TeamEvent> AFTER_APPLY_AI_DIFFICULTY_FOR_TEAM =
            createTeamEvent();
    public static final RustedFabricEvent<TeamEvent> BEFORE_UPDATE_AI_TEAM_NAME =
            createTeamEvent();
    public static final RustedFabricEvent<TeamResultEvent> AFTER_UPDATE_AI_TEAM_NAME =
            RustedFabricEvent.create(listeners -> (networkEngine, team, changed) -> {
                for (TeamResultEvent listener : listeners) {
                    listener.onEvent(networkEngine, team, changed);
                }
            });
    public static final RustedFabricEvent<GameSetupEvent> BEFORE_APPLY_PROXY_CONTROL_SETUP =
            createGameSetupEvent();
    public static final RustedFabricEvent<GameSetupEvent> AFTER_APPLY_PROXY_CONTROL_SETUP =
            createGameSetupEvent();

    private NetworkPacketEvents() {
    }

    private static RustedFabricEvent<NetworkOnly> createNetworkOnlyEvent() {
        return RustedFabricEvent.create(listeners -> networkEngine -> {
            for (NetworkOnly listener : listeners) {
                listener.onEvent(networkEngine);
            }
        });
    }

    private static RustedFabricEvent<NetworkBoolean> createNetworkBooleanEvent() {
        return RustedFabricEvent.create(listeners -> (networkEngine, value) -> {
            for (NetworkBoolean listener : listeners) {
                listener.onEvent(networkEngine, value);
            }
        });
    }

    private static RustedFabricEvent<NetworkString> createNetworkStringEvent() {
        return RustedFabricEvent.create(listeners -> (networkEngine, text) -> {
            for (NetworkString listener : listeners) {
                listener.onEvent(networkEngine, text);
            }
        });
    }

    private static RustedFabricEvent<PacketEvent> createPacketEvent() {
        return RustedFabricEvent.create(listeners -> (networkEngine, packet) -> {
            for (PacketEvent listener : listeners) {
                listener.onEvent(networkEngine, packet);
            }
        });
    }

    private static RustedFabricEvent<TeamEvent> createTeamEvent() {
        return RustedFabricEvent.create(listeners -> (networkEngine, team) -> {
            for (TeamEvent listener : listeners) {
                listener.onEvent(networkEngine, team);
            }
        });
    }

    private static RustedFabricEvent<GameSetupEvent> createGameSetupEvent() {
        return RustedFabricEvent.create(listeners -> (networkEngine, gameSetup) -> {
            for (GameSetupEvent listener : listeners) {
                listener.onEvent(networkEngine, gameSetup);
            }
        });
    }

    @FunctionalInterface
    public interface NetworkOnly {
        void onEvent(Object networkEngine);
    }

    @FunctionalInterface
    public interface NetworkBoolean {
        void onEvent(Object networkEngine, boolean value);
    }

    @FunctionalInterface
    public interface NetworkString {
        void onEvent(Object networkEngine, String text);
    }

    @FunctionalInterface
    public interface PacketEvent {
        void onEvent(Object networkEngine, Object packet);
    }

    @FunctionalInterface
    public interface ReadyCheck {
        void onEvent(Object networkEngine, boolean includeSpectators, int minimumPlayerCount);
    }

    @FunctionalInterface
    public interface ReadyCheckResult {
        void onEvent(Object networkEngine, boolean includeSpectators, int minimumPlayerCount, boolean ready);
    }

    @FunctionalInterface
    public interface LocalPlayerName {
        void onEvent(Object networkEngine, String playerName);
    }

    @FunctionalInterface
    public interface LocalPlayerNameResult {
        void onEvent(Object networkEngine, String playerName, String resultName);
    }

    @FunctionalInterface
    public interface TeamEvent {
        void onEvent(Object networkEngine, Object team);
    }

    @FunctionalInterface
    public interface TeamResultEvent {
        void onEvent(Object networkEngine, Object team, boolean changed);
    }

    @FunctionalInterface
    public interface GameSetupEvent {
        void onEvent(Object networkEngine, Object gameSetup);
    }
}
