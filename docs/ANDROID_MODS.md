# Android mod packages

## User experience

The intended first release keeps the installed Rusted Warfare APK and signature unchanged:

1. Install and enable the Rusted Fabric Loader module in a compatible root/Xposed framework once.
2. Open the Loader app, choose **Import mod**, and select a `.rfmod` file with Android's system file
   picker.
3. Review the mod name, version, requested capabilities, active mapping compatibility, hash, and an
   explicit trusted-code warning.
4. Enable, disable, update, or remove the mod from the Loader's list.
5. Force-stop and reopen Rusted Warfare after any change. Live reload is intentionally excluded
   from v1.

Mod distributors may also serve or share files as `application/vnd.rustedfabric.mod`; Android then
offers **Open with Rusted Fabric Loader**, which enters the same verification/import path without
manual directory copying.

The management app keeps the imported source archive in its own private storage. A narrowly scoped,
read-only provider makes only enabled, verified archives available to the supported game process.
The provider accepts only the official game UID while the installed package still matches the exact
mapping profile. The injected backend copies each archive by SHA-256 into the game's private code
cache before loading it.

This management path is implemented in the `0.5.0-mod-management` scaffold. It still requires a
rooted test device and a real external probe `.rfmod` before it can be called device-validated.

Neither installation path patches, resigns, or redistributes the game APK. The Loader distribution
contains only Loader code, mappings, API contracts, and user-installed mods.

## `.rfmod` v1 format

An Android mod is a ZIP-compatible, code-only archive with this layout:

```text
example.rfmod
├── classes.dex
├── META-INF/rusted-fabric.mod.properties
├── LICENSE                         (optional)
├── NOTICE                          (optional)
└── assets/...                      (optional mod-owned data)
```

The metadata file is UTF-8, one `key=value` pair per line:

```properties
schemaVersion=1
id=portable_probe
version=1.0.0
name=Portable Probe
entrypoint=example.mod.PortableEntrypoint
apiVersion=0.1
mappingProfiles=rw-android-1.15-code176-v1.0
capabilities=event.engine.init
multiplayerMode=required
multiplayerProtocol=portable-units-v1
multiplayerSyncHash=0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef
platform=android
dex=classes.dex
```

`entrypoint` must be a public concrete class with a public no-argument constructor that implements
`io.github.endx.rustedfabricapi.api.RustedFabricModEntrypoint`. Its `onInitialize` method receives
the same platform-neutral `RustedFabricAPIContext` used by the Windows backend.

Multiple comma-separated mapping profiles and capabilities are permitted. Capabilities are
requirements: the Loader rejects the mod if any declared capability is unavailable. API version
matching is exact in the experimental `0.1` format.

`multiplayerMode` is optional for archive compatibility but defaults to `unsafe`. Use `client_only`
only when the mod cannot affect synchronized game state. Use `server_only` only for host behavior
that requires no client code, assets, or synchronized state; vanilla clients may then join.
Use `optional` when either side can run independently and peer presence only enables optional,
backward-compatible enhancements. A missing optional mod never blocks a connection.
Content/gameplay mods use `required` and
must provide the same protocol and platform-neutral synchronized-content SHA-256 in their Windows
and Android packages. The hash is not the JAR, DEX, APK, or `.rfmod` archive hash. See
[`MULTIPLAYER.md`](MULTIPLAYER.md).

## Verification and loading rules

Before any mod class executes, the Loader enforces archive, expanded-size, entry-count, metadata,
DEX-size, and path-traversal limits and records SHA-256 for both archive and DEX. V1 accepts one
`classes.dex`; it rejects extra executable files, native libraries, Java class files, APKs, and
secondary DEX files.

The DEX may reference mapped game classes and the common API. It may not define game classes,
Android/Java platform classes, common API classes, or Loader/Xposed implementation classes. The
declared entrypoint must be one of the DEX's actual class definitions.

The bridge ClassLoader resolves:

- verified mod definitions from the mod DEX, child-first;
- `io.github.endx.rustedfabricapi.api.*` from the Loader's common API;
- mapped game types and ordinary runtime types from the game's ClassLoader;
- Loader and Xposed implementation packages from nowhere visible to ordinary mod linkage.

This class separation prevents accidental payload embedding and common class-identity conflicts. It
is not a security boundary: enabled mod code runs inside the game process and should be treated as
trusted native-to-the-process code.

The Loader may ship built-in management and diagnostic functions. Game-affecting features should be
ordinary first-party `.rfmod` packages so users can disable or replace them. The Loader does not add
an in-game mod list; a mod may provide one independently on either platform.

## Windows and Android builds

Windows and Android mods can share Java source for `RustedFabricModEntrypoint`, context checks, and
platform-neutral lifecycle events. They cannot use one unchanged binary:

- Windows ships a Fabric `.jar` containing JVM `.class` files and may use Mixins/Slick/LWJGL.
- Android ships an `.rfmod` containing DEX and must use supported method-hook/API boundaries.

The recommended project layout is a common source module plus small Windows and Android packaging
modules. Platform-specific code belongs behind adapters, guarded by `context.platform()` and
capabilities where a shared entrypoint needs to select an implementation.
