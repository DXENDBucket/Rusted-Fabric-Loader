# Rusted Fabric Android Xposed bootstrap

This standalone Android build packages the Phase 1 bootstrap as a Modern Xposed API 102 module.
It is intentionally isolated from the desktop Gradle build so an Android SDK is not required for
desktop development.

The module has one static scope: `com.corrodinggames.rts`. It hooks
`android.app.Application.attach(Context)` after the original call and records only the process,
application class, ClassLoader class, and pending mapping status. It does not load mappings or
mods, reference game classes, modify arguments/results, access saves, or change the installed APK.

## Build prerequisites

- JDK 17
- Android SDK platform 35 and Build Tools 35.0.0
- Gradle 8.11.1 or newer in the 8.x line

Create an ignored `local.properties` containing `sdk.dir=...`, then run from the repository root:

```powershell
./gradlew -p android/xposed :module:assembleDebug
./gradlew -p android/xposed :module:verifyNoGamePayload
```

The libxposed API is compile-only and resolves from its official API 102 release AAR. It is not
embedded in the module APK. Install the generated debug APK, enable it in an API 102-compatible
Xposed framework, select Rusted Warfare, and force-stop/start the game. The expected log tags are
`module-loaded`, `hook-installed`, and `application-attached` under `RustedFabric/Bootstrap`.

This phase is successful only if the game reaches its menu both with the module enabled and
disabled and no save, game file, package signature, or gameplay behavior changes.
