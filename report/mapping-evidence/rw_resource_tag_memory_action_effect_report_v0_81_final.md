# v0.81 Custom resource/tag/memory action-effect report

Base: v0.80 (`13385` rows)  
Output: v0.81 (`13401` rows)  
Added rows: `16`  
Updated rows: `0`  
Validation status: `pass`

## Main additions

v0.81 adds a compact resource/tag/memory surface:

```text
getCreditGenerationPerSecond override family
getGenerationResourcesPerSecond / getGlobalGenerationResourcesPerSecond
getResourceRate / getResourceMaxConcurrentReclaimingThis / getSimilarResourceTags
getQueuedActionResourceDelta
```

The biggest family is `Unit.cy() -> getCreditGenerationPerSecond`, propagated to built-in generators and `CustomUnit`:

```text
Unit.getCreditGenerationPerSecond
CustomUnit.getCreditGenerationPerSecond
com/corrodinggames/rts/game/units/d/e.getCreditGenerationPerSecond
com/corrodinggames/rts/game/units/d/g.getCreditGenerationPerSecond
com/corrodinggames/rts/game/units/d/h.getCreditGenerationPerSecond
```

## Key evidence

```text
HUD resource display calls Unit.cy() and prints "Income: $"
Extractor/fabricator update paths add cy() * Team.ap / Team.ao to team credits
CustomUnit.cy() reads generationResources credit amount and generationRateScale when generationResourcesActive
CustomUnit.cz()/cA() return generationResourcesPerSecond/globalGenerationResourcesPerSecond while active
CustomUnit.cQ()/cR()/g() expose resourceMaxConcurrentReclaimingThis/similarResourcesHaveTag/resourceRate
CustomUnit.by() iterates queued CustomAction entries and aggregates customTimerDelta/addResources/addResourcesScaledByAIHandicaps
```

## Validation summary

```text
status:                                  pass
mapping rows:                            13401
added rows:                              16
updated rows:                            0
class rows:                              1159
field rows:                              5062
method rows:                             7180
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
resource/tag/memory partial coverage:    0
missing v0.81 new rows:                  0
inherited missing warnings:              71
skipped low-confidence rows:             7
```

## Skipped

This pass leaves `Unit.cP()/CustomUnit.cP()`, `Unit.dh()/CustomUnit.dh()`, and `CustomAction.F()Z` for later because their contracts are less clean than the resource-rate and queued-resource-delta rows.
