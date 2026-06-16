# v0.80 Custom action/effect deep branch report

Base: v0.79 (`13342` rows)  
Output: v0.80 (`13385` rows)  
Added rows: `43`  
Updated rows: `0`  
Validation status: `pass`

## Main additions

v0.80 adds three related surfaces:

```text
LocalizedString dynamic resolver / cached locale surface
MutableUnitStats runtime delta and field-list helper surface
CustomAction target-point dispatch and CustomUnit runtime action context
```

LocalizedString rows include:

```text
LocalizedString.EMPTY
LocalizedString.dynamicTextResolvers
LocalizedString.localizedEntries
LocalizedString.cachedText
LocalizedString.cachedLocaleVersion
LocalizedString.translationKey
LocalizedString.dynamicParseError
LocalizedString.metadata
LocalizedString.fromLiteral / fromData
LocalizedString.refreshTextAndDynamicResolvers
LocalizedString.parseDynamicTextResolvers
LocalizedString.resolveDynamicTextForUnit / resolveForUnit / resolveStaticText
LocalizedString.refreshLocalizedText
```

Mutable stat helper rows include:

```text
MutableUnitStats.registerMutableStatAccessor
MutableUnitStats.getMutableStatAccessorById
MutableUnitStats.writeRuntimeStatDelta
MutableUnitStats.readRuntimeStatDelta
MutableUnitStats.parseMutableStatAccessorListFromConfig
MutableUnitStats.parseMutableStatAccessorList
```

Action/context rows include:

```text
UnitAction.usesActionTargetPoint
UnitSpecificActionProxy.usesActionTargetPoint
CustomAction.usesActionTargetPoint
CustomUnit.currentActionTargetPoint
CustomUnit.currentActionTargetUnit
CustomUnit.currentActionRepeatedCount
OrderableUnit.checkTargetedActionOrder / CustomUnit.checkTargetedActionOrder
OrderableUnit.onTargetedActionQueued / CustomUnit.onTargetedActionQueued
CustomUnit.getActionsForCurrentMetadata
CustomUnit.findActionById
CustomUnit.findBuildQueueActionForUnitType
CustomUnit.getUpgradeActions / getFirstUpgradeActionId / collectSecondaryUpgradeActionIds
CustomUnit.generationDelayTimer / generationResourcesActive / updateUnitMemoryTimer
```

## Key evidence

```text
%{...} dynamic text parser branches
LogicBooleanLoader.parseBooleanBlock(metadata, expression, false)
MutableUnitStats serialized accessor id/value/default-value deltas
AI/HUD branches on UnitAction.A() before issuing actionId + PointF commands
"checkTargetedActionOrder:"
UnitReference$ThisActionTargetReference reads CustomUnit.dN/dM
ThisActionRepeatedCount reads CustomUnit.dO
CustomUnit.update uses generationDelay/updateUnitMemory timers
```

## Validation summary

```text
status:                         pass
mapping rows:                   13385
added rows:                     43
updated rows:                   0
class rows:                     1159
field rows:                     5062
method rows:                    7164
duplicate mapping keys:         0
bad constructor mappings:       0
orphan member rows:             0
CSV/Tiny mismatch:              0
named field collisions:         0
named method collisions:        0
override-family conflicts:      0
custom action/effect partial:   0
missing v0.80 new rows:         0
inherited missing warnings:     71
skipped low-confidence rows:    6
```

## Skipped

The pass still skips `CustomAction.F()` because no non-proxy runtime branch was found; constructors/bridges and unrelated draw scratch fields are also left out.
