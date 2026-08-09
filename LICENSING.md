# Licensing by module

This repository uses path-scoped licenses. A license in a more specific directory overrides the
repository default for files within that directory.

## Apache License 2.0 repository default

Unless a more specific license applies, files in this repository are licensed under the Apache
License 2.0 in [`LICENSE`](LICENSE). This includes:

- the Rusted Fabric Loader and GameProvider;
- Rusted Fabric API;
- official Java mods;
- `android/jvm-launcher-core`;
- `android/jvm-game-provider`;
- `android/lwjgl2-compat`.

These Apache-2.0 modules may be incorporated into the GPLv3 Android application. Their source files
remain available under Apache-2.0, while the combined Android APK is distributed under GPLv3.

## GNU GPL version 3 Android distribution

Files under [`android/launcher`](android/launcher) are licensed under GNU GPL version 3 as stated in
[`android/launcher/LICENSE`](android/launcher/LICENSE). This scope includes the Android application,
its native bridge, Android build scripts, launcher documentation, and the resulting APK.

Each APK packages the applicable license texts, third-party notices, and corresponding-source
directions. A release APK must be built from a clean public Git revision and published with access
to the matching source.

## Third-party and user-provided content

Third-party components retain their original licenses; see
[`THIRD_PARTY_NOTICES.md`](THIRD_PARTY_NOTICES.md). Nothing in this repository relicenses those
components.

Rusted Warfare game files and user-provided Java runtimes are not part of this repository or its
license grants. The Android launcher imports those files from the user and does not package them in
the distributed APK.
