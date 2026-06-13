package io.github.endx.rustedfabricapi.api.event;

public final class NetworkHandshakeEvents {
    public static final RustedFabricEvent<NetworkConnectionPacket> BEFORE_SEND_PRE_REGISTER_INFO_REQUEST =
            RustedFabricEvent.create(listeners -> (networkEngine, connection) -> {
                for (NetworkConnectionPacket listener : listeners) {
                    listener.onPacket(networkEngine, connection);
                }
            });

    public static final RustedFabricEvent<NetworkConnectionPacket> AFTER_SEND_PRE_REGISTER_INFO_REQUEST =
            RustedFabricEvent.create(listeners -> (networkEngine, connection) -> {
                for (NetworkConnectionPacket listener : listeners) {
                    listener.onPacket(networkEngine, connection);
                }
            });

    public static final RustedFabricEvent<NetworkConnectionPacket> BEFORE_SEND_PRE_REGISTER_INFO =
            RustedFabricEvent.create(listeners -> (networkEngine, connection) -> {
                for (NetworkConnectionPacket listener : listeners) {
                    listener.onPacket(networkEngine, connection);
                }
            });

    public static final RustedFabricEvent<NetworkConnectionPacket> AFTER_SEND_PRE_REGISTER_INFO =
            RustedFabricEvent.create(listeners -> (networkEngine, connection) -> {
                for (NetworkConnectionPacket listener : listeners) {
                    listener.onPacket(networkEngine, connection);
                }
            });

    public static final RustedFabricEvent<NetworkConnectionPacket> BEFORE_SEND_REGISTER_CONNECTION =
            RustedFabricEvent.create(listeners -> (networkEngine, connection) -> {
                for (NetworkConnectionPacket listener : listeners) {
                    listener.onPacket(networkEngine, connection);
                }
            });

    public static final RustedFabricEvent<NetworkConnectionPacket> AFTER_SEND_REGISTER_CONNECTION =
            RustedFabricEvent.create(listeners -> (networkEngine, connection) -> {
                for (NetworkConnectionPacket listener : listeners) {
                    listener.onPacket(networkEngine, connection);
                }
            });

    public static final RustedFabricEvent<NetworkConnectionPacket> BEFORE_SEND_SERVER_INFO =
            RustedFabricEvent.create(listeners -> (networkEngine, connection) -> {
                for (NetworkConnectionPacket listener : listeners) {
                    listener.onPacket(networkEngine, connection);
                }
            });

    public static final RustedFabricEvent<NetworkConnectionPacket> AFTER_SEND_SERVER_INFO =
            RustedFabricEvent.create(listeners -> (networkEngine, connection) -> {
                for (NetworkConnectionPacket listener : listeners) {
                    listener.onPacket(networkEngine, connection);
                }
            });

    public static final RustedFabricEvent<NetworkConnectionPacket> BEFORE_SEND_INCORRECT_PASSWORD =
            RustedFabricEvent.create(listeners -> (networkEngine, connection) -> {
                for (NetworkConnectionPacket listener : listeners) {
                    listener.onPacket(networkEngine, connection);
                }
            });

    public static final RustedFabricEvent<NetworkConnectionPacket> AFTER_SEND_INCORRECT_PASSWORD =
            RustedFabricEvent.create(listeners -> (networkEngine, connection) -> {
                for (NetworkConnectionPacket listener : listeners) {
                    listener.onPacket(networkEngine, connection);
                }
            });

    public static final RustedFabricEvent<NetworkConnectionMessagePacket> BEFORE_SEND_KICK =
            RustedFabricEvent.create(listeners -> (networkEngine, connection, reason) -> {
                for (NetworkConnectionMessagePacket listener : listeners) {
                    listener.onPacket(networkEngine, connection, reason);
                }
            });

    public static final RustedFabricEvent<NetworkConnectionMessagePacket> AFTER_SEND_KICK =
            RustedFabricEvent.create(listeners -> (networkEngine, connection, reason) -> {
                for (NetworkConnectionMessagePacket listener : listeners) {
                    listener.onPacket(networkEngine, connection, reason);
                }
            });

    public static final RustedFabricEvent<NetworkConnectionPacket> BEFORE_SEND_UPDATE_PLAYER =
            RustedFabricEvent.create(listeners -> (networkEngine, connection) -> {
                for (NetworkConnectionPacket listener : listeners) {
                    listener.onPacket(networkEngine, connection);
                }
            });

    public static final RustedFabricEvent<NetworkConnectionPacket> AFTER_SEND_UPDATE_PLAYER =
            RustedFabricEvent.create(listeners -> (networkEngine, connection) -> {
                for (NetworkConnectionPacket listener : listeners) {
                    listener.onPacket(networkEngine, connection);
                }
            });

    private NetworkHandshakeEvents() {
    }

    @FunctionalInterface
    public interface NetworkConnectionPacket {
        void onPacket(Object networkEngine, Object connection);
    }

    @FunctionalInterface
    public interface NetworkConnectionMessagePacket {
        void onPacket(Object networkEngine, Object connection, String message);
    }
}
