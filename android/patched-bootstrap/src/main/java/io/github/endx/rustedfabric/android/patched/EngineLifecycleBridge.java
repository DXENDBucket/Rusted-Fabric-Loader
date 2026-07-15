package io.github.endx.rustedfabric.android.patched;

import android.util.Log;

import java.util.concurrent.atomic.AtomicBoolean;

import io.github.endx.rustedfabricapi.api.RustedFabricAPIContext;
import io.github.endx.rustedfabricapi.api.RustedFabricRuntime;
import io.github.endx.rustedfabricapi.api.event.RuntimeLifecycleEvents;
import io.github.endx.rustedfabricapi.api.session.GameSession;
import io.github.endx.rustedfabricapi.api.session.GameSessionRuntime;
import io.github.endx.rustedfabricapi.android.AndroidMultiplayerTransport;

/** Stable, zero-argument targets called from the woven game DEX. */
public final class EngineLifecycleBridge {
    private static final String TAG = "RustedFabric/Local";
    private static final AtomicBoolean BEFORE_SENT = new AtomicBoolean();
    private static final AtomicBoolean AFTER_SENT = new AtomicBoolean();
    private static final AtomicBoolean GAME_READY_SENT = new AtomicBoolean();
    private static final AndroidMultiplayerTransport NETWORK =
            new AndroidMultiplayerTransport((message, failure) -> {
                if (failure == null) Log.i("RustedFabric/Network", message);
                else Log.e("RustedFabric/Network", message, failure);
            });

    private EngineLifecycleBridge() {
    }

    public static void beforeEngineInitialization() {
        dispatchOnce(BEFORE_SENT, RuntimeLifecycleEvents.BEFORE_ENGINE_INITIALIZATION, "before");
    }

    public static void afterEngineInitialization() {
        dispatchOnce(AFTER_SENT, RuntimeLifecycleEvents.AFTER_ENGINE_INITIALIZATION, "after");
        dispatchOnce(GAME_READY_SENT, RuntimeLifecycleEvents.GAME_READY, "game-ready");
        GameSessionRuntime.transition(GameSession.Kind.SINGLE_PLAYER);
    }

    /** Called after the mapped client register packet has been sent. */
    public static void afterClientRegistration(Object networkEngine, Object connection) {
        safely("client-register", () -> NETWORK.afterClientRegistration(
                networkEngine, connection));
    }

    /** Called after the mapped server-info packet has been sent. */
    public static void afterServerInfo(Object networkEngine, Object connection) {
        safely("server-info", () -> NETWORK.afterServerInfo(
                networkEngine, connection));
    }

    /** Called before the game's system packet switch; unknown RFH1 packets are otherwise harmless. */
    public static void onSystemPacket(Object networkEngine, Object packet) {
        safely("system-packet", () -> NETWORK.receive(networkEngine, packet));
    }

    public static void afterNetworkReset(Object networkEngine, boolean chatOnly) {
        if (!chatOnly) safely("network-reset", NETWORK::resetToSinglePlayer);
    }

    public static boolean allowGameStart(Object networkEngine, Object connection) {
        try {
            return NETWORK.allowGameStart(connection);
        } catch (ThreadDeath | VirtualMachineError critical) {
            throw critical;
        } catch (Throwable failure) {
            Log.e(TAG, "Network start gate failed; refusing unsafe start", failure);
            return false;
        }
    }

    private static void dispatchOnce(AtomicBoolean guard,
            RuntimeLifecycleEvents.EngineInitializationEvent event, String phase) {
        if (!guard.compareAndSet(false, true)) return;
        try {
            RustedFabricAPIContext context = RustedFabricRuntime.currentContext().orElse(null);
            if (context == null) {
                Log.w(TAG, "Skipped " + phase + " engine event: runtime is not ready");
                return;
            }
            RuntimeLifecycleEvents.DispatchResult result = event.dispatch(context);
            Log.i(TAG, "Engine " + phase + " event: listeners=" + result.listenerCount()
                    + " failed=" + result.failureCount());
        } catch (ThreadDeath | VirtualMachineError critical) {
            throw critical;
        } catch (Throwable failure) {
            // The injected bridge must not turn an optional Loader failure into a game crash.
            Log.e(TAG, "Engine " + phase + " event failed; continuing game startup", failure);
        }
    }

    private static void safely(String phase, Runnable action) {
        try {
            action.run();
        } catch (ThreadDeath | VirtualMachineError critical) {
            throw critical;
        } catch (Throwable failure) {
            Log.e(TAG, "Network " + phase + " callback failed; continuing", failure);
        }
    }
}
