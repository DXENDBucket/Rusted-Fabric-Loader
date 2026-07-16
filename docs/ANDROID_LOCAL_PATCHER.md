# Android no-root local patcher

## User flow

The Loader APK now contains the first complete no-root patch path:

1. The user taps **Create no-root game copy** and selects an APK with Android's document picker.
2. The Loader copies that selection into its private cache and requires an exact supported profile.
3. It rewrites the package and provider authority to the side-by-side package
   `io.github.endx.rwpatch`, changes the manifest Application to
   `io.github.endx.rustedfabric.android.patched.PatchedApplication`, weaves the exact mapped
   engine initialization method, game update/render boundaries, projectile lifecycle, unit
   damage/death, and the five
   pinned RFH1 network boundaries, then injects a code-only secondary DEX.
4. It rebuilds and aligns the APK, stripping the source signature without retaining it.
5. It creates or reuses a non-exportable RSA key in Android Keystore, signs with APK signature
   schemes v1/v2/v3, and verifies the result with `apksig`.
6. It streams the result into `PackageInstaller`. Android still shows its normal install
   confirmation; this cannot and should not be bypassed on an unprivileged device.
7. All temporary APK and DEX files are deleted after the install session has accepted them.

The generated app has a different package name, so the supported official game installation is
left in place. Loader releases must keep the same Android application data: uninstalling the Loader
also destroys its non-exportable local signing key, after which an already-installed patched copy
must be uninstalled before a newly signed copy can replace it.

## Runtime and trust boundary

The injected Application extends the game's original `RWApplication`. The repository contains only
a signature-only `compileOnly` stub for that superclass. Build gates reject the stub class
definition, game implementations, Xposed code, APK files, and mod payloads from the injected DEX.
At runtime the actual superclass is resolved from the APK selected by the user.

The injected runtime asks the separately installed Loader's read-only provider for enabled mods.
The provider accepts the side-by-side package only when all of these match:

- exact package `io.github.endx.rwpatch`;
- exact patched Application entrypoint;
- the current signing certificate from this Loader installation's Android Keystore key.

Mod archives are copied by hash into the patched game's private code cache and verified again
before `DexClassLoader` executes their public `RustedFabricModEntrypoint`.

## Current compatibility

The first profile accepts only Android 1.15 version code 176 with APK SHA-256
`328f37106985a2ba424efec9ac312ede0395f3bac56e3d5db5d642dd6aecc04c`.
Community translations or other modified APKs are deliberately rejected until their structural
profiles arrive; a familiar package name alone is never enough.

The local backend exposes `event.engine.init`, `mod.dex.v1`, `session.v1`,
`multiplayer.compat.v1`, `multiplayer.handshake.rfh1`, `mapping.profile.exact`, and
`platform.android.local-patch`, plus `event.game.lifecycle.v1` and
`event.projectile.lifecycle.v1`, plus partial `event.unit.damage.v1`. It initializes mods during
`Application.onCreate`. The patcher
inserts engine lifecycle callbacks, client/server hello callbacks, the system-packet receiver,
network-reset session transition, a cancellable start-game compatibility gate, frame scheduling,
and projectile creation/update/impact/removal callbacks, plus cancellable damage-before and
damage-after callbacks across all eight mapped Android R8 unit implementations. It also weaves the
modifiable death-effect result into 14 concrete unit classes and cancellable before/after death
events into the mapped custom-unit complete-death sequence. Impact callbacks receive the same
immutable target/collision snapshot as Windows. Projectile explosion injection is tied to
the verified `impactTriggered` write inside Android 1.15's inlined update method.

Weaving is fail-closed: the full source APK hash must match, the mapped owner/name/descriptor must
occur exactly once, the method shape and access flags must match, and an already-woven method is
rejected. Branches and try/catch labels are rebuilt by dexlib2 instead of editing instruction bytes
or offsets manually.

The build and a real local reference APK have passed manifest rewrite,
lifecycle/RFH1/frame/projectile/unit-damage/death DEX weaving,
bootstrap injection, ZIP alignment, and v1/v2/v3 signature verification. Decompiled output confirms
the start-game gate before original instructions and all lifecycle/network callbacks. Installation,
startup, event delivery, and mod loading still need validation on a physical unrooted Android device;
no ADB device was attached during this implementation pass.

## Development modules

- `android:local-patcher-core`: binary manifest rewrite, profile-pinned DEX lifecycle weaving,
  deterministic aligned ZIP rebuild, signing and verification; contains no Android UI or game
  payload.
- `android:local-patcher-cli`: developer CLI for reproducible desktop patch/sign checks.
- `android/game-api-stubs`: one compile-only superclass signature, never packaged.
- `android/patched-bootstrap`: builds the code-only secondary DEX.
- `android/xposed:module`: Loader/mod manager UI, Android Keystore identity, picker, and
  PackageInstaller integration. The APK can still act as the Modern Xposed backend on rooted setups.

Build and audit from the repository root with JDK 17:

```powershell
./gradlew :android:local-patcher-core:check :android:local-patcher-cli:check
./gradlew -p android/xposed :game-api-stubs:check :patched-bootstrap:check
./gradlew -p android/xposed :module:check :module:verifyNoGamePayload
```

The ignored reference APK under `libs/android/` and all locally produced patched APKs under
`build/` are development inputs/outputs only. They must never be committed or published.
