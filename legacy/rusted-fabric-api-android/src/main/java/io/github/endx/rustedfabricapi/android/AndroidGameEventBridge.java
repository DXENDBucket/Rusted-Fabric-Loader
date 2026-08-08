package io.github.endx.rustedfabricapi.android;

import java.util.IdentityHashMap;

import io.github.endx.rustedfabricapi.api.event.GameLifecycleEvents;
import io.github.endx.rustedfabricapi.api.event.ProjectileEvents;
import io.github.endx.rustedfabricapi.api.event.UnitDamageEvents;
import io.github.endx.rustedfabricapi.api.game.ProjectileImpactSnapshot;
import io.github.endx.rustedfabricapi.api.thread.GameThreadScheduler;

/** Namespace-neutral callbacks shared by the local-patch and Xposed Android backends. */
public final class AndroidGameEventBridge {
    private static final ThreadLocal<FrameState> FRAME_STATE =
            ThreadLocal.withInitial(FrameState::new);
    private static final ThreadLocal<IdentityHashMap<Object, ProjectileImpactSnapshot>> PENDING_EXPLOSIONS =
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
        ProjectileImpactSnapshot impact = PENDING_EXPLOSIONS.get().remove(projectile);
        if (impact != null) {
            ProjectileEvents.AFTER_PROJECTILE_EXPLOSION.invoker()
                    .onProjectileExplosion(projectile);
            ProjectileEvents.AFTER_PROJECTILE_IMPACT.invoker()
                    .onProjectileImpact(projectile, impact);
        }
        ProjectileEvents.AFTER_PROJECTILE_UPDATE.invoker()
                .afterProjectileUpdate(projectile, delta);
    }

    public static void beforeProjectileExplosion(Object projectile) {
        ProjectileImpactSnapshot impact = ProjectileImpactSnapshot.capture(projectile);
        PENDING_EXPLOSIONS.get().put(projectile, impact);
        ProjectileEvents.BEFORE_PROJECTILE_IMPACT.invoker()
                .onProjectileImpact(projectile, impact);
        ProjectileEvents.BEFORE_PROJECTILE_EXPLOSION.invoker()
                .onProjectileExplosion(projectile);
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

    public static boolean beforeUnitApplyDamage(Object unit, Object attacker, float amount,
                                                Object projectile) {
        return UnitDamageEvents.BEFORE_UNIT_APPLY_DAMAGE.invoker()
                .beforeUnitApplyDamage(unit, attacker, amount, projectile);
    }

    public static void afterUnitApplyDamage(float appliedAmount, Object unit, Object attacker,
                                            float amount, Object projectile) {
        UnitDamageEvents.AFTER_UNIT_APPLY_DAMAGE.invoker()
                .afterUnitApplyDamage(unit, attacker, amount, projectile, appliedAmount);
    }

    public static boolean beforeUnitDeathSequence(Object unit) {
        return UnitDamageEvents.BEFORE_UNIT_DEATH_SEQUENCE.invoker()
                .beforeUnitDeathSequence(unit);
    }

    public static void afterUnitDeathSequence(Object unit) {
        UnitDamageEvents.AFTER_UNIT_DEATH_SEQUENCE.invoker().afterUnitDeathSequence(unit);
    }

    public static boolean modifyUnitDeathEffectsResult(Object unit, boolean vanillaKeepObject) {
        Boolean result = UnitDamageEvents.MODIFY_UNIT_DEATH_EFFECTS_RESULT.invoker()
                .modifyUnitDeathEffectsResult(unit, vanillaKeepObject);
        return result != null ? result.booleanValue() : vanillaKeepObject;
    }

    private static final class FrameState {
        Object engine;
        int delta;
        boolean updatePending;
    }
}
