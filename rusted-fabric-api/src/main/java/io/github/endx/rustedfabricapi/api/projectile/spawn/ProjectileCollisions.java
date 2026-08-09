package io.github.endx.rustedfabricapi.api.projectile.spawn;

import io.github.endx.rustedfabricapi.impl.projectile.ProjectileCollisionRuntime;
import rustedwarfare.game.Projectile;

import java.util.Objects;

/** Applies the game's native unit-contact and hover-path terrain collision settings. */
public final class ProjectileCollisions {
    private ProjectileCollisions() { }

    public static void apply(Projectile projectile, ProjectileCollisionSpec spec) {
        Projectile checkedProjectile = Objects.requireNonNull(projectile, "projectile");
        ProjectileCollisionSpec checkedSpec = Objects.requireNonNull(spec, "spec");
        ProjectileCollisionRuntime.apply(checkedProjectile, checkedSpec);
    }

    /** Internal update hook that marks a native impact when an extended rule matches. */
    public static void applyExtendedCollision(Projectile projectile) {
        ProjectileCollisionRuntime.applyExtendedCollision(projectile);
    }

    public static void forget(Projectile projectile) {
        ProjectileCollisionRuntime.forget(projectile);
    }
}
