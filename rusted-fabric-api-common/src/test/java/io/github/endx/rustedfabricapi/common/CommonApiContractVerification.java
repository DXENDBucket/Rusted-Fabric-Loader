package io.github.endx.rustedfabricapi.common;

import io.github.endx.rustedfabricapi.api.RustedFabricAPIContext;
import io.github.endx.rustedfabricapi.api.RustedFabricAPIEntrypoint;
import io.github.endx.rustedfabricapi.api.RustedFabricAPIKeys;
import io.github.endx.rustedfabricapi.api.RustedFabricModEntrypoint;
import io.github.endx.rustedfabricapi.api.RustedFabricPlatform;
import io.github.endx.rustedfabricapi.api.RustedFabricRuntime;
import io.github.endx.rustedfabricapi.api.event.RuntimeLifecycleEvents;
import io.github.endx.rustedfabricapi.api.event.MultiplayerCompatibilityEvents;
import io.github.endx.rustedfabricapi.api.multiplayer.MultiplayerCompatibility;
import io.github.endx.rustedfabricapi.api.multiplayer.MultiplayerManifest;
import io.github.endx.rustedfabricapi.api.multiplayer.MultiplayerMod;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class CommonApiContractVerification {
    private CommonApiContractVerification() {
    }

    public static void main(String[] args) {
        RustedFabricAPIContext context = androidContext();
        verifyContext(context);
        verifySafeEvents(context);
        verifyPortableModEntrypoint(context);
        verifyMultiplayerCompatibility();
        verifyEntrypointInstallsContext();
        System.out.println("Cross-platform Rusted Fabric API contracts passed");
    }

    private static void verifyMultiplayerCompatibility() {
        String hash = repeat('a', 64);
        MultiplayerManifest windows = new MultiplayerManifest("windows", Arrays.asList(
                MultiplayerMod.required("shared_units", "1.2.0", "units-v1", hash),
                MultiplayerMod.clientOnly("desktop_hud", "2.0.0")));
        MultiplayerManifest android = new MultiplayerManifest("android", Arrays.asList(
                MultiplayerMod.required("shared_units", "1.2.0", "units-v1", hash),
                MultiplayerMod.clientOnly("touch_controls", "3.0.0")));
        MultiplayerManifest decoded = MultiplayerManifest.decode(windows.encode());
        require(decoded.encode().equals(windows.encode()), "manifest encoding is not canonical");
        require(decoded.fingerprint().equals(windows.fingerprint()),
                "manifest fingerprint changed after decoding");

        final int[] evaluations = {0};
        MultiplayerCompatibilityEvents.Registration registration =
                MultiplayerCompatibilityEvents.COMPATIBILITY_EVALUATED.register(report -> {
                    evaluations[0]++;
                    throw new IllegalStateException("synthetic multiplayer listener failure");
                });
        MultiplayerCompatibility.Report compatible =
                MultiplayerCompatibility.evaluate(windows, android);
        require(compatible.compatible(), "platform-specific client mods must be ignored");
        require(evaluations[0] == 1, "compatibility event was not delivered");
        registration.close();

        MultiplayerManifest mismatch = new MultiplayerManifest("android", Arrays.asList(
                MultiplayerMod.required("shared_units", "1.2.0", "units-v1", repeat('b', 64))));
        MultiplayerCompatibility.Report rejected =
                MultiplayerCompatibility.evaluate(windows, mismatch);
        require(!rejected.compatible()
                        && rejected.issues().stream().anyMatch(issue ->
                        issue.problem() == MultiplayerCompatibility.Problem.SYNC_HASH_MISMATCH),
                "synchronized content mismatch was accepted");
        require(!MultiplayerCompatibility.evaluateVanillaPeer(windows).compatible(),
                "vanilla peer was accepted with a required mod");
        MultiplayerManifest clientOnly = new MultiplayerManifest("android",
                Arrays.asList(MultiplayerMod.clientOnly("touch_controls", "1.0")));
        require(MultiplayerCompatibility.evaluateVanillaPeer(clientOnly).compatible(),
                "client-only mod should remain compatible with vanilla peers");
    }

    private static void verifyPortableModEntrypoint(RustedFabricAPIContext context) {
        final RustedFabricAPIContext[] received = new RustedFabricAPIContext[1];
        RustedFabricModEntrypoint entrypoint = value -> received[0] = value;
        entrypoint.onInitialize(context);
        require(received[0] == context, "portable mod entrypoint did not receive context");
    }

    private static RustedFabricAPIContext androidContext() {
        Map<String, Object> raw = new HashMap<>();
        raw.put(RustedFabricAPIKeys.K_CONTEXT_VERSION, 3);
        raw.put(RustedFabricAPIKeys.K_LOADER_VERSION, "0.3.0");
        raw.put(RustedFabricAPIKeys.K_GAME_VERSION, "1.15");
        raw.put(RustedFabricAPIKeys.K_MAPPINGS_VERSION, "android-1.15-v1.0");
        raw.put(RustedFabricAPIKeys.K_MAPPING_PROFILE_ID, "rw-android-1.15-code176-v1.0");
        raw.put(RustedFabricAPIKeys.K_PLATFORM, "android");
        raw.put(RustedFabricAPIKeys.K_ANDROID, Boolean.TRUE);
        raw.put(RustedFabricAPIKeys.K_RUNTIME_NAMESPACE, "official");
        raw.put(RustedFabricAPIKeys.K_CAPABILITIES,
                new ArrayList<>(Arrays.asList("mapping.profile.exact", "event.engine.init")));
        raw.put(RustedFabricAPIKeys.K_MULTIPLAYER_MANIFEST,
                MultiplayerManifest.empty("android").encode());
        return new RustedFabricAPIContext(raw);
    }

    private static void verifyContext(RustedFabricAPIContext context) {
        require(context.contextVersion() == 3, "context version missing");
        require(context.platform() == RustedFabricPlatform.ANDROID, "Android platform missing");
        require(context.androidRuntime(), "legacy Android accessor must remain compatible");
        require(context.hasCapability("event.engine.init"), "capability missing");
        require("rw-android-1.15-code176-v1.0".equals(context.mappingProfileId()),
                "mapping profile missing");
        require(context.multiplayerManifest().isPresent()
                        && "android".equals(context.multiplayerManifest().get().platform()),
                "multiplayer manifest missing");
        boolean immutable = false;
        try {
            context.capabilities().add("unexpected");
        } catch (UnsupportedOperationException expected) {
            immutable = true;
        }
        require(immutable, "capabilities must be immutable");
    }

    private static void verifySafeEvents(RustedFabricAPIContext context) {
        List<String> calls = new ArrayList<>();
        RuntimeLifecycleEvents.Registration first =
                RuntimeLifecycleEvents.BEFORE_ENGINE_INITIALIZATION.register(value -> {
                    calls.add("first:" + value.platform());
                    throw new IllegalStateException("synthetic listener failure");
                });
        RuntimeLifecycleEvents.Registration second =
                RuntimeLifecycleEvents.BEFORE_ENGINE_INITIALIZATION.register(
                        value -> calls.add("second:" + value.mappingProfileId()));
        RuntimeLifecycleEvents.DispatchResult result =
                RuntimeLifecycleEvents.BEFORE_ENGINE_INITIALIZATION.dispatch(context);
        require(result.listenerCount() == 2, "both listeners must run");
        require(result.failureCount() == 1, "listener failure must be counted");
        require(calls.size() == 2 && calls.get(0).startsWith("first:")
                        && calls.get(1).startsWith("second:"),
                "listener order or isolation failed");
        require(first.unregister(), "first registration should unregister once");
        require(!first.unregister(), "registration should be idempotent");
        second.close();
        require(RuntimeLifecycleEvents.BEFORE_ENGINE_INITIALIZATION.listenerCount() == 0,
                "listeners leaked after unregister");
    }

    private static void verifyEntrypointInstallsContext() {
        Map<String, Object> raw = new HashMap<>();
        raw.put(RustedFabricAPIKeys.K_CONTEXT_VERSION, 3);
        raw.put(RustedFabricAPIKeys.K_PLATFORM, "windows");
        final RustedFabricAPIContext[] received = new RustedFabricAPIContext[1];
        RustedFabricAPIEntrypoint entrypoint = new RustedFabricAPIEntrypoint() {
            @Override
            protected void onRustedFabricAPI(RustedFabricAPIContext context) {
                received[0] = context;
            }
        };
        entrypoint.accept(raw);
        require(received[0] != null && received[0].platform() == RustedFabricPlatform.WINDOWS,
                "entrypoint did not receive Windows context");
        require(RustedFabricRuntime.currentContext().orElse(null) == received[0],
                "entrypoint did not install the process context");
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private static String repeat(char value, int count) {
        char[] result = new char[count];
        Arrays.fill(result, value);
        return new String(result);
    }
}
