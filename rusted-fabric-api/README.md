# Rusted Fabric API Common

This module is the platform-neutral foundation embedded in the Windows Fabric API Jar. It has no
dependency on Fabric Loader, Mixin, Android classes, desktop rendering libraries, or Rusted Warfare
implementation classes. Android scaffolding is frozen and is not a current release target.

It currently provides:

- immutable `RustedFabricAPIContext` version 3 with platform, mapping profile, and capabilities;
- the process-wide `RustedFabricRuntime` context holder;
- the platform-neutral `RustedFabricModEntrypoint` contract;
- exception-isolated, one-shot engine initialization event contracts;
- Fabric-style named event phases with cycle-safe dependency ordering;
- typed, deterministic optional inter-mod service discovery;
- reverse-order lifecycle scopes for removable registrations and feature cleanup;
- the existing typed API entrypoint adapter and compatibility keys.

The common classes are embedded in the Windows `rusted-fabric-api` Jar, so ordinary Fabric mods do
not install a second dependency Jar. Game-object types and mapped desktop helpers live in
`rusted-fabric-api-desktop`; loader-neutral contracts and utilities stay here.
