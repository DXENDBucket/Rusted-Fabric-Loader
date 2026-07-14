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
`RustedWarfareGameEngine.init(Context)` boundary, installs a platform-neutral
`RustedFabricAPIContext`, and dispatches one before/after initialization event pair. Listener
failures are isolated and the original method is always called. The API and events retain no
Context/game objects, access no saves, and do not change the installed APK.

Version `0.8.0-cross-platform` includes the strict `.rfmod` verifier, standalone management
activity, app-private atomic registry, official-game-authorized read-only provider, game-code-cache
transfer, common/game bridge ClassLoader, and Android DEX entrypoint loader. Enabled mods initialize
before the first lifecycle dispatch; one failed mod is logged without stopping other mods or game
startup. It also includes the no-root, user-selected APK patch/sign/PackageInstaller flow and its
code-only `PatchedApplication` runtime. The no-root backend now weaves the mapped engine method and
dispatches the same before/after event pair. Both backends are build-validated but still need
physical device tests. See `../../docs/ANDROID_MODS.md` for the mod format and
`../../docs/ANDROID_LOCAL_PATCHER.md` for the no-root flow and current limitations.

The manager now shows patch stages, localized actionable failures, persisted installation results,
and each enabled mod's multiplayer declaration. Cross-platform manifests and compatibility
evaluation are available through the common API; network transport/enforcement is deliberately not
claimed until a backwards-compatible game handshake extension is implemented.

The same common API classes and `RuntimeLifecycleEvents` are embedded into the Windows Fabric API
Jar. Mod source can therefore share initialization listeners, while its final Windows Jar and
Android DEX remain separate builds.

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
`module-loaded`, `application-attached`, `api-context-ready`, `before-engine-initialization`,
`after-engine-initialization`, `hook-installed`, and `game-engine-initialized` under
`RustedFabric/Bootstrap`. A non-matching installation logs `game-hook-skipped` instead.

This phase is successful only if the game reaches its menu both with the module enabled and
disabled and no save, game file, package signature, or gameplay behavior changes.
