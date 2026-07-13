# Android mapping profiles

This directory is reserved for versioned Android mapping handoffs. Do not place APK, DEX, decompiled sources, or other game payloads here.

Each profile lives in its own directory:

```text
android/mappings/<profile-id>/
+-- profile.properties
`-- mappings.tiny
```

The inspector accepts a profile without `mappings.tiny` while mapping work is in progress, but reports it as `PENDING_MAPPING`. A minimal placeholder profile is:

```properties
id=rw-android-1.15-code176
packageName=com.corrodinggames.rts
versionCode=176
apkSha256=
mappingFile=mappings.tiny
anchor.gameEngine=Lcom/corrodinggames/rts/gameFramework/l;
```

`apkSha256` is an optional exact-match fast path. Package, version, and structural anchors provide the fallback for resource-only or lightly modified community builds.
