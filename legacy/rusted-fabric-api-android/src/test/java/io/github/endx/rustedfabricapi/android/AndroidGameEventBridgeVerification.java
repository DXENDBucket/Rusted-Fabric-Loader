package io.github.endx.rustedfabricapi.android;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.github.endx.rustedfabricapi.api.RustedFabricAPIContext;
import io.github.endx.rustedfabricapi.api.RustedFabricAPIKeys;
import io.github.endx.rustedfabricapi.api.RustedFabricCapabilities;
import io.github.endx.rustedfabricapi.api.RustedFabricRuntime;
import io.github.endx.rustedfabricapi.api.event.GameLifecycleEvents;
import io.github.endx.rustedfabricapi.api.event.ProjectileEvents;
import io.github.endx.rustedfabricapi.api.event.RustedFabricEvent;
import io.github.endx.rustedfabricapi.api.event.UnitDamageEvents;
import io.github.endx.rustedfabricapi.api.thread.GameThreadScheduler;

public final class AndroidGameEventBridgeVerification {
    private AndroidGameEventBridgeVerification() {
    }

    public static void main(String[] args) {
        installContext();
        verifyFrames();
        verifyProjectiles();
        verifyUnitDamage();
        verifyUnitDeath();
        System.out.println("Android frame/projectile/unit-damage/death bridge contracts passed");
    }

    private static void verifyFrames() {
        List<String> calls = new ArrayList<>();
        List<RustedFabricEvent.Registration> registrations = Arrays.asList(
                GameLifecycleEvents.BEFORE_FRAME_UPDATE.subscribe(
                        (engine, container, delta) -> calls.add("before-update:" + delta)),
                GameLifecycleEvents.AFTER_FRAME_UPDATE.subscribe(
                        (engine, container, delta) -> calls.add("after-update:" + delta)),
                GameLifecycleEvents.BEFORE_FRAME_RENDER.subscribe(
                        (engine, container, graphics) -> calls.add("before-render")),
                GameLifecycleEvents.AFTER_FRAME_RENDER.subscribe(
                        engine -> calls.add("after-render")));
        GameThreadScheduler.onNextUpdate(() -> calls.add("update-task"));
        GameThreadScheduler.onNextRender(() -> calls.add("render-task"));
        Object engine = new Object();
        AndroidGameEventBridge.beforeFrameUpdate(engine, 16);
        AndroidGameEventBridge.beforeFrameRender(engine, new Object());
        AndroidGameEventBridge.afterFrameRender(engine);
        AndroidGameEventBridge.afterFrameUpdate(engine);
        require(calls.equals(Arrays.asList("update-task", "before-update:16",
                        "after-update:16", "render-task", "before-render", "after-render")),
                "Android frame callback order changed: " + calls);
        registrations.forEach(RustedFabricEvent.Registration::close);
    }

    private static void verifyProjectiles() {
        List<String> calls = new ArrayList<>();
        List<RustedFabricEvent.Registration> registrations = Arrays.asList(
                ProjectileEvents.AFTER_PROJECTILE_CREATED.subscribe(
                        (projectile, source) -> calls.add("created")),
                ProjectileEvents.BEFORE_PROJECTILE_UPDATE.subscribe(
                        (projectile, delta) -> calls.add("before-update")),
                ProjectileEvents.BEFORE_PROJECTILE_EXPLOSION.subscribe(
                        projectile -> calls.add("before-explosion")),
                ProjectileEvents.BEFORE_PROJECTILE_IMPACT.subscribe(
                        (projectile, impact) -> calls.add("before-impact:" + impact.kind())),
                ProjectileEvents.AFTER_PROJECTILE_EXPLOSION.subscribe(
                        projectile -> calls.add("after-explosion")),
                ProjectileEvents.AFTER_PROJECTILE_IMPACT.subscribe(
                        (projectile, impact) -> calls.add("after-impact:" + impact.kind())),
                ProjectileEvents.AFTER_PROJECTILE_UPDATE.subscribe(
                        (projectile, delta) -> calls.add("after-update")),
                ProjectileEvents.BEFORE_PROJECTILE_REMOVAL.subscribe(
                        (projectile, reason) -> calls.add("before-remove:" + reason)),
                ProjectileEvents.AFTER_PROJECTILE_REMOVAL.subscribe(
                        (projectile, reason) -> calls.add("after-remove:" + reason)));
        Object projectile = new FakeProjectile();
        AndroidGameEventBridge.afterProjectileCreated(projectile, new Object());
        AndroidGameEventBridge.beforeProjectileUpdate(projectile, 1.0F);
        AndroidGameEventBridge.beforeProjectileExplosion(projectile);
        AndroidGameEventBridge.afterProjectileUpdate(projectile, 1.0F);
        AndroidGameEventBridge.beforeProjectileRemoval(projectile);
        AndroidGameEventBridge.afterProjectileRemoval(projectile);
        require(calls.equals(Arrays.asList("created", "before-update",
                        "before-impact:UNIT_TARGET", "before-explosion",
                        "after-explosion", "after-impact:UNIT_TARGET", "after-update",
                        "before-remove:REMOVED_FROM_GAME",
                        "after-remove:REMOVED_FROM_GAME")),
                "Android projectile callback order changed: " + calls);
        registrations.forEach(RustedFabricEvent.Registration::close);
    }

    private static void verifyUnitDamage() {
        List<String> calls = new ArrayList<>();
        Object unit = new Object();
        Object attacker = new Object();
        Object projectile = new Object();
        List<RustedFabricEvent.Registration> registrations = Arrays.asList(
                UnitDamageEvents.BEFORE_UNIT_APPLY_DAMAGE.subscribe(
                        (value, source, amount, shot) -> {
                            calls.add("before:" + amount);
                            return amount > 20.0F;
                        }),
                UnitDamageEvents.AFTER_UNIT_APPLY_DAMAGE.subscribe(
                        (value, source, amount, shot, applied) ->
                                calls.add("after:" + applied)));
        require(!AndroidGameEventBridge.beforeUnitApplyDamage(
                        unit, attacker, 12.0F, projectile),
                "ordinary Android damage was cancelled");
        AndroidGameEventBridge.afterUnitApplyDamage(
                9.0F, unit, attacker, 12.0F, projectile);
        require(AndroidGameEventBridge.beforeUnitApplyDamage(
                        unit, attacker, 24.0F, projectile),
                "Android damage cancellation was ignored");
        AndroidGameEventBridge.afterUnitApplyDamage(
                0.0F, unit, attacker, 24.0F, projectile);
        require(calls.equals(Arrays.asList("before:12.0", "after:9.0",
                        "before:24.0", "after:0.0")),
                "Android unit damage callback order changed: " + calls);
        registrations.forEach(RustedFabricEvent.Registration::close);
    }

    private static void verifyUnitDeath() {
        List<String> calls = new ArrayList<>();
        Object unit = new Object();
        List<RustedFabricEvent.Registration> registrations = Arrays.asList(
                UnitDamageEvents.BEFORE_UNIT_DEATH_SEQUENCE.subscribe(value -> {
                    calls.add("before-death");
                    return false;
                }),
                UnitDamageEvents.AFTER_UNIT_DEATH_SEQUENCE.subscribe(
                        value -> calls.add("after-death")),
                UnitDamageEvents.MODIFY_UNIT_DEATH_EFFECTS_RESULT.subscribe(
                        (value, keepObject) -> {
                            calls.add("death-effects:" + keepObject);
                            return Boolean.valueOf(!keepObject);
                        }));
        require(!AndroidGameEventBridge.beforeUnitDeathSequence(unit),
                "Android custom-unit death was unexpectedly cancelled");
        AndroidGameEventBridge.afterUnitDeathSequence(unit);
        require(!AndroidGameEventBridge.modifyUnitDeathEffectsResult(unit, true),
                "Android death-effects result was not modified");
        require(calls.equals(Arrays.asList("before-death", "after-death",
                        "death-effects:true")),
                "Android unit death callback order changed: " + calls);
        registrations.forEach(RustedFabricEvent.Registration::close);
    }

    private static void installContext() {
        Map<String, Object> values = new HashMap<>();
        values.put(RustedFabricAPIKeys.K_PLATFORM, "android");
        values.put(RustedFabricAPIKeys.K_CAPABILITIES,
                Arrays.asList(RustedFabricCapabilities.GAME_LIFECYCLE,
                        RustedFabricCapabilities.PROJECTILE_LIFECYCLE,
                        RustedFabricCapabilities.UNIT_DAMAGE));
        RustedFabricRuntime.installContext(new RustedFabricAPIContext(values));
    }

    private static final class FakeProjectile {
        Object targetUnit = new Object();
        boolean targetGround;
        boolean collideWithUnits = true;
        boolean collideWithTerrain;
        boolean hasFixedTargetPosition;
        float impactX = 10.0F;
        float impactY = 20.0F;
        float impactHeight = 3.0F;
        float targetX = 11.0F;
        float targetY = 21.0F;
        float contactCollisionRadius = 4.0F;
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
