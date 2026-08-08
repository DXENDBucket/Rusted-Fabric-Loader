# One-source Windows and Android mods

`gradle/rusted-fabric-javamod.gradle` packages one Java source set into both supported formats:

- `<id>-<version>.jar` for Windows Fabric;
- `<id>-<version>.javamod` for the Android Loader.

The Java entrypoint extends `RustedFabricAPIEntrypoint`. That base class implements both Fabric's
typed context callback and Android's `RustedFabricModEntrypoint`, so the source does not contain a
platform adapter. The outputs remain different because Windows executes JVM class files while
Android executes DEX.

Use `portable-example-mod/build.gradle` as the template. Its `rustedFabricPortableMod` map declares
identity, entrypoint, required capabilities, supported Android mapping profiles, and multiplayer
mode. Then run:

```powershell
.\gradlew.bat :portable-example-mod:buildPortableMod
```

The build invokes Android SDK D8, packages only mod-owned DEX/metadata/assets, and runs the same
strict archive verifier used by the Loader. The common API, game APK, game classes, Android Loader,
and Fabric Loader are not embedded in either mod output.

Use `client_only`, `server_only`, or `optional` only when the corresponding multiplayer guarantees
are true. Gameplay/content mods that change synchronized state must use `required` and supply the
same protocol plus platform-neutral synchronized-data SHA-256 in both output metadata records.

The `.javamod` suffix is intentionally distinct from Rusted Warfare's `.rwmod`: it identifies a
Java/DEX Loader mod and avoids implying that the archive is an ordinary built-in game content mod.
