# Rusted Fabric Loader

This repository provides the Rusted Fabric Loader GameProvider along with supporting build scripts. The loader expects the Fabric runtime libraries and the compiled GameProvider jars to be available at launch time.

## Launching with a custom script
The following Windows batch script demonstrates how to start the client using prebuilt artifacts from the repository. It assumes the `clean build copyFabricRuntime` Gradle task has been run so that all required Fabric jars are available.

```bat
@echo off
setlocal

set "JAVA_EXE=jvm64\bin\java"
set "HEAP_OPTS=-Xmx4096M"

rem Fabric Loader and libs
set "FABRIC_CP=fabric-libs/*"

rem GameProvider
set "GP_CP=rusted-fabric-loader/*"

set "CLASSPATH=%FABRIC_CP%;%GP_CP%"

%JAVA_EXE% %HEAP_OPTS% ^
  -verbose:class ^
  -Dfile.encoding=UTF-8 ^
  -cp "%CLASSPATH%" ^
  net.fabricmc.loader.impl.launch.knot.KnotClient ^
  %* > classlog.txt 2>&1

endlocal
pause
```

- `fabric-libs` should contain all jars produced by `copyFabricRuntime`.
- `rusted-fabric-loader` should contain the GameProvider jars from the build output.

Adjust `JAVA_EXE` and memory options as needed for your environment.

## Installing into a game directory
Use `installToGameDir` to build and install the loader artifacts into an existing Rusted Warfare directory:

```bat
set "JAVA_HOME=C:\Program Files\Java\jdk-21"
set "PATH=%JAVA_HOME%\bin;%PATH%"
gradlew.bat installToGameDir -PgameDir="C:\Users\57991\Desktop\Rusted Warfare"
```

The task creates or updates:

```text
fabric-libs/
rusted-fabric-loader/
javamods/
run-rusted-fabric.bat
```

By default it also builds and installs the official-runtime example mod. To avoid building or updating the example mod during install, run:

```bat
gradlew.bat installToGameDir -PgameDir="C:\Users\57991\Desktop\Rusted Warfare" -PinstallExampleMod=false
```

This does not delete existing files in `javamods`.

## Mappings
The `src/main/resources/mappings/mappings.tiny` file is packaged into the GameProvider jar at `mappings/mappings.tiny`, the resource path Fabric Loader checks by default.

`game-lib-named.jar` contains remapped game classes, so it is generated locally from a developer's own `game-lib.jar` rather than distributed with the loader. Put `game-lib.jar` in `libs/`, or pass its path with `-PgameLibJar`.

Run `generateNamedGameJar` to create a development jar at `build/rusted-dev/game-lib-named.jar`:

```bat
set "JAVA_HOME=C:\Program Files\Java\jdk-21"
set "PATH=%JAVA_HOME%\bin;%PATH%"
gradlew.bat generateNamedGameJar
```

With an explicit game jar path:

```bat
gradlew.bat generateNamedGameJar -PgameLibJar=C:\Games\RustedWarfare\game-lib.jar
```

The mappings and output can also be changed:

```bat
gradlew.bat generateNamedGameJar ^
  -PgameLibJar=C:\Games\RustedWarfare\game-lib.jar ^
  -PmappingsTiny=C:\rw-dev\my-mappings.tiny ^
  -PnamedGameJar=C:\rw-dev\game-lib-named.jar
```

Named development launch:

```bat
java ^
  -Drusted.devNamed=true ^
  -Drusted.namedGameJar=build/rusted-dev/game-lib-named.jar ^
  -cp "fabric-libs/*;rusted-fabric-loader/*" ^
  net.fabricmc.loader.impl.launch.knot.KnotClient
```

If you generated the named jar with a custom mappings file, pass the same mappings to Fabric Loader at launch:

```bat
java ^
  -Drusted.devNamed=true ^
  -Drusted.namedGameJar=C:\rw-dev\game-lib-named.jar ^
  -Dfabric.mappingPath=C:\rw-dev\my-mappings.tiny ^
  -cp "fabric-libs/*;rusted-fabric-loader/*" ^
  net.fabricmc.loader.impl.launch.knot.KnotClient
```

Mods compiled against `game-lib-named.jar` can use mapped classes such as `rustedwarfare.core.GameEngine` in this named runtime. Before using those mods with the normal official game jar, remap them back:

```bat
gradlew.bat remapNamedJarToOfficial -PremapInput=path\to\named-mod.jar -PremapOutput=path\to\mod-official.jar
```

Use the same `-PmappingsTiny=...` value when remapping a mod that was compiled with custom mappings.

Mapping coverage is still partial: only classes, methods, and fields present in `mappings.tiny` receive named identifiers. Unmapped game symbols remain in their original names.

Mixin classes should be authored against the named development jar. `RemapJar` remaps both bytecode references and mixin metadata strings in annotations such as `@Mixin(targets)`, `@Inject(method)`, `@Redirect(method)`, and nested `@At(target)` values. The Rusted Fabric API is installed as a remapped official-runtime jar by `installToGameDir`, so its mixin config only lists `*NamedMixin` classes.

To build the API runtime jar directly:

```bat
gradlew.bat :rusted-fabric-api:remapJarToOfficial
```

## Example Mod
The `example-mod` subproject is a small test mod for the named development pipeline. It imports mapped game classes such as `rustedwarfare.core.GameEngine`, logs Fabric `main` and `client` entrypoints, and logs the Rusted-specific `classpath_ready` and `before_game` callbacks.

Build the named development jar:

```bat
gradlew.bat :example-mod:build
```

This creates:

```text
example-mod/build/libs/rusted-fabric-example-mod-1.0-SNAPSHOT.jar
```

Use that jar in a named development launch with `-Drusted.devNamed=true` and `game-lib-named.jar`.

Build the official-runtime jar:

```bat
gradlew.bat :example-mod:remapJarToOfficial
```

This creates:

```text
example-mod/build/libs/rusted-fabric-example-mod-1.0-SNAPSHOT-official.jar
```

Use the official jar with the normal `game-lib.jar` runtime.

## Modding with Fabric
Rusted Fabric Loader uses the standard Fabric mod discovery process and adds a few conveniences for Rusted Warfare:

- **Mod search paths** – Mods are loaded from Fabric's defaults **plus** a `javamods` directory next to the game files. You can override the location with `-Drusted.javamodsDir=/path/to/dir` if you prefer a different folder layout. `-Drusted.gameDir=...` also controls where the loader looks for the game files themselves. Both values are resolved and registered as extra Fabric mod directories at startup.
- **Game entrypoint** – The loader launches `com.corrodinggames.rts.java.Main` in official mode or `rustedwarfare.client.RustedWarfareMain` in named development mode after running Fabric's `main` and `client` entrypoints, so traditional Fabric mods continue to work before the game starts.
- **Custom API hooks** – The bundled Rusted Fabric API exposes two additional entrypoints for mod authors:
  - `rustedfabricloader:classpath_ready` runs after the game classpath (game-lib.jar, libs/, filtered android.jar) is injected. Implement this with a `Consumer<Map<String, Object>>` (extend `RustedFabricAPIEntrypoint` for convenience) to inspect the provided context or register transformers.
  - `rustedfabricloader:before_game` runs immediately before the game main class is invoked. The same `Consumer<Map<String, Object>>` contract applies.

Each callback receives a `RustedFabricAPIContext` describing the game directory, launch arguments, and whether the runtime is Android, enabling mods to adjust behavior for desktop vs. mobile builds.

### Java custom unit assets
`rusted-fabric-api` includes `io.github.endx.rustedfabricapi.api.asset.JavaUnitAssetLoader` for Java-authored custom units. It is intentionally namespace-neutral: public methods accept and return `Object`, while the implementation resolves named and official game classes reflectively. The helper covers the v0.22 asset contracts:

- Load native images and sounds through `CustomUnitLoader` helpers.
- Apply `CustomUnitMetadata.image` plus frame layout fields consistently.
- Generate team-color body, turret, and zoomed-out icon arrays through the native `createTeamColorImages` helper.
- Set shadow/build icon fields, create sound/effect lists, register metadata in `pendingCustomUnitTypes`, and validate the basic asset contract before native finalization.
