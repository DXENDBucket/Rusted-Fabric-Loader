# v0.83 Projectile motion / laser / ballistic semantic contracts

## Scope

v0.83 continues from v0.82 and audits the remaining high-confidence `Projectile` motion, laser, contact-collision, and ballistic runtime fields. It deliberately avoids legacy serialization-only booleans and ambiguous draw paint constants.

## Uninterceptable / deflection contract

- `Projectile.uninterceptable` is set when a projectile template's `deflectionPower` is too low to use as a real deflection strength.
- Laser-defence / anti-projectile scans skip `uninterceptable` projectiles; non-skipped projectiles use `Projectile.deflectionPower`.

## Continuous damage contract

- `Projectile.continuousDamage` keeps a laser-style projectile alive after contact and repeatedly applies damage each frame.
- `Projectile.continuousDamageRampUpDuration` is the denominator used by `getLifeProgressRatio`: while `ageTimer < continuousDamageRampUpDuration`, damage ramps by `ageTimer / duration`; otherwise the ratio is `1`.
- `Projectile.continuousDamageSmokeTimer` is an auxiliary visual timer for target-following smoke/explosion puffs during continuous damage.

## Lighting-effect contract

- `Projectile.lightingEffectRefreshTimer` controls refreshes of `lightingEffectSegmentOffsets`.
- `Projectile.lightingEffectSegmentOffsets` is a randomized segment-jitter array used when drawing the `lightingEffect` line.

## Target and contact-collision contract

- `Projectile.hasFixedTargetPosition` means the projectile can operate without a live target unit, using fixed `targetGroundX/Y` coordinates.
- `Projectile.collideWithUnits` enables broad-phase/final-radius unit contact checks.
- `Projectile.contactCollisionRadius` is the projectile-side contact radius added to a target unit's radius.
- `Projectile.collideWithTerrain` enables path-grid terrain impact checks.

## Ballistic contract

- `Projectile.ballisticReachedPeak` flips once ballistic height reaches `ballisticHeight`.
- `Projectile.ballisticHeightSpeed` controls vertical interpolation speed. Negative values fall back to normal projectile speed.

## Impact contract

- `Projectile.impactTriggered` is reset when a source/position is assigned and set when update resolves impact.
- Once set, update runs the area-damage expansion/removal countdown instead of normal travel collision.
- `Projectile.areaDamageUnitScratchList` is the static scratch list used for unit broad-phase collection during area damage.

## Deliberate exclusions

Skipped candidates are listed in `docs/rw_projectile_motion_laser_ballistic_skipped_rows_v0_83.csv`. The important exclusions are `Projectile.T`, `Projectile.aB`, `Projectile.aj`, and visual paint constants.
