# v0.81 Custom resource/tag/memory action-effect semantic contracts

## Scope

v0.81 continues from v0.80 and intentionally stays narrow. It names resource/tag/memory-adjacent runtime accessors that are directly supported by metadata fields, HUD/accounting callsites, or custom action queue resource-effect logic.

## Generation contracts

- `getCreditGenerationPerSecond` is the `Unit.cy()` override family. The base returns `0`; built-in credit generators return fixed/tiered rates; `CustomUnit` returns the credit component of `generationResources` scaled by `generationRateScale` while `generationResourcesActive` is true.
- `getGenerationResourcesPerSecond` is the full custom-resource rate view. Base `Unit` wraps credit generation into a `StoredResourceSet`; `CustomUnit` returns `CustomUnitMetadata.generationResourcesPerSecond` while active.
- `getGlobalGenerationResourcesPerSecond` is the non-built-in global custom-resource subset. Base `Unit` returns empty; `CustomUnit` returns `CustomUnitMetadata.globalGenerationResourcesPerSecond` while active.

## Resource reclaim/tag contracts

- `getResourceRate` exposes custom metadata `resourceRate`; base `Unit` returns `0`.
- `getResourceMaxConcurrentReclaimingThis` exposes `resourceMaxConcurrentReclaimingThis`; base `Unit` returns `Integer.MAX_VALUE`.
- `getSimilarResourceTags` exposes `similarResourcesHaveTag`; base `Unit` returns `null`.

## Queued action resource delta contract

- `getQueuedActionResourceDelta` is rooted at `OrderableUnit.by()`, which returns an empty `ResourceAmount`.
- `CustomUnit.by()` scans the current build/action queue, resolves queued `CustomAction` rows, and accumulates `customTimerDelta`, `addResources`, and `addResourcesScaledByAIHandicaps` into a `ResourceAmount`.

## Deliberate exclusions

Skipped candidates are listed in `docs/rw_resource_tag_memory_action_effect_skipped_rows_v0_81.csv`. The important ones are `cP()` construction-state side effects, `dh()` static type tags, and `CustomAction.F()Z` because its runtime consumer is still not established.
