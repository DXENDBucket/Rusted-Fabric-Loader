# Third-Party Notices

This project ("Rusted Fabric Loader" / "Rusted Fabric API") includes and/or depends on third-party software components.
The following notices are provided for attribution and license compliance.

> Notes
> - Some dependencies may be used only at build time and are not redistributed.
> - If you redistribute any third-party binaries, you must also redistribute their license texts as required by their licenses.

---

## Fabric Loader

- Name: Fabric Loader
- Homepage/Source: https://github.com/FabricMC/fabric-loader
- License: Apache License 2.0
- Used for: Mod discovery/loading, entrypoints, Knot launcher, etc.
- Version used in this project: `0.18.1` (update if changed)

---

## SpongePowered Mixin (via Fabric's sponge-mixin)

- Name: SpongePowered Mixin / fabricmc:sponge-mixin
- Homepage/Source:
    - https://github.com/SpongePowered/Mixin
    - https://maven.fabricmc.net/net/fabricmc/sponge-mixin/
- License: MIT License
- Used for: Bytecode mixin/injection support
- Version used in this project: `0.13.4+mixin.0.8.5` (update if changed)

---

## ASM

- Name: ASM (OW2 ASM)
- Homepage/Source: https://asm.ow2.io/
- License: BSD 3-Clause License
- Used for: Bytecode manipulation utilities (direct dependency)
- Versions used in this project: `9.2` (update if changed)

---

## Google Gson

- Name: Gson
- Homepage/Source: https://github.com/google/gson
- License: Apache License 2.0
- Used for: JSON parsing
- Version used in this project: `2.8.7` (update if changed)

---

## Guava

- Name: Guava
- Homepage/Source: https://github.com/google/guava
- License: Apache License 2.0
- Used for: General-purpose utilities
- Versions used in this project: `21.0`, `27.1-android` (update if changed)

---

## dexlib2 (smali project)

- Name: dexlib2
- Homepage/Source: https://github.com/JesusFreke/smali
- License: BSD 3-Clause License
- Used for: Structure-aware Android DEX instruction, branch, and exception-table rewriting in the
  no-root local patcher
- Version used in this project: `2.5.2` (update if changed)

---

## Apache Commons Compress

- Name: Apache Commons Compress
- Homepage/Source: https://commons.apache.org/proper/commons-compress/
- License: Apache License 2.0
- Used for: Bounded TAR parsing for user-selected Android JVM runtime archives
- Version used in this project: `1.28.0`

---

## XZ for Java

- Name: XZ for Java
- Homepage/Source: https://tukaani.org/xz/java.html
- License: BSD Zero Clause License (0BSD)
- Used for: Streaming decompression of user-selected `.tar.xz` JVM runtime archives
- Version used in this project: `1.12`

---

## Fold Craft Launcher Android LWJGL bundle

- Name: Fold Craft Launcher LWJGL/Android runtime files
- Homepage/Source: https://github.com/FCL-Team/FoldCraftLauncher
- License: GNU General Public License 3.0 for the FCL distribution; embedded upstream components
  retain the licenses listed below
- Used for: Android-aware GLFW/LWJGL boundary, OpenAL, and FreeType binaries in the desktop JVM port
- Release used: `1.3.2.4`
- Corresponding source: https://github.com/FCL-Team/FoldCraftLauncher/tree/1.3.2.4

The Android build extracts only the SHA-256-pinned paths declared in
`android/launcher/app/build.gradle`. The GPLv3 text, corresponding-source directions, exact source
release, and this notice are packaged in the APK under `assets/rusted-fabric/`. The Android launcher
distribution is GPL-3.0; the Loader, API, and reusable JVM modules remain Apache-2.0.

---

## PojavLauncher Android components

- Name: PojavLauncher Android runtime components
- Homepage/Source: https://github.com/PojavLauncherTeam/PojavLauncher
- License: GNU Lesser General Public License 3.0 for PojavLauncher code; bundled upstream
  components retain their own licenses
- Revision used: `b12ad048157b3aa255d078c235dd4571e1900309`

The Android build fetches the pinned GL4ES bridge binary and applicable notices by immutable
revision and verifies their SHA-256 values. The LGPLv3 license text is packaged in the APK.

---

## GL4ES

- Name: GL4ES
- Homepage/Source: https://github.com/PojavLauncherTeam/gl4es
- License: MIT License
- Used for: Translating the desktop OpenGL 1.x/2.1 calls used by LWJGL2 and Slick2D to Android
  OpenGL ES
- Revision represented by the packaged Pojav binary: PojavLauncher revision
  `b12ad048157b3aa255d078c235dd4571e1900309`; upstream source audited at
  `3d8906d46a93066f21ea01aeaf1a36ec972efd15`

The full MIT license text is packaged in the APK.

---

## LWJGL 3 / LWJGLX

- Name: LWJGL 3 and the LWJGLX LWJGL2 compatibility layer
- Homepage/Source:
    - https://github.com/LWJGL/lwjgl3
    - https://github.com/PojavLauncherTeam/lwjglx
- License: BSD 3-Clause (LWJGL); compatibility changes are distributed through the FCL/Pojav bundle
  described above
- Used for: Native Java/OpenGL binding and the desktop game's `org.lwjgl` compatibility surface;
  an isolated Vulkan binding in the optional official Vulkan Mod
- LWJGL baseline in the Pojav bundle: `3.2.3`
- LWJGL version embedded by the desktop Vulkan Mod: `3.4.2`

The LWJGL BSD license text is packaged in the APK and retained inside the original JARs embedded by
the Vulkan Mod. LWJGL 3 is loaded in a child-first driver class loader there so it does not replace
the desktop game's incompatible LWJGL 2 classes.

---

## FabricMC Access Widener

- Name: access-widener
- Homepage/Source: https://github.com/FabricMC/access-widener
- License: Apache License 2.0
- Used for: Access widening tooling
- Version used in this project: `2.1.0` (update if changed)

---

## FabricMC Tiny Mappings Parser

- Name: tiny-mappings-parser
- Homepage/Source: https://github.com/FabricMC/tiny-mappings-parser
- License: Apache License 2.0
- Used for: Mappings parsing utilities
- Version used in this project: `0.2.2.14` (update if changed)

---

## Disclaimer

Third-party trademarks and names are the property of their respective owners.
This project is not affiliated with or endorsed by the above third-party projects.

If you believe any attribution is missing or incorrect, please open an issue.
