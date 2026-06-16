# v0.79 Attachment / leg / decal runtime semantic contracts

## Scope

v0.79 continues the custom-unit line from v0.76-v0.78 and focuses on runtime residuals adjacent to attachments, leg/arm drawing, and decal-adjacent render behavior.

The pass is intentionally narrow: decal template fields were already mostly complete, so v0.79 does not bulk-map enum/scratch constructor boilerplate. The useful new surface is the bridge from attached units into the command UI and the bridge from `LegOrArmTemplate` declarations into `LegRuntimeState` update/draw state.

## Naming contracts

- `getAttachedUnitActions(boolean)` is the selected-unit command UI hook. Base `Unit` returns null; `CustomUnit` returns a reused buffer only when attached child units expose actions through `AttachmentSlot.showAllActionsFrom`.
- `collectAttachedUnitActions` appends wrapped child actions; it does **not** detach units or clean a list.
- `attachedUnitActionBuffer` is a temporary/reused action list on `CustomUnit`, not a waypoint-action cache.
- `hasDrawOverBodyLegs` and `hasDrawUnderAllUnitsLegs` are derived metadata flags set by individual leg/arm template declarations. They gate extra draw passes around the body/under-all-units render layers.
- `drawLegLayer(unit, dt, drawOverBody, drawUnderAllUnits)` is the static leg/arm renderer. It filters templates by `drawOverBody` and `drawUnderAllUnits`, applies hidden/alpha/height checks, and draws shadows/end images/middle images/link lines.
- `LegRuntimeState.footX/footY/footHeight/footDir/moving/needsPositionReset/fallingReset` are persistent enough to be save/load fields and are therefore stable runtime state names.
- `markLegsForFalling` only marks leg runtime state for falling/reset behavior. The old `markAttachmentsFalling` name was inaccurate.

## Deliberate exclusions

Skipped candidates are listed in `docs/rw_attachment_leg_decal_runtime_skipped_rows_v0_79.csv`.

Notably skipped:

- `LegRuntimeState.l/p/q`, because their exact terrain/contact/runtime-offset semantics need more branch evidence;
- `LegOrArmTemplate.z/A`, because they are still only copied and not yet tied to a clear parser/runtime branch;
- `DecalLayer` synthetic enum machinery and `DecalBehavior` constructor/static initializer rows, because they do not materially improve named-jar readability.
