# Android desktop-JVM backend

## Objective

This backend follows the launcher model used by Android Java-game launchers: run the desktop Java
edition in a Loader-owned ARM64 JVM instead of modifying an Android game APK. Rusted Fabric starts
through `net.fabricmc.loader.impl.launch.knot.KnotClient`, so mappings, Mixins, API implementations,
and ordinary JVM mod bytecode can be shared with Windows.

The repository and Loader APK never contain Rusted Warfare game files. The normal user flow is to
compress the desktop game directory as ZIP on a computer, transfer it to the phone, and select it
with Android's system file picker. Direct directory selection remains available as an advanced
alternative. Imported files remain in the Loader's private storage and are not included in build
artifacts.

## Implemented scaffold

- `android:jvm-launcher-core` validates a desktop installation and produces an immutable JVM launch
  plan without Android framework dependencies.
- The management APK exposes an **Experimental desktop JVM backend** screen with ZIP and directory
  import actions.
- A ZIP may contain the game files at its root or inside one wrapper directory such as
  `Rusted Warfare/`. The correct root is detected from the game JAR, assets, resources, and required
  libraries rather than from the folder name.
- The importer accepts only `game-lib.jar`, `assets`, `res`, `libs`, and optional `font` from that
  root. Under `libs`, only JAR files are extracted.
- Windows executables, DLLs, bundled desktop JVMs, saves, replays, screenshots, and existing mods are
  excluded.
- Imports are staged, validated, and atomically activated. A failed import leaves the previous
  verified copy intact.
- ZIP path traversal, ambiguous names, duplicate outputs, multiple game roots, excessive expansion,
  excessive size/file count, and deep nesting are rejected. Path-containment checks also protect the
  private import area.
- Launch planning fails closed until all runtime adapters report ready.
- The Loader APK now contains its own small `arm64-v8a` JNI host. It loads
  `runtime/lib/server/libjvm.so` with `dlopen`, resolves `JNI_CreateJavaVM`, supplies the generated
  classpath/JVM options, enters the imported game working directory, updates the Android linker
  search path, and invokes a Java `main(String[])`. Recoverable native failures are returned as
  explicit codes and diagnostics.
- The APK packages a small Loader-owned self-test JAR and runs it through the JNI host in the
  separate `:desktop_jvm` Android process. The setup screen reports the external Java version and
  architecture written by that JAR. A fatal HotSpot or linker crash therefore does not terminate the
  management process. The isolated process exits after reporting its result because HotSpot does not
  support repeatedly creating fresh VMs in one process. Full game launch remains disabled until all
  platform adapters exist.
- A developer runtime-import action accepts an ARM64 OpenJDK 17 ZIP or its original TAR.XZ. It
  requires `release`, `lib/server/libjvm.so`, `lib/libjava.so`, and `lib/modules`, verifies Java 17,
  requires Linux/AArch64 release metadata, checks that both shared libraries are 64-bit
  little-endian AArch64 ELF, records the source ZIP SHA-256, and atomically installs it under
  Loader-private storage. Darwin/Mach-O ARM64 archives are explicitly rejected.

The local desktop installation can be checked without copying it:

```powershell
./gradlew :android:jvm-launcher-core:inspectDesktopGame `
  -PdesktopGameDir="D:\SteamLibrary\steamapps\common\Rusted Warfare"
```

An extracted Android JVM can be checked separately before developer import:

```powershell
./gradlew :android:jvm-launcher-core:inspectJvmRuntime `
  -PjvmRuntimeDir="C:\path\to\extracted-jre17-arm64"
```

The probe deliberately requires `OS_NAME=Linux`, `OS_ARCH=aarch64` (or `arm64`), and AArch64 ELF
builds of both `libjvm.so` and `libjava.so`. An archive labeled ARM64 but containing Darwin/Mach-O
libraries is rejected even if its Java version is otherwise correct.

The complete bounded archive importer can also be exercised on a workstation:

```powershell
./gradlew :android:jvm-launcher-core:importJvmRuntimeArchive `
  -PjvmRuntimeArchive="C:\path\to\jre17-arm64.tar.xz" `
  -PjvmRuntimeOutput="C:\path\to\empty-output"
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
ARM64 OpenJDK 17 runtime (runs the desktop game's Java 13 bytecode)
Loader-owned JNI/libjvm host bridge
LWJGL2 display and OpenGL bridge
OpenAL ARM64 implementation
Android touch/keyboard/controller input bridge
ARM64 rocketConnector/libRocket implementation or replacement
optional Steam compatibility shim
```

The two areas must remain separate. In particular, a Windows DLL found in the selected directory is
never treated as an Android runtime dependency.

## Next execution milestones

1. Run the implemented isolated-process self-test on a physical ARM64 device and document any
   remaining Android linker-namespace adjustments required by the chosen OpenJDK build.
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

## Build prerequisites

The native host is built with Android NDK `27.2.12479018` and CMake `3.22.1`. These are SDK tooling
only and are not committed. A Java runtime is also never committed or silently downloaded: a release
runtime needs a reviewed source/license, a pinned SHA-256 catalog entry, and a reproducible packaging
record before it can become a one-tap consumer download.
