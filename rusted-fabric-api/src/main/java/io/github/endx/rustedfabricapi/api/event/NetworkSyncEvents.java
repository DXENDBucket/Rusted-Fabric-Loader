package io.github.endx.rustedfabricapi.api.event;

public final class NetworkSyncEvents {
    public static final RustedFabricEvent<NetworkDelta> BEFORE_UPDATE_DESYNC_DETECTION =
            RustedFabricEvent.create(listeners -> (networkEngine, delta) -> {
                for (NetworkDelta listener : listeners) {
                    listener.onEvent(networkEngine, delta);
                }
            });

    public static final RustedFabricEvent<NetworkDelta> AFTER_UPDATE_DESYNC_DETECTION =
            RustedFabricEvent.create(listeners -> (networkEngine, delta) -> {
                for (NetworkDelta listener : listeners) {
                    listener.onEvent(networkEngine, delta);
                }
            });

    public static final RustedFabricEvent<NetworkOnly> BEFORE_QUEUE_QUICK_RESYNC_COMMAND =
            createNetworkOnlyEvent();
    public static final RustedFabricEvent<NetworkOnly> AFTER_QUEUE_QUICK_RESYNC_COMMAND =
            createNetworkOnlyEvent();
    public static final RustedFabricEvent<NetworkOnly> BEFORE_APPLY_QUICK_RESYNC_SAVE =
            createNetworkOnlyEvent();
    public static final RustedFabricEvent<NetworkOnly> AFTER_APPLY_QUICK_RESYNC_SAVE =
            createNetworkOnlyEvent();
    public static final RustedFabricEvent<NetworkOnly> BEFORE_RESET_RESYNC_STATE =
            createNetworkOnlyEvent();
    public static final RustedFabricEvent<NetworkOnly> AFTER_RESET_RESYNC_STATE =
            createNetworkOnlyEvent();
    public static final RustedFabricEvent<NetworkOnly> BEFORE_MARK_RETURN_TO_BATTLEROOM_PENDING =
            createNetworkOnlyEvent();
    public static final RustedFabricEvent<NetworkOnly> AFTER_MARK_RETURN_TO_BATTLEROOM_PENDING =
            createNetworkOnlyEvent();
    public static final RustedFabricEvent<NetworkOnly> BEFORE_RETURN_TO_BATTLEROOM =
            createNetworkOnlyEvent();
    public static final RustedFabricEvent<NetworkOnly> AFTER_RETURN_TO_BATTLEROOM =
            createNetworkOnlyEvent();
    public static final RustedFabricEvent<NetworkOnly> BEFORE_PRUNE_DISCONNECTED_CONNECTIONS =
            createNetworkOnlyEvent();
    public static final RustedFabricEvent<NetworkOnly> AFTER_PRUNE_DISCONNECTED_CONNECTIONS =
            createNetworkOnlyEvent();
    public static final RustedFabricEvent<NetworkOnly> BEFORE_CLEAR_BAN_ENTRIES =
            createNetworkOnlyEvent();
    public static final RustedFabricEvent<NetworkOnly> AFTER_CLEAR_BAN_ENTRIES =
            createNetworkOnlyEvent();
    public static final RustedFabricEvent<NetworkOnly> BEFORE_PRUNE_EXPIRED_BAN_ENTRIES =
            createNetworkOnlyEvent();
    public static final RustedFabricEvent<NetworkOnly> AFTER_PRUNE_EXPIRED_BAN_ENTRIES =
            createNetworkOnlyEvent();

    public static final RustedFabricEvent<ScheduleReturnToBattleroom> BEFORE_SCHEDULE_RETURN_TO_BATTLEROOM =
            RustedFabricEvent.create(listeners -> (networkEngine, seconds) -> {
                for (ScheduleReturnToBattleroom listener : listeners) {
                    listener.onEvent(networkEngine, seconds);
                }
            });

    public static final RustedFabricEvent<ScheduleReturnToBattleroom> AFTER_SCHEDULE_RETURN_TO_BATTLEROOM =
            RustedFabricEvent.create(listeners -> (networkEngine, seconds) -> {
                for (ScheduleReturnToBattleroom listener : listeners) {
                    listener.onEvent(networkEngine, seconds);
                }
            });

    public static final RustedFabricEvent<ConnectionEvent> BEFORE_SEND_RETURN_TO_BATTLEROOM =
            createConnectionEvent();
    public static final RustedFabricEvent<ConnectionEvent> AFTER_SEND_RETURN_TO_BATTLEROOM =
            createConnectionEvent();
    public static final RustedFabricEvent<ConnectionEvent> BEFORE_REMOVE_CONNECTION =
            createConnectionEvent();
    public static final RustedFabricEvent<ConnectionEvent> AFTER_REMOVE_CONNECTION =
            createConnectionEvent();

    public static final RustedFabricEvent<ConnectionReasonEvent> BEFORE_CLOSE_CONNECTION_WITH_REASON =
            createConnectionReasonEvent();
    public static final RustedFabricEvent<ConnectionReasonEvent> AFTER_CLOSE_CONNECTION_WITH_REASON =
            createConnectionReasonEvent();
    public static final RustedFabricEvent<ConnectionReasonEvent> BEFORE_CLOSE_FORWARDED_CHILD_CONNECTIONS =
            createConnectionReasonEvent();
    public static final RustedFabricEvent<ConnectionReasonEvent> AFTER_CLOSE_FORWARDED_CHILD_CONNECTIONS =
            createConnectionReasonEvent();

    public static final RustedFabricEvent<BanConnection> BEFORE_BAN_CONNECTION =
            RustedFabricEvent.create(listeners -> (networkEngine, connection, reason, minutes) -> {
                for (BanConnection listener : listeners) {
                    listener.onEvent(networkEngine, connection, reason, minutes);
                }
            });

    public static final RustedFabricEvent<AfterBanConnection> AFTER_BAN_CONNECTION =
            RustedFabricEvent.create(listeners -> (networkEngine, connection, reason, minutes, result) -> {
                for (AfterBanConnection listener : listeners) {
                    listener.onEvent(networkEngine, connection, reason, minutes, result);
                }
            });

    public static final RustedFabricEvent<CreateForwardedConnection> BEFORE_CREATE_FORWARDED_CONNECTION =
            RustedFabricEvent.create(listeners -> (networkEngine, parentConnection, forwardedClientId, host, queryString) -> {
                for (CreateForwardedConnection listener : listeners) {
                    listener.onEvent(networkEngine, parentConnection, forwardedClientId, host, queryString);
                }
            });

    public static final RustedFabricEvent<AfterCreateForwardedConnection> AFTER_CREATE_FORWARDED_CONNECTION =
            RustedFabricEvent.create(listeners -> (networkEngine, parentConnection, forwardedClientId, host, queryString, childConnection) -> {
                for (AfterCreateForwardedConnection listener : listeners) {
                    listener.onEvent(networkEngine, parentConnection, forwardedClientId, host, queryString, childConnection);
                }
            });

    private NetworkSyncEvents() {
    }

    private static RustedFabricEvent<NetworkOnly> createNetworkOnlyEvent() {
        return RustedFabricEvent.create(listeners -> networkEngine -> {
            for (NetworkOnly listener : listeners) {
                listener.onEvent(networkEngine);
            }
        });
    }

    private static RustedFabricEvent<ConnectionEvent> createConnectionEvent() {
        return RustedFabricEvent.create(listeners -> (networkEngine, connection) -> {
            for (ConnectionEvent listener : listeners) {
                listener.onEvent(networkEngine, connection);
            }
        });
    }

    private static RustedFabricEvent<ConnectionReasonEvent> createConnectionReasonEvent() {
        return RustedFabricEvent.create(listeners -> (networkEngine, connection, reason) -> {
            for (ConnectionReasonEvent listener : listeners) {
                listener.onEvent(networkEngine, connection, reason);
            }
        });
    }

    @FunctionalInterface
    public interface NetworkOnly {
        void onEvent(Object networkEngine);
    }

    @FunctionalInterface
    public interface NetworkDelta {
        void onEvent(Object networkEngine, float delta);
    }

    @FunctionalInterface
    public interface ScheduleReturnToBattleroom {
        void onEvent(Object networkEngine, float seconds);
    }

    @FunctionalInterface
    public interface ConnectionEvent {
        void onEvent(Object networkEngine, Object connection);
    }

    @FunctionalInterface
    public interface ConnectionReasonEvent {
        void onEvent(Object networkEngine, Object connection, String reason);
    }

    @FunctionalInterface
    public interface BanConnection {
        void onEvent(Object networkEngine, Object connection, String reason, int minutes);
    }

    @FunctionalInterface
    public interface AfterBanConnection {
        void onEvent(Object networkEngine, Object connection, String reason, int minutes, boolean result);
    }

    @FunctionalInterface
    public interface CreateForwardedConnection {
        void onEvent(Object networkEngine, Object parentConnection, int forwardedClientId, String host, String queryString);
    }

    @FunctionalInterface
    public interface AfterCreateForwardedConnection {
        void onEvent(Object networkEngine, Object parentConnection, int forwardedClientId,
                     String host, String queryString, Object childConnection);
    }
}
