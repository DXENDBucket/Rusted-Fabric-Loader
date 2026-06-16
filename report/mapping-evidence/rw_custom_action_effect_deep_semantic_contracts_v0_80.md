# v0.80 Custom action/effect deep branch semantic contracts

## Scope

v0.80 continues the custom-unit line from v0.76-v0.79. Most `custom/a/a/*` action-effect fields were already named earlier, so this pass targets deeper residuals around localized action text, self-mutation stat writers, target-point custom action dispatch, current action LogicBoolean context, and periodic generation/update-memory timers.

## LocalizedString contracts

- `LocalizedString.EMPTY` is the static empty singleton created from `fromLiteral("")`.
- `localizedEntries` are raw locale/text pairs copied from `LocalizedStringData` or built by `fromLiteral`.
- `translationKey` bypasses locale entries and asks the translation manager for the current localized text.
- `cachedText` and `cachedLocaleVersion` cache the resolved locale text until the global locale version changes.
- `refreshTextAndDynamicResolvers(boolean)` updates `cachedText`, then parses dynamic `%{...}` expressions when present.
- `parseDynamicTextResolvers` creates a sequence of static and LogicBoolean-backed text resolver pieces.
- `resolveForUnit` refreshes on locale change and evaluates dynamic resolvers against the supplied unit.
- `resolveStaticText` returns locale-resolved text without unit-context dynamic expansion.

## MutableUnitStats contracts

- `registerMutableStatAccessor` is a static initializer helper for accessor registry population.
- `getMutableStatAccessorById` resolves compact serialized accessor ids.
- `writeRuntimeStatDelta` writes only runtime stat fields whose values differ from metadata defaults.
- `readRuntimeStatDelta` reads those ids and values, marks the unit runtime stats dirty, and writes values through the accessor.
- `parseMutableStatAccessorListFromConfig` / `parseMutableStatAccessorList` are used for comma-separated mutable-stat field lists such as keep-current-field declarations and stat-copy filters.

## Target-point action contracts

- `UnitAction.usesActionTargetPoint` is the base override-family contract. The base returns false; `CustomAction` returns true; `UnitSpecificActionProxy` delegates.
- AI and HUD callsites branch on this method before issuing an `actionId + PointF` command rather than a build/unit placement command.
- `CustomUnit.checkTargetedActionOrder` validates `fireTurretXAtGround`, range, passability restrictions, and related map-location constraints.
- `CustomUnit.onTargetedActionQueued` runs queue-time custom action side effects such as queue sounds/effects and guide decals.

## Current action context contracts

- `currentActionTargetPoint`, `currentActionTargetUnit`, and `currentActionRepeatedCount` are saved/restored around `executeActionWithContext`.
- `UnitReference$ThisActionTargetReference` reads the target unit first, then falls back to the target point marker.
- `LogicBooleanGameFunctions$ThisActionRepeatedCount` reads the repeat count for action-recursion LogicBoolean usage.

## Periodic generation/update-memory contracts

- `generationDelayTimer` accumulates update delta until `CustomUnitMetadata.generationDelay`, then applies `generationResources` if active.
- `generationResourcesActive` caches the current `generationActiveLogic` result and is used when updating team registry state.
- `updateUnitMemoryTimer` accumulates until `updateUnitMemoryRate`, then executes `updateUnitMemory`; it is serialized for compatibility.

## Deliberate exclusions

Skipped candidates are listed in `docs/rw_custom_action_effect_deep_skipped_rows_v0_80.csv`. Notably skipped: `CustomAction.F()` because a non-proxy runtime consumer was not found, low-value constructors/bridges, and unrelated draw/animation scratch fields.
