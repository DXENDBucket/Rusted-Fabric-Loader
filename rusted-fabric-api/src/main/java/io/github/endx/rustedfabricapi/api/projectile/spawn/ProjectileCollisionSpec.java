package io.github.endx.rustedfabricapi.api.projectile.spawn;

/** Immutable native contact-collision settings applied before a projectile's first update. */
public final class ProjectileCollisionSpec {
    private static final ProjectileCollisionSpec NONE = new ProjectileCollisionSpec(
            false, false, 0.0F, TerrainTransitionSpec.none(),
            UnitCollisionFilterSpec.nativeGroundOnly());

    private final boolean collideWithUnits;
    private final boolean collideWithTerrain;
    private final float contactRadius;
    private final TerrainTransitionSpec terrainTransition;
    private final UnitCollisionFilterSpec unitFilter;

    private ProjectileCollisionSpec(boolean units, boolean terrain, float radius,
                                    TerrainTransitionSpec transition,
                                    UnitCollisionFilterSpec filter) {
        if (!Float.isFinite(radius) || radius < 0.0F) {
            throw new IllegalArgumentException("contactRadius must be finite and >= 0");
        }
        collideWithUnits = units;
        collideWithTerrain = terrain;
        contactRadius = radius;
        terrainTransition = java.util.Objects.requireNonNull(transition, "terrainTransition");
        unitFilter = java.util.Objects.requireNonNull(filter, "unitFilter");
    }

    public static ProjectileCollisionSpec none() { return NONE; }

    public static ProjectileCollisionSpec of(boolean units, boolean terrain, float radius) {
        if (!units && !terrain && radius == 0.0F) return NONE;
        return new ProjectileCollisionSpec(units, terrain, radius,
                TerrainTransitionSpec.none(), UnitCollisionFilterSpec.nativeGroundOnly());
    }

    public static ProjectileCollisionSpec of(boolean units, boolean terrain, float radius,
                                             TerrainTransitionSpec transition) {
        TerrainTransitionSpec checked = java.util.Objects.requireNonNull(
                transition, "terrainTransition");
        if (!units && !terrain && radius == 0.0F && !checked.enabled()) return NONE;
        return new ProjectileCollisionSpec(units, terrain, radius, checked,
                UnitCollisionFilterSpec.nativeGroundOnly());
    }

    public static ProjectileCollisionSpec of(boolean units, boolean terrain, float radius,
                                             TerrainTransitionSpec transition,
                                             UnitCollisionFilterSpec filter) {
        TerrainTransitionSpec checkedTransition = java.util.Objects.requireNonNull(
                transition, "terrainTransition");
        UnitCollisionFilterSpec checkedFilter = java.util.Objects.requireNonNull(
                filter, "unitFilter");
        if (!units && !terrain && radius == 0.0F && !checkedTransition.enabled()
                && !checkedFilter.enabled()) return NONE;
        return new ProjectileCollisionSpec(units, terrain, radius,
                checkedTransition, checkedFilter);
    }

    public boolean collideWithUnits() { return collideWithUnits; }
    public boolean collideWithTerrain() { return collideWithTerrain; }
    public float contactRadius() { return contactRadius; }
    public TerrainTransitionSpec terrainTransition() { return terrainTransition; }
    public UnitCollisionFilterSpec unitFilter() { return unitFilter; }
}
