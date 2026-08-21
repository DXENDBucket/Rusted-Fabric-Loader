# Cross-platform multiplayer compatibility

## Current status

API context version 5 defines and validates the `RFM1` cross-platform mod manifest. Both host
platforms derive it from the same Fabric metadata in enabled Java mods. The common API provides
deterministic encoding, SHA-256 fingerprinting, compatibility evaluation, and exception-isolated
manifest/evaluation events. The shared Loader runtime wraps it in the same `RFH1` envelope and
carry it in game system packet `179`, which unmodified 1.15 peers safely ignore.

After client registration/server info, each Loader sends its hello and evaluates the remote
manifest. An incompatible Loader peer is disconnected with the first concrete mismatch. A peer
that does not answer within five seconds is treated as vanilla/legacy and remains allowed: Loader
or API support is never itself a prerequisite for joining a game. The transport is capped at
256 KiB. This is Loader-level compatibility negotiation, not encryption or an anti-cheat system.

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
- `required`: changes synchronized content or behavior. Two Loader peers must have the same mod ID,
  version, multiplayer protocol, and synchronized-content SHA-256. A vanilla/legacy peer cannot
  participate in this negotiation and is allowed through; the game or mod must use native
  synchronization where that peer also needs the content.
- `unsafe`: no reviewed multiplayer declaration. This is the default for old or incomplete
  metadata and makes the local setup unsuitable for modded multiplayer.

These modes control compatibility, not initial class loading. Mods initialize normally and use
`GameSessionEvents` plus `GameSession.kind()` to activate client or host behavior. An `optional` mod
can additionally inspect the peer manifest delivered by `PEER_EVALUATED` before enabling a feature
that talks to its counterpart.

Shared `fabric.mod.json` metadata:

```json
"custom": {
  "rusted_fabric:multiplayer": {
    "mode": "required",
    "protocol": "portable-units-v1",
    "syncHash": "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef"
  }
}
```

The sync hash must be generated from deterministic gameplay data, such as normalized unit
definitions and protocol-visible configuration. Do not use the whole mod Jar hash: harmless
client-only resources or packaging changes should not alter synchronized-content compatibility.

An installed mod whose synchronized behavior is genuinely opt-in may declare itself `optional`
statically and call `MultiplayerRequirements.activate(MultiplayerMod.required(...))` when such
content is actually parsed or enabled. Subsequent handshakes use the effective manifest, where the
runtime requirement replaces that optional row. The returned activation handle can undo a
temporary requirement; content loaders should normally keep it active for the process lifetime.
This mechanism must be activated before opening a peer connection and must never be used to hide
already-active deterministic behavior.

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

The evaluator ignores differing `client_only`, `server_only`, and `optional` mods. Between two
Loader peers it rejects unsafe declarations, missing required mods, mode differences, and
required-mod version/protocol/hash differences. `evaluateVanillaPeer` always succeeds because the
absence of Loader negotiation is not evidence that the native game connection is incompatible.

`RuntimeLifecycleEvents.LOADER_READY` is the earliest portable point at which the static local
manifest is available. `MultiplayerCompatibilityEvents.LOCAL_MANIFEST_READY` announces its current
effective form; later runtime requirements are folded into each handshake. The
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
