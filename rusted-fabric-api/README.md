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

`api.projectile.pattern` deterministically expands `single`, `fan`, `ring`, and `line` layouts.
Expansion is pure and testable; `ProjectilePatternEmitter.emit` then creates the real projectiles in
sequence order. A pattern is bounded to 1024 projectiles and never creates an intermediate parent
projectile. Gameplay mods must ensure the same pattern inputs and template are present on every
simulation peer.
