package io.github.endx.rustedfabricapi.impl.projectile;

import io.github.endx.rustedfabricapi.api.map.Maps;
import io.github.endx.rustedfabricapi.api.projectile.spawn.ProjectileCollisionSpec;
import io.github.endx.rustedfabricapi.api.projectile.spawn.TerrainKind;
import io.github.endx.rustedfabricapi.api.projectile.spawn.TerrainTransitionSpec;
import io.github.endx.rustedfabricapi.api.projectile.spawn.UnitCollisionFilterSpec;
import io.github.endx.rustedfabricapi.api.projectile.spawn.UnitCollisionLayer;
import io.github.endx.rustedfabricapi.api.unit.tag.UnitTags;
import io.github.endx.rustedfabricapi.mixin.accessor.ProjectileCollisionAccessor;
import io.github.endx.rustedfabricapi.mixin.accessor.UnitCollisionAccessor;
import rustedwarfare.game.Projectile;
import rustedwarfare.map.MapEngine;
import rustedwarfare.map.MapTile;
import rustedwarfare.unit.Unit;
import rustedwarfare.util.UnitArrayList;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

/** Internal native collision state and mixin bridge. */
public final class ProjectileCollisionRuntime {
    private static final Map<Projectile, CollisionState> EXTENDED =
            Collections.synchronizedMap(new WeakHashMap<Projectile, CollisionState>());

    private ProjectileCollisionRuntime() { }

    public static void apply(Projectile projectile, ProjectileCollisionSpec spec) {
        projectile.collideWithUnits = spec.collideWithUnits() && !spec.unitFilter().enabled();
        projectile.collideWithTerrain = spec.collideWithTerrain();
        projectile.contactCollisionRadius = spec.contactRadius();
        if (spec.terrainTransition().enabled()
                || (spec.collideWithUnits() && spec.unitFilter().enabled())) {
            EXTENDED.put(projectile, new CollisionState(spec, sample(projectile)));
        } else {
            EXTENDED.remove(projectile);
        }
    }

    public static void applyExtendedCollision(Projectile projectile) {
        CollisionState state = EXTENDED.get(projectile);
        ProjectileCollisionAccessor access =
                (ProjectileCollisionAccessor) (Object) projectile;
        if (state == null || access.rustedfabricapi$isImpactTriggered()) return;
        if (matchesUnit(projectile, state.spec)) return;
        TerrainTransitionSpec transition = state.spec.terrainTransition();
        if (!transition.enabled()) return;
        TerrainSample current = sample(projectile);
        TerrainSample previous = state.previous;
        state.previous = current;
        if (previous != null && current != null
                && (previous.tileX != current.tileX || previous.tileY != current.tileY)
                && matches(transition.from(), previous)
                && matches(transition.to(), current)) {
            EXTENDED.remove(projectile);
            projectile.targetUnit = null;
            access.rustedfabricapi$setImpactTriggered(true);
        }
    }

    public static void forget(Projectile projectile) {
        EXTENDED.remove(projectile);
    }

    private static boolean matchesUnit(Projectile projectile, ProjectileCollisionSpec spec) {
        if (!spec.collideWithUnits() || !spec.unitFilter().enabled()) return false;
        UnitCollisionFilterSpec filter = spec.unitFilter();
        UnitArrayList units = Unit.allUnits;
        Unit[] backing = units.getBackingArray();
        int size = Math.min(units.size(), backing.length);
        for (int index = 0; index < size; index++) {
            Unit unit = backing[index];
            if (unit == projectile.sourceUnit || !matchesUnitFilter(unit, filter)) continue;
            float radius = spec.contactRadius()
                    + ((UnitCollisionAccessor) (Object) unit)
                            .rustedfabricapi$getCollisionRadius();
            float dx = projectile.x - unit.x;
            float dy = projectile.y - unit.y;
            if (dx * dx + dy * dy >= radius * radius) continue;
            EXTENDED.remove(projectile);
            projectile.targetUnit = unit;
            ((ProjectileCollisionAccessor) (Object) projectile)
                    .rustedfabricapi$setImpactTriggered(true);
            return true;
        }
        return false;
    }

    private static boolean matchesUnitFilter(Unit unit, UnitCollisionFilterSpec filter) {
        if (unit == null || unit.dead || unit.removed
                || (!filter.includeTransported() && unit.transportingUnit != null)
                || unit.height < filter.minHeight() || unit.height > filter.maxHeight()) {
            return false;
        }
        UnitCollisionLayer layer = unit.isUnderwater()
                ? UnitCollisionLayer.UNDERWATER
                : unit.isFlying() ? UnitCollisionLayer.AIR : UnitCollisionLayer.GROUND;
        return filter.layers().contains(layer)
                && (filter.movementTypes().isEmpty()
                    || filter.movementTypes().contains(unit.getMovementType()))
                && UnitTags.containsAll(UnitTags.runtime(unit), filter.requiredTags())
                && !UnitTags.anyMatches(UnitTags.runtime(unit), filter.forbiddenTags());
    }

    private static TerrainSample sample(Projectile projectile) {
        MapEngine map = Maps.currentOrNull();
        if (map == null) return null;
        int tileX = (int) Math.floor(map.worldToTileX(projectile.x));
        int tileY = (int) Math.floor(map.worldToTileY(projectile.y));
        if (!map.isInMapBounds(tileX, tileY)) {
            return TerrainSample.outOfBounds(tileX, tileY);
        }
        return new TerrainSample(tileX, tileY,
                map.getTileAtTilePositionSafe(tileX, tileY));
    }

    private static boolean matches(TerrainKind kind, TerrainSample tile) {
        switch (kind) {
            case ANY: return true;
            case OUT_OF_BOUNDS: return tile.outOfBounds;
            case WATER: return !tile.outOfBounds && tile.water && !tile.waterBridge;
            case WATER_BRIDGE: return !tile.outOfBounds && tile.waterBridge;
            case LAVA: return !tile.outOfBounds && tile.lava;
            case CLIFF: return !tile.outOfBounds && tile.cliff;
            case RESOURCE_POOL: return !tile.outOfBounds && tile.resourcePool;
            case LAND:
                return !tile.outOfBounds && !tile.water && !tile.waterBridge
                        && !tile.lava && !tile.cliff && !tile.resourcePool;
            default: throw new AssertionError(kind);
        }
    }

    private static final class CollisionState {
        final ProjectileCollisionSpec spec;
        TerrainSample previous;

        CollisionState(ProjectileCollisionSpec spec, TerrainSample previous) {
            this.spec = spec;
            this.previous = previous;
        }
    }

    private static final class TerrainSample {
        final int tileX, tileY;
        final boolean outOfBounds, water, waterBridge, lava, cliff, resourcePool;

        TerrainSample(int tileX, int tileY, MapTile tile) {
            this(tileX, tileY, false,
                    tile != null && tile.isWater,
                    tile != null && tile.isWaterBridge,
                    tile != null && tile.isLava,
                    tile != null && tile.isCliff,
                    tile != null && tile.isResourcePool);
        }

        private TerrainSample(int tileX, int tileY, boolean outOfBounds,
                              boolean water, boolean waterBridge, boolean lava,
                              boolean cliff, boolean resourcePool) {
            this.tileX = tileX;
            this.tileY = tileY;
            this.outOfBounds = outOfBounds;
            this.water = water;
            this.waterBridge = waterBridge;
            this.lava = lava;
            this.cliff = cliff;
            this.resourcePool = resourcePool;
        }

        static TerrainSample outOfBounds(int tileX, int tileY) {
            return new TerrainSample(tileX, tileY, true,
                    false, false, false, false, false);
        }
    }
}
