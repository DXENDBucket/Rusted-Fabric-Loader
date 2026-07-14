# Rusted Fabric API Common

This module is the source-compatible API boundary shared by Windows Fabric Jar mods and Android DEX
mods. It has no dependency on Fabric Loader, Mixin, Xposed, Android classes, desktop rendering
libraries, or Rusted Warfare implementation classes.

It currently provides:

- immutable `RustedFabricAPIContext` version 3 with platform, mapping profile, and capabilities;
- the process-wide `RustedFabricRuntime` context holder;
- exception-isolated, one-shot engine initialization event contracts;
- the existing typed API entrypoint adapter and compatibility keys.

The common classes are embedded in the Windows `rusted-fabric-api` Jar and compiled into the Android
Xposed module. A portable mod should keep listener/business logic against this surface, then build a
Fabric Jar for Windows and a DEX archive for Android. Platform-specific UI, storage, rendering, and
hook code stays in separate source sets.
