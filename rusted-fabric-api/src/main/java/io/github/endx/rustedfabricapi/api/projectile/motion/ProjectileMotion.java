package io.github.endx.rustedfabricapi.api.projectile.motion;

import io.github.endx.rustedfabricapi.impl.projectile.ProjectileMotionRuntime;
import rustedwarfare.game.Projectile;

import java.util.Objects;

/** Per-projectile motion controls applied before the native projectile update. */
public final class ProjectileMotion {
    private ProjectileMotion() { }

    /** Overrides the guided forward speed for this projectile. */
    public static void setFlightSpeed(Projectile projectile, float value) {
        Objects.requireNonNull(projectile, "projectile").speed = finite(value, "flightSpeed");
    }

    /** Sets the projectile's independent world-space X/Y velocity components. */
    public static void setVelocity(Projectile projectile, float dx, float dy) {
        Projectile checked = Objects.requireNonNull(projectile, "projectile");
        checked.initialUnguidedSpeedX = finite(dx, "dx");
        checked.initialUnguidedSpeedY = finite(dy, "dy");
    }

    /** Places the projectile at an exact world-space position. */
    public static void setPosition(Projectile projectile, float x, float y) {
        Projectile checked = Objects.requireNonNull(projectile, "projectile");
        checked.x = finite(x, "x");
        checked.y = finite(y, "y");
    }

    /** Overrides native turnSpeed and turnSpeedWhenNear for only this projectile. */
    public static void setTurnSpeed(Projectile projectile, float value) {
        ProjectileMotionRuntime.setTurnSpeed(
                Objects.requireNonNull(projectile, "projectile"), finite(value, "turnSpeed"));
    }

    /** Removes the per-projectile turn override and resumes the template's native value. */
    public static void clearTurnSpeed(Projectile projectile) {
        ProjectileMotionRuntime.clearTurnSpeed(Objects.requireNonNull(projectile, "projectile"));
    }

    private static float finite(float value, String name) {
        if (!Float.isFinite(value)) throw new IllegalArgumentException(name + " must be finite");
        return value;
    }
}
