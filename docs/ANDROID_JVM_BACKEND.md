# Android desktop-JVM backend

## Objective

This backend follows the launcher model used by Android Java-game launchers: run the desktop Java
edition in a Loader-owned ARM64 JVM instead of modifying an Android game APK. Rusted Fabric starts
through `net.fabricmc.loader.impl.launch.knot.KnotClient`, so mappings, Mixins, API implementations,
and ordinary JVM mod bytecode can be shared with Windows.

The repository and Loader APK never contain Rusted Warfare game files. Users select their own Steam
installation with Android's system directory picker. Imported files remain in the Loader's private
storage and are not included in build artifacts.

## Implemented scaffold

- `android:jvm-launcher-core` validates a desktop installation and produces an immutable JVM launch
  plan without Android framework dependencies.
- The management APK exposes an **Experimental desktop JVM backend** screen.
- The importer accepts only `game-lib.jar`, `assets`, `res`, `libs`, and optional `font` from the
  selected root. Under `libs`, only JAR files are copied.
- Windows executables, DLLs, bundled desktop JVMs, saves, replays, screenshots, and existing mods are
  excluded.
- Imports are staged, validated, and atomically activated. A failed import leaves the previous
  verified copy intact.
- Size, file-count, nesting-depth, and path-containment limits protect the private import area.
- Launch planning fails closed until all runtime adapters report ready.

The local desktop installation can be checked without copying it:

```powershell
./gradlew :android:jvm-launcher-core:inspectDesktopGame `
  -PdesktopGameDir="D:\SteamLibrary\steamapps\common\Rusted Warfare"
```

## Runtime boundary

User-owned portable game data:

```text
game-lib.jar
assets/
res/
libs/*.jar
font/ (optional)
```

Loader-owned runtime components:

```text
ARM64 OpenJDK 13 runtime
libjli/libjvm host bridge
LWJGL2 display and OpenGL bridge
OpenAL ARM64 implementation
Android touch/keyboard/controller input bridge
ARM64 rocketConnector/libRocket implementation or replacement
optional Steam compatibility shim
```

The two areas must remain separate. In particular, a Windows DLL found in the selected directory is
never treated as an Android runtime dependency.

## Next execution milestones

1. Package or install a redistributable ARM64 Java 13 runtime and start a trivial Java main class
   through a small JNI/libjli host.
2. Create an Android `Surface`-backed LWJGL2 display bridge and render the Slick2D initialization
   screen.
3. Add OpenAL and Android input adapters.
4. Determine whether `rocketConnector` can be rebuilt for ARM64 or must be replaced, then reach the
   main menu with Steam integration disabled.
5. Export the existing desktop Fabric loader classpath into private storage and execute the generated
   `JvmLaunchPlan` through Knot.
6. Validate map rendering, audio, touch controls, saves, and multiplayer before enabling the launch
   button in release builds.

The native no-root APK backend remains available while these milestones are incomplete.
