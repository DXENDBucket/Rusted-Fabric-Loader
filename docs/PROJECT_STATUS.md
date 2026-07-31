# Project status

## Active targets

Rusted Fabric Loader has two connected active targets:

1. the Windows desktop Loader, which is the working reference runtime and ordinary Jar mod target;
2. the Android PC-edition port, which imports user-owned Steam files and runs the same Fabric/Knot
   stack in a Loader-owned Linux/AArch64 JVM.

Current baseline:

- Loader/API version: `0.1.0` (experimental)
- mappings: `1.1 FINAL`
- development JDK: `17`
- emitted game/mod bytecode: Java `13`
- mod package: ordinary Fabric-style Jar discovered from `javamods/`

The maintained API path is `rusted-fabric-api` plus `rusted-fabric-api-desktop`. The desktop API Jar
embeds the platform-neutral module, so the Windows runtime installs one API Jar. This same
Fabric/Knot and desktop-game foundation is what the Android PC port intends to execute.

The Android native-APK paths are frozen: local APK patching, DEX weaving, and Xposed game hooks are
not the current direction. Their source remains as historical evidence. Active Android work belongs
to `android:jvm-launcher-core`, the PC import UI, and the isolated JNI/HotSpot host.

The PC-port scaffold can currently import and validate the desktop game, import a Linux/AArch64
Java 17 runtime, create HotSpot in a separate Android process, and execute a Loader-owned smoke-test
Jar. Full game launch is intentionally disabled until four runtime adapters exist:

- an Android Surface-backed LWJGL2 renderer bridge;
- OpenAL for ARM64 Android;
- Android touch/keyboard/controller input through the JInput/LWJGL boundary;
- an ARM64 `rocketConnector` implementation or compatible replacement.

## Verification boundary

The repository can mechanically verify compilation, mappings, named-to-official remapping, API
contracts, example mods, and distribution layout:

```bat
set "JAVA_HOME=C:\Program Files\Java\jdk-17"
gradlew.bat verifyDistribution :rusted-fabric-api-desktop:check :example-mod:build
gradlew.bat :android:jvm-launcher-core:check
gradlew.bat -p android/xposed :module:assembleDebug :module:verifyNoGamePayload
```

These checks do not replace launching the actual game. Changes touching Mixin injection points,
rendering, synchronized commands, native status effects, saves, or multiplayer behavior still need
an eventual desktop runtime test.

## Local-only inputs

The repository intentionally does not distribute Rusted Warfare or user installations. The
following stay outside Git:

- `libs/game-lib.jar`
- every APK, DEX, patched game, desktop-game ZIP, or extracted Android runtime under `libs/`
- generated `game-lib-named.jar` and all Gradle build outputs
- `local.properties` and IDE/workspace state
- external installations such as `D:\SteamLibrary\steamapps\common\Rusted Warfare`

Only mappings, code, tests, documentation, and evidence produced without bundling the game belong
in version control.

## Repository layout

| Path | Status | Purpose |
| --- | --- | --- |
| `src/` | maintained | GameProvider, tooling, mappings, and Loader runtime |
| `rusted-fabric-api/` | maintained | platform-neutral public contracts |
| `rusted-fabric-api-desktop/` | maintained | Windows mapped API and Mixin implementation |
| `example-mod/` | maintained | broad desktop API and remapping example |
| `portable-example-mod/` | compatibility fixture | shared-source packaging example |
| `docs/` | maintained | API, mappings, multiplayer, and historical design notes |
| `report/mapping-evidence/` | maintained evidence | mapping provenance and coverage records |
| `android/jvm-launcher-core/` | active experimental | PC import validation, JVM probing, and launch plans |
| `android/xposed/module/` | active host plus legacy code | manager UI and JNI JVM host; old Xposed hooks are frozen |
| `rusted-fabric-api-android/` | transitional | currently packaged by the manager; not the PC game's Fabric API |
| other `android/` paths | frozen/reference | native APK patcher, DEX weaving, and Xposed experiments |

## Current limitations

- The `0.1.x` API is experimental; mapping corrections may still require source changes.
- The mapping table names only known symbols. Unmapped game members retain their official names.
- Gameplay-changing callbacks must be deterministic on every multiplayer participant unless an API
  explicitly documents a server-only or optional-client contract.
- Build success proves API and remapping consistency, not actual in-game behavior.
- The Android PC port does not yet reach the game main class because its platform adapters are
  deliberately fail-closed.
