package io.github.endx.rustedfabric.android.patched;

import android.util.Log;

import com.corrodinggames.rts.appFramework.RWApplication;

/** Application wrapper injected into a verified user-owned APK by the local patcher. */
public final class PatchedApplication extends RWApplication {
    private static final String TAG = "RustedFabric/Local";

    @Override
    public void onCreate() {
        super.onCreate();
        try {
            PatchedRuntime.start(this);
        } catch (ThreadDeath | VirtualMachineError critical) {
            throw critical;
        } catch (Throwable failure) {
            // A loader failure must never prevent the unmodified game application from starting.
            Log.e(TAG, "Local loader startup failed; continuing without mods", failure);
        }
    }
}
