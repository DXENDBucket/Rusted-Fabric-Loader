# v0.79 Attachment / leg / decal runtime report

Base: v0.78 (`13314` rows)  
Output: v0.79 (`13342` rows)  
Added rows: `28`  
Updated rows: `3`  
Validation status: `pass`

## Main additions

v0.79 adds the selected-unit attached-action hook:

```text
Unit.getAttachedUnitActions
CustomUnit.getAttachedUnitActions
CustomUnit.attachedUnitActionBuffer
AttachmentBehavior.collectAttachedUnitActions
AttachmentBehavior.instance
```

It also names leg draw-layer metadata and behavior state:

```text
CustomUnitMetadata.hasDrawOverBodyLegs
CustomUnitMetadata.hasDrawUnderAllUnitsLegs
LegAnimationBehavior.instance
LegAnimationBehavior.drawLegLayer
LegAnimationBehavior.scratchSourceRect / scratchDestRect / scratchPaint
```

The biggest block is `LegRuntimeState`:

```text
index
footX / footY / footHeight / footDir
targetX / targetY / distanceToTargetSquared
moveWarmupTimer
landingEffectEmitted
moving
needsPositionReset
fallingReset
positionDirty
spinAngle
alpha
```

Runtime helper methods:

```text
CustomUnit.ensureLegRuntimeStates
CustomUnit.refreshLegRuntimeStates
CustomUnit.markLegsForFalling
```

## Updates / corrections

```text
CustomUnit.cachedWaypointActions -> CustomUnit.attachedUnitActionBuffer
AttachmentBehavior.detachOrCleanList -> AttachmentBehavior.collectAttachedUnitActions
CustomUnit.markAttachmentsFalling -> CustomUnit.markLegsForFalling
```

The first two are attachment/action UI corrections. The third is a leg-runtime correction: the method iterates `legRuntimeStates`, sets `fallingReset` and `needsPositionReset`, and refreshes leg placement.

## Validation

```text
status: pass
mapping rows: 13342
added rows: 28
updated rows: 3
duplicate mapping keys: 0
field collisions: 0
method collisions: 0
override-family conflicts: 0
old attachment/leg name residue: 0
partial coverage misses: 0
```
