# v0.82 Projectile/effect runtime report

Base: v0.81 (`13401` rows)  
Output: v0.82 (`13439` rows)  
Added rows: `38`  
Updated rows: `0`  
Validation status: `pass`

## Main additions

v0.82 adds selected runtime rows around projectiles and effects:

```text
Projectile.attachedLightEffect
EffectInstance.castLightOnGround / lightEffect
EffectInstance line target / text draw / tint cache fields
EffectEngine.createLineEffect / createLightEffect / createAttachedLightEffect
EffectEngine built-in small/large/smoke/shockwave/explosion helpers
```

## Key evidence

```text
CustomProjectileTemplate.lightColor/lightSize/lightCastOnGround
Projectile.aP reuse requires EffectInstance.attachedObject==projectile and EffectInstance.d==true
EffectEngine.a(WorldObject,int,float) creates and attaches light effects
EffectEngine.a(FFFFFF) sets EffectInstance.L/M/N/O and draw() renders a line
EffectInstance.draw renders aa text with ab/ac/ad
EffectInstance.draw blends x -> y over z and caches alpha/color/filter state in au/av/aw/C/D
EffectTemplate.spawnEffect dispatches BuiltInEffectType small/medium/large/smoke/shockwave/largeExplosion/smallExplosion/resourcePoolSmoke into EffectEngine helpers
```

## Validation summary

```text
status:                                  pass
mapping rows:                            13439
added rows:                              38
updated rows:                            0
class rows:                              1159
field rows:                              5083
method rows:                             7197
duplicate mapping keys:                  0
bad constructor mappings:                0
orphan member rows:                      0
CSV/Tiny mismatch:                       0
named field collisions:                  0
named method collisions:                 0
override-family conflicts:               0
named inherited collisions:              0
current ActionDisplayGroup residue:      0
old UnitTemplateOverrideMap residue:      0
old generationCredits residue:           0
old fp->useAsBuilder residue:            0
old projectile typo residue:             0
old attachment/leg name residue:         0
projectile/effect runtime partial cover: 0
missing v0.82 new rows:                  0
inherited missing warnings:              71
skipped low-confidence rows:             6
```

## Skipped

This pass does not map the remaining projectile motion/laser/ballistic residual fields or all numeric effect kind constants. Those need a dedicated projectile-motion/laser audit rather than guessing names from serialization order.
