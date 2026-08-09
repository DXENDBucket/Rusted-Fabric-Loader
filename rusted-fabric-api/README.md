# Rusted Fabric API

This is the only supported Rusted Fabric API module. It contains the public mod contracts, mapped
game-facing implementations, Fabric/Mixin integration, resources, tests, and named-to-official
remapping task in one Jar.

There are no separate common, desktop, or Android API variants. Windows and the Android desktop-JVM
port execute the same game, Fabric runtime, API Jar, and ordinary Java mod format.

Build and verify the official-runtime artifact with:

```powershell
./gradlew.bat :rusted-fabric-api:check :rusted-fabric-api:remapJarToOfficial
```

The source and binary artifacts target the game's Java 13 bytecode level. Development requires
JDK 17 and the ignored local game input described in the repository README.

## Projectile spawning and patterns

`api.projectile.spawn` creates a fully initialized native projectile from a
`CustomProjectileTemplate` with a unit, point, or fixed-direction target. Each real spawn passes
through cancellable `ProjectileSpawnEvents.BEFORE_SPAWN` and then
`ProjectileSpawnEvents.AFTER_SPAWN` after target binding and created effects are complete.
`ProjectileCollisionSpec` can be attached to a spawn or turret-pattern plan before the first
update. `ProjectileCollisions` deliberately exposes the native contact switches: unit contact uses
the projectile radius plus the candidate unit's collision radius, while terrain contact tests the
current tile against the game's `hover` path-blocking grid (including map bounds and blocking
terrain/buildings/objects). It does not mean every ground tile or every land/water transition.
`TerrainTransitionSpec` adds explicit `from -> to` ground-tile transitions, and
`UnitCollisionFilterSpec` opts into deterministic contact filtering by live ground/air/underwater
layer, native movement type, absolute height, runtime tags, and transported state. Extended matches
set the native target and impact latch rather than applying damage themselves.

`api.projectile.pattern` deterministically expands `single`, `fan`, `ring`, and `line` layouts.
Expansion is pure and testable; `ProjectilePatternEmitter.emit` then creates the real projectiles in
sequence order. A pattern is bounded to 1024 projectiles and never creates an intermediate parent
projectile. Gameplay mods must ensure the same pattern inputs and template are present on every
simulation peer.

`TurretProjectilePatternEvents.PLAN` runs after native turret projectile selection. A listener can
provide a `TurretProjectilePatternPlan`; the runtime then replaces the selected template and the
projectile creation/initialization calls without cancelling the enclosing firing method. Native
`onShoot`, muzzle effects, sound, recoil, counters, and post-fire state therefore retain their
original ordering and execute once per turret shot.

`ProjectileRenderEvents.DRAW` exposes the native shadow, body, on-top, and pre-UI projectile render
stages for client-side additions. It is a presentation event and must not drive synchronized
gameplay.

`CustomProjectileAssets` runs the native `[effect_*]` and `[decal_*]` loading passes for independent
projectile-definition formats, including resolution of nested custom-effect references. This keeps
format mods on a public API instead of exposing Mixin accessors.
