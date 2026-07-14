# Android loader development plan

## Goals and boundaries

The Android loader must operate on a user's installed copy of Rusted Warfare without distributing game classes, resources, native libraries, or a rebuilt game APK. The first backend is root-first and targets an ART hook framework such as LSPosed/Zygisk. A non-root local patch backend can be added later, but it is not part of the first prototype.

Game APKs are local development inputs only. They must remain under ignored paths and must never be copied into build outputs, reports, fixtures, or Git history.

## Current local reference APK

The local reference input is ignored at `libs/android/rusted-warfare-1.15-code176-base.apk`.

- APK SHA-256: `328F37106985A2BA424EFEC9AC312EDE0395F3BAC56E3D5DB5D642DD6AECC04C`
- package: `com.corrodinggames.rts`
- version: `1.15`, version code `176`
- application: `com.corrodinggames.rts.appFramework.RWApplication`
- launcher activity: `com.corrodinggames.rts.appFramework.IntroScreen`
- compile SDK: `29`; target SDK: `30`; minimum SDK: `8`
- one `classes.dex`: 1,641 class definitions, 10,847 field IDs, and 14,167 method IDs
- no packaged native `.so` libraries
- signer certificate SHA-256: `25450E9E56D2E64771E0514580AA85952C613AD4048FB3523CB1F07B65A63984`

The finalized Android v1.0 handoff is imported under
`android/mappings/rw-android-1.15-code176-v1.0`. It covers all 1,602 first-party classes and contains
9,213 loader-safe member mappings. Its named namespace is shared with the frozen PC v1.1 mapping,
but Android official owners must be used: for example, Android `GameEngine` is
`com/corrodinggames/rts/gameFramework/k`, not the PC owner. The profile therefore pins the exact APK
and mapping SHA-256 before reporting `VERIFIED`.

## Proposed modules

### `android:apk-inspector`

A JVM command-line tool that reads a user-supplied or installed APK and emits a code-free compatibility report. It owns:

- binary Android manifest inspection;
- DEX header, class, field, and method inventory;
- APK and signer fingerprints;
- structural anchor matching;
- variant capability reports;
- checks that reports contain no game bytecode, resource payloads, or local paths.

Phase 0 implementation is available. For the ignored local reference APK, run:

```powershell
./gradlew :android:apk-inspector:inspectReferenceApk
```

The deterministic report is written under the module's ignored `build/reports` directory. Run
`:android:apk-inspector:check` to verify the synthetic manifest/DEX parser contracts and confirm
that the inspector JAR contains no APK, DEX, or game-class payload.

### `android:rusted-fabric-android-api`

Android-safe API and context definitions shared by the hook backend and Android mods. It must not depend on desktop-only Slick, LWJGL, Swing, Knot launch classes, or Java desktop entrypoints.

### `android:rusted-fabric-android-xposed`

The root backend and distributable module. It owns:

- target-package scoping and explicit user binding for package-renamed community builds;
- the earliest safe hook at `Application.attach(Context)`;
- acquisition of the game's `ClassLoader`;
- version and structural-profile selection;
- method hooks and event dispatch;
- Android mod DEX loading;
- per-feature compatibility and failure isolation.

The Phase 1 scaffold now lives in the isolated `android/xposed` Android build. It targets Modern
Xposed API 102, uses `META-INF/xposed` entry metadata, and has a static official-package scope.
The shared `android:bootstrap-core` remains buildable without an Android SDK. The first hook calls
the original `Application.attach(Context)` unchanged, then captures ClassLoader diagnostics without
retaining Android or game objects. Mapping and mod loading are deliberately absent.

### Shared loader core

Mapping selection, mod metadata, event implementation, diagnostics, and compatibility reporting should be extracted from desktop-specific launch code only when the Android prototype demonstrates a real shared boundary. The existing desktop GameProvider remains unchanged during the bootstrap phase.

## Compatibility model

Compatibility is evaluated in layers:

1. known APK and signer profile;
2. manifest identity and Android entrypoints;
3. DEX class and inheritance anchors;
4. mapped member presence and descriptors;
5. optional normalized method fingerprints for instruction-sensitive hooks;
6. per-feature capability status.

The finalized official mapping is exact-only: it must not be applied to an APK with a different hash.
Resource-only translations and other community variants can receive a separate structural profile
after their DEX anchors and mapped members are verified. A modified method disables only the hooks
that depend on that method. Packed, encrypted, or globally re-obfuscated builds require a separate
mapping and may be rejected before the game starts.

Compatibility states are `VERIFIED`, `STRUCTURAL`, `PARTIAL`, `UNSUPPORTED`, and `PACKED`. Unknown variants can export a report containing hashes and structural fingerprints, never APK contents or decompiled code.

## Mixin migration policy

Desktop Mixin classes are not loaded directly on Android. Each existing hook is classified before migration:

- `HEAD` injection -> before-method hook;
- `RETURN` injection -> after-method hook;
- cancellable injection -> early result or exception control;
- return-value modification -> after-method result replacement;
- argument modification -> before-method argument replacement;
- field/invoke ordinal, local capture, redirect, and instruction-level injection -> manual redesign or deferred DEX weaving.

Every Android hook declares its required mapping anchors, supported structural profiles, criticality, and fallback behavior. Optional hook failure must never abort game startup.

## Delivery phases

### Phase 0: reproducible inspection

- [x] Add the APK inspector without adding an Android SDK dependency.
- [x] Produce a deterministic compatibility JSON report for the local reference APK.
- [x] Add artifact and Git gates that reject `.apk`, `.dex`, and embedded game class bodies.
- [x] Import and checksum-pin the finalized Android 1.15 vc176 mapping and API metadata.

Exit gate: the inspector identifies the reference APK, its Android entrypoints, all required core anchors, and produces no copyrighted payload.

### Phase 1: process bootstrap

- [x] Scaffold the smallest possible Modern Xposed-compatible module.
- [x] Statically scope the first prototype to the official Rusted Warfare package.
- [x] Hook the base `Application.attach(Context)` boundary without referencing game classes.
- [x] Record only package/process, application class, profile status, and ClassLoader class.
- [x] Build and audit the debug APK with the configured Android SDK.
- [ ] Install the APK on a rooted test device with a Modern Xposed API 102-compatible framework.
- [ ] Validate startup on a rooted test device with the module enabled and disabled.

Exit gate: enabling the module adds one diagnostic log entry and changes no game behavior, APK, signature, files, or save data.

### Phase 2: first API events

- [x] Select the official profile by installed APK package/version/SHA-256 without copying the APK.
- [x] Hook the stable `RustedWarfareGameEngine.init(Context)` boundary.
- [x] Build the shared `RustedFabricAPIContext` with `platform=ANDROID` and `androidRuntime=true`.
- [x] Dispatch the same exception-isolated before/after initialization events as Windows Fabric.
- [ ] Validate the profile selection and initialization probe on a rooted test device.
- [ ] Implement activity foreground/background events.
- [x] Isolate each portable listener so failures cannot escape into the game call.
- [ ] Add hook timeouts and a capability status screen.

Exit gate: the official reference APK reaches the menu and a skirmish with all three events observed and no startup regression.

### Phase 3: Android mod loading

- Define an Android mod archive containing metadata plus prebuilt DEX, without game classes.
- Import mods through the Storage Access Framework and copy them to application-private storage.
- Load mod DEX through a bridge ClassLoader that resolves the common API from the module loader and
  mapped game types from the game ClassLoader.
- Enforce API and mapping version requirements before executing entrypoints.
- Require restart for install, update, enable, or disable operations in the first version.

Exit gate: a minimal external mod receives the Phase 2 events and can be removed without modifying the game installation.

### Phase 4: event and API migration

- Inventory all current Mixins by injection category and runtime risk.
- Port method-boundary hooks first: lifecycle, unit registration, damage, queue actions, effects, maps, and save/replay boundaries.
- Defer instruction-sensitive hooks until an equivalent stable method boundary is found.
- Split desktop-only and Android-only diagnostics and examples.

Exit gate: each migrated feature has an independent compatibility probe and can disable itself without disabling the loader.

### Phase 5: variants and distribution

- Add structural overlays for known community translations and package-renamed builds.
- Verify resource-only variants without requiring a new full mapping profile.
- Sign and publish only the loader module, API, inspector, metadata, mappings, and checksums.
- Add a release audit proving no game APK, DEX, assets, native libraries, absolute local paths, or reconstructed game code are present.

## Immediate implementation order

1. Install the profile-probe APK on a rooted test device and capture the bootstrap logs.
2. Build the first Android API context with `androidRuntime=true`.
3. Add before/after engine initialization events around the verified probe.
4. Produce an inventory of the existing 84 Mixins by hook category before porting more hooks.

No step should require committing, publishing, or embedding the local reference APK.
