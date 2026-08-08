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
