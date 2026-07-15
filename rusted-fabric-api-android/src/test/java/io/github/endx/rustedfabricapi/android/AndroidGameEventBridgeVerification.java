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
import io.github.endx.rustedfabricapi.api.thread.GameThreadScheduler;

public final class AndroidGameEventBridgeVerification {
    private AndroidGameEventBridgeVerification() {
    }

    public static void main(String[] args) {
        installContext();
        verifyFrames();
        verifyProjectiles();
        System.out.println("Android frame/projectile bridge contracts passed");
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
                ProjectileEvents.AFTER_PROJECTILE_EXPLOSION.subscribe(
                        projectile -> calls.add("after-explosion")),
                ProjectileEvents.AFTER_PROJECTILE_UPDATE.subscribe(
                        (projectile, delta) -> calls.add("after-update")),
                ProjectileEvents.BEFORE_PROJECTILE_REMOVAL.subscribe(
                        (projectile, reason) -> calls.add("before-remove:" + reason)),
                ProjectileEvents.AFTER_PROJECTILE_REMOVAL.subscribe(
                        (projectile, reason) -> calls.add("after-remove:" + reason)));
        Object projectile = new Object();
        AndroidGameEventBridge.afterProjectileCreated(projectile, new Object());
        AndroidGameEventBridge.beforeProjectileUpdate(projectile, 1.0F);
        AndroidGameEventBridge.beforeProjectileExplosion(projectile);
        AndroidGameEventBridge.afterProjectileUpdate(projectile, 1.0F);
        AndroidGameEventBridge.beforeProjectileRemoval(projectile);
        AndroidGameEventBridge.afterProjectileRemoval(projectile);
        require(calls.equals(Arrays.asList("created", "before-update", "before-explosion",
                        "after-explosion", "after-update", "before-remove:REMOVED_FROM_GAME",
                        "after-remove:REMOVED_FROM_GAME")),
                "Android projectile callback order changed: " + calls);
        registrations.forEach(RustedFabricEvent.Registration::close);
    }

    private static void installContext() {
        Map<String, Object> values = new HashMap<>();
        values.put(RustedFabricAPIKeys.K_PLATFORM, "android");
        values.put(RustedFabricAPIKeys.K_CAPABILITIES,
                Arrays.asList(RustedFabricCapabilities.GAME_LIFECYCLE,
                        RustedFabricCapabilities.PROJECTILE_LIFECYCLE));
        RustedFabricRuntime.installContext(new RustedFabricAPIContext(values));
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
