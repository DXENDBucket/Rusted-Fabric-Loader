package io.github.endx.rustedfabricapi.api.multiplayer;

/** Named-namespace adapter; remapping rewrites no common API surface or payload format. */
public final class DesktopMultiplayerTransport {
    private static final MultiplayerNetworkBridge BRIDGE = new MultiplayerNetworkBridge(
            new MultiplayerNetworkBridge.Mapping(
                    "rustedwarfare.network.Packet",
                    "type|b", "bytes|c", "connection|a",
                    "sendPacketToConnection|a", "sendDisconnectReasonAndClose|a",
                    "connectionId|c"),
            (message, failure) -> {
                String line = "[Rusted Fabric multiplayer] " + message;
                if (failure == null) System.out.println(line);
                else {
                    System.err.println(line + ": " + failure);
                    failure.printStackTrace(System.err);
                }
            });

    private DesktopMultiplayerTransport() {
    }

    public static void afterClientRegistration(Object engine, Object connection) {
        BRIDGE.connectionReady(engine, connection, MultiplayerNetworkBridge.Side.CLIENT);
    }

    public static void afterServerInfo(Object engine, Object connection) {
        BRIDGE.connectionReady(engine, connection, MultiplayerNetworkBridge.Side.HOST);
    }

    public static boolean receive(Object engine, Object packet) {
        return BRIDGE.receive(engine, packet);
    }

    public static void resetToSinglePlayer() {
        BRIDGE.resetToSinglePlayer();
    }

    public static boolean allowGameStart(Object connection) {
        return BRIDGE.allowGameStart(connection);
    }
}
