# Rusted Fabric Windows Installer

This module builds a single-file WinForms installer for players who already own the Windows version
of Rusted Warfare. The installer contains Rusted Fabric Loader and its runtime dependencies, but no
game files.

## Player flow

The installer detects common Steam library locations or lets the player select the game directory.
It validates `game-lib.jar` and `jvm64/bin/java.exe` before changing anything. The component choices
are:

- Rusted Fabric API: selected by default.
- Java Mod Menu: selected by default and requires the API.
- INI Essentials: optional and not selected by default.
- Example Mod: development-only and never included.

Installation creates `RustedFabricLauncher.exe`, a `run-rusted-fabric.bat` fallback,
`fabric-libs/`, `rusted-fabric-loader/`, and selected Jars under `javamods/`. The EXE launcher uses
the JVM already shipped with the user's game.

The installer records its paths and SHA-256 hashes in
`rusted-fabric-loader/install-manifest.json`. On update or component removal, a previously managed
file is deleted only if its current hash still matches the manifest. Unrelated third-party Java
mods are preserved. Writes are staged and existing destinations are backed up during the commit.

## Build and verify

From the repository root:

```bat
gradlew.bat windowsInstaller
```

The lifecycle task verifies the embedded payload and performs an isolated installation into a fake
game directory. The output is:

```text
installer/windows/build/dist/Rusted-Fabric-Installer-<version>.exe
```

Build the local EndXiom self-signed flavor with:

```bat
gradlew.bat windowsInstallerEndXiomDevSigned
```

Its output ends in `-EndXiom-dev-signed.exe`. See
[`docs/CODE_SIGNING.md`](../../docs/CODE_SIGNING.md) for the trust limitations and public release
process.
