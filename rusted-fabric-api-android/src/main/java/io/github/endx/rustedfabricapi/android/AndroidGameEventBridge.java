package io.github.endx.rustedfabricapi.android;

import java.util.IdentityHashMap;

import io.github.endx.rustedfabricapi.api.event.GameLifecycleEvents;
import io.github.endx.rustedfabricapi.api.event.ProjectileEvents;
import io.github.endx.rustedfabricapi.api.thread.GameThreadScheduler;

/** Namespace-neutral callbacks shared by the local-patch and Xposed Android backends. */
public final class AndroidGameEventBridge {
    private static final ThreadLocal<FrameState> FRAME_STATE =
            ThreadLocal.withInitial(FrameState::new);
    private static final ThreadLocal<IdentityHashMap<Object, Boolean>> PENDING_EXPLOSIONS =
            ThreadLocal.withInitial(IdentityHashMap::new);

    private AndroidGameEventBridge() {
    }

    public static void beforeFrameUpdate(Object engine, int delta) {
        FrameState state = FRAME_STATE.get();
        state.engine = engine;
        state.delta = delta;
        state.updatePending = true;
        GameThreadScheduler.executeUpdatePhase();
        GameLifecycleEvents.BEFORE_FRAME_UPDATE.invoker()
                .beforeFrameUpdate(engine, null, delta);
    }

    public static void afterFrameUpdate(Object engine) {
        FrameState state = FRAME_STATE.get();
        if (!state.updatePending) return;
        state.updatePending = false;
        GameLifecycleEvents.AFTER_FRAME_UPDATE.invoker()
                .afterFrameUpdate(engine != null ? engine : state.engine, null, state.delta);
    }

    public static void beforeFrameRender(Object engine, Object graphics) {
        afterFrameUpdate(engine);
        GameThreadScheduler.executeRenderPhase();
        GameLifecycleEvents.BEFORE_FRAME_RENDER.invoker()
                .beforeFrameRender(engine, null, graphics);
    }

    public static void afterFrameRender(Object engine) {
        GameLifecycleEvents.AFTER_FRAME_RENDER.invoker().afterFrameRender(engine);
    }

    public static void afterProjectileCreated(Object projectile, Object sourceUnit) {
        if (projectile != null) {
            ProjectileEvents.AFTER_PROJECTILE_CREATED.invoker()
                    .afterProjectileCreated(projectile, sourceUnit);
        }
    }

    public static void beforeProjectileUpdate(Object projectile, float delta) {
        PENDING_EXPLOSIONS.get().remove(projectile);
        ProjectileEvents.BEFORE_PROJECTILE_UPDATE.invoker()
                .beforeProjectileUpdate(projectile, delta);
    }

    public static void afterProjectileUpdate(Object projectile, float delta) {
        if (PENDING_EXPLOSIONS.get().remove(projectile) != null) {
            ProjectileEvents.AFTER_PROJECTILE_EXPLOSION.invoker()
                    .onProjectileExplosion(projectile);
        }
        ProjectileEvents.AFTER_PROJECTILE_UPDATE.invoker()
                .afterProjectileUpdate(projectile, delta);
    }

    public static void beforeProjectileExplosion(Object projectile) {
        ProjectileEvents.BEFORE_PROJECTILE_EXPLOSION.invoker()
                .onProjectileExplosion(projectile);
        PENDING_EXPLOSIONS.get().put(projectile, Boolean.TRUE);
    }

    public static void beforeProjectileRemoval(Object projectile) {
        ProjectileEvents.BEFORE_PROJECTILE_REMOVAL.invoker().onProjectileRemoval(
                projectile, ProjectileEvents.RemovalReason.REMOVED_FROM_GAME);
    }

    public static void afterProjectileRemoval(Object projectile) {
        PENDING_EXPLOSIONS.get().remove(projectile);
        ProjectileEvents.AFTER_PROJECTILE_REMOVAL.invoker().onProjectileRemoval(
                projectile, ProjectileEvents.RemovalReason.REMOVED_FROM_GAME);
    }

    private static final class FrameState {
        Object engine;
        int delta;
        boolean updatePending;
    }
}
