# v0.77 CustomUnitMetadata declaration/resource residuals semantic contracts

## Scope

v0.77 continues the v0.76 custom-unit line into high-confidence `CustomUnitMetadata` declarations and loader helpers.  It focuses on fields and methods with direct INI-string, getter, or runtime-use evidence.

Covered systems:

- periodic resource generation and generated resource display/accounting;
- placement rules and mixed-selection/attackability tags;
- movement/attack/AI declaration residual fields;
- effect-presence flags for movement, repair, and reclaim effects;
- stable metadata getter/reference factory helpers;
- live custom-unit metadata replacement and leg-neighbor cache generation.

## Naming contracts

- `generationResources` is the raw `ResourceAmount` parsed from both `generation_resources` and legacy `generation_credits`. It can contain credits and arbitrary custom resources.
- `generationResourcesPerSecond` and `globalGenerationResourcesPerSecond` are derived display/accounting sets scaled by `generationRateScale = 40 / generation_delay`.
- `hasPeriodicResourceGeneration` means the generation resource amount is non-empty; actual runtime production still depends on `generationActiveLogic`.
- `placementRules` is the `PlacementRuleParser` result from the whole custom-unit INI, not a single rule.
- `pathingMovementType` is copied from `movementType` for mobile units and forced to `NONE` for buildings.
- `hasAttackTagFilters` is a derived unit-level/turret-level targeting flag and should not be treated as a direct INI boolean.
- `createUnitTypeReference` and `createActionReferenceList` create deferred references and append them to metadata post-load resolution lists.

## Deliberate exclusions

Skipped candidates are listed in `docs/rw_custom_unit_metadata_resources_skipped_rows_v0_77.csv`.

Notably skipped:

- `CustomUnitMetadata.bg`, because construction animations and conversion-action paths both write it;
- `CustomUnitMetadata.ex`, because its exact predicate comes from turret-level tag-filter analysis;
- `CustomUnitMetadata.em/en`, because they are derived from turret range/offset heuristics and need a turret-template pass;
- `d(int)` and `b(int)` price overloads, because they ignore the parameter in this build but the interface-level parameter meaning is not proven.
