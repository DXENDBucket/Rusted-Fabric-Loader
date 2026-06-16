# v0.78 Custom turret/projectile/effect declaration semantic contracts

## Scope

v0.78 continues the v0.76-v0.77 custom-unit line and focuses on high-confidence metadata fields and helper methods derived from turret/projectile/effect declarations.

The pass deliberately avoids bulk naming `TurretTemplate`, `ProjectileTemplate`, or `EffectTemplate` residual fields without strong parser/runtime evidence. It does include `LegOrArmTemplate` visual-declaration residuals because the parser keys and runtime image/neighbour usage are direct and stable.

## Naming contracts

- `hasLaserDefenceTurrets` is a derived metadata flag set by turret declarations with `laserDefenceEnergyUse`; it is not a direct core metadata INI boolean.
- `hasProjectileInterceptorTurrets` is a derived metadata flag set by turret declarations with `interceptProjectiles_withTags`; runtime uses it to enter projectile-interceptor logic.
- `hasTurretLimitingAngles` records whether any turret has a non-negative `limitingAngle` declaration.
- `requiresTurretTagFilterTargetCheck` is a derived targeting flag: it is set only when unit-level targetability is not enough and turret-level tag filters must be checked.
- `reloadProgressTurretIndex` and `warmupProgressTurretIndex` are derived UI/progress indices selected during custom-unit finalization from turret delay/warmup characteristics.
- `hasAttachedTurretLinks` marks that at least one turret is linked through `attachedTo`; runtime uses it to propagate parent turret angle/timer changes.
- `mainNanoTurret` is the turret template selected by `isMainNanoTurret` and used by nano turret index/beam helpers.
- `hasChargeEffectImages` marks that at least one turret has `chargeEffectImage`; rendering uses it as a fast path before iterating turret charge effects.
- `addTurretAimAngle` is the runtime angle-delta helper; the `CustomUnit` override keeps attached child turrets synchronized.
- `getMainNanoTurretIndex` returns the configured main nano turret index, or `-1` when unavailable.
- `LegOrArmTemplate.middleImage` is fed by `image_leg` / `image_middle`; `endImage` is fed by `image_foot` / `image_end`.
- `shieldDeflectionMultiplier` intentionally uses semantic runtime spelling even though one config key preserves the historical `shieldDefectionMultiplier` typo.

## Deliberate exclusions

Skipped candidates are listed in `docs/rw_custom_turret_projectile_effect_skipped_rows_v0_78.csv`.

Notably skipped:

- `TurretTemplate.ap`, `TurretTemplate.az`, and `TurretTemplate.aJ`, because current evidence is mostly constructor/copy/default plumbing;
- `CustomUnitMetadata.bg`, because it still mixes construction-animation and conversion-action paths and is better handled in a custom action/effect deep branch pass;
- `LegOrArmTemplate.z` and `LegOrArmTemplate.A`, because the available parser/runtime evidence was not strong enough to assign stable names.
