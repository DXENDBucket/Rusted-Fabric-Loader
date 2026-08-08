# Rusted Fabric Android Launcher

This standalone Android application runs user-imported Rusted Warfare desktop files through the
same Fabric/Knot runtime used on Windows. It does not use, patch, or bundle the Android game APK,
and it does not require root or an Xposed framework.

The launcher imports a desktop game ZIP or directory and a Linux/AArch64 Java 17 runtime into
application-private storage. Its ARM64 host supplies the Android Surface, LWJGL2/GL4ES, OpenAL,
input, and libRocket compatibility layers required by the desktop game.

The application contains only Loader-owned code and reviewed third-party runtime components.
`verifyNoGamePayload` rejects APKs that contain Rusted Warfare classes or game payloads.

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

## Active modules

- `app`: Android setup UI, isolated JVM process, Surface/input bridge, and native runtime adapters.
- `android/jvm-launcher-core`: desktop-file import and immutable JVM launch planning.
- `android/jvm-game-provider`: self-contained Fabric GameProvider launcher asset.
- `android/lwjgl2-compat`: Java-side LWJGL2 and touch compatibility classes.

Retired Android native-APK patching, Xposed hooks, Android DEX mods, and their mapping/API modules
are archived under `legacy/` and are not included in this build.
