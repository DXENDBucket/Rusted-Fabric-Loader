# Mapping workflow

The canonical mapping file is `src/main/resources/mappings/mappings.tiny`. It uses Tiny v2 namespaces in this order:

```text
official -> intermediary -> named
```

The current mapping baseline is `1.1 FINAL` (mapping-only completion + executable gate): 1440 classes, 5470 fields, and 7880 methods, for 14790 rows in total. Effective non-generated class coverage is 1440/1440 (100%) after excluding 13 generated Android `R` classes. These counts are lower bounds in the automated validator, allowing mapping coverage to grow while preventing accidental truncation.

The v1.1 baseline was imported byte-for-byte from the validated mapping handoff and is based on v1.0 FINAL. Its release audit reports a passing static and executable gate. Structural `$N` class names must remain structural unless direct semantic evidence supports a better name.

## Android mapping profile

Android uses a separate official namespace and must never consume the PC Tiny directly. The finalized
Android 1.15/vc176 profile is stored at
`android/mappings/rw-android-1.15-code176-v1.0` and is bound to APK SHA-256
`328f37106985a2ba424efec9ac312ede0395f3bac56e3d5db5d642dd6aecc04c`.

- `mappings.tiny` is the loader-safe runtime mapping: 1,602 classes and 9,213 members.
- `mappings-strict.tiny` and `mapping-table.csv` are the conservative API-generation inputs.
- `pc-android-class-crosswalk.csv` preserves the shared named class namespace.
- `optimizer-alias-collisions.csv` documents two R8-colliding methods intentionally excluded from
  runtime member renaming.

Run `gradlew.bat :android:apk-inspector:check` to verify imported hashes, row counts, profile policy,
and the absence of APK/DEX/game implementation payloads.

## Required checks

After editing mappings or named Mixins, run:

```bat
gradlew.bat check
gradlew.bat generateNamedGameJar
gradlew.bat :rusted-fabric-api:remapJarToOfficial
gradlew.bat :example-mod:remapJarToOfficial
gradlew.bat verifyDistribution
```

`validateMappings`, which is part of `check`, verifies:

- the expected Tiny v2 header and namespaces;
- non-empty and structurally valid class/member rows;
- no orphan or duplicate member keys;
- constructors remain named `<init>`;
- mapping coverage does not fall below the v1.1 FINAL baseline;
- every named Mixin source is configured exactly once;
- no hand-maintained official Mixin copy remains;
- strict Mixin failure settings remain enabled.

`verifyDistribution` then inspects the produced Jars, checks provider/API/example version metadata, confirms every configured named Mixin exists, and rejects named game targets left inside the official-runtime Mixin bytecode.

A multi-target Mixin must be split when one named method maps to different official selectors across its targets. `RemapJar` fails the build on this ambiguity; merging the selectors can accidentally match an unrelated overload on another target class.

## Semantic evidence

Automated structure checks cannot prove that a readable name is semantically correct. Each mapping pass should continue to include focused evidence under `report/mapping-evidence/`, including additions/updates, skipped low-confidence candidates, semantic contracts, and collision/inheritance audit output.

Do not name ambiguous scratch fields only to increase coverage. Preserve the current practice of deferring them to a focused runtime-flow audit.

## Runtime verification

Use `installToGameDir -PgameDir=...` only after all checks and remap tasks pass. The game directory is local machine state and must be supplied as a command-line property; never commit an absolute installation path.
