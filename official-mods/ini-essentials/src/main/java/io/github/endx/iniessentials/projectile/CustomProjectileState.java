package io.github.endx.iniessentials.projectile;

import io.github.endx.rustedfabricapi.api.projectile.spawn.ProjectileSpawnSpec;
import rustedwarfare.custom.CustomUnit;
import rustedwarfare.game.Projectile;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/** Runtime data owned by one independent CustomProjectile instance. */
final class CustomProjectileState {
    final CustomProjectileDefinitions.Definition definition;
    final Projectile projectile;
    final CustomUnit source;
    final ProjectileSpawnSpec spawnSpec;
    final float originX;
    final float originY;
    private final CustomProjectileSpawnRequest.Resolved spawnOverrides;
    private final Map<String, Float> memory = new LinkedHashMap<String, Float>();
    boolean impactActionRan;
    boolean removalActionRan;
    Float requestedOffsetX;
    Float requestedOffsetY;

    CustomProjectileState(CustomProjectileDefinitions.Definition definition,
                          Projectile projectile, CustomUnit source,
                          ProjectileSpawnSpec spawnSpec,
                          CustomProjectileSpawnRequest.Resolved spawnOverrides) {
        this.definition = definition;
        this.projectile = projectile;
        this.source = source;
        this.spawnSpec = spawnSpec;
        this.spawnOverrides = spawnOverrides;
        // AFTER_SPAWN runs after the native template's created-effects pass, so these are the
        // actual initial coordinates seen by gameplay rather than only the requested spec origin.
        this.originX = projectile.x;
        this.originY = projectile.y;
    }

    Float spawnOverride(String name) {
        return spawnOverrides != null ? spawnOverrides.value(name) : null;
    }

    float memory(String name) {
        Float value = memory.get(name.toLowerCase(Locale.ROOT));
        return value != null ? value.floatValue() : 0.0F;
    }

    void setMemory(String name, float value) {
        if (!Float.isFinite(value)) throw new IllegalArgumentException("memory value must be finite");
        memory.put(name.toLowerCase(Locale.ROOT), value);
    }
}
