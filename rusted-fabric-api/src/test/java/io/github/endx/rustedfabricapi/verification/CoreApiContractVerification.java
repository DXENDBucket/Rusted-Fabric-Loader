package io.github.endx.rustedfabricapi.verification;

import io.github.endx.rustedfabricapi.api.RustedFabricAPIContext;
import io.github.endx.rustedfabricapi.api.RustedFabricAPIEntrypoint;
import io.github.endx.rustedfabricapi.api.RustedFabricAPIKeys;
import io.github.endx.rustedfabricapi.api.RustedFabricPlatform;
import io.github.endx.rustedfabricapi.api.RustedFabricRuntime;
import io.github.endx.rustedfabricapi.api.RustedFabricCapabilities;
import io.github.endx.rustedfabricapi.api.ApiSupportMatrix;
import io.github.endx.rustedfabricapi.api.event.RuntimeLifecycleEvents;
import io.github.endx.rustedfabricapi.api.event.MultiplayerCompatibilityEvents;
import io.github.endx.rustedfabricapi.api.multiplayer.MultiplayerCompatibility;
import io.github.endx.rustedfabricapi.api.multiplayer.MultiplayerManifest;
import io.github.endx.rustedfabricapi.api.multiplayer.MultiplayerMod;
import io.github.endx.rustedfabricapi.api.multiplayer.MultiplayerHandshake;
import io.github.endx.rustedfabricapi.api.multiplayer.MultiplayerPeerCompatibility;
import io.github.endx.rustedfabricapi.api.multiplayer.MultiplayerNetworkBridge;
import io.github.endx.rustedfabricapi.api.multiplayer.MultiplayerRequirements;
import io.github.endx.rustedfabricapi.api.event.GameSessionEvents;
import io.github.endx.rustedfabricapi.api.session.GameSession;
import io.github.endx.rustedfabricapi.api.session.GameSessionRuntime;
import io.github.endx.rustedfabricapi.api.unit.event.UnitDamageEvents;
import io.github.endx.rustedfabricapi.api.unit.event.UnitDamageResult;
import io.github.endx.rustedfabricapi.api.custom.event.DamageEventData;
import io.github.endx.rustedfabricapi.api.custom.event.NativeEventData;
import io.github.endx.rustedfabricapi.api.custom.PerActionAutoTriggerCooldowns;
import io.github.endx.rustedfabricapi.impl.combat.NativeDamageMath;
import io.github.endx.rustedfabricapi.impl.custom.DamageEventDataRuntime;
import io.github.endx.rustedfabricapi.impl.custom.NativeEventDataRuntime;
import rustedwarfare.unit.Unit;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class CoreApiContractVerification {
    private CoreApiContractVerification() {
    }

    public static void main(String[] args) throws Exception {
        RustedFabricAPIContext context = androidContext();
        verifyContext(context);
        verifySupportMatrix(context);
        verifySafeEvents(context);
        verifySharedModEntrypoint(context);
        verifyMultiplayerCompatibility();
        verifyEntrypointInstallsContext();
        verifySharedSessions();
        verifyNetworkBridge();
        verifyLethalHealthModifier();
        verifyDamageEventContextContracts();
        verifyPerActionAutoTriggerCooldownContract();
        ProjectileCombatEventContractVerification.verify();
        ProjectilePatternContractVerification.verify();
        IniExtensionContractVerification.verify();
        IniActionEffectContractVerification.verify();
        LogicUnitPropertyContractVerification.verify();
        System.out.println("Rusted Fabric API core contracts passed");
    }

    private static void verifySupportMatrix(RustedFabricAPIContext context) {
        require(ApiSupportMatrix.entries().size() == 64,
                "public API support matrix does not cover every advertised API group");
        require(ApiSupportMatrix.expectedSupport(
                        RustedFabricCapabilities.BUILD_PRODUCTION_MODIFIER,
                        ApiSupportMatrix.Backend.RUNTIME) == ApiSupportMatrix.Level.FULL,
                "dynamic build-production modifiers are not advertised");
        require(ApiSupportMatrix.expectedSupport(
                        RustedFabricCapabilities.CUSTOM_UNIT_ECONOMY,
                        ApiSupportMatrix.Backend.RUNTIME) == ApiSupportMatrix.Level.FULL,
                "custom-unit periodic economy support is not advertised");
        require(ApiSupportMatrix.expectedSupport(
                        RustedFabricCapabilities.UNIT_MOVEMENT_OVERRIDE,
                        ApiSupportMatrix.Backend.RUNTIME) == ApiSupportMatrix.Level.FULL,
                "per-unit movement override support is not advertised");
        require(ApiSupportMatrix.expectedSupport(
                        RustedFabricCapabilities.CUSTOM_AUTO_TRIGGER_COOLDOWN,
                        ApiSupportMatrix.Backend.RUNTIME) == ApiSupportMatrix.Level.FULL,
                "per-action auto-trigger cooldown support is not advertised");
        require(ApiSupportMatrix.expectedSupport(RustedFabricCapabilities.CLIENT_RENDER_ALPHA_MASK,
                        ApiSupportMatrix.Backend.RUNTIME) == ApiSupportMatrix.Level.FULL,
                "client alpha-mask support is not advertised");
        require(ApiSupportMatrix.expectedSupport(RustedFabricCapabilities.CLIENT_RENDER_BAR,
                        ApiSupportMatrix.Backend.RUNTIME) == ApiSupportMatrix.Level.FULL,
                "client bar-image support is not advertised");
        require(ApiSupportMatrix.expectedSupport(RustedFabricCapabilities.CLIENT_DECAL_LAYER,
                        ApiSupportMatrix.Backend.RUNTIME) == ApiSupportMatrix.Level.FULL,
                "client Decal-layer support is not advertised");
        require(ApiSupportMatrix.expectedSupport(RustedFabricCapabilities.LOGIC_UNIT_PROPERTIES,
                        ApiSupportMatrix.Backend.RUNTIME) == ApiSupportMatrix.Level.FULL,
                "LogicBoolean unit-property support is not advertised");
        require(ApiSupportMatrix.expectedSupport(RustedFabricCapabilities.PROJECTILE_SPAWN,
                        ApiSupportMatrix.Backend.RUNTIME) == ApiSupportMatrix.Level.FULL,
                "projectile spawn support is not advertised");
        require(ApiSupportMatrix.expectedSupport(RustedFabricCapabilities.PROJECTILE_COLLISION,
                        ApiSupportMatrix.Backend.RUNTIME) == ApiSupportMatrix.Level.FULL,
                "projectile collision support is not advertised");
        require(ApiSupportMatrix.expectedSupport(RustedFabricCapabilities.PROJECTILE_ASSETS,
                        ApiSupportMatrix.Backend.RUNTIME) == ApiSupportMatrix.Level.FULL,
                "projectile asset support is not advertised");
        require(ApiSupportMatrix.expectedSupport(RustedFabricCapabilities.CLIENT_PROJECTILE_RENDER,
                        ApiSupportMatrix.Backend.RUNTIME) == ApiSupportMatrix.Level.FULL,
                "client projectile-render support is not advertised");
        require(ApiSupportMatrix.expectedSupport(
                        RustedFabricCapabilities.TURRET_PROJECTILE_PATTERN,
                        ApiSupportMatrix.Backend.RUNTIME) == ApiSupportMatrix.Level.FULL,
                "turret projectile pattern support is not advertised");
        require(ApiSupportMatrix.expectedSupport(RustedFabricCapabilities.UNIT_LIFECYCLE,
                        ApiSupportMatrix.Backend.RUNTIME) == ApiSupportMatrix.Level.FULL,
                "unit lifecycle support is not advertised");
        require(ApiSupportMatrix.expectedSupport(RustedFabricCapabilities.GAME_LIFECYCLE,
                        ApiSupportMatrix.Backend.RUNTIME) == ApiSupportMatrix.Level.FULL,
                "game lifecycle support is not advertised");
        require(ApiSupportMatrix.expectedSupport(RustedFabricCapabilities.PROJECTILE_LIFECYCLE,
                        ApiSupportMatrix.Backend.RUNTIME) == ApiSupportMatrix.Level.FULL,
                "projectile lifecycle support is not advertised");
        require(ApiSupportMatrix.expectedSupport(RustedFabricCapabilities.PROJECTILE_COMBAT,
                        ApiSupportMatrix.Backend.RUNTIME) == ApiSupportMatrix.Level.FULL,
                "projectile combat support is not advertised");
        require(ApiSupportMatrix.expectedSupport(RustedFabricCapabilities.UNIT_DAMAGE,
                        ApiSupportMatrix.Backend.RUNTIME) == ApiSupportMatrix.Level.FULL,
                "unit damage support is not advertised");
        require(ApiSupportMatrix.expectedSupport(RustedFabricCapabilities.CUSTOM_EVENT_CONTEXT,
                        ApiSupportMatrix.Backend.RUNTIME) == ApiSupportMatrix.Level.FULL,
                "custom event context support is not advertised");
        require(ApiSupportMatrix.expectedSupport(RustedFabricCapabilities.GEOMETRY,
                        ApiSupportMatrix.Backend.RUNTIME) == ApiSupportMatrix.Level.FULL,
                "geometry support is not advertised");
        require(ApiSupportMatrix.expectedSupport(RustedFabricCapabilities.FOG,
                        ApiSupportMatrix.Backend.RUNTIME) == ApiSupportMatrix.Level.FULL,
                "fog support is not advertised");
        require(ApiSupportMatrix.expectedSupport(RustedFabricCapabilities.LOGIC_NUMBER_FUNCTIONS,
                        ApiSupportMatrix.Backend.RUNTIME) == ApiSupportMatrix.Level.FULL,
                "logic-number function support is not advertised");
        require(ApiSupportMatrix.expectedSupport(RustedFabricCapabilities.LOGIC_UNIT_REFERENCE,
                        ApiSupportMatrix.Backend.RUNTIME) == ApiSupportMatrix.Level.FULL,
                "unit-reference expression support is not advertised");
        require(ApiSupportMatrix.available(context, RustedFabricCapabilities.RUNTIME_LIFECYCLE),
                "runtime capability and expected support matrix disagree");
    }

    private static void verifyMultiplayerCompatibility() {
        String hash = repeat('a', 64);
        MultiplayerManifest windows = new MultiplayerManifest("windows", Arrays.asList(
                MultiplayerMod.required("shared_units", "1.2.0", "units-v1", hash),
                MultiplayerMod.clientOnly("desktop_hud", "2.0.0")));
        MultiplayerManifest android = new MultiplayerManifest("android", Arrays.asList(
                MultiplayerMod.required("shared_units", "1.2.0", "units-v1", hash),
                MultiplayerMod.clientOnly("touch_controls", "3.0.0")));
        MultiplayerManifest serverOnly = new MultiplayerManifest("windows", Arrays.asList(
                MultiplayerMod.serverOnly("host_admin", "1.0.0")));
        MultiplayerManifest optional = new MultiplayerManifest("windows", Arrays.asList(
                MultiplayerMod.optional("quality_tools", "2.0.0")));
        MultiplayerManifest decoded = MultiplayerManifest.decode(windows.encode());
        require(decoded.encode().equals(windows.encode()), "manifest encoding is not canonical");
        require(decoded.fingerprint().equals(windows.fingerprint()),
                "manifest fingerprint changed after decoding");
        require(MultiplayerHandshake.decodeHello(MultiplayerHandshake.encodeHello(windows))
                        .encode().equals(windows.encode()),
                "RFH1 handshake did not preserve the canonical manifest");

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
        require(MultiplayerCompatibility.evaluateVanillaPeer(serverOnly).compatible(),
                "server-only mod should allow vanilla clients");
        require(MultiplayerManifest.decode(serverOnly.encode()).mods().get(0).mode()
                        == MultiplayerMod.Mode.SERVER_ONLY,
                "server-only mode was not preserved by the wire manifest");
        require(MultiplayerCompatibility.evaluateVanillaPeer(optional).compatible(),
                "optional mod should remain compatible when the peer has no Loader");
        MultiplayerManifest optionalOtherVersion = new MultiplayerManifest("android", Arrays.asList(
                MultiplayerMod.optional("quality_tools", "1.5.0")));
        require(MultiplayerCompatibility.evaluate(optional, optionalOtherVersion).compatible(),
                "optional peer enhancements must not require matching versions");
        require(MultiplayerManifest.decode(optional.encode()).mods().get(0).mode()
                        == MultiplayerMod.Mode.OPTIONAL,
                "optional mode was not preserved by the wire manifest");
        MultiplayerMod activated = MultiplayerMod.required("dynamic_test", "1.0.0",
                "dynamic_v1", repeat('d', 64));
        MultiplayerManifest optionalDynamic = new MultiplayerManifest("windows", Arrays.asList(
                MultiplayerMod.optional("dynamic_test", "1.0.0")));
        require(MultiplayerRequirements.effective(optionalDynamic).mods().get(0).mode()
                        == MultiplayerMod.Mode.OPTIONAL,
                "inactive dynamic requirement changed an optional mod");
        try (MultiplayerRequirements.Activation ignored = MultiplayerRequirements.activate(activated)) {
            MultiplayerManifest effective = MultiplayerRequirements.effective(optionalDynamic);
            require(effective.mods().get(0).mode() == MultiplayerMod.Mode.REQUIRED
                            && repeat('d', 64).equals(effective.mods().get(0).syncHash()),
                    "active dynamic requirement did not replace its optional manifest row");
        }
        require(MultiplayerRequirements.effective(optionalDynamic).mods().get(0).mode()
                        == MultiplayerMod.Mode.OPTIONAL,
                "closed dynamic requirement remained active");
        final int[] peerEvents = {0};
        MultiplayerCompatibilityEvents.Registration peerRegistration =
                MultiplayerCompatibilityEvents.PEER_EVALUATED.register(result -> peerEvents[0]++);
        MultiplayerPeerCompatibility peer = MultiplayerPeerCompatibility.evaluate(
                "peer-1", windows, android);
        require(peer.compatible() && peer.remoteManifest().isPresent(),
                "live Loader peer compatibility failed");
        require(peerEvents[0] == 1, "live peer event was not delivered");
        peerRegistration.close();
    }

    private static void verifySharedSessions() {
        List<GameSession.Kind> started = new ArrayList<>();
        GameSessionEvents.Registration registration =
                GameSessionEvents.SESSION_STARTED.register(session -> started.add(session.kind()));
        GameSession single = GameSessionRuntime.transition(GameSession.Kind.SINGLE_PLAYER);
        require(!single.multiplayer() && RustedFabricRuntime.currentSession().orElse(null) == single,
                "single-player session is not exposed through the shared API");
        GameSession host = GameSessionRuntime.transition(GameSession.Kind.MULTIPLAYER_HOST);
        require(host.multiplayer() && host.host(), "host session transition failed");
        require(started.equals(Arrays.asList(GameSession.Kind.SINGLE_PLAYER,
                        GameSession.Kind.MULTIPLAYER_HOST)),
                "session events were not platform-neutral or ordered");
        GameSessionRuntime.endCurrent();
        registration.close();
    }

    private static void verifyNetworkBridge() throws InterruptedException {
        Map<String, Object> raw = new HashMap<>();
        raw.put(RustedFabricAPIKeys.K_CONTEXT_VERSION, 5);
        raw.put(RustedFabricAPIKeys.K_PLATFORM, "windows");
        raw.put(RustedFabricAPIKeys.K_MULTIPLAYER_MANIFEST,
                MultiplayerManifest.empty("windows").encode());
        RustedFabricRuntime.installContext(new RustedFabricAPIContext(raw));
        List<String> logs = new ArrayList<>();
        MultiplayerNetworkBridge bridge = new MultiplayerNetworkBridge(
                new MultiplayerNetworkBridge.Mapping(FakePacket.class.getName(),
                        "type", "bytes", "connection", "send", "disconnect", "id"),
                (message, failure) -> logs.add(message), 100L);
        require(bridge.allowGameStart(null),
                "empty single-player broadcast was rejected by the handshake gate");
        FakeEngine engine = new FakeEngine();
        FakeConnection connection = new FakeConnection();
        bridge.connectionReady(engine, connection, MultiplayerNetworkBridge.Side.CLIENT);
        require(engine.sent != null && engine.sent.type == MultiplayerHandshake.GAME_PACKET_TYPE,
                "shared network bridge did not send RFH1");
        engine.sent.connection = connection;
        require(bridge.receive(engine, engine.sent), "shared network bridge ignored RFH1");
        require(connection.disconnectReason == null, "compatible RFH1 peer was disconnected");
        require(bridge.isLoaderPeer(connection) && bridge.hasLoaderPeer(),
                "compatible RFH1 peer was not marked as a Loader peer");
        require(bridge.peerManifest(connection).isPresent()
                        && bridge.firstLoaderPeerManifest().isPresent(),
                "compatible peer manifest was not retained for optional feature discovery");
        require(!logs.isEmpty(), "network bridge diagnostics were not emitted");
        bridge.connectionClosed(connection);
        require(!bridge.isLoaderPeer(connection) && !bridge.peerManifest(connection).isPresent(),
                "closed connection retained Loader peer state");
        bridge.resetToSinglePlayer();
        require(!bridge.isLoaderPeer(connection) && !bridge.hasLoaderPeer(),
                "Loader peer state survived a network reset");
        require(!bridge.peerManifest(connection).isPresent(),
                "peer manifest survived a network reset");

        raw.put(RustedFabricAPIKeys.K_MULTIPLAYER_MANIFEST,
                new MultiplayerManifest("windows", Arrays.asList(
                        MultiplayerMod.serverOnly("host_admin", "1.0.0"),
                        MultiplayerMod.optional("quality_tools", "2.0.0"))).encode());
        RustedFabricRuntime.installContext(new RustedFabricAPIContext(raw));
        MultiplayerNetworkBridge vanillaFriendly = new MultiplayerNetworkBridge(
                new MultiplayerNetworkBridge.Mapping(FakePacket.class.getName(),
                        "type", "bytes", "connection", "send", "disconnect", "id"),
                (message, failure) -> { }, 100L);
        FakeConnection vanilla = new FakeConnection();
        vanillaFriendly.connectionReady(new FakeEngine(), vanilla,
                MultiplayerNetworkBridge.Side.HOST);
        Thread.sleep(200L);
        require(vanilla.disconnectReason == null && vanillaFriendly.allowGameStart(vanilla),
                "server-only/optional host rejected a vanilla client");
        require(vanillaFriendly.allowGameStart(null),
                "compatible broadcast did not evaluate all registered peers");
        require(!vanillaFriendly.isLoaderPeer(vanilla),
                "vanilla timeout was incorrectly marked as a Loader peer");

        raw.put(RustedFabricAPIKeys.K_MULTIPLAYER_MANIFEST,
                new MultiplayerManifest("windows", Arrays.asList(MultiplayerMod.required(
                        "shared", "1", "shared-v1", repeat('c', 64)))).encode());
        RustedFabricRuntime.installContext(new RustedFabricAPIContext(raw));
        MultiplayerNetworkBridge strict = new MultiplayerNetworkBridge(
                new MultiplayerNetworkBridge.Mapping(FakePacket.class.getName(),
                        "type", "bytes", "connection", "send", "disconnect", "id"),
                (message, failure) -> { }, 100L);
        FakeConnection legacy = new FakeConnection();
        strict.connectionReady(new FakeEngine(), legacy, MultiplayerNetworkBridge.Side.HOST);
        Thread.sleep(200L);
        require(legacy.disconnectReason != null,
                "legacy peer was not rejected when a required mod was active");
        require(!strict.allowGameStart(null),
                "incompatible broadcast ignored a rejected registered peer");
    }

    private static void verifyLethalHealthModifier() {
        require(NativeDamageMath.projectedHp(10.0F, 1.0F, 0.0F, 0.0F,
                        15.0F, 1.0F, 1.0F, 1.0F) == -5.0F,
                "unshielded overkill projection did not preserve negative HP");
        require(NativeDamageMath.projectedHp(5.0F, 1.0F, 0.0F, 3.0F,
                        10.0F, 1.0F, 1.0F, 1.0F) == -2.0F,
                "shielded overkill projection diverged from native damage math");
        require(NativeDamageMath.projectedHp(5.0F, 1.0F, 0.0F, 20.0F,
                        10.0F, 1.0F, 1.0F, 1.0F) == 5.0F,
                "fully deflected damage incorrectly changed projected HP");
        require(UnitDamageEvents.MODIFY_LETHAL_HEALTH.invoker()
                        .modify(null, null, 12.0F, null, 0.0F, -2.0F, 0.0F) == 0.0F,
                "empty lethal-health event changed the native zero clamp");
        io.github.endx.rustedfabricapi.api.event.RustedFabricEvent.Registration damageRegistration =
                UnitDamageEvents.MODIFY_LETHAL_HEALTH.subscribe(
                        (unit, attacker, requested, projectile, nativeValue,
                         unclampedValue, currentValue) -> Float.valueOf(unclampedValue));
        require(UnitDamageEvents.MODIFY_LETHAL_HEALTH.invoker()
                        .modify(null, null, 12.0F, null, 0.0F, -2.0F, 0.0F) == -2.0F,
                "lethal-health listener could not select the unclamped value");
        damageRegistration.close();
    }

    private static void verifyDamageEventContextContracts() {
        Unit unit = new Unit();
        Unit attacker = new Unit();
        UnitDamageResult result = new UnitDamageResult(
                unit, attacker, null, 12.0F, 2.0F,
                10.0F, -2.0F, 5.0F, 1.0F);
        require(result.unit() == unit && result.attacker().orElse(null) == attacker,
                "damage result lost target/attacker context");
        require(result.hpDamage() == 12.0F && result.shieldDamage() == 4.0F,
                "damage result did not preserve actual HP/shield reductions");
        require(result.nativeRemainingDamage() == 2.0F && result.wasLethal(),
                "damage result remaining/lethal semantics are wrong");

        final int[] resultEvents = {0};
        io.github.endx.rustedfabricapi.api.event.RustedFabricEvent.Registration resultRegistration =
                UnitDamageEvents.AFTER_DAMAGE_RESULT.subscribe(value -> {
                    if (value == result) resultEvents[0]++;
                });
        UnitDamageEvents.AFTER_DAMAGE_RESULT.invoker().afterDamage(result);
        require(resultEvents[0] == 1, "detailed damage event was not delivered");
        resultRegistration.close();

        final int[] usage = {0};
        try (DamageEventData.Registration ignored = DamageEventData.enable(() -> usage[0]++)) {
            DamageEventDataRuntime.onEventDataNameParsed("HpDamage");
            DamageEventDataRuntime.onEventDataNameParsed("unrelated");
            require(usage[0] == 1, "enhanced eventData usage detection is not case-insensitive");
        }
        require(DamageEventData.fieldNames().contains("damage")
                        && DamageEventData.fieldNames().contains("wasLethal"),
                "damage event-data catalog is incomplete");

        final int[] nativeUsage = {0};
        try (NativeEventData.Registration ignored =
                     NativeEventData.enable(() -> nativeUsage[0]++)) {
            NativeEventDataRuntime.onEventDataNameParsed("TeleportFromX");
            NativeEventDataRuntime.onEventDataNameParsed("MessageHasData");
            NativeEventDataRuntime.onEventDataNameParsed("unrelated");
            require(nativeUsage[0] == 2,
                    "native eventData usage detection is not case-insensitive");
        }
        require(NativeEventData.fieldNames().size() == 73
                        && NativeEventData.fieldNames().contains("actionId")
                        && NativeEventData.fieldNames().contains("waypointTargetUnit")
                        && NativeEventData.fieldNames().contains("oldTeamId")
                        && NativeEventData.fieldNames().contains("teleportFromX")
                        && NativeEventData.fieldNames().contains("removedUnit")
                        && NativeEventData.fieldNames().contains("killedUnitHp")
                        && NativeEventData.fieldNames().contains("finishedActionId")
                        && NativeEventData.fieldNames().contains("touchedUnit")
                        && NativeEventData.fieldNames().contains("transportUsedSlots")
                        && NativeEventData.fieldNames().contains("carrierUsedSlots")
                        && NativeEventData.fieldNames().contains("messageHasData")
                        && !NativeEventData.fieldNames().contains("messageSender"),
                "native event-data catalog is incomplete");

    }

    private static void verifySharedModEntrypoint(RustedFabricAPIContext context) {
        final RustedFabricAPIContext[] received = new RustedFabricAPIContext[1];
        RustedFabricAPIEntrypoint entrypoint = new RustedFabricAPIEntrypoint() {
            @Override
            protected void onRustedFabricAPI(RustedFabricAPIContext value) {
                received[0] = value;
            }
        };
        entrypoint.accept(context.asMap());
        require(received[0].platform() == context.platform(),
                "shared Fabric entrypoint did not adapt the raw context");
    }

    private static RustedFabricAPIContext androidContext() {
        Map<String, Object> raw = new HashMap<>();
        raw.put(RustedFabricAPIKeys.K_CONTEXT_VERSION, 5);
        raw.put(RustedFabricAPIKeys.K_LOADER_VERSION, "0.1.0");
        raw.put(RustedFabricAPIKeys.K_GAME_VERSION, "1.15");
        raw.put(RustedFabricAPIKeys.K_MAPPINGS_VERSION, "1.1 FINAL");
        raw.put(RustedFabricAPIKeys.K_MAPPING_PROFILE_ID, "rw-pc-1.15-v1.1");
        raw.put(RustedFabricAPIKeys.K_PLATFORM, "android");
        raw.put(RustedFabricAPIKeys.K_ANDROID, Boolean.TRUE);
        raw.put(RustedFabricAPIKeys.K_RUNTIME_NAMESPACE, "official");
        raw.put(RustedFabricAPIKeys.K_CAPABILITIES,
                new ArrayList<>(Arrays.asList("mapping.profile.exact", "event.engine.init",
                        RustedFabricCapabilities.RUNTIME_LIFECYCLE)));
        raw.put(RustedFabricAPIKeys.K_MULTIPLAYER_MANIFEST,
                MultiplayerManifest.empty("android").encode());
        return new RustedFabricAPIContext(raw);
    }

    private static void verifyContext(RustedFabricAPIContext context) {
        require(context.contextVersion() == 5, "context version missing");
        require(context.platform() == RustedFabricPlatform.ANDROID, "Android platform missing");
        require(context.androidRuntime(), "legacy Android accessor must remain compatible");
        require(context.hasCapability("event.engine.init"), "capability missing");
        require("rw-pc-1.15-v1.1".equals(context.mappingProfileId()),
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

    private static void verifyPerActionAutoTriggerCooldownContract() {
        Object metadata = new Object();
        Object actionConfig = new Object();
        require(!PerActionAutoTriggerCooldowns.isEnabled(metadata),
                "unconfigured metadata unexpectedly enabled independent auto-trigger cooldowns");
        PerActionAutoTriggerCooldowns.configureSeconds(metadata, actionConfig, 0.5F);
        require(PerActionAutoTriggerCooldowns.isEnabled(metadata),
                "per-action auto-trigger cooldown configuration was not retained");
        try {
            PerActionAutoTriggerCooldowns.configureSeconds(metadata, actionConfig, -1.0F);
            throw new AssertionError("negative auto-trigger cooldown was accepted");
        } catch (IllegalArgumentException expected) {
            // Expected public validation contract.
        }
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

    public static final class FakePacket {
        public int type;
        public byte[] bytes;
        public FakeConnection connection;
        public FakePacket(int type) { this.type = type; }
    }

    public static final class FakeConnection {
        public int id = 7;
        public String disconnectReason;
        public void disconnect(String reason) { disconnectReason = reason; }
    }

    public static final class FakeEngine {
        public FakePacket sent;
        public void send(FakeConnection connection, FakePacket packet) { sent = packet; }
    }
}
