package io.github.endx.rustedfabricapi.android;

import java.lang.reflect.Field;

import io.github.endx.rustedfabricapi.api.multiplayer.MultiplayerNetworkBridge;

/** Exact Rusted Warfare 1.15 Android adapter for the shared RFH1 transport. */
public final class AndroidMultiplayerTransport {
    private static final String PACKET_CLASS = "com.corrodinggames.rts.gameFramework.j.bi";

    private final MultiplayerNetworkBridge.Logger logger;
    private final MultiplayerNetworkBridge bridge;

    public AndroidMultiplayerTransport(MultiplayerNetworkBridge.Logger logger) {
        this.logger = logger != null ? logger : (message, failure) -> { };
        this.bridge = new MultiplayerNetworkBridge(mapping(), this.logger);
    }

    public static MultiplayerNetworkBridge.Mapping mapping() {
        return new MultiplayerNetworkBridge.Mapping(
                PACKET_CLASS, "b", "c", "a", "a", "a", "d");
    }

    public void afterClientRegistration(Object engine, Object connection) {
        bridge.connectionReady(engine, connection, MultiplayerNetworkBridge.Side.CLIENT);
    }

    public void afterServerInfo(Object engine, Object connection) {
        if (isServer(engine)) {
            bridge.connectionReady(engine, connection, MultiplayerNetworkBridge.Side.HOST);
        }
    }

    public boolean receive(Object engine, Object packet) {
        return bridge.receive(engine, packet);
    }

    public void resetToSinglePlayer() {
        bridge.resetToSinglePlayer();
    }

    public boolean allowGameStart(Object connection) {
        return bridge.allowGameStart(connection);
    }

    private boolean isServer(Object engine) {
        try {
            Field field = engine.getClass().getDeclaredField("D");
            field.setAccessible(true);
            return field.getBoolean(engine);
        } catch (ReflectiveOperationException failure) {
            logger.log("Could not read mapped Android server flag", failure);
            return false;
        }
    }
}
