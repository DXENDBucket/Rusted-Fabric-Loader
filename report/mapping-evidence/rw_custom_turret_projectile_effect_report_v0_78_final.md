# v0.78 Custom turret/projectile/effect declaration report

Base: v0.77.1 (`13287` rows)  
Output: v0.78 (`13314` rows)  
Added rows: `27`  
Updated rows: `2`  
Validation status: `pass`

## Main additions

v0.78 names high-confidence `CustomUnitMetadata` fields that bridge custom turret/projectile/effect declarations into runtime systems:

```text
bE -> hasLaserDefenceTurrets
bF -> hasProjectileInterceptorTurrets
bG -> hasTurretLimitingAngles
ex -> requiresTurretTagFilterTargetCheck
em -> reloadProgressTurretIndex
en -> warmupProgressTurretIndex
fU -> hasAttachedTurretLinks
fV -> mainNanoTurret
fP -> hasChargeEffectImages
```

It also completes a small runtime helper family:

```text
OrderableUnit.addTurretAimAngle
CustomUnit.addTurretAimAngle
OrderableUnit.getMainNanoTurretIndex
CustomUnit.getMainNanoTurretIndex
```

And it adds the high-confidence `LegOrArmTemplate` visual-declaration surface:

```text
index
name
isLeg
alwaysHidden
middleImage / middleTeamImages
endImage / endTeamImages / endShadowImage
hasZoomedOutDrawOverride
neighboringLegIndices
copyFrom
parseFromConfig
```

## Updates

```text
CustomProjectileTemplate.shieldDefectionMultiplier -> shieldDeflectionMultiplier
CustomProjectileTemplate.ballisticDelaymoveHeight  -> ballisticDelayMoveHeight
```

The first update keeps the mapping aligned with base/runtime projectile semantics while acknowledging that the INI key itself carries the historical `shieldDefectionMultiplier` spelling.

## Key evidence

- `laserDefenceEnergyUse` sets `l.bE` and installs turret projectile behaviour.
- `interceptProjectiles_withTags` sets `l.bF`; runtime checks the flag before projectile-interceptor logic.
- `limitingAngle` sets `l.bG` when any turret has a limiting angle.
- Loader finalization scans turret tag filters and sets `l.ex`; runtime targetability then checks turret tag filters.
- Loader finalization selects turret indices into `l.em` and `l.en`; `CustomUnit.bW()` uses those indices for reload/warmup progress display.
- `attachedTo` sets `l.fU`; runtime propagates angle/timer changes to attached child turrets.
- `isMainNanoTurret` stores the current `TurretTemplate` in `l.fV`; runtime nano helpers use it for the main nano turret index.
- `chargeEffectImage` sets `l.fP`; render code uses it to draw charge-effect images by warmup ratio.
- `image_leg` / `image_middle`, `image_foot` / `image_end`, `image_*_teamColors`, and shadow keys map directly onto the new `LegOrArmTemplate` image fields.
- `computeLegNeighborIndexCache` writes nearby leg/arm template indices into `neighboringLegIndices`.

## Validation notes

```text
duplicate_mapping_keys: 0
bad_constructor_mappings: 0
orphan_member_rows: 0
csv_tiny_mismatch: 0
named_field_collisions: 0
named_method_collisions: 0
override_family_named_conflicts: 0
named_inherited_collisions: 0
current_ActionDisplayGroup_residue: 0
old_UnitTemplateOverrideMap_residue: 0
old_generationCredits_residue: 0
old_fp_useAsBuilder_residue: 0
old_projectile_typo_residue: 0
custom_turret_projectile_effect_partial_coverage: 0
missing_v0_78_new_rows: 0
inherited_missing_warnings: 71
skipped_low_confidence_rows: 7
```

Evidence folder: `evidence/javap_v0_78_custom_turret_projectile_effect/`.
