# v0.82 Projectile/effect runtime semantic contracts

## Scope

v0.82 continues from v0.81 and targets the custom projectile/effect runtime boundary. It does **not** try to finish every `Projectile` motion field or every `EffectEngine` visual helper; it names the rows with direct evidence from custom projectile templates, effect draw/update code, and the `EffectTemplate.builtInEffect` dispatcher.

## Attached projectile light contract

- `Projectile.attachedLightEffect` is a cached `EffectInstance` for projectiles using custom template `lightColor` and `instantReuseLast`.
- `EffectEngine.createAttachedLightEffect` creates the light effect at a `WorldObject`, zeroes relative offsets, and attaches it.
- `EffectInstance.lightEffect` is the guard used by reuse logic.
- `EffectInstance.castLightOnGround` is set when the custom projectile template `lightCastOnGround` flag is true.

## Draw/runtime effect-state contract

- `EffectEngine.createLineEffect` sets `drawLineTo` and stores `lineTargetX/Y/Height`.
- `EffectInstance.text`, `textPaint`, `textOffsetX`, and `textOffsetY` are consumed directly by `EffectInstance.draw`.
- `fadeToColor` and `fadeToColorTime` are the color interpolation pair in `draw`.
- `cachedPaintAlpha`, `cachedPaintColor`, and `hasAppliedColorFilter` are mutable-paint cache state for avoiding repeated paint/color-filter updates.

## Built-in effect dispatch contract

`EffectTemplate.spawnEffect` maps custom `builtInEffect` values to selected `EffectEngine` helper methods:

```text
small          -> createSmallBuiltInEffect
medium/large   -> createLargeBuiltInEffect, with medium scale adjusted by caller
smoke          -> createSmokeBuiltInEffect
shockwave      -> createShockwaveBuiltInEffect
largeExplosion -> emitLargeExplosionBuiltInEffect
smallExplosion -> createSmallExplosionBuiltInEffect
resourcePoolSmoke -> createResourcePoolSmokeEffect
```

## Deliberate exclusions

Skipped candidates are listed in `docs/rw_projectile_effect_runtime_skipped_rows_v0_82.csv`. The important exclusions are ambiguous projectile motion/laser/ballistic residuals, raw effect kind constants, and `EffectInstance.an/ao` draw-position flags.
