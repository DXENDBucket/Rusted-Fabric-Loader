# Official mods

This directory contains optional mods maintained and distributed with Rusted Fabric Loader. Core
loader and API projects remain at the repository root.

## Identifier convention

Project-owned Fabric runtime IDs are lowercase snake_case:

- `rusted_warfare` — the built-in game identity exposed by the GameProvider;
- `rusted_fabric_api` — the shared API;
- `java_mod_menu` — the official in-game Java mod list;
- `ini_essentials` — opt-in custom-unit INI extensions;
- `vulkan_mod` — experimental client renderer replacement and platform-isolated Vulkan runtime;
- `rusted_fabric_example` — the development and contract example.

`fabricloader` is the reserved upstream Fabric Loader ID. It intentionally keeps Fabric's standard
spelling and must remain unchanged in dependency declarations; it is not the Rusted Fabric Loader
project ID.

New IDs should use short descriptive words separated by underscores. An independent official mod
does not need a redundant `rusted_fabric_` prefix when its name is already unambiguous. A Fabric mod
ID is a permanent compatibility key: dependencies, resource namespaces, configurations, and saved
data may refer to it, so an ID must not be renamed after public release.

Gradle project names, directories, Maven artifacts, and Jar names use readable kebab-case instead;
for example, the Fabric ID `java_mod_menu` is built by `official-mods/java-mod-menu` and emitted as
`java-mod-menu-<version>.jar`. Those names are packaging identifiers, not Fabric mod IDs.

## Version management

Loader/distribution and official-mod versions are declared independently in the repository-root
`versions.properties`. A release may update only the components that actually changed. Installer
and Android launcher builds resolve their bundled Jar names from that file, so changing a component
version there is sufficient; do not add hard-coded version strings to packaging tasks.
