package io.github.endx.rustedfabric.android.patched;

import android.util.Log;

import java.util.concurrent.atomic.AtomicBoolean;

import io.github.endx.rustedfabricapi.api.RustedFabricAPIContext;
import io.github.endx.rustedfabricapi.api.RustedFabricRuntime;
import io.github.endx.rustedfabricapi.api.event.RuntimeLifecycleEvents;

/** Stable, zero-argument targets called from the woven game DEX. */
public final class EngineLifecycleBridge {
    private static final String TAG = "RustedFabric/Local";
    private static final AtomicBoolean BEFORE_SENT = new AtomicBoolean();
    private static final AtomicBoolean AFTER_SENT = new AtomicBoolean();
    private static final AtomicBoolean GAME_READY_SENT = new AtomicBoolean();

    private EngineLifecycleBridge() {
    }

    public static void beforeEngineInitialization() {
        dispatchOnce(BEFORE_SENT, RuntimeLifecycleEvents.BEFORE_ENGINE_INITIALIZATION, "before");
    }

    public static void afterEngineInitialization() {
        dispatchOnce(AFTER_SENT, RuntimeLifecycleEvents.AFTER_ENGINE_INITIALIZATION, "after");
        dispatchOnce(GAME_READY_SENT, RuntimeLifecycleEvents.GAME_READY, "game-ready");
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
}
