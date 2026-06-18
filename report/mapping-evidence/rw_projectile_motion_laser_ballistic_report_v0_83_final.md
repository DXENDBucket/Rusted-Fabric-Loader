# v0.83 Projectile motion / laser / ballistic report

Base: v0.82 (`13439` rows)  
Output: v0.83 (`13453` rows)  
Added rows: `14`  
Updated rows: `0`  
Validation status: `pass`

## Main additions

v0.83 adds high-confidence `Projectile` residual fields:

```text
Projectile.uninterceptable
Projectile.continuousDamage
Projectile.continuousDamageRampUpDuration
Projectile.lightingEffectRefreshTimer
Projectile.lightingEffectSegmentOffsets
Projectile.collideWithUnits
Projectile.collideWithTerrain
Projectile.contactCollisionRadius
Projectile.hasFixedTargetPosition
Projectile.ballisticReachedPeak
Projectile.ballisticHeightSpeed
Projectile.continuousDamageSmokeTimer
Projectile.impactTriggered
Projectile.areaDamageUnitScratchList
```

## Key evidence

```text
BaseProjectileTemplate.deflectionPower < 0.5 -> Projectile.C=true
Anti-projectile scan skips Projectile.C and uses Projectile.H for deflection strength
Experimental laser projectile setup sets E=true and initializes J/F
Projectile.getLifeProgressRatio returns J/F while J < F
continuousDamage branch applies U/60*delta*ratio and suppresses normal one-shot removal
lightingEffect draw branch refreshes N and fills O with random segment offsets
fixed-ground projectile creators set aC=true and n/o target coordinates
ballistic branch raises eq toward aJ, sets aK, then lowers toward target height
contact collision branch uses as + aA; terrain collision branch uses at + path grid
impact sets bn=true and later drives area expand/removal countdowns
area-damage broadphase uses static scratch list bi
```

## Validation summary

```text
status:                                      pass
mapping rows:                                13453
added rows:                                  14
updated rows:                                0
class rows:                                  1159
field rows:                                  5097
method rows:                                 7197
duplicate mapping keys:                      0
bad constructor mappings:                    0
orphan member rows:                          0
CSV/Tiny mismatch:                           0
named field collisions:                      0
named method collisions:                     0
override-family conflicts:                   0
named inherited collisions:                  0
current ActionDisplayGroup residue:          0
old UnitTemplateOverrideMap residue:         0
old generationCredits residue:               0
old fp->useAsBuilder residue:                0
old projectile typo residue:                 0
old attachment/leg name residue:             0
projectile motion/laser/ballistic partial:   0
missing v0.83 new rows:                      0
inherited missing warnings:                  71
skipped low-confidence rows:                 6
```

## Skipped

This pass does not map `Projectile.T`, `Projectile.aB`, `Projectile.aj`, or visual paint constants. Those either appear only in legacy IO or need a separate unit-type/method-contract audit.
