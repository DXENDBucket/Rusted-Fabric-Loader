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

## Modding with Fabric
Rusted Fabric Loader uses the standard Fabric mod discovery process and adds a few conveniences for Rusted Warfare:

- **Mod search paths** – Mods are loaded from Fabric's defaults **plus** a `javamods` directory next to the game files. You can override the location with `-Drusted.javamodsDir=/path/to/dir` if you prefer a different folder layout. `-Drusted.gameDir=...` also controls where the loader looks for the game files themselves. Both values are resolved and registered as extra Fabric mod directories at startup.
- **Game entrypoint** – The loader launches `com.corrodinggames.rts.java.Main` after running Fabric's `main` and `client` entrypoints, so traditional Fabric mods continue to work before the game starts.
- **Custom API hooks** – The bundled Rusted Fabric API exposes two additional entrypoints for mod authors:
  - `rustedfabricloader:classpath_ready` runs after the game classpath (game-lib.jar, libs/, filtered android.jar) is injected. Implement this with a `Consumer<Map<String, Object>>` (extend `RustedFabricAPIEntrypoint` for convenience) to inspect the provided context or register transformers.
  - `rustedfabricloader:before_game` runs immediately before the game main class is invoked. The same `Consumer<Map<String, Object>>` contract applies.

Each callback receives a `RustedFabricAPIContext` describing the game directory, launch arguments, and whether the runtime is Android, enabling mods to adjust behavior for desktop vs. mobile builds.
