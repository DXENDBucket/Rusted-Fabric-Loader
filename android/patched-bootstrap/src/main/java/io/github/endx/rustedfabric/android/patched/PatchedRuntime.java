package io.github.endx.rustedfabric.android.patched;

import android.app.Application;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.ArrayList;
import java.util.List;

import io.github.endx.rustedfabricapi.api.RustedFabricAPIContext;
import io.github.endx.rustedfabricapi.api.RustedFabricAPIKeys;
import io.github.endx.rustedfabricapi.api.RustedFabricCapabilities;
import io.github.endx.rustedfabricapi.api.RustedFabricRuntime;
import io.github.endx.rustedfabricapi.api.event.MultiplayerCompatibilityEvents;
import io.github.endx.rustedfabricapi.api.event.RuntimeLifecycleEvents;
import io.github.endx.rustedfabricapi.api.multiplayer.MultiplayerManifest;
import io.github.endx.rustedfabricapi.api.multiplayer.MultiplayerMod;

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
        RuntimeLifecycleEvents.LOADER_READY.dispatch(context);
        context.multiplayerManifest().ifPresent(
                MultiplayerCompatibilityEvents.LOCAL_MANIFEST_READY::dispatch);
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
        values.put(RustedFabricAPIKeys.K_CONTEXT_VERSION, 5);
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
        values.put(RustedFabricAPIKeys.K_MULTIPLAYER_MANIFEST,
                readMultiplayerManifest(application).encode());
        values.put(RustedFabricAPIKeys.K_CAPABILITIES, Arrays.asList(
                "event.engine.init", "event.runtime.ready", "mapping.profile.exact",
                "mod.dex.v1", "session.v1", "multiplayer.compat.v1",
                "event.runtime.lifecycle.v1", "event.unit.lifecycle.v1",
                "event.command.issue.v1", RustedFabricCapabilities.GAME_LIFECYCLE,
                RustedFabricCapabilities.PROJECTILE_LIFECYCLE,
                RustedFabricCapabilities.UNIT_DAMAGE,
                "multiplayer.handshake.rfh1",
                "platform.android.local-patch"));
        return new RustedFabricAPIContext(values);
    }

    private static MultiplayerManifest readMultiplayerManifest(Application application) {
        String[] authorities = {
                "io.github.endx.rustedfabric.android.xposed.mods",
                "io.github.endx.rustedfabric.android.xposed.debug.mods"
        };
        String[] columns = {"id", "version", "multiplayer_mode",
                "multiplayer_protocol", "multiplayer_sync_hash"};
        for (String authority : authorities) {
            List<MultiplayerMod> mods = new ArrayList<>();
            try (Cursor cursor = application.getContentResolver().query(
                    Uri.parse("content://" + authority + "/enabled"),
                    columns, null, null, null)) {
                if (cursor == null) continue;
                int id = cursor.getColumnIndexOrThrow(columns[0]);
                int version = cursor.getColumnIndexOrThrow(columns[1]);
                int mode = cursor.getColumnIndexOrThrow(columns[2]);
                int protocol = cursor.getColumnIndexOrThrow(columns[3]);
                int hash = cursor.getColumnIndexOrThrow(columns[4]);
                while (cursor.moveToNext()) {
                    mods.add(new MultiplayerMod(cursor.getString(id), cursor.getString(version),
                            MultiplayerMod.Mode.parse(cursor.getString(mode)),
                            cursor.getString(protocol), cursor.getString(hash)));
                }
                return new MultiplayerManifest("android", mods);
            } catch (RuntimeException unavailable) {
                Log.w(TAG, "Could not read multiplayer metadata from " + authority, unavailable);
            }
        }
        return MultiplayerManifest.empty("android");
    }

    private static String processName(Application application) {
        if (Build.VERSION.SDK_INT >= 28) {
            return Application.getProcessName();
        }
        return application.getPackageName();
    }
}
