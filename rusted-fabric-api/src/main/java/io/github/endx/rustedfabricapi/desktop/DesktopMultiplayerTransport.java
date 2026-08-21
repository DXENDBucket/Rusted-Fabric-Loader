package io.github.endx.rustedfabricapi.desktop;

import io.github.endx.rustedfabricapi.api.multiplayer.MultiplayerNetworkBridge;
import io.github.endx.rustedfabricapi.api.multiplayer.MultiplayerManifest;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.WeakHashMap;

/** Named-namespace adapter; remapping rewrites no common API surface or payload format. */
public final class DesktopMultiplayerTransport {
    private static final Map<Object, Boolean> READY_CLIENTS =
            Collections.synchronizedMap(new WeakHashMap<Object, Boolean>());
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

    /**
     * Starts the Loader handshake once the native protocol has validated the real game server.
     *
     * <p>In particular this must not run immediately after {@code sendRegisterConnection}.
     * A Relay entrance socket uses that same native method while it is still negotiating a
     * redirect and rejects arbitrary game packets at that stage.</p>
     *
     * @return true once for each newly ready client connection
     */
    public static boolean clientConnectionReady(Object engine, Object connection) {
        if (engine == null || connection == null) return false;
        synchronized (READY_CLIENTS) {
            if (READY_CLIENTS.put(connection, Boolean.TRUE) != null) return false;
        }
        BRIDGE.connectionReady(engine, connection, MultiplayerNetworkBridge.Side.CLIENT);
        return true;
    }

    public static void afterServerInfo(Object engine, Object connection) {
        BRIDGE.connectionReady(engine, connection, MultiplayerNetworkBridge.Side.HOST);
    }

    public static boolean receive(Object engine, Object packet) {
        return BRIDGE.receive(engine, packet);
    }

    public static void resetToSinglePlayer() {
        READY_CLIENTS.clear();
        BRIDGE.resetToSinglePlayer();
    }

    public static boolean allowGameStart(Object connection) {
        return BRIDGE.allowGameStart(connection);
    }

    public static boolean isLoaderPeer(Object connection) {
        return BRIDGE.isLoaderPeer(connection);
    }

    public static boolean hasLoaderPeer() {
        return BRIDGE.hasLoaderPeer();
    }

    public static Optional<MultiplayerManifest> peerManifest(Object connection) {
        return BRIDGE.peerManifest(connection);
    }

    public static Optional<MultiplayerManifest> firstLoaderPeerManifest() {
        return BRIDGE.firstLoaderPeerManifest();
    }

    public static void connectionClosed(Object connection) {
        READY_CLIENTS.remove(connection);
        BRIDGE.connectionClosed(connection);
    }
}
