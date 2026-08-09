package io.github.endx.rustedfabricapi.api.projectile.spawn;

/** How an API-spawned projectile obtains its runtime target. */
public enum ProjectileAimMode {
    /** Tracks one native unit target. */
    UNIT,
    /** Uses one fixed world-space target point. */
    POINT,
    /** Uses a direction and a distant fixed target, without requiring a target unit. */
    DIRECTION
}
