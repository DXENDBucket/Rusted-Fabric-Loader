# Rusted Fabric Android Launcher

This standalone Android application runs user-imported Rusted Warfare desktop files through the
same Fabric/Knot runtime used on Windows. It does not use, patch, or bundle the Android game APK,
and it does not require root or an Xposed framework.

The launcher imports a desktop game ZIP or directory and a Linux/AArch64 Java 17 runtime into
application-private storage. Its ARM64 host supplies the Android Surface, LWJGL2/GL4ES, OpenAL,
input, and libRocket compatibility layers required by the desktop game.

User-editable content lives in the conventional shared `Internal storage/rustedWarfare` root:
`units`, `maps`, `javamods`, and editable `javamods-dev` workspaces. The launcher migrates older private content without overwriting
same-named shared files. An Android-only game file bridge reads these directories directly, so an
INI author can edit with MT Manager and use the game's hot reload without relying on unsupported
private-to-shared symbolic links. Java development workspaces use the in-game API's manual unit
and resource reload; recursive shared-storage polling is disabled on Android. Each content row on the
launcher has an **Open folder** action. Android 11 and newer require the user to grant the launcher
one-time **All files access**; this sideload-oriented permission is required because the embedded
desktop JVM uses ordinary file APIs rather than Android document-provider streams.

The main interface uses three persistent bottom destinations: **Launch**, **Mods & folders**, and
**Settings**. Setup and game launching stay on the first page; the content library imports and
lists INI mods, custom maps, ordinary Fabric Jars, and `.javamod` packages on the second page.
Environment replacement and diagnostics live on the third page. Maps and Java mods can be enabled
or disabled without rewriting them; switch changes are staged until the user confirms the dialog.
All managed content, including the bundled API, can be disabled, deleted, or replaced by importing
a newer Jar with the same mod ID. The APK supplies the same official Rusted Fabric API, Java Mod
Menu, and Performance Profiler selected by default on Windows, plus INI Essentials installed
disabled by default. A manual official-ID replacement is preserved when it is newer than the
bundled copy; an equal or newer
launcher bundle updates the managed official Jar while preserving its enabled/disabled state. None
of these Jars contains the game itself.

Installing a newer APK over the same application ID and signing identity is an in-place update.
Android preserves the imported desktop game, ARM64 Java runtime, user mods, maps, and launcher
settings; the launcher only refreshes its own official mods when the APK version advances. Release
builds therefore must use a greater `versionCode` and the same release signing key. Importing the
game or Java again is only needed when the user deliberately chooses their replacement actions.
The v0.1.0 public APK used the `.debug` application ID and the repository maintainer's existing
Android debug certificate. Current release builds temporarily retain those identities so that
v0.1.0 users receive a true in-place update, while the release build itself is non-debuggable.

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

Every APK includes the GPLv3 text, `THIRD_PARTY_NOTICES.md`, the repository's bilingual
`DISCLAIMER.md`, and a generated source offer containing
the exact Git revision and pinned FCL source release. `assembleRelease` rejects a dirty or unknown
Git revision. Release APKs must be published together with this repository's corresponding source
archive. See [`SOURCE_OFFER.md`](SOURCE_OFFER.md) for the packaged template.

On first launch, the user must acknowledge that Java mods execute with the game/Loader's
permissions. Importing a Java mod shows the warning again before opening the file picker. The
acceptance does not imply that Rusted Fabric reviewed or endorsed any third-party mod.

## Active modules

- `app`: Android setup UI, isolated JVM process, Surface/input bridge, and native runtime adapters.
- `android/jvm-launcher-core`: desktop-file import, managed content storage, and immutable JVM
  launch planning.
- `android/jvm-game-provider`: self-contained Fabric GameProvider launcher asset.
- `android/lwjgl2-compat`: Java-side LWJGL2 and touch compatibility classes.

Retired Android native-APK patching, Xposed hooks, Android DEX mods, and their mapping/API modules
are archived under `legacy/` and are not included in this build.
