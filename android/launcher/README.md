# Rusted Fabric Android Launcher

This standalone Android application runs user-imported Rusted Warfare desktop files through the
same Fabric/Knot runtime used on Windows. It does not use, patch, or bundle the Android game APK,
and it does not require root or an Xposed framework.

The launcher imports a desktop game ZIP or directory and a Linux/AArch64 Java 17 runtime into
application-private storage. Its ARM64 host supplies the Android Surface, LWJGL2/GL4ES, OpenAL,
input, and libRocket compatibility layers required by the desktop game.

After setup, the same screen becomes a persistent content library. It imports and lists INI mods,
custom maps, ordinary Fabric Jars, and `.javamod` packages. Maps and Java mods can be enabled or
disabled without rewriting them; switch changes are staged until the user confirms the dialog.
All managed content, including the bundled API, can be disabled, deleted, or replaced by importing
a newer Jar with the same mod ID. The APK supplies the same official Rusted Fabric API and Java Mod
Menu selected by default on Windows, plus INI Essentials installed disabled by default. A manual
official-ID replacement takes precedence over the bundled copy. None of these Jars contains the game
itself.

The application contains only Loader-owned code and reviewed third-party runtime components.
`verifyNoGamePayload` rejects APKs that contain Rusted Warfare classes or game payloads.

The active launcher has no compatibility path for Xposed, APK patching, or Android DEX mods.
Those retired implementations are not compiled, packaged, detected, or offered in its interface.

## Build

Requirements:

- JDK 17
- Android SDK platform 35 and Build Tools 35.0.0
- NDK 27.2.12479018
- CMake 3.22.1

Create an ignored `local.properties` with `sdk.dir=...`, then run from the repository root:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-17'
./gradlew.bat -p android/launcher :app:assembleDebug :app:verifyNoGamePayload
```

The debug APK is written to `android/launcher/app/build/outputs/apk/debug/app-debug.apk`.

## License and source

The Android launcher distribution is licensed under GNU GPL version 3. Its full license is in
[`LICENSE`](LICENSE). The reusable Loader, API, and JVM support modules outside this directory keep
their Apache-2.0 licenses; Apache-2.0 code is included in the Android APK under the GPLv3
distribution terms.

Every APK includes the GPLv3 text, `THIRD_PARTY_NOTICES.md`, and a generated source offer containing
the exact Git revision and pinned FCL source release. `assembleRelease` rejects a dirty or unknown
Git revision. Release APKs must be published together with this repository's corresponding source
archive. See [`SOURCE_OFFER.md`](SOURCE_OFFER.md) for the packaged template.

## Active modules

- `app`: Android setup UI, isolated JVM process, Surface/input bridge, and native runtime adapters.
- `android/jvm-launcher-core`: desktop-file import, managed content storage, and immutable JVM
  launch planning.
- `android/jvm-game-provider`: self-contained Fabric GameProvider launcher asset.
- `android/lwjgl2-compat`: Java-side LWJGL2 and touch compatibility classes.

Retired Android native-APK patching, Xposed hooks, Android DEX mods, and their mapping/API modules
are archived under `legacy/` and are not included in this build.
