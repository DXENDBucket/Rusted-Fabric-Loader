package io.github.endx.rustedfabricapi.api.projectile.spawn;

/** Stable ground-tile predicates for projectile terrain-transition collision. */
public enum TerrainKind {
    ANY,
    LAND,
    WATER,
    WATER_BRIDGE,
    LAVA,
    CLIFF,
    RESOURCE_POOL,
    OUT_OF_BOUNDS
}
