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
matchPolicy=structural
packageName=com.corrodinggames.rts
versionCode=176
apkSha256=
mappingFile=mappings.tiny
anchor.gameEngine=Lcom/corrodinggames/rts/gameFramework/k;
```

Use `matchPolicy=exact` for a finalized mapping that has only been proven against one APK. Such a
profile requires package, version, and APK SHA-256 to match. `mappingFileSha256` can additionally pin
the mapping file itself; a mismatch is a hard error.

Use `matchPolicy=structural` only for a separately verified variant profile. Package, version, and
every declared structural anchor must match when its APK hash is unknown. A resource-only or
community-modified APK must not silently inherit an exact official profile.
