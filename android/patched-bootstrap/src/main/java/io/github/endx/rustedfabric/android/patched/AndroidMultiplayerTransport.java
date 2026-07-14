package io.github.endx.rustedfabric.android.patched;

import android.util.Log;

import java.lang.reflect.Field;

import io.github.endx.rustedfabricapi.api.multiplayer.MultiplayerNetworkBridge;

/** Official Android 1.15 mapping adapter for the shared RFH1 transport. */
final class AndroidMultiplayerTransport {
    private static final String TAG = "RustedFabric/Network";
    private static final MultiplayerNetworkBridge BRIDGE = new MultiplayerNetworkBridge(
            mapping(), (message, failure) -> {
                if (failure == null) Log.i(TAG, message);
                else Log.e(TAG, message, failure);
            });

    private AndroidMultiplayerTransport() {
    }

    static MultiplayerNetworkBridge.Mapping mapping() {
        return new MultiplayerNetworkBridge.Mapping(
                "com.corrodinggames.rts.gameFramework.j.bi",
                "b", "c", "a", "a", "a", "d");
    }

    static void afterClientRegistration(Object engine, Object connection) {
        BRIDGE.connectionReady(engine, connection, MultiplayerNetworkBridge.Side.CLIENT);
    }

    static void afterServerInfo(Object engine, Object connection) {
        if (isServer(engine)) {
            BRIDGE.connectionReady(engine, connection, MultiplayerNetworkBridge.Side.HOST);
        }
    }

    static boolean receive(Object engine, Object packet) {
        return BRIDGE.receive(engine, packet);
    }

    static void resetToSinglePlayer() {
        BRIDGE.resetToSinglePlayer();
    }

    static boolean allowGameStart(Object connection) {
        return BRIDGE.allowGameStart(connection);
    }

    private static boolean isServer(Object engine) {
        try {
            Field field = engine.getClass().getDeclaredField("D");
            field.setAccessible(true);
            return field.getBoolean(engine);
        } catch (ReflectiveOperationException failure) {
            Log.e(TAG, "Could not read mapped server flag", failure);
            return false;
        }
    }
}
