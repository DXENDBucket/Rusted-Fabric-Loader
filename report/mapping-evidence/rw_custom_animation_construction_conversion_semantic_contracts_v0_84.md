# v0.84 Custom animation / construction / conversion semantic contracts

## Scope

v0.84 continues from v0.83 and maps only the highest-confidence custom-unit runtime residuals around build-queue effects, creation/active events, and leg animation base snapshots. It avoids deep method naming for the giant `CustomUnit.update` branch and leaves ambiguous draw/movement scratch fields for a later pass.

## Build-queue runtime effect gate

- `CustomUnitMetadata.hasBuildQueueRuntimeEffects` is a metadata-level capability flag.
- The loader sets it when a unit has queued-units animation or `whenBuilding_*` runtime effects such as `whenBuilding_playAnimation`, `whenBuilding_temporarilyConvertTo`, `whenBuilding_triggerAction`, `whenBuilding_rotateTo`, or `whenBuilding_cannotMove`.
- `CustomUnit.update` checks this flag on the active metadata or temporary revert metadata before entering the build-queue conversion/animation branch.
- `CustomUnit.currentBuildQueueActionBlocksMovement` is the per-unit runtime latch populated from the current `CustomAction.isWhenBuildingCannotMove()` result. While true, the unit reports it cannot move and movement velocity is zeroed.

## Creation and active event latches

- `CustomUnit.createdEventPending` starts true in the constructor. The first update clears it, runs creation behavior hooks, fires `CustomUnitEventType.CREATED`, and also gates the created animation path.
- `CustomUnit.completeAndActiveEventPending` starts true in the constructor. A later update clears it and fires `CustomUnitEventType.COMPLETE_AND_ACTIVE` after behavior update hooks have run.

## Leg animation base snapshots

- `CustomUnit.lastLegBaseX`, `lastLegBaseY`, `lastLegBaseHeight`, and `lastLegBaseDir` store the previous base transform used by leg animation.
- `LegAnimationBehavior` computes deltas from current unit position/height/direction against these fields, updates leg runtime state, then refreshes the fields to current values.
- `CustomUnit.du()` and position reset paths initialize the snapshots from current unit position/height and body direction or the configured main-turret direction.

## Auto-trigger cooldown

- `CustomUnit.autoTriggerCooldownTimer` is the runtime cooldown latch for auto-triggered custom actions.
- It decrements while positive and is reset from `CustomUnitMetadata.autoTriggerCooldownTime` after an auto-trigger fires.

## Deliberate exclusions

Skipped candidates are listed in `docs/rw_custom_animation_construction_conversion_skipped_rows_v0_84.csv`. The main exclusions are ambiguous custom-unit draw/motion scratch fields, generic action flags without a stable consumer, and deeper resource-conversion action-effect internals.
