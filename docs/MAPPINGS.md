# Mapping workflow

The canonical mapping file is `src/main/resources/mappings/mappings.tiny`. It uses Tiny v2 namespaces in this order:

```text
official -> intermediary -> named
```

The current mapping baseline is `0.90` (Accelerated Completion RC): 1372 classes, 5190 fields, and 7457 methods, for 14019 rows in total. First-party class coverage is 1372/1453 (94.43%). These counts are lower bounds in the automated validator, allowing mapping coverage to grow while preventing accidental truncation.

The v0.90 baseline was imported from the validated mapping handoff. Its release audit reports zero orphan mappings, duplicate keys, named collisions, override-family conflicts, inherited missing warnings, and CSV/Tiny drift. Structural `$N` class names must remain structural unless direct semantic evidence supports a better name.

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
- mapping coverage does not fall below the v0.90 baseline;
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
