# Cross-platform multiplayer compatibility

## Current status

API context version 4 defines and validates the `RFM1` cross-platform mod manifest. Windows builds
derive it from Fabric metadata; Android builds derive it from enabled `.rfmod` metadata. The common
API provides deterministic encoding, SHA-256 fingerprinting, compatibility evaluation, and
exception-isolated manifest/evaluation events.

This version does **not** yet put `RFM1` onto Rusted Warfare's network packets and therefore does not
claim automatic peer enforcement. The Loader UI reports unsafe local configurations; mods or later
network hooks can evaluate a remote manifest with the same API. A backwards-compatible transport,
packet-size limits, host policy, and user-facing kick reason must be implemented before the Loader
automatically blocks or accepts a real connection.

## Mod modes

- `client_only`: presentation, controls, diagnostics, or accessibility only. It may be installed on
  one peer and absent on another. Declaring this falsely can still cause a desync.
- `required`: changes synchronized content or behavior. Every peer must have the same mod ID,
  version, multiplayer protocol, and synchronized-content SHA-256.
- `unsafe`: no reviewed multiplayer declaration. This is the default for old or incomplete
  metadata and makes the local setup unsuitable for modded multiplayer.

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

Android `.rfmod` metadata:

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

The evaluator ignores differing `client_only` mods. It rejects unsafe declarations, missing
required mods, mode differences, and required-mod version/protocol/hash differences. Comparing with
`evaluateVanillaPeer` succeeds only when every enabled mod is genuinely client-only.

`RuntimeLifecycleEvents.LOADER_READY` is the earliest portable point at which the local manifest is
final. `MultiplayerCompatibilityEvents.LOCAL_MANIFEST_READY` announces it, and
`COMPATIBILITY_EVALUATED` observes decisions without allowing listener failures to affect the game.
