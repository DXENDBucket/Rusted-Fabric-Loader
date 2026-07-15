# Rusted Fabric API

## Compatibility status

Rusted Fabric API `0.1.x` is experimental and targets Rusted Warfare `1.15` with the mapping version recorded in `fabric.mod.json`. Event names and callback arguments should be treated as source-compatible within a `0.1.x` line where practical, but mapping corrections can still require signature changes.

Mods can declare the game dependency exposed by the GameProvider:

```json
"depends": {
  "fabricloader": ">=0.18.1",
  "rusted_warfare": "1.15",
  "rustedfabricapi": ">=0.1.0"
}
```

## Loader lifecycle entrypoints

Two Rusted-specific Fabric entrypoints are available:

- `rustedfabricloader:classpath_ready`: the game Jar and libraries are on the launch classpath, before standard Fabric initializers run.
- `rustedfabricloader:before_game`: standard `main` and `client` initializers have completed, immediately before the game main method is invoked.

Implement `RustedFabricAPIEntrypoint` to receive a typed `RustedFabricAPIContext`. The context is an
immutable snapshot; its launch-argument array and capabilities are defensively copied.
The same base class also implements Android's `RustedFabricModEntrypoint`, so one entrypoint source
can be packaged for both platforms. See [`PORTABLE_MOD_BUILD.md`](PORTABLE_MOD_BUILD.md).
`contextVersion()` is currently `5`. Version 5 adds the shared game-session API and live `RFH1`
handshake capability. Version 4 added the canonical multiplayer manifest and the
`multiplayer.compat.v1` capability. Version 3 added `platform()`, `mappingProfileId()`,
`capabilities()`, `packageName()`, and `processName()`. The older `androidRuntime()` accessor remains
available.

## Windows and Android portability

`rusted-fabric-api` contains the complete platform-neutral public surface: context, runtime holder,
events, helpers, sessions, and multiplayer contracts. Its classes are embedded in the Windows API
Jar and compiled into the Android loader DEX,
so a mod can use the same imports and listener source on both platforms:

```java
RuntimeLifecycleEvents.AFTER_ENGINE_INITIALIZATION.register(context -> {
    if (context.hasCapability("event.engine.init")) {
        // portable initialization logic
    }
});
```

`LOADER_READY` fires after enabled mods are loaded, and `GAME_READY` fires after the first successful
engine initialization. Both are exception-isolated and available on Windows and Android. Portable
multiplayer manifests, evaluation, and events live under `api.multiplayer` and
`MultiplayerCompatibilityEvents`; see [`MULTIPLAYER.md`](MULTIPLAYER.md).

`RustedFabricRuntime.currentSession()` and `GameSessionEvents` are also common API. They are active
for single-player as well as host/client play, so portable gameplay mods do not need separate
offline and online entrypoints.

The distributed binary is still platform-specific: Windows uses a Fabric Jar containing JVM class
files, while Android requires a DEX mod package. The provided Gradle convention builds both outputs
from one common source set. Put
Slick/LWJGL, desktop Mixins, Android UI/storage, and other platform APIs behind separate adapters.
Android entrypoint classes implement `RustedFabricModEntrypoint`; the `.javamod` v1 format and loading
rules are documented in [`ANDROID_MODS.md`](ANDROID_MODS.md).

The three Gradle modules are explicit build boundaries, not competing APIs:

- `rusted-fabric-api`: the public cross-platform API. It has no Fabric, Mixin, Android framework,
  Slick/LWJGL, or game implementation class dependency.
- `rusted-fabric-api-desktop`: Windows Fabric/Mixin hooks, named-to-official remapping, and the desktop
  RFH1 adapter. Its distributable Jar embeds `rusted-fabric-api` so users install one API Jar.
- `rusted-fabric-api-android`: the Android RFH1/mapping adapter shared by both local-patch and Xposed
  backends. Android packaging compiles it and `rusted-fabric-api` into DEX.

Mod source targets `rusted-fabric-api`; it uses a platform backend only for platform-specific
integration or build tooling. This separation prevents desktop dependencies from entering Android
while mechanically verifying that the public API remains portable.

Backend coverage is machine-readable in
`rusted-fabric-api/src/main/resources/rustedfabricapi/api-support-matrix.csv`. Each public event
group has a versioned capability key and a `full`, `partial`, or `unavailable` level for Windows,
Android local patch, and Android Xposed. `RustedFabricCapabilities` exposes the stable keys;
`ApiSupportMatrix.available(context, capability)` combines expected backend coverage with the
capabilities actually advertised by the running Loader. The build fails if a public event class or
capability row is missing.

## Event behavior

Events under `io.github.endx.rustedfabricapi.api.event` invoke listeners synchronously in registration order on the thread that reached the corresponding game method. They do not switch to a render, update, or network thread.

- Existing game-object events propagate listener exceptions to the intercepted game call. A listener should catch failures it can recover from.
- Registration is intended for initialization time. There is currently no unregister operation.
- `BEFORE_*` callbacks returning `true` generally cancel the operation, but the callback interface remains the source of truth.
- `MODIFY_*` callbacks are chained in registration order; each listener receives the value produced by the previous listener.
- Game objects are commonly exposed as `Object`. This keeps the public API Jar namespace-neutral across named development and official runtime. Mods may cast to mapped game types when they are compiled and remapped through the supported pipeline.

`RuntimeLifecycleEvents` is the cross-platform exception: each listener is isolated, failures are
counted in `DispatchResult`, registrations can be unregistered, and no game or platform object is
exposed. The before/after engine initialization events are one-shot on both backends.

## API layers

- `api.event`: public experimental event surface for mods.
- `rusted-fabric-api`: all cross-platform public contracts, events, helpers, sessions, and multiplayer
  APIs, with no Fabric, Xposed, Android framework, Slick, or game implementation class dependency.
- `rusted-fabric-api-desktop`: internal Windows hooks and remapping support.
- `rusted-fabric-api-android`: internal Android mapping and network transport support.
- `api.asset`, `api.ini`, and `api.logic`: higher-level experimental helpers backed by current mappings.
- `api.diagnostic`: development diagnostics. Output and reflected member coverage are not a stable compatibility contract. Mapping v1.1 includes `PlatformRuntimeDiagnostics` for operating-system, platform-extension, and file-change-engine state.
- `api.util.RustedReflection`: low-level compatibility support; prefer higher-level APIs when one exists.
- `mixin`: internal implementation. Mods must not reference these classes.

## Mixin failure policy

The API uses only named Mixin sources. Official-runtime targets and selectors are generated by `remapJarToOfficial`. The configuration is required and every injection currently has `require = 1`, so a target drift fails during startup instead of silently disabling an API event.

Run `gradlew.bat check` before installing a build. It validates the Mixin source/config inventory and runs dependency-free API contract checks.
