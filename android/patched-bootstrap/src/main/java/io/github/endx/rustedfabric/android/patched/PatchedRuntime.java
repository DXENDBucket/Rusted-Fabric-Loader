package io.github.endx.rustedfabric.android.patched;

import android.app.Application;
import android.os.Build;
import android.util.Log;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import io.github.endx.rustedfabricapi.api.RustedFabricAPIContext;
import io.github.endx.rustedfabricapi.api.RustedFabricAPIKeys;
import io.github.endx.rustedfabricapi.api.RustedFabricRuntime;

/** Starts the code-only local-patch backend before the first game Activity is created. */
final class PatchedRuntime {
    private static final String TAG = "RustedFabric/Local";
    private static final AtomicBoolean STARTED = new AtomicBoolean();

    private PatchedRuntime() {
    }

    static void start(Application application) {
        if (!STARTED.compareAndSet(false, true)) {
            return;
        }
        RustedFabricAPIContext context = createContext(application);
        RustedFabricRuntime.installContext(context);
        BootstrapModLoader.Summary summary = new BootstrapModLoader().loadAll(
                application, application.getClassLoader(), context);
        if (summary.discoveryFailure != null) {
            Log.w(TAG, "Mod provider is unavailable; game will run without Loader mods",
                    summary.discoveryFailure);
        } else {
            Log.i(TAG, "Local backend ready: discovered=" + summary.discovered
                    + " loaded=" + summary.loaded + " failed=" + summary.failed);
        }
    }

    private static RustedFabricAPIContext createContext(Application application) {
        Map<String, Object> values = new HashMap<>();
        values.put(RustedFabricAPIKeys.K_CONTEXT_VERSION, 3);
        values.put(RustedFabricAPIKeys.K_LOADER_VERSION, BuildConfig.VERSION_NAME);
        values.put(RustedFabricAPIKeys.K_GAME_VERSION, "1.15");
        values.put(RustedFabricAPIKeys.K_MAPPINGS_VERSION, "android-1.15-v1.0");
        values.put(RustedFabricAPIKeys.K_MAPPING_PROFILE_ID,
                "rw-android-1.15-code176-v1.0");
        values.put(RustedFabricAPIKeys.K_PLATFORM, "android");
        values.put(RustedFabricAPIKeys.K_ANDROID, Boolean.TRUE);
        values.put(RustedFabricAPIKeys.K_RUNTIME_NAMESPACE, "official");
        values.put(RustedFabricAPIKeys.K_ENTRYPOINT_KEY, "rustedfabricloader:runtime");
        values.put(RustedFabricAPIKeys.K_PACKAGE_NAME, application.getPackageName());
        values.put(RustedFabricAPIKeys.K_PROCESS_NAME, processName(application));
        values.put(RustedFabricAPIKeys.K_GAME_ARGS, new String[0]);
        values.put(RustedFabricAPIKeys.K_CAPABILITIES, Arrays.asList(
                "event.engine.init", "mapping.profile.exact", "mod.dex.v1",
                "platform.android.local-patch"));
        return new RustedFabricAPIContext(values);
    }

    private static String processName(Application application) {
        if (Build.VERSION.SDK_INT >= 28) {
            return Application.getProcessName();
        }
        return application.getPackageName();
    }
}
