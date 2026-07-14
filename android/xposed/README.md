# Rusted Fabric Android Xposed bootstrap

This standalone Android build packages the bootstrap and first mapped probe as a Modern Xposed API
102 module.
It is intentionally isolated from the desktop Gradle build so an Android SDK is not required for
desktop development.

The module has one static scope: `com.corrodinggames.rts`. After the original
`android.app.Application.attach(Context)` call, it streams the installed base APK from
`ApplicationInfo.sourceDir` through SHA-256 without copying it. Package, version, and hash must
exactly select `rw-android-1.15-code176-v1.0`; otherwise all game hooks stay disabled.

For the verified profile, the module hooks the mapped
`RustedWarfareGameEngine.init(Context)` boundary and records one after-initialization event. Both
hooks always call the original implementation first and do not modify arguments or results, retain
Context/game objects, access saves, load mods, or change the installed APK.

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
`module-loaded`, `application-attached`, `hook-installed`, and `game-engine-initialized` under
`RustedFabric/Bootstrap`. A non-matching installation logs `game-hook-skipped` instead.

This phase is successful only if the game reaches its menu both with the module enabled and
disabled and no save, game file, package signature, or gameplay behavior changes.
