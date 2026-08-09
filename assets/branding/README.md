# Branding assets

`rusted-fabric-icon.png` is the shared, unmodified 1254 x 1254 source artwork supplied by the
project owner. Derived application icons preserve that artwork without generative changes:

- `rusted-fabric-icon.ico`: Windows multi-resolution icon (16 through 256 pixels), embedded in the
  player installer and installed EXE launcher. Desktop shortcuts use the launcher's embedded icon.
- `android/launcher/app/src/main/res/mipmap-*/ic_launcher.png`: Android launcher icons from mdpi
  through xxxhdpi.

Keep the PNG as the source of truth when regenerating platform-specific icon formats.
