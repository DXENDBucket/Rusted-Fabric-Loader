# v0.77 CustomUnitMetadata declaration/resource residuals report

Base: v0.76 (`13234` rows)  
Output: v0.77 (`13287` rows)  
Added rows: `53`  
Updated rows: `1`  
Validation status: `pass`

## Main additions

- Added `CustomUnitMetadata` resource generation fields: `generationResources`, `hasPeriodicResourceGeneration`, `generationResourcesPerSecond`, `globalGenerationResourcesPerSecond`, `generationRateScale`, and `generationActiveLogic`.
- Added high-confidence placement/resource/UI fields such as `placementRules`, `energyDisplayName`, `showActionsWithMixedSelectionIfOtherUnitsHaveTag`, `canOnlyBeAttackedByUnitsWithTags`, and `unitsSpawnedOnDeath`.
- Added movement/attack/AI declaration residuals: `pathingMovementType`, `useAsBuilder`, `useAsTransport`, `moveAccelerationSpeed`, `moveDecelerationSpeed`, `ignoreMoveOrders`, `landOnGround`, `attackMovement`, `mainTurretIndex`, target filter booleans/tag sets, `aiTags`, harvester-base tag gating, and AI high-priority tracking.
- Added stable getter/reference helpers on `CustomUnitMetadata` and two loader helpers: `replaceLiveCustomUnitsOfType` and `computeLegNeighborIndexCache`.

## Important correction

`CustomUnitMetadata.co` was updated:

```text
old: generationCredits
new: generationResources
```

Evidence: the loader parses `generation_resources` into `l.co`, then merges legacy `generation_credits` into the same `ResourceAmount`; runtime generation applies that complete `ResourceAmount`, not only credits.

## Validation notes

- Duplicate mapping keys: `0`
- Bad constructor mappings: `0`
- Orphan member rows: `0`
- CSV/Tiny mismatch: `0`
- Override-family conflicts: `0`
- Named inherited collisions: `0`
- ActionDisplayGroup residue: `0`
- Old UnitTemplateOverrideMap residue: `0`
- Old generationCredits residue: `0`
- CustomUnitMetadata resources partial coverage: `0`
- Inherited missing warnings: `71`
- Low-confidence skipped rows: `4`

Evidence folder: `evidence/javap_v0_77_custom_unit_metadata_resources/`.
