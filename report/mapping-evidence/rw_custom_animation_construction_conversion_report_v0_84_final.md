# v0.84 Custom animation / construction / conversion report

Base: v0.83 (`13453` rows)  
Output: v0.84 (`13462` rows)  
Added rows: `9`  
Updated rows: `0`  
Validation status: `pass`

## Main additions

```text
CustomUnitMetadata.hasBuildQueueRuntimeEffects
CustomUnit.currentBuildQueueActionBlocksMovement
CustomUnit.createdEventPending
CustomUnit.completeAndActiveEventPending
CustomUnit.lastLegBaseX
CustomUnit.lastLegBaseY
CustomUnit.lastLegBaseHeight
CustomUnit.lastLegBaseDir
CustomUnit.autoTriggerCooldownTimer
```

## Key evidence

```text
CustomUnitLoader sets l.bg for queuedUnits animation and whenBuilding_* effects
CustomUnit.update checks metadata.bg/revertMetadata.bg before temporary conversion/playAnimation/rotateTo/triggerAction handling
CustomAction.isWhenBuildingCannotMove is stored in CustomUnit.g and movement velocity is zeroed while it is true
Constructor initializes h/i true
First update clears i and fires CustomUnitEventType.CREATED
Later update clears h and fires CustomUnitEventType.COMPLETE_AND_ACTIVE
LegAnimationBehavior computes deltas from eo/ep/eq/bodyDir minus dP/dQ/dR/dS and refreshes them afterward
CustomUnit.du/reset-position paths initialize dP/dQ/dR/dS from current transform
Auto-trigger update decrements w and resets it from autoTriggerCooldownTime after firing
```

## Validation summary

```text
status:                                      pass
mapping rows:                                13462
added rows:                                  9
updated rows:                                0
class rows:                                  1159
field rows:                                  5106
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
custom animation/construction partial:       0
missing v0.84 new rows:                      0
inherited missing warnings:                  71
skipped low-confidence rows:                 6
```

## Skipped

This pass does not name the remaining custom-unit draw/motion scratch fields or generic action-effect flags. Those should be handled with a dedicated animation/render scratch audit or a resource-conversion action-effect pass.
