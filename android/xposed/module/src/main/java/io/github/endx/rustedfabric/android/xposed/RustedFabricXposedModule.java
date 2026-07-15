package io.github.endx.rustedfabric.android.xposed;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.database.Cursor;
import android.os.Build;

import java.io.FileInputStream;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.ArrayList;
import java.util.List;

import io.github.endx.rustedfabric.android.bootstrap.AndroidMappingProfile;
import io.github.endx.rustedfabric.android.bootstrap.BootstrapDiagnostics;
import io.github.endx.rustedfabric.android.bootstrap.BootstrapPolicy;
import io.github.endx.rustedfabric.android.bootstrap.Sha256;
import io.github.endx.rustedfabric.android.xposed.mod.EnabledModClient;
import io.github.endx.rustedfabric.android.xposed.storage.ModContentProvider;
import io.github.endx.rustedfabricapi.api.RustedFabricAPIContext;
import io.github.endx.rustedfabricapi.api.RustedFabricAPIKeys;
import io.github.endx.rustedfabricapi.api.RustedFabricCapabilities;
import io.github.endx.rustedfabricapi.api.RustedFabricRuntime;
import io.github.endx.rustedfabricapi.api.event.CommandEvents;
import io.github.endx.rustedfabricapi.api.event.RuntimeLifecycleEvents;
import io.github.endx.rustedfabricapi.api.event.MultiplayerCompatibilityEvents;
import io.github.endx.rustedfabricapi.api.event.UnitLifecycleEvents;
import io.github.endx.rustedfabricapi.api.multiplayer.MultiplayerManifest;
import io.github.endx.rustedfabricapi.api.multiplayer.MultiplayerMod;
import io.github.endx.rustedfabricapi.api.session.GameSession;
import io.github.endx.rustedfabricapi.api.session.GameSessionRuntime;
import io.github.endx.rustedfabricapi.android.AndroidMultiplayerTransport;
import io.github.endx.rustedfabricapi.android.AndroidGameEventBridge;
import io.github.libxposed.api.XposedModule;

/** Modern Xposed entrypoint for exact profile selection and the first diagnostic-only game hook. */
public final class RustedFabricXposedModule extends XposedModule {
    private static final String TAG = "RustedFabric/Bootstrap";
    private static final int LOG_INFO = 4;
    private static final int LOG_ERROR = 6;
    private final AtomicBoolean gameEngineHookInstalled = new AtomicBoolean();
    private final AtomicBoolean gameEngineInitializationStarted = new AtomicBoolean();
    private final AtomicBoolean gameEngineInitialized = new AtomicBoolean();
    private final AtomicBoolean networkHooksInstalled = new AtomicBoolean();
    private final AtomicBoolean portableGameplayHooksInstalled = new AtomicBoolean();
    private final AtomicBoolean frameHooksInstalled = new AtomicBoolean();
    private final AtomicBoolean projectileHooksInstalled = new AtomicBoolean();
    private final AndroidMultiplayerTransport networkTransport = new AndroidMultiplayerTransport(
            (message, failure) -> log(failure == null ? LOG_INFO : LOG_ERROR,
                    "RustedFabric/Network", message, failure));
    private volatile String processName = "unknown";

    public RustedFabricXposedModule() {
        // The framework attaches XposedInterface after construction. Do not initialize here.
    }

    @Override
    public void onModuleLoaded(ModuleLoadedParam param) {
        processName = param.getProcessName();
        log(LOG_INFO, TAG, "module-loaded process=" + safe(processName)
                + " api=" + getApiVersion());
    }

    @Override
    @SuppressLint({"NewApi", "DiscouragedPrivateApi"})
    // getDefaultClassLoader is libxposed API 102; Application.attach is the intentional hook boundary.
    public void onPackageLoaded(PackageLoadedParam param) {
        String packageName = param.getPackageName();
        if (!BootstrapPolicy.shouldInstall(packageName, param.isFirstPackage())) {
            return;
        }

        try {
            ClassLoader defaultLoader = param.getDefaultClassLoader();
            Class<?> applicationClass = Class.forName("android.app.Application", false, defaultLoader);
            Class<?> contextClass = Class.forName("android.content.Context", false, defaultLoader);
            Method attach = applicationClass.getDeclaredMethod("attach", contextClass);

            hook(attach)
                    .setId("rusted-fabric:application-attach")
                    .setPriority(PRIORITY_LOWEST)
                    .intercept(chain -> {
                        Object result = chain.proceed();
                        try {
                            Context context = (Context) chain.getArg(0);
                            ClassLoader targetLoader = context.getClassLoader();
                            Object application = chain.getThisObject();
                            long verificationStarted = System.nanoTime();
                            AndroidMappingProfile.Selection selection = selectMappingProfile(
                                    context, packageName);
                            BootstrapDiagnostics.Snapshot snapshot = BootstrapDiagnostics.captureOnce(
                                    packageName, processName,
                                    application == null ? null : application.getClass().getName(),
                                    targetLoader == null ? null : targetLoader.getClass().getName(),
                                    selection.diagnosticStatus());
                            log(LOG_INFO, TAG, "application-attached package="
                                    + snapshot.getPackageName() + " process=" + snapshot.getProcessName()
                                    + " app=" + snapshot.getApplicationClassName()
                                    + " loader=" + snapshot.getClassLoaderClassName()
                                    + " mapping=" + snapshot.getMappingProfileStatus()
                                    + " verificationMs=" + elapsedMillis(verificationStarted));
                            if (selection.isVerified()) {
                                RustedFabricAPIContext apiContext = createApiContext(
                                        context, packageName);
                                RustedFabricRuntime.installContext(apiContext);
                                EnabledModClient.LoadSummary mods = new EnabledModClient().loadAll(
                                        context, targetLoader, apiContext,
                                        (modId, failure) -> log(LOG_ERROR, TAG,
                                                "mod-load-failed id=" + modId, failure));
                                if (mods.getDiscoveryFailure() != null) {
                                    log(LOG_ERROR, TAG, "mod-discovery-failed",
                                            mods.getDiscoveryFailure());
                                } else {
                                    log(LOG_INFO, TAG, "mods-loaded discovered="
                                            + mods.getDiscovered() + " loaded=" + mods.getLoaded()
                                            + " failed=" + mods.getFailed());
                                }
                                RuntimeLifecycleEvents.LOADER_READY.dispatch(apiContext);
                                apiContext.multiplayerManifest().ifPresent(
                                        MultiplayerCompatibilityEvents.LOCAL_MANIFEST_READY::dispatch);
                                if (installGameEngineInitHook(targetLoader, contextClass, apiContext)) {
                                    installNetworkHooks(targetLoader);
                                    installPortableGameplayHooks(targetLoader);
                                    installFrameHooks(targetLoader);
                                    installProjectileHooks(targetLoader);
                                    log(LOG_INFO, TAG, "api-context-ready platform="
                                            + apiContext.platform() + " capabilities="
                                            + apiContext.capabilities().size());
                                }
                            } else {
                                log(LOG_INFO, TAG, "game-hook-skipped mapping="
                                        + selection.diagnosticStatus());
                            }
                        } catch (Throwable diagnosticFailure) {
                            // Verification and diagnostics are optional and must never change startup.
                            log(LOG_ERROR, TAG, "profile verification failed; game hooks disabled",
                                    diagnosticFailure);
                        }
                        return result;
                    });
            log(LOG_INFO, TAG, "hook-installed target=android.app.Application#attach");
        } catch (Throwable hookFailure) {
            // Failure to install the optional bootstrap hook must leave the game untouched.
            log(LOG_ERROR, TAG, "hook installation failed", hookFailure);
        }
    }

    @SuppressWarnings("deprecation")
    private AndroidMappingProfile.Selection selectMappingProfile(Context context, String packageName)
            throws Exception {
        PackageInfo packageInfo = context.getPackageManager().getPackageInfo(packageName, 0);
        long versionCode = Build.VERSION.SDK_INT >= 28
                ? packageInfo.getLongVersionCode() : packageInfo.versionCode;
        String sourceDir = context.getApplicationInfo().sourceDir;
        if (sourceDir == null || sourceDir.isEmpty()) {
            return AndroidMappingProfile.select(packageName, packageInfo.versionName, versionCode, null);
        }
        String apkSha256;
        try (FileInputStream input = new FileInputStream(sourceDir)) {
            apkSha256 = Sha256.digest(input);
        }
        return AndroidMappingProfile.select(packageName, packageInfo.versionName, versionCode, apkSha256);
    }

    private RustedFabricAPIContext createApiContext(Context context, String packageName) {
        Map<String, Object> values = new HashMap<>();
        values.put(RustedFabricAPIKeys.K_CONTEXT_VERSION, 5);
        values.put(RustedFabricAPIKeys.K_LOADER_VERSION, BuildConfig.VERSION_NAME);
        values.put(RustedFabricAPIKeys.K_GAME_VERSION, AndroidMappingProfile.VERSION_NAME);
        values.put(RustedFabricAPIKeys.K_MAPPINGS_VERSION, "android-1.15-v1.0");
        values.put(RustedFabricAPIKeys.K_MAPPING_PROFILE_ID, AndroidMappingProfile.ID);
        values.put(RustedFabricAPIKeys.K_PLATFORM, "android");
        values.put(RustedFabricAPIKeys.K_ANDROID, Boolean.TRUE);
        values.put(RustedFabricAPIKeys.K_RUNTIME_NAMESPACE, "official");
        values.put(RustedFabricAPIKeys.K_ENTRYPOINT_KEY, "rustedfabricloader:runtime");
        values.put(RustedFabricAPIKeys.K_PACKAGE_NAME, packageName);
        values.put(RustedFabricAPIKeys.K_PROCESS_NAME, processName);
        values.put(RustedFabricAPIKeys.K_GAME_ARGS, new String[0]);
        values.put(RustedFabricAPIKeys.K_MULTIPLAYER_MANIFEST,
                readMultiplayerManifest(context).encode());
        values.put(RustedFabricAPIKeys.K_CAPABILITIES, Arrays.asList(
                "event.engine.init", "event.runtime.ready", "mapping.profile.exact",
                "mod.dex.v1", "session.v1", "multiplayer.compat.v1",
                "event.runtime.lifecycle.v1", "event.unit.lifecycle.v1",
                "event.command.issue.v1", RustedFabricCapabilities.GAME_LIFECYCLE,
                RustedFabricCapabilities.PROJECTILE_LIFECYCLE,
                "multiplayer.handshake.rfh1",
                "platform.android.xposed"));
        return new RustedFabricAPIContext(values);
    }

    private MultiplayerManifest readMultiplayerManifest(Context context) {
        String[] columns = {ModContentProvider.COLUMN_ID, ModContentProvider.COLUMN_VERSION,
                ModContentProvider.COLUMN_MULTIPLAYER_MODE,
                ModContentProvider.COLUMN_MULTIPLAYER_PROTOCOL,
                ModContentProvider.COLUMN_MULTIPLAYER_SYNC_HASH};
        List<MultiplayerMod> mods = new ArrayList<>();
        try (Cursor cursor = context.getContentResolver().query(
                ModContentProvider.ENABLED_MODS_URI, columns, null, null, null)) {
            if (cursor == null) return MultiplayerManifest.empty("android");
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
        } catch (RuntimeException unavailable) {
            log(LOG_ERROR, TAG, "multiplayer manifest unavailable", unavailable);
            return MultiplayerManifest.empty("android");
        }
        return new MultiplayerManifest("android", mods);
    }

    private boolean installGameEngineInitHook(ClassLoader targetLoader, Class<?> contextClass,
                                              RustedFabricAPIContext apiContext) {
        if (!gameEngineHookInstalled.compareAndSet(false, true)) {
            return true;
        }
        try {
            Class<?> gameEngineClass = Class.forName(
                    AndroidMappingProfile.gameEngineInitBinaryClassName(), false, targetLoader);
            Method init = gameEngineClass.getDeclaredMethod(
                    AndroidMappingProfile.GAME_ENGINE_INIT_NAME, contextClass);
            hook(init)
                    .setId("rusted-fabric:game-engine-init")
                    .setPriority(PRIORITY_LOWEST)
                    .intercept(chain -> {
                        if (!gameEngineInitializationStarted.compareAndSet(false, true)) {
                            return chain.proceed();
                        }
                        logDispatch("before-engine-initialization",
                                RuntimeLifecycleEvents.BEFORE_ENGINE_INITIALIZATION.dispatch(apiContext));
                        Object result = chain.proceed();
                        if (gameEngineInitialized.compareAndSet(false, true)) {
                            logDispatch("after-engine-initialization",
                                    RuntimeLifecycleEvents.AFTER_ENGINE_INITIALIZATION.dispatch(apiContext));
                            logDispatch("game-ready",
                                    RuntimeLifecycleEvents.GAME_READY.dispatch(apiContext));
                            GameSessionRuntime.transition(GameSession.Kind.SINGLE_PLAYER);
                            log(LOG_INFO, TAG, "game-engine-initialized profile="
                                    + AndroidMappingProfile.ID + " event=after-"
                                    + AndroidMappingProfile.GAME_ENGINE_INIT_NAMED);
                        }
                        return result;
                    });
            log(LOG_INFO, TAG, "hook-installed target="
                    + AndroidMappingProfile.GAME_ENGINE_INIT_OWNER + '#'
                    + AndroidMappingProfile.GAME_ENGINE_INIT_NAME
                    + AndroidMappingProfile.GAME_ENGINE_INIT_DESCRIPTOR);
            return true;
        } catch (Throwable hookFailure) {
            gameEngineHookInstalled.set(false);
            log(LOG_ERROR, TAG, "GameEngine init hook installation failed", hookFailure);
            return false;
        }
    }

    private boolean installNetworkHooks(ClassLoader targetLoader) {
        if (!networkHooksInstalled.compareAndSet(false, true)) return true;
        try {
            Class<?> engine = Class.forName(
                    AndroidMappingProfile.binaryName(AndroidMappingProfile.NETWORK_ENGINE_OWNER),
                    false, targetLoader);
            Class<?> connection = Class.forName(
                    AndroidMappingProfile.binaryName(AndroidMappingProfile.NETWORK_CONNECTION_OWNER),
                    false, targetLoader);
            Class<?> packet = Class.forName(
                    AndroidMappingProfile.binaryName(AndroidMappingProfile.NETWORK_PACKET_OWNER),
                    false, targetLoader);
            Method register = engine.getDeclaredMethod(
                    AndroidMappingProfile.NETWORK_REGISTER_NAME, connection);
            Method serverInfo = engine.getDeclaredMethod(
                    AndroidMappingProfile.NETWORK_SERVER_INFO_NAME, connection);
            Method systemPacket = engine.getDeclaredMethod(
                    AndroidMappingProfile.NETWORK_DISPATCH_NAME, packet);
            Method reset = engine.getDeclaredMethod(
                    AndroidMappingProfile.NETWORK_RESET_NAME, boolean.class);
            Method start = engine.getDeclaredMethod(
                    AndroidMappingProfile.NETWORK_START_NAME, connection, boolean.class);
            register.setAccessible(true);
            serverInfo.setAccessible(true);
            systemPacket.setAccessible(true);
            reset.setAccessible(true);
            start.setAccessible(true);

            hook(register).setId("rusted-fabric:rfh1-client-register")
                    .setPriority(PRIORITY_LOWEST).intercept(chain -> {
                        Object result = chain.proceed();
                        networkTransport.afterClientRegistration(
                                chain.getThisObject(), chain.getArg(0));
                        return result;
                    });
            hook(serverInfo).setId("rusted-fabric:rfh1-server-info")
                    .setPriority(PRIORITY_LOWEST).intercept(chain -> {
                        Object result = chain.proceed();
                        networkTransport.afterServerInfo(chain.getThisObject(), chain.getArg(0));
                        return result;
                    });
            hook(systemPacket).setId("rusted-fabric:rfh1-system-packet")
                    .setPriority(PRIORITY_HIGHEST).intercept(chain -> {
                        if (networkTransport.receive(chain.getThisObject(), chain.getArg(0))) return null;
                        return chain.proceed();
                    });
            hook(reset).setId("rusted-fabric:rfh1-network-reset")
                    .setPriority(PRIORITY_LOWEST).intercept(chain -> {
                        Object result = chain.proceed();
                        if (!Boolean.TRUE.equals(chain.getArg(0))) {
                            networkTransport.resetToSinglePlayer();
                        }
                        return result;
                    });
            hook(start).setId("rusted-fabric:rfh1-start-gate")
                    .setPriority(PRIORITY_HIGHEST).intercept(chain -> {
                        if (!networkTransport.allowGameStart(chain.getArg(0))) return Boolean.FALSE;
                        return chain.proceed();
                    });
            log(LOG_INFO, "RustedFabric/Network", "RFH1 network hooks installed");
            return true;
        } catch (Throwable failure) {
            networkHooksInstalled.set(false);
            log(LOG_ERROR, "RustedFabric/Network", "RFH1 hook installation failed", failure);
            return false;
        }
    }

    private boolean installPortableGameplayHooks(ClassLoader targetLoader) {
        if (!portableGameplayHooksInstalled.compareAndSet(false, true)) return true;
        try {
            Class<?> team = Class.forName(
                    AndroidMappingProfile.binaryName(AndroidMappingProfile.TEAM_OWNER),
                    false, targetLoader);
            Class<?> unit = Class.forName(
                    AndroidMappingProfile.binaryName(AndroidMappingProfile.UNIT_OWNER),
                    false, targetLoader);
            Class<?> command = Class.forName(
                    AndroidMappingProfile.binaryName(AndroidMappingProfile.COMMAND_OWNER),
                    false, targetLoader);
            Method register = team.getDeclaredMethod(
                    AndroidMappingProfile.TEAM_REGISTER_NAME, unit);
            Method unregister = team.getDeclaredMethod(
                    AndroidMappingProfile.TEAM_UNREGISTER_NAME, unit);
            Method issue = command.getDeclaredMethod(AndroidMappingProfile.COMMAND_ISSUE_NAME);
            register.setAccessible(true);
            unregister.setAccessible(true);
            issue.setAccessible(true);

            hook(register).setId("rusted-fabric:unit-register")
                    .setPriority(PRIORITY_LOWEST).intercept(chain -> {
                        Object value = chain.getArg(0);
                        UnitLifecycleEvents.BEFORE_UNIT_REGISTER.invoker()
                                .beforeUnitRegister(value);
                        Object result = chain.proceed();
                        UnitLifecycleEvents.AFTER_UNIT_REGISTER.invoker()
                                .afterUnitRegister(value);
                        return result;
                    });
            hook(unregister).setId("rusted-fabric:unit-unregister")
                    .setPriority(PRIORITY_LOWEST).intercept(chain -> {
                        Object value = chain.getArg(0);
                        UnitLifecycleEvents.BEFORE_UNIT_UNREGISTER.invoker()
                                .beforeUnitUnregister(value);
                        Object result = chain.proceed();
                        UnitLifecycleEvents.AFTER_UNIT_UNREGISTER.invoker()
                                .afterUnitUnregister(value);
                        return result;
                    });
            hook(issue).setId("rusted-fabric:command-issue")
                    .setPriority(PRIORITY_HIGHEST).intercept(chain -> {
                        Object value = chain.getThisObject();
                        if (CommandEvents.BEFORE_COMMAND_ISSUE.invoker()
                                .beforeCommandIssue(value)) return null;
                        Object result = chain.proceed();
                        CommandEvents.AFTER_COMMAND_ISSUE.invoker().afterCommandIssue(value);
                        return result;
                    });
            log(LOG_INFO, TAG, "portable gameplay hooks installed: unit lifecycle, command issue");
            return true;
        } catch (Throwable failure) {
            portableGameplayHooksInstalled.set(false);
            log(LOG_ERROR, TAG, "portable gameplay hook installation failed", failure);
            return false;
        }
    }

    private boolean installFrameHooks(ClassLoader targetLoader) {
        if (!frameHooksInstalled.compareAndSet(false, true)) return true;
        try {
            Class<?> engine = Class.forName(
                    AndroidMappingProfile.binaryName(AndroidMappingProfile.FRAME_LOOP_OWNER),
                    false, targetLoader);
            Class<?> canvas = Class.forName(
                    AndroidMappingProfile.binaryName(AndroidMappingProfile.FRAME_CANVAS_OWNER),
                    false, targetLoader);
            Method loop = engine.getDeclaredMethod(AndroidMappingProfile.FRAME_LOOP_NAME,
                    float.class, int.class);
            Method render = engine.getDeclaredMethod(AndroidMappingProfile.FRAME_LOOP_NAME,
                    canvas, float.class);
            loop.setAccessible(true);
            render.setAccessible(true);
            hook(loop).setId("rusted-fabric:frame-loop")
                    .setPriority(PRIORITY_HIGHEST).intercept(chain -> {
                        Object value = chain.getThisObject();
                        AndroidGameEventBridge.beforeFrameUpdate(value,
                                ((Number) chain.getArg(1)).intValue());
                        Object result = chain.proceed();
                        AndroidGameEventBridge.afterFrameUpdate(value);
                        return result;
                    });
            hook(render).setId("rusted-fabric:frame-render")
                    .setPriority(PRIORITY_HIGHEST).intercept(chain -> {
                        Object value = chain.getThisObject();
                        AndroidGameEventBridge.beforeFrameRender(value, chain.getArg(0));
                        Object result = chain.proceed();
                        AndroidGameEventBridge.afterFrameRender(value);
                        return result;
                    });
            log(LOG_INFO, TAG, "Android frame update/render hooks installed");
            return true;
        } catch (Throwable failure) {
            frameHooksInstalled.set(false);
            log(LOG_ERROR, TAG, "Android frame hook installation failed", failure);
            return false;
        }
    }

    private boolean installProjectileHooks(ClassLoader targetLoader) {
        if (!projectileHooksInstalled.compareAndSet(false, true)) return true;
        try {
            Class<?> projectile = Class.forName(
                    AndroidMappingProfile.binaryName(AndroidMappingProfile.PROJECTILE_OWNER),
                    false, targetLoader);
            Class<?> unit = Class.forName(
                    AndroidMappingProfile.binaryName(AndroidMappingProfile.UNIT_OWNER),
                    false, targetLoader);
            Method create = projectile.getDeclaredMethod(
                    AndroidMappingProfile.PROJECTILE_METHOD_NAME,
                    unit, float.class, float.class);
            Method createExtended = projectile.getDeclaredMethod(
                    AndroidMappingProfile.PROJECTILE_METHOD_NAME,
                    unit, float.class, float.class, float.class, int.class);
            Method update = projectile.getDeclaredMethod(
                    AndroidMappingProfile.PROJECTILE_METHOD_NAME, float.class);
            Method remove = projectile.getDeclaredMethod(
                    AndroidMappingProfile.PROJECTILE_METHOD_NAME);
            create.setAccessible(true);
            createExtended.setAccessible(true);
            update.setAccessible(true);
            remove.setAccessible(true);
            hook(create).setId("rusted-fabric:projectile-create")
                    .setPriority(PRIORITY_LOWEST).intercept(chain -> {
                        Object result = chain.proceed();
                        AndroidGameEventBridge.afterProjectileCreated(result, chain.getArg(0));
                        return result;
                    });
            hook(createExtended).setId("rusted-fabric:projectile-create-extended")
                    .setPriority(PRIORITY_LOWEST).intercept(chain -> {
                        Object result = chain.proceed();
                        AndroidGameEventBridge.afterProjectileCreated(result, chain.getArg(0));
                        return result;
                    });
            hook(update).setId("rusted-fabric:projectile-update")
                    .setPriority(PRIORITY_HIGHEST).intercept(chain -> {
                        Object value = chain.getThisObject();
                        float delta = ((Number) chain.getArg(0)).floatValue();
                        AndroidGameEventBridge.beforeProjectileUpdate(value, delta);
                        Object result = chain.proceed();
                        AndroidGameEventBridge.afterProjectileUpdate(value, delta);
                        return result;
                    });
            hook(remove).setId("rusted-fabric:projectile-remove")
                    .setPriority(PRIORITY_HIGHEST).intercept(chain -> {
                        Object value = chain.getThisObject();
                        AndroidGameEventBridge.beforeProjectileRemoval(value);
                        Object result = chain.proceed();
                        AndroidGameEventBridge.afterProjectileRemoval(value);
                        return result;
                    });
            log(LOG_INFO, TAG, "Android projectile hooks installed (explosion boundary requires local patch)");
            return true;
        } catch (Throwable failure) {
            projectileHooksInstalled.set(false);
            log(LOG_ERROR, TAG, "Android projectile hook installation failed", failure);
            return false;
        }
    }

    private void logDispatch(String event, RuntimeLifecycleEvents.DispatchResult result) {
        log(result.succeeded() ? LOG_INFO : LOG_ERROR, TAG, event
                + " listeners=" + result.listenerCount()
                + " failures=" + result.failureCount());
    }

    private static long elapsedMillis(long startedNanos) {
        return (System.nanoTime() - startedNanos) / 1_000_000L;
    }

    private static String safe(String value) {
        return value == null ? "unknown" : value;
    }
}
