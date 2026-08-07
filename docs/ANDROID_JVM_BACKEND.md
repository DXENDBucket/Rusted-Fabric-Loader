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
- The Loader now packages a separate ARM64 Surface/EGL bridge. A landscape `SurfaceView` lives in
  the same `:desktop_jvm` process that will host HotSpot, registers its `ANativeWindow` through a
  reference-counted native boundary, creates an OpenGL ES context, clears the real display surface,
  and swaps buffers. The setup screen exposes this as a renderer self-test. This verifies the
  Android window/EGL foundation only; it does not make the LWJGL2 adapter launch-ready.
- The JVM host preloads the imported runtime's own `libfreetype.so` before `libfontmanager.so`.
  Neither library is copied into the Loader APK or repository.

The local desktop installation can be checked without copying it:

```powershell
./gradlew :android:jvm-launcher-core:inspectDesktopGame `
  -PdesktopGameDir="D:\SteamLibrary\steamapps\common\Rusted Warfare"
```

A user-owned desktop ZIP can be exercised through the same bounded importer used by the Android
setup screen. The output directory must be empty and should remain under an ignored local build
directory when it contains game files:

```powershell
./gradlew :android:jvm-launcher-core:importDesktopGameArchive `
  -PdesktopGameArchive="build\android-jvm-test\rusted-warfare.zip" `
  -PdesktopGameOutput="build\android-jvm-test\extracted-game"
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

## Physical-device verification

The JVM boundary passed its first physical-device test on 2026-08-07 using an ARM64 `PKG110`
running Android 16:

- the user-owned desktop ZIP imported and validated 1,222 files (108 MB);
- the Pojav Android OpenJDK 17 archive imported and validated 144 files (169 MB);
- `librustedfabric_jvmhost.so` loaded directly from the APK with `extractNativeLibs=false`;
- the isolated `:desktop_jvm` process loaded the private `libjvm.so`, created HotSpot, executed the
  Loader-owned JAR, and returned `rusted-fabric-jvm-smoke=ok`, Java `17-internal`, Linux/AArch64;
- Android SELinux granted execution of the imported JVM libraries from app-private storage.

The same device subsequently passed the first real renderer test. The `:desktop_jvm` process
registered a 2376x1080 Android Surface, created Android META-EGL 1.5 / OpenGL ES 3.2 on an Adreno
750, cleared it, and completed `eglSwapBuffers`. A second HotSpot run confirmed that the imported
runtime already supplies both `libfreetype.so` and `libfontmanager.so`; loading FreeType first fixes
the dependency warning. The local full logs and user-owned test inputs remain under the Git-ignored
`build/android-jvm-test/` directory.

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

1. Implement the LWJGL2 Java/JNI adapter on top of the verified `ANativeWindow`/EGL boundary and
   render the Slick2D initialization screen. The existing Linux/X11 LWJGL2 classes cannot be used
   unchanged on Android.
2. Export the existing desktop Fabric Loader classpath into private storage and execute the
   generated `JvmLaunchPlan` through Knot.
3. Add OpenAL and Android input adapters.
4. Determine whether `rocketConnector` can be rebuilt for ARM64 or must be replaced, then reach the
   main menu with Steam integration disabled.
5. Validate map rendering, audio, touch controls, saves, and multiplayer before enabling the launch
   button in release builds.

The older native no-root APK backend remains in source history for reference, but it is frozen and
is not the fallback implementation for this direction.

## Build prerequisites

The native host is built with Android NDK `27.2.12479018` and CMake `3.22.1`. These are SDK tooling
only and are not committed. A Java runtime is also never committed or silently downloaded: a release
runtime needs a reviewed source/license, a pinned SHA-256 catalog entry, and a reproducible packaging
record before it can become a one-tap consumer download.
