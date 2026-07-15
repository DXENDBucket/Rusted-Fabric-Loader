# Cross-platform multiplayer compatibility

## Current status

API context version 5 defines and validates the `RFM1` cross-platform mod manifest. Windows builds
derive it from Fabric metadata; Android builds derive it from enabled `.javamod` metadata. The common
API provides deterministic encoding, SHA-256 fingerprinting, compatibility evaluation, and
exception-isolated manifest/evaluation events. Both backends wrap it in the same `RFH1` envelope and
carry it in game system packet `179`, which unmodified 1.15 peers safely ignore.

After client registration/server info, each Loader sends its hello and evaluates the remote
manifest. An incompatible Loader peer is disconnected with the first concrete mismatch. A peer
that does not answer within five seconds is treated as vanilla/legacy: it remains allowed only when
all local mods are truly `client_only`, `server_only`, or `optional`. The transport is capped at 256 KiB. This is Loader-level
compatibility negotiation, not encryption or an anti-cheat system.

## Mod modes

- `client_only`: presentation, controls, diagnostics, or accessibility only. It may be installed on
  one peer and absent on another. Declaring this falsely can still cause a desync.
- `server_only`: host administration or other host-authoritative behavior that requires no client
  code, assets, or synchronized state. It may be absent on every joining client, including vanilla
  clients. A mod that adds shared units or changes deterministic simulation must not use this mode.
- `optional`: the mod can operate independently on a client or host and remains useful when installed
  on only one side. If both peers have it, the mod may inspect the remote manifest from
  `PEER_EVALUATED` and enable extra negotiated features. Missing or different optional versions never
  reject a connection, so such enhancements must have their own backward-compatible negotiation and
  must not be required for deterministic simulation.
- `required`: changes synchronized content or behavior. Every peer must have the same mod ID,
  version, multiplayer protocol, and synchronized-content SHA-256.
- `unsafe`: no reviewed multiplayer declaration. This is the default for old or incomplete
  metadata and makes the local setup unsuitable for modded multiplayer.

These modes control compatibility, not initial class loading. Mods initialize normally and use
`GameSessionEvents` plus `GameSession.kind()` to activate client or host behavior. An `optional` mod
can additionally inspect the peer manifest delivered by `PEER_EVALUATED` before enabling a feature
that talks to its counterpart.

Windows `fabric.mod.json`:

```json
"custom": {
  "rustedfabric:multiplayer": {
    "mode": "required",
    "protocol": "portable-units-v1",
    "syncHash": "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef"
  }
}
```

Android `.javamod` metadata:

```properties
multiplayerMode=required
multiplayerProtocol=portable-units-v1
multiplayerSyncHash=0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef
```

The sync hash must be generated from the common deterministic gameplay data shared by both builds,
such as normalized unit definitions and protocol-visible configuration. Do not hash the Windows
JAR or Android DEX/archive: platform binaries are expected to differ.

## Common API

```java
MultiplayerManifest local = context.multiplayerManifest().orElseThrow();
MultiplayerManifest remote = MultiplayerManifest.decode(receivedPayload);
MultiplayerCompatibility.Report report =
        MultiplayerCompatibility.evaluate(local, remote);
if (!report.compatible()) {
    for (MultiplayerCompatibility.Issue issue : report.issues()) {
        // Show issue.problem() and issue.modId() to the user.
    }
}
```

The evaluator ignores differing `client_only`, `server_only`, and `optional` mods. It rejects unsafe declarations, missing
required mods, mode differences, and required-mod version/protocol/hash differences. Comparing with
`evaluateVanillaPeer` succeeds when every enabled mod is genuinely client-only, server-only, or optional.

`RuntimeLifecycleEvents.LOADER_READY` is the earliest portable point at which the local manifest is
final. `MultiplayerCompatibilityEvents.LOCAL_MANIFEST_READY` announces it, and
`COMPATIBILITY_EVALUATED` observes decisions without allowing listener failures to affect the game.
`PEER_EVALUATED` adds the live connection ID and distinguishes Loader peers from vanilla/legacy
timeouts.

## Sessions, including single-player

Portable gameplay code does not need a multiplayer-only API. `RustedFabricRuntime.currentSession()`
returns the current `GameSession`; its kind is `SINGLE_PLAYER`, `MULTIPLAYER_HOST`, or
`MULTIPLAYER_CLIENT`. `GameSessionEvents.SESSION_STARTED` and `SESSION_ENDED` use the same imports
and behavior on Windows and Android. A network connection transitions the session kind, while a
network reset returns it to single-player.

```java
GameSessionEvents.SESSION_STARTED.register(session -> {
    // Shared gameplay setup belongs here. Branch only when behavior really differs.
    if (session.multiplayer()) {
        session.localManifest().ifPresent(manifest -> log(manifest.fingerprint()));
    }
});
```
