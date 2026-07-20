package io.github.endx.rustedfabricapi.api.projectile.event;

import io.github.endx.rustedfabricapi.api.event.RustedFabricEvent;
import io.github.endx.rustedfabricapi.api.game.ProjectileImpactSnapshot;
import rustedwarfare.game.Projectile;
import rustedwarfare.unit.Unit;

/** Projectile lifecycle events with mapped game types. */
public final class ProjectileEvents {
    public static final RustedFabricEvent<Created> AFTER_CREATED =
            RustedFabricEvent.create(listeners -> (projectile, sourceUnit) -> {
                for (Created listener : listeners) {
                    listener.afterCreated(projectile, sourceUnit);
                }
            });

    public static final RustedFabricEvent<Updated> BEFORE_UPDATE = updateEvent();
    public static final RustedFabricEvent<Updated> AFTER_UPDATE = updateEvent();
    public static final RustedFabricEvent<Explosion> BEFORE_EXPLOSION = explosionEvent();
    public static final RustedFabricEvent<Explosion> AFTER_EXPLOSION = explosionEvent();

    public static final RustedFabricEvent<Impact> BEFORE_IMPACT =
            RustedFabricEvent.create(listeners -> (projectile, impact) -> {
                for (Impact listener : listeners) {
                    listener.onImpact(projectile, impact);
                }
            });

    public static final RustedFabricEvent<Impact> AFTER_IMPACT =
            RustedFabricEvent.create(listeners -> (projectile, impact) -> {
                for (Impact listener : listeners) {
                    listener.onImpact(projectile, impact);
                }
            });

    public static final RustedFabricEvent<Removal> BEFORE_REMOVAL = removalEvent();
    public static final RustedFabricEvent<Removal> AFTER_REMOVAL = removalEvent();

    private ProjectileEvents() {
    }

    private static RustedFabricEvent<Updated> updateEvent() {
        return RustedFabricEvent.create(listeners -> (projectile, delta) -> {
            for (Updated listener : listeners) {
                listener.onUpdate(projectile, delta);
            }
        });
    }

    private static RustedFabricEvent<Explosion> explosionEvent() {
        return RustedFabricEvent.create(listeners -> projectile -> {
            for (Explosion listener : listeners) {
                listener.onExplosion(projectile);
            }
        });
    }

    private static RustedFabricEvent<Removal> removalEvent() {
        return RustedFabricEvent.create(listeners -> (projectile, reason) -> {
            for (Removal listener : listeners) {
                listener.onRemoval(projectile, reason);
            }
        });
    }

    public enum RemovalReason {
        REQUESTED,
        REMOVED_FROM_GAME
    }

    @FunctionalInterface
    public interface Created {
        void afterCreated(Projectile projectile, Unit sourceUnit);
    }

    @FunctionalInterface
    public interface Updated {
        void onUpdate(Projectile projectile, float delta);
    }

    @FunctionalInterface
    public interface Explosion {
        void onExplosion(Projectile projectile);
    }

    @FunctionalInterface
    public interface Impact {
        void onImpact(Projectile projectile, ProjectileImpactSnapshot impact);
    }

    @FunctionalInterface
    public interface Removal {
        void onRemoval(Projectile projectile, RemovalReason reason);
    }
}
