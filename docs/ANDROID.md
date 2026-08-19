# Android launcher

## Supported architecture

Android support is a no-root port of the desktop Java game. The user imports their own Rusted
Warfare desktop directory or ZIP and a Linux/AArch64 Java 17 runtime. The launcher starts the same
Fabric/Knot GameProvider, API, Mixins, and Java mods used on Windows.

The launcher does not inspect, patch, sign, install, or bundle an Android game APK. Xposed and
LSPosed are not used. The retired native-APK experiments and their Android-only mappings are frozen
under `legacy/`.

## Active modules

- `android/launcher/app`: setup UI, isolated JVM process, Android Surface, JNI bridges, runtime
  assets, and the final APK.
- `android/jvm-launcher-core`: bounded desktop/content import, Java runtime validation, content
  enable state, and launch plans.
- `android/jvm-game-provider`: self-contained Fabric/Knot launcher Jar without game payloads.
- `android/lwjgl2-compat`: Java-side LWJGL2, input, memory, and touch compatibility.
- `rusted-fabric-api`: the sole API and Mixin implementation used by both Windows and Android.

See [ANDROID_JVM_BACKEND.md](ANDROID_JVM_BACKEND.md) for the runtime boundary and implementation
details.

## User-owned inputs

Portable desktop game data accepted by the importer:

```text
game-lib.jar
assets/
res/
libs/*.jar
font/ (optional)
```

The importer rejects executables, DLLs, saves, existing mods, path traversal, ambiguous roots, and
oversized archives. Saves, replays, screenshots, cache, game binaries, and Java are created or kept
privately after validation. User-editable content is exposed under the shared
`Internal storage/rustedWarfare` root. The Android JVM receives that root explicitly; it does not
use private-to-shared symbolic links, which Android's emulated-storage layer cannot traverse.

Once the game is imported, the launcher manages new content independently of that bounded game
import:

- INI mods accept `.ini`, `.rwmod`, and ZIP inputs under shared `rustedWarfare/units`; their enable
  state remains in the game's native mod menu.
- Maps accept `.tmx` files and ZIP inputs. Disable moves an item between `mods/maps` and
  `mods-disabled/maps` without altering its contents.
- Java mods accept Fabric Jars and `.javamod` packages after validating `fabric.mod.json`. Disable
  moves them between `javamods` and `javamods-disabled`, taking effect on the next launch.
- Rusted Fabric API, Java Mod Menu, Vulkan Mod, and Performance Profiler are provisioned enabled
  from Loader-owned APK assets. INI Essentials is also provisioned and listed, but starts disabled. Updates preserve later user
  enable choices unless a manually imported same-ID Jar takes precedence.
- Enable switches are committed only by the dialog's confirmation button; cancellation discards
  pending switch changes. Every managed item, including the bundled API, can be disabled or deleted.
  Importing a newer Jar with the same official mod ID replaces the bundled copy, and later launcher
  starts preserve that manual replacement.

All archive content uses bounded expansion and path-containment checks. User content stays in the
shared `rustedWarfare` directory and is never copied into an APK or repository artifact. The three
active directories have launcher **Open folder** buttons for the system file manager or compatible
tools such as MT Manager. Android 11+ requires one-time all-files access so the desktop JVM can use
ordinary file APIs and observe INI edits during hot reload.

The Java importer accepts ZIP or TAR.XZ and requires Linux/AArch64 Java 17 metadata plus valid
AArch64 ELF builds of `libjvm.so` and `libjava.so`.

## Build

Requirements:

- JDK 17
- Android SDK platform/build tools 35
- NDK 27.2.12479018
- CMake 3.22.1

```powershell
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-17'
./gradlew.bat -p android/launcher :app:assembleDebug :app:verifyNoGamePayload
```

The output is `android/launcher/app/build/outputs/apk/debug/app-debug.apk`. The payload audit checks
that the launcher contains its required JVM/render components and three Loader-owned official mod
Jars, while containing no Rusted Warfare classes or game files.

## Testing boundary

Build success verifies Java/native compilation and packaging. Physical-device testing is still
required for touch behavior, UI layout, audio, saves, maps, multiplayer, background/foreground
transitions, device cutouts, and long-running stability.

All test game files and imported runtimes must remain in ignored local storage and must never be
committed or published with the launcher.
