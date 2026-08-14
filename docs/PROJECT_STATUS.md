# Project status

## Active targets

Rusted Fabric has one game/runtime architecture on two host platforms:

1. Windows launches the desktop game through Fabric/Knot directly.
2. Android imports the user's desktop files and runs that same Fabric/Knot stack in a private
   Linux/AArch64 JVM.

Both hosts use ordinary Java/Fabric mods and the single `rusted-fabric-api` module. The discontinued
native Android APK, Xposed, DEX-mod, and local-patcher architectures are archived under `legacy/`
and are not part of active builds.

Current baseline:

- Loader version: `0.4.3`; API version: `0.3.8` (experimental)
- mappings: `1.1 FINAL`
- development JDK: 17
- emitted game/mod bytecode: Java 13
- mod package: ordinary Fabric-style Jar discovered from `javamods/`

## Android PC-port status

The no-root launcher can import and validate a desktop installation and Linux/AArch64 Java 17,
start Fabric and the desktop game in an isolated HotSpot process, render through an Android Surface
using LWJGL2/GL4ES, translate touch input, and reach/play the game on a physical ARM64 phone.

Physical-device testing has established a playable baseline with mobile-style UI and controls,
audio, saves, custom maps, and multiplayer exercised on the current test device. Further Android
work is regression-driven while API development resumes. The launcher itself never contains a game
APK, desktop game files, or a Java runtime supplied by the user.

## Verification

Use JDK 17 for Gradle; JDK 25 is not supported by the current Gradle/Groovy toolchain.

```bat
set "JAVA_HOME=C:\Program Files\Java\jdk-17"
gradlew.bat check verifyDistribution :official-mods:example-mod:build
gradlew.bat -p android/launcher :app:assembleDebug :app:verifyNoGamePayload
```

Build checks do not replace in-game validation for changed mappings, Mixin injection points,
rendering, synchronized commands, saves, or multiplayer behavior.

## Local-only inputs

The following must remain outside Git:

- `libs/game-lib.jar` and every APK/DEX;
- desktop-game ZIPs, imported/extracted game directories, and Android Java runtimes;
- generated named game Jars and Gradle build outputs;
- `local.properties`, IDE state, and external installations such as
  `D:\SteamLibrary\steamapps\common\Rusted Warfare`.

## Repository layout

| Path | Status | Purpose |
| --- | --- | --- |
| `src/` | maintained | GameProvider, tooling, mappings, and Loader runtime |
| `rusted-fabric-api/` | maintained | sole public API, mapped implementation, Mixins, and remapping |
| `official-mods/java-mod-menu/` | maintained | localized in-game list of loaded Java mods |
| `official-mods/example-mod/` | maintained | API and named-to-official remapping example |
| `android/launcher/` | maintained | no-root Android application and native JVM/render host |
| `android/jvm-launcher-core/` | maintained | desktop import, JVM validation, and launch planning |
| `android/jvm-game-provider/` | maintained | self-contained Android Fabric launcher asset |
| `android/lwjgl2-compat/` | maintained | Android LWJGL2/input compatibility classes |
| `legacy/` | frozen | retired native-APK/Xposed/DEX-mod code and documentation |
| `report/mapping-evidence/` | maintained evidence | mapping provenance and coverage records |

## Current limitations

- The API and mappings remain experimental and may require source changes when corrected.
- Unmapped game members retain their official names.
- Gameplay-changing callbacks must remain deterministic for multiplayer unless their contract
  explicitly permits server-only, client-only, or optional behavior.
- The Android launcher currently targets ARM64 devices and requires user-supplied desktop game and
  compatible Java runtime inputs.
