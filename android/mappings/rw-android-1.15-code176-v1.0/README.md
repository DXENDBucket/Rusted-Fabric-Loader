# Android 1.15 vc176 mapping profile

This profile imports the mapping-only handoff `rw-android-1.15-vc176-mapping-v1.0-final`.
It is bound to the official `com.corrodinggames.rts` APK with SHA-256
`328f37106985a2ba424efec9ac312ede0395f3bac56e3d5db5d642dd6aecc04c`.
Do not apply it directly to a modified or package-renamed APK.

## Included inputs

- `mappings.tiny`: loader-safe runtime mapping, `official -> intermediary -> named`;
- `mappings-strict.tiny`: conservative class surface for API generation;
- `mapping-table.csv`: confidence, category, evidence, and API-stability metadata;
- `pc-android-class-crosswalk.csv`: the frozen shared named-class namespace;
- `optimizer-alias-collisions.csv`: the R8 virtual-family exception that runtime remapping must preserve.

The runtime mapping contains 1,602 first-party classes and 9,213 member mappings. For public API
generation, accept only stable/reviewed entries with high/medium confidence, exclude generated R and
Android-core-residual categories, and keep internal helpers private unless a concrete hook needs one.

The two colliding `a(GameInputStream)` virtual methods documented in
`optimizer-alias-collisions.csv` intentionally remain in their official namespace in the loader-safe
Tiny. Expose distinct adapter names outside the original hierarchy if those operations become API.

## Provenance

- source archive: `rw_android_1_15_v1_0_FINAL_MAPPING_CODEX_HANDOFF.zip`
- source archive SHA-256: `4c5eb582fef7bb02b55b4af16c6d1f3897aa3b70932b03db9ed3cdd128058b1b`
- all 46 hashes in the handoff `SHA256SUMS.txt`: verified
- runtime Tiny SHA-256: `7df59d61092a7665f023242b0221baf3ba5a3e8a3f2415bfd85a247070676d07`
- strict Tiny SHA-256: `c336cc01392b244e026350a1d0a3bf92fd5791b6ef872dbb5d0d0334ce6b7d7b`

Decoded resources, game strings, the decoded manifest, mapping-generation scripts with session-local
paths, and analysis-only artifacts are not imported because the loader does not need them. No APK,
DEX, game asset, signing key, or decompiled game implementation is stored in this profile.
