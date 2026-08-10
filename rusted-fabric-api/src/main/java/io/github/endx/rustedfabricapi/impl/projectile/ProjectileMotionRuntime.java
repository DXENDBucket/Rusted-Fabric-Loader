package io.github.endx.rustedfabricapi.impl.projectile;

import rustedwarfare.game.Projectile;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

/** Internal storage used by the projectile update mixin. */
public final class ProjectileMotionRuntime {
    private static final Map<Projectile, Float> TURN_SPEEDS = Collections.synchronizedMap(
            new WeakHashMap<Projectile, Float>());

    private ProjectileMotionRuntime() { }

    public static void setTurnSpeed(Projectile projectile, float value) {
        TURN_SPEEDS.put(projectile, value);
    }

    public static void clearTurnSpeed(Projectile projectile) {
        TURN_SPEEDS.remove(projectile);
    }

    public static float resolveTurnSpeed(Projectile projectile, float nativeValue) {
        Float value = TURN_SPEEDS.get(projectile);
        return value != null ? value.floatValue() : nativeValue;
    }

    public static void forget(Projectile projectile) {
        TURN_SPEEDS.remove(projectile);
    }
}
