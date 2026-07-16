package io.github.endx.rustedfabricapi.api.event;

import io.github.endx.rustedfabricapi.api.game.ProjectileImpactSnapshot;

/** High-frequency projectile lifecycle boundaries backed by the mapped Projectile runtime. */
public final class ProjectileEvents {
    public static final RustedFabricEvent<AfterProjectileCreated> AFTER_PROJECTILE_CREATED =
            RustedFabricEvent.create(listeners -> (projectile, sourceUnit) -> {
                for (AfterProjectileCreated listener : listeners) {
                    listener.afterProjectileCreated(projectile, sourceUnit);
                }
            });

    public static final RustedFabricEvent<BeforeProjectileUpdate> BEFORE_PROJECTILE_UPDATE =
            RustedFabricEvent.create(listeners -> (projectile, delta) -> {
                for (BeforeProjectileUpdate listener : listeners) {
                    listener.beforeProjectileUpdate(projectile, delta);
                }
            });

    public static final RustedFabricEvent<AfterProjectileUpdate> AFTER_PROJECTILE_UPDATE =
            RustedFabricEvent.create(listeners -> (projectile, delta) -> {
                for (AfterProjectileUpdate listener : listeners) {
                    listener.afterProjectileUpdate(projectile, delta);
                }
            });

    public static final RustedFabricEvent<ProjectileExplosion> BEFORE_PROJECTILE_EXPLOSION =
            RustedFabricEvent.create(listeners -> projectile -> {
                for (ProjectileExplosion listener : listeners) {
                    listener.onProjectileExplosion(projectile);
                }
            });

    public static final RustedFabricEvent<ProjectileExplosion> AFTER_PROJECTILE_EXPLOSION =
            RustedFabricEvent.create(listeners -> projectile -> {
                for (ProjectileExplosion listener : listeners) {
                    listener.onProjectileExplosion(projectile);
                }
            });

    public static final RustedFabricEvent<ProjectileImpact> BEFORE_PROJECTILE_IMPACT =
            RustedFabricEvent.create(listeners -> (projectile, impact) -> {
                for (ProjectileImpact listener : listeners) {
                    listener.onProjectileImpact(projectile, impact);
                }
            });

    public static final RustedFabricEvent<ProjectileImpact> AFTER_PROJECTILE_IMPACT =
            RustedFabricEvent.create(listeners -> (projectile, impact) -> {
                for (ProjectileImpact listener : listeners) {
                    listener.onProjectileImpact(projectile, impact);
                }
            });

    public static final RustedFabricEvent<ProjectileRemoval> BEFORE_PROJECTILE_REMOVAL =
            RustedFabricEvent.create(listeners -> (projectile, reason) -> {
                for (ProjectileRemoval listener : listeners) {
                    listener.onProjectileRemoval(projectile, reason);
                }
            });

    public static final RustedFabricEvent<ProjectileRemoval> AFTER_PROJECTILE_REMOVAL =
            RustedFabricEvent.create(listeners -> (projectile, reason) -> {
                for (ProjectileRemoval listener : listeners) {
                    listener.onProjectileRemoval(projectile, reason);
                }
            });

    private ProjectileEvents() {
    }

    public enum RemovalReason {
        REQUESTED,
        REMOVED_FROM_GAME
    }

    @FunctionalInterface
    public interface AfterProjectileCreated {
        void afterProjectileCreated(Object projectile, Object sourceUnit);
    }

    @FunctionalInterface
    public interface BeforeProjectileUpdate {
        void beforeProjectileUpdate(Object projectile, float delta);
    }

    @FunctionalInterface
    public interface AfterProjectileUpdate {
        void afterProjectileUpdate(Object projectile, float delta);
    }

    @FunctionalInterface
    public interface ProjectileExplosion {
        void onProjectileExplosion(Object projectile);
    }

    @FunctionalInterface
    public interface ProjectileImpact {
        void onProjectileImpact(Object projectile, ProjectileImpactSnapshot impact);
    }

    @FunctionalInterface
    public interface ProjectileRemoval {
        void onProjectileRemoval(Object projectile, RemovalReason reason);
    }
}
