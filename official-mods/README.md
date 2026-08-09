# Official mods

This directory contains optional mods maintained and distributed with Rusted Fabric Loader. Core
loader and API projects remain at the repository root.

## Identifier convention

Project-owned Fabric runtime IDs are lowercase and compact. Loader components use the
`rustedfabric` prefix:

- `rustedwarfare` — the built-in game identity exposed by the GameProvider;
- `rustedfabricapi` — the shared API;
- `rustedfabricmodmenu` — the official in-game Java mod list;
- `rustedfabricexample` — the development and contract example.

New official mods should use `rustedfabric` followed by a short alphanumeric feature name. A Fabric
mod ID is a permanent compatibility key: dependencies, resource namespaces, configurations, and
saved data may refer to it, so an ID must not be renamed after public release.

Gradle project names, directories, Maven artifacts, and Jar names use readable kebab-case instead;
for example, the Fabric ID `rustedfabricmodmenu` is built by `official-mods/mod-menu` and emitted as
`rusted-fabric-mod-menu-<version>.jar`. Those names are packaging identifiers, not Fabric mod IDs.
