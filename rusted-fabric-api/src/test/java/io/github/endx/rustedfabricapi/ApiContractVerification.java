package io.github.endx.rustedfabricapi;

import io.github.endx.rustedfabricapi.api.RustedFabricAPIContext;
import io.github.endx.rustedfabricapi.api.RustedFabricAPIEntrypoint;
import io.github.endx.rustedfabricapi.api.RustedFabricAPIKeys;
import io.github.endx.rustedfabricapi.api.RustedFabricCapabilities;
import io.github.endx.rustedfabricapi.api.RustedFabricRuntime;
import io.github.endx.rustedfabricapi.api.event.RustedFabricEvent;
import io.github.endx.rustedfabricapi.api.event.EventPhaseContractVerification;
import io.github.endx.rustedfabricapi.api.game.ProjectileSnapshot;
import io.github.endx.rustedfabricapi.api.game.ProjectileImpactSnapshot;
import io.github.endx.rustedfabricapi.api.game.Projectiles;
import io.github.endx.rustedfabricapi.api.game.CustomUnitRuntimeSnapshot;
import io.github.endx.rustedfabricapi.api.lifecycle.LifecycleScopeContractVerification;
import io.github.endx.rustedfabricapi.api.service.ServiceRegistryContractVerification;
import io.github.endx.rustedfabricapi.api.thread.GameThreadScheduler;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;

public final class ApiContractVerification {
    private ApiContractVerification() {
    }

    public static void main(String[] args) {
        ServiceRegistryContractVerification.verify();
        LifecycleScopeContractVerification.verify();
        EventPhaseContractVerification.verify();
        verifiesListenerOrderAndSnapshotRefresh();
        verifiesListenerCleanup();
        verifiesContextDefensiveCopies();
        verifiesEntrypointAdapter();
        verifiesGameThreadScheduling();
        verifiesProjectileSnapshot();
        verifiesAndroidOfficialProjectileLayout();
        verifiesCustomUnitRuntimeSnapshot();
        verifiesAndroidOfficialCustomUnitLayout();
        System.out.println("Rusted Fabric API contract verification passed");
    }

    private static void verifiesListenerCleanup() {
        List<String> calls = new ArrayList<String>();
        RustedFabricEvent<Probe> event = RustedFabricEvent.create(listeners -> value -> {
            for (Probe listener : listeners) listener.accept(value);
        });
        Probe direct = value -> calls.add("direct:" + value);
        event.register(direct);
        RustedFabricEvent.Registration registration =
                event.subscribe(value -> calls.add("scoped:" + value));
        require(event.listenerCount() == 2, "listener count did not update");
        require(registration.unregister(), "subscription handle did not unregister");
        require(!registration.unregister(), "subscription handle was not idempotent");
        require(event.unregister(direct), "direct listener could not be removed");
        event.invoker().accept("ignored");
        require(calls.isEmpty(), "removed listener was still invoked");
    }

    private static void verifiesListenerOrderAndSnapshotRefresh() {
        List<String> calls = new ArrayList<String>();
        RustedFabricEvent<Probe> event = RustedFabricEvent.create(listeners -> value -> {
            for (Probe listener : listeners) {
                listener.accept(value);
            }
        });

        event.register(value -> calls.add("first:" + value));
        event.register(value -> calls.add("second:" + value));
        event.invoker().accept("ready");

        require(calls.size() == 2, "expected two listener calls");
        require("first:ready".equals(calls.get(0)), "listeners must retain registration order");
        require("second:ready".equals(calls.get(1)), "second listener was not called");
    }

    private static void verifiesContextDefensiveCopies() {
        String[] sourceArgs = new String[] { "one", "two" };
        Map<String, Object> source = new HashMap<String, Object>();
        source.put(RustedFabricAPIKeys.K_CONTEXT_VERSION, Integer.valueOf(2));
        source.put(RustedFabricAPIKeys.K_LOADER_VERSION, "0.1.0");
        source.put(RustedFabricAPIKeys.K_GAME_VERSION, "1.15");
        source.put(RustedFabricAPIKeys.K_MAPPINGS_VERSION, "1.1");
        source.put(RustedFabricAPIKeys.K_GAME_DIR, Paths.get("game"));
        source.put(RustedFabricAPIKeys.K_GAME_ARGS, sourceArgs);
        source.put(RustedFabricAPIKeys.K_RUNTIME_NAMESPACE, "named");
        source.put(RustedFabricAPIKeys.K_ANDROID, Boolean.FALSE);

        RustedFabricAPIContext context = new RustedFabricAPIContext(source);
        sourceArgs[0] = "changed-at-source";
        String[] returnedArgs = context.gameArgs();
        returnedArgs[1] = "changed-by-caller";

        require(context.contextVersion() == 2, "context version missing");
        require("0.1.0".equals(context.loaderVersion()), "loader version missing");
        require("1.15".equals(context.gameVersion()), "game version missing");
        require("1.1".equals(context.mappingsVersion()), "mappings version missing");
        require("one".equals(context.gameArgs()[0]), "constructor must copy launch arguments");
        require("two".equals(context.gameArgs()[1]), "gameArgs must return a defensive copy");
        require("named".equals(context.runtimeNamespace()), "runtime namespace missing");
        require(!context.androidRuntime(), "unexpected Android runtime flag");

        boolean immutable = false;
        try {
            context.asMap().put("unexpected", "value");
        } catch (UnsupportedOperationException expected) {
            immutable = true;
        }
        require(immutable, "raw context view must be immutable");
    }

    private static void verifiesEntrypointAdapter() {
        final int[] observedVersion = new int[1];
        RustedFabricAPIEntrypoint entrypoint = new RustedFabricAPIEntrypoint() {
            @Override
            protected void onRustedFabricAPI(RustedFabricAPIContext context) {
                observedVersion[0] = context.contextVersion();
            }
        };
        Map<String, Object> raw = new HashMap<String, Object>();
        raw.put(RustedFabricAPIKeys.K_CONTEXT_VERSION, Integer.valueOf(2));
        entrypoint.accept(raw);
        require(observedVersion[0] == 2, "entrypoint adapter did not expose the typed context");
    }

    private static void verifiesGameThreadScheduling() {
        Map<String, Object> raw = new HashMap<String, Object>();
        raw.put(RustedFabricAPIKeys.K_CAPABILITIES,
                Arrays.asList(RustedFabricCapabilities.GAME_LIFECYCLE));
        RustedFabricRuntime.installContext(new RustedFabricAPIContext(raw));
        List<String> calls = new ArrayList<String>();
        CompletableFuture<Void> failed = GameThreadScheduler.onNextUpdate(() -> {
            calls.add("failed");
            throw new IllegalStateException("synthetic");
        });
        CompletableFuture<Void> succeeded = GameThreadScheduler.onNextUpdate(() -> {
            require(GameThreadScheduler.isUpdateThread(), "update thread was not identified");
            calls.add("succeeded");
        });
        CompletableFuture<Void> render = GameThreadScheduler.onNextRender(() -> {
            require(GameThreadScheduler.isRenderThread(), "render thread was not identified");
            calls.add("render");
        });
        GameThreadScheduler.executeUpdatePhase();
        GameThreadScheduler.executeRenderPhase();
        require(failed.isCompletedExceptionally(), "failed task did not retain its failure");
        require(succeeded.isDone() && !succeeded.isCompletedExceptionally(),
                "later update task did not run after an isolated failure");
        require(render.isDone(), "render task was not drained");
        require(calls.equals(Arrays.asList("failed", "succeeded", "render")),
                "scheduled task order changed");
    }

    private static void verifiesProjectileSnapshot() {
        FakeProjectile value = new FakeProjectile();
        ProjectileSnapshot snapshot = ProjectileSnapshot.capture(value);
        require(snapshot.id() == 42L && snapshot.sourceUnit() == value.sourceUnit,
                "projectile identity/source snapshot failed");
        require(snapshot.x() == 10.0F && snapshot.y() == 20.0F && snapshot.height() == 3.0F,
                "projectile position snapshot failed");
        require(snapshot.directDamage() == 12.0F && snapshot.areaDamage() == 6.0F,
                "projectile damage snapshot failed");
        require(snapshot.ballistic() && !snapshot.removalRequested(),
                "projectile flags snapshot failed");
        value.directDamage = 99.0F;
        require(snapshot.directDamage() == 12.0F,
                "projectile snapshot was not immutable");
        ProjectileImpactSnapshot impact = Projectiles.impactSnapshot(value);
        require(impact.kind() == ProjectileImpactSnapshot.Kind.UNIT_TARGET
                        && impact.impactX() == 13.0F && impact.impactY() == 23.0F,
                "named projectile impact snapshot failed");
        Projectiles.removeImmediately(value);
        require(value.removed, "named projectile was not removed immediately");
    }

    private static void verifiesAndroidOfficialProjectileLayout() {
        AndroidFakeProjectile value = new AndroidFakeProjectile();
        ProjectileSnapshot snapshot = ProjectileSnapshot.capture(value);
        require(snapshot.id() == 84L && snapshot.x() == 11.0F
                        && snapshot.y() == 22.0F && snapshot.height() == 4.0F,
                "Android official projectile base fields used the PC layout");
        Projectiles.requestRemoval(value);
        require(value.aS, "Android projectile removal flag was not set");
        Projectiles.removeImmediately(value);
        require(value.removed, "Android projectile was not removed immediately");
        ProjectileImpactSnapshot impact = Projectiles.impactSnapshot(value);
        require(impact.kind() == ProjectileImpactSnapshot.Kind.UNIT_TARGET
                        && impact.impactHeight() == 6.0F,
                "Android projectile impact snapshot failed");
    }

    private static void verifiesCustomUnitRuntimeSnapshot() {
        FakeCustomUnit value = new FakeCustomUnit();
        CustomUnitRuntimeSnapshot snapshot = CustomUnitRuntimeSnapshot.capture(value);
        require(snapshot.metadataHasBuildQueueRuntimeEffects()
                        && !snapshot.revertMetadataHasBuildQueueRuntimeEffects(),
                "custom-unit metadata runtime gates were not captured");
        require(snapshot.currentBuildQueueActionBlocksMovement()
                        && snapshot.createdEventPending()
                        && !snapshot.completeAndActiveEventPending(),
                "custom-unit construction latches were not captured");
        require(snapshot.autoTriggerCooldownTimer() == 3.5F
                        && snapshot.lastLegBaseDirection() == 180.0F
                        && snapshot.hasLastLegBasePosition(),
                "custom-unit cooldown/leg base state was not captured");
        value.currentBuildQueueActionBlocksMovement = false;
        require(snapshot.currentBuildQueueActionBlocksMovement(),
                "custom-unit runtime snapshot was not immutable");
    }

    private static void verifiesAndroidOfficialCustomUnitLayout() {
        AndroidFakeCustomUnit value = new AndroidFakeCustomUnit();
        CustomUnitRuntimeSnapshot snapshot = CustomUnitRuntimeSnapshot.capture(value);
        require(!snapshot.hasLastLegBasePosition()
                        && Float.isNaN(snapshot.lastLegBaseX())
                        && Float.isNaN(snapshot.lastLegBaseY()),
                "unmapped Android leg-base X/Y fields exposed reused dP/dQ values");
        require(snapshot.lastLegBaseHeight() == 6.0F
                        && snapshot.lastLegBaseDirection() == 270.0F,
                "mapped Android leg-base height/direction were not captured");
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private interface Probe {
        void accept(String value);
    }

    private static final class FakeProjectile {
        long id = 42L;
        Object sourceUnit = new Object();
        Object targetUnit = new Object();
        float x = 10.0F;
        float y = 20.0F;
        float height = 3.0F;
        float targetX = 30.0F;
        float targetY = 40.0F;
        float remainingLife = 5.0F;
        float ageTimer = 2.0F;
        float speed = 4.0F;
        float direction = 90.0F;
        float directDamage = 12.0F;
        float areaDamage = 6.0F;
        float areaRadius = 8.0F;
        boolean instant;
        boolean continuousDamage;
        boolean ballistic = true;
        boolean impactTriggered;
        boolean removalRequested;
        boolean removed;
        boolean targetGround;
        boolean collideWithUnits = true;
        boolean collideWithTerrain;
        boolean hasFixedTargetPosition;
        float impactX = 13.0F;
        float impactY = 23.0F;
        float impactHeight = 4.0F;
        float contactCollisionRadius = 2.0F;

        void removeFromGame() {
            removed = true;
        }
    }

    private static final class AndroidFakeProjectile
            extends com.corrodinggames.rts.gameFramework.ah {
        Object j = new Object();
        Object l = new Object();
        float n = 31.0F;
        float o = 41.0F;
        float h = 5.0F;
        float J = 2.0F;
        float t = 4.0F;
        float az = 90.0F;
        float U = 12.0F;
        float Y = 6.0F;
        float Z = 8.0F;
        boolean A;
        boolean E;
        boolean aH = true;
        boolean bn;
        boolean aS;
        boolean removed;
        boolean m;
        boolean collideWithUnits = true;
        boolean at;
        boolean aC;
        float aV = 14.0F;
        float aW = 24.0F;
        float aX = 6.0F;
        float aA = 3.0F;

        AndroidFakeProjectile() {
            ej = 84L;
            eo = 1234;
            ep = 5678;
            eq = 11.0F;
            er = 22.0F;
            es = 4.0F;
        }

        void a() {
            removed = true;
        }
    }

    private static final class FakeCustomUnitMetadata {
        boolean hasBuildQueueRuntimeEffects;

        FakeCustomUnitMetadata(boolean value) {
            hasBuildQueueRuntimeEffects = value;
        }
    }

    private static final class FakeCustomUnit {
        Object unitMetadata = new FakeCustomUnitMetadata(true);
        Object revertMetadata = new FakeCustomUnitMetadata(false);
        boolean currentBuildQueueActionBlocksMovement = true;
        boolean createdEventPending = true;
        boolean completeAndActiveEventPending;
        float autoTriggerCooldownTimer = 3.5F;
        float lastLegBaseX = 1.0F;
        float lastLegBaseY = 2.0F;
        float lastLegBaseHeight = 3.0F;
        float lastLegBaseDir = 180.0F;
    }

    private static final class AndroidFakeCustomUnit
            extends com.corrodinggames.rts.gameFramework.ah {
        Object x = new FakeCustomUnitMetadata(true);
        Object z = new FakeCustomUnitMetadata(false);
        boolean g = true;
        boolean h;
        boolean i = true;
        float w = 2.5F;
        Object dP = new Object();
        int dQ = 99;
        float dR = 6.0F;
        float dS = 270.0F;
    }
}
