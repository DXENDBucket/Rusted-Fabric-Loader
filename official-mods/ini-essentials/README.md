# INI Essentials

INI Essentials adds opt-in fields and extensions to Rusted Warfare custom-unit INI files. Omitting
all documented extensions leaves native INI parsing and runtime behavior unchanged.

Runtime evaluation is a project-wide design rule: values that can meaningfully depend on unit
state are compiled as native LogicBoolean number/boolean expressions. Section names, event kinds,
phases, and assignment target names remain static because they define structure rather than state.

An extension can add a new key, extend a native key's accepted value range, or add a new textual
format. Native keys with native-valid values remain on the native parser path. Extended values and
formats must declare an explicit activation rule, so the extension cannot accidentally capture an
ordinary value.

## Current fields

```ini
[core]
allowNegativeHp: memory.berserk or self.resource.rage > 0
```

`allowNegativeHp` is optional and defaults to `false`. It is a runtime LogicBoolean evaluated for
the damaged unit, so memory, resources and ordinary boolean logic can control when overkill leaves
HP below zero. Installing INI Essentials alone remains multiplayer-optional; parsing a potentially
enabled synchronized field promotes it to a required matching peer dependency.

Camera action effects are available in both visible and hidden custom actions:

```ini
[action_focus]
cameraCenterOn: actionTarget,memory.cameraOffsetX,-self.resource.cameraLift
cameraTargetZoom: clamp(memory.zoom,0.5,3)
cameraStopMovement: memory.lockCamera
```

`cameraCenterAt`, `cameraCenterBy`, and `cameraCenterOn` are mutually exclusive inside one action.
Their coordinates, offsets and zoom are runtime numeric expressions; `cameraStopMovement` is a
runtime LogicBoolean. `cameraCenterOn` accepts `self`, `target`, or `actionTarget`, followed by
optional dynamic X/Y offsets. The
effects run only on the client locally controlling the acting unit's team; they do not alter
deterministic simulation state or another player's camera. A missing unit/action target makes the
corresponding contextual move a safe no-op.

## Geometry, math, and fog

`[geometry_NAME]` declares a finite reusable mask. Numeric fields are runtime LogicBoolean
expressions, so they can read unit state and use the additional math functions registered by INI
Essentials. Masks are data definitions rather than game objects: fog is the first consumer, while
effects, area queries, collision filters, and projectile layouts can reuse the same API later.

```ini
[geometry_front]
type: sector
radius: clamp(pow(self.hp,0.5)*20,80,260)
innerRadius: 0
startAngle: -90
sweepAngle: 180
rotation: self.dir

[fog_revealFront]
operation: reveal
geometry: front
team: own
anchor: self
duration: 5
follow: true

[action_scan]
applyFog: revealFront
```

Supported primitive masks are `circle`, `ellipse`, `rectangle`, `sector`, `ring`, `arc`, `line`,
and `polygon`. `union`, `intersection`, and `difference` combine named masks through `components`.
Every mask supports point containment, bounds, transform, fill sampling, and outline sampling in the
public API; consumers can rasterize it without spawning marker units.

Fog definitions support four operations: `reveal` makes tiles currently visible, `explore` removes
black unexplored shroud without hiding visible tiles, `conceal` restores explored LOS cover, and
`shroud` restores completely unexplored fog. Fog is stored per team. A positive `duration` reapplies
the operation for that many seconds after the native LOS pass; a negative value remains active until
the session ends. `setFogMode: off|basic|los` changes the global native mode and initializes missing
team fog maps as unexplored.
`duration` is a runtime number expression. `follow` is a runtime LogicBoolean checked while the
source is active; when false, the source uses the geometry snapshot captured by its action.

Runtime numeric expressions gain `pow`, `exp`, `ln`, `log10`, `log(value,base)`, `cbrt`, `abs`, `floor`, `ceil`,
`round`, `sign`, `clamp`, `lerp`, `inverse_lerp`, `hypot`, `atan2`, `atan`, `asin`, `acos`, `tan`,
`smoothstep`, `pi`, `tau`, and `e`. Trigonometric inputs and inverse outputs use the game's degree
convention. Invalid domains retain deterministic IEEE float behavior; geometry rejects non-finite
results before changing gameplay state.

## Screen-space overlays

`[overlay_NAME]` draws a `bar`, `text`, or `image` in HUD coordinates while evaluating dynamic
values against live instances of the unit type that declared the section. Images use the native
custom-unit resource path loader; text uses native localization and `%{...}` dynamic text.

```ini
[overlay_boss]
type: bar
anchor: topCenter
team: enemy
instanceMode: highestPriority
priority: self.maxHp
offsetY: 28
width: clamp(screenWidth()*0.55,280,640)
height: 26
value: self.hp
maxValue: self.maxHp
color: #B71C1C
text: Boss %{self.hp}/%{self.maxHp}
textColor: #FFFFFF
```

`instanceCondition` filters units before selection. `instanceMode` can keep `all`, `first`, `last`,
`highestPriority`, or `lowestPriority`; `maxInstances` applies afterward. `fogVisibility` defaults
to `visible`, preventing an enemy overlay from exposing units still hidden by fog.

Multi-instance layouts use `indexMode: compact|stable|explicit`. Compact indices close gaps every
frame, stable indices remain attached to a unit for its lifetime, and explicit layout evaluates the
dynamic `slot` field. `columns`, `spacingX`, and `spacingY` provide a grid. Dynamic fields and text
can read `overlayIndex()`, `overlayStableIndex()`, `overlayCount()`, `overlayRow()`,
`overlayColumn()`, `overlaySlot()`, `overlayUnitId()`, `screenWidth()`, `screenHeight()`, and
`uiScale()`. These context functions return zero outside overlay evaluation.

## Dynamic projectile rules

Native projectile mutators keep their tag checks, resource side effects, effect selection, and
execution order. A mutator damage multiplier can additionally use a runtime condition and runtime
number expression:

```ini
[projectile_shell]
directDamage: 40
mutatorArmour_ifUnitWithTags: armoured
mutatorArmour_ifCondition: self.resource.charge > 0 and eventSource.hp > 0
mutatorArmour_directDamageMultiplier: 1 + memory.armourBonus
mutatorArmour_areaDamageMultiplier: clamp(eventSource.hp/eventSource.maxHp,0.25,1)
```

For the new expressions, `self` is the firing custom unit and `eventSource` is the unit currently
being hit, including area-damage targets. That makes shooter and target memory/resources available
without changing the native meaning of `ifUnitWithTags` or `ifUnitWithoutTags`. Plain numeric
multipliers without an INI Essentials condition remain on the native parser path. Native mutators
run first; matching extended multipliers are then multiplied in their INI declaration order.

A turret can also declare any number of ordered projectile replacement rules:

```ini
[turret_main]
projectile: shell

projectileRule_antiAir_projectile: missileAA
projectileRule_antiAir_ifTargetWithTags: flying
projectileRule_antiAir_ifCondition: self.resource.missiles > 0

projectileRule_finisher_projectile: heavyShell
projectileRule_finisher_ifCondition: eventSource.hp < eventSource.maxHp * 0.25
projectileRule_finisher_ifTargetWithoutTags: boss
```

Rules use their first appearance in the section as their order, and the first complete match wins.
The general condition can read shooter memory/resources and the target through `eventSource`; the
two target-tag fields provide direct native-style tag filters. If no rule matches, the native
`projectile` / `altProjectile` result is used unchanged.

## Ordered event rules

`[event_NAME]` adds a separate rule layer in front of the native queued
`autoTriggerOnEvent` action list. It does not insert effects into, or reorder, the native action
chain. Rules are evaluated in their INI declaration order. The initial `queued` phase can cancel
the complete event action list or update the shared `eventData(...)` values read by those actions:

```ini
[event_scaleDamageHandlers]
event: tookDamage
phase: queued
when: eventData(name='damage',type='number') > 0
multiplyEventNumber: damage=0.5;hpDamage=0.5
setEventBoolean: wasLethal=false
cancelEventActions: memory.suppressDamageHandlers
```

Assignments in `setEventNumber`, `addEventNumber`, `multiplyEventNumber`, and `setEventBoolean`
are separated with semicolons and evaluated from left to right. Later expressions can read values
written earlier through `eventData(...)`. Their expressions, `when`, and `cancelEventActions` can
also use memory, resources, unit state, and INI Essentials math functions. `cancelEventActions`
cancels the native handlers for the
queued notification; it intentionally does not claim to undo damage, teleportation, queue changes,
or other game work that already occurred. Event-specific synchronous `before` phases remain
separate from this queued phase and native action ordering.

The first synchronous phase is available for `tookDamage`:

```ini
[event_reduceActualDamage]
event: tookDamage
phase: before
when: eventData(name='damage',type='number') > 0
setEventValue: max(0,eventData(name='damage',type='number')-self.resource.block)
multiplyEventValue: self.resource.damageTakenMultiplier
cancelEvent: memory.damageImmune
```

The names deliberately differ from queued notification fields. `cancelEvent` prevents the native
damage operation itself, so no HP/shield change, attachment forwarding, or `tookDamage` action
notification is produced. `setEventValue`, `addEventValue`, and `multiplyEventValue` change the
actual damage passed into the untouched native pipeline. They run in that fixed order and each is a
runtime numeric expression; later expressions see the current value as
`eventData(name='damage',type='number')`. The before hook runs prior to native armour, immunity, and
attachment redirection. Other event kinds reject `phase: before` until their own synchronous native
boundary is implemented.

The native `tookDamage` action event now receives typed damage context through the game's existing
`eventData(...)` function:

```ini
[hiddenAction_on_damage]
autoTriggerOnEvent: tookDamage
showMessageToPlayer: Damage: %{eventData(name="hpDamage", type="number")}, attacker: %{eventSource}
```

Available names are `damage`, `rawDamage`, `hpDamage`, `shieldDamage`, `remainingDamage`,
`hpBefore`, `hpAfter`, `shieldBefore`, `shieldAfter`, and `wasLethal`. `damage` is the value passed
to the native damage routine after attachment forwarding, immunity, and custom-unit armour;
`hpDamage` and `shieldDamage` are the actual non-negative reductions. Existing `eventSource` and
projectile-tag filtering retain their native behavior. Parsing any enhanced name activates the
matching multiplayer requirement because these values can influence synchronized actions.

The same opt-in event-data bridge now fills gaps in five other native event groups:

- `queueItemAdded` and `queueItemCancelled`: action ID, queue item metadata, queue sizes and targets.
- `newWaypointGivenByPlayer`: order type, point/unit target, player queue flag, build type and action ID.
- `teamChanged`: old/new team IDs and alliance groups.
- `teleported`: position, height and direction before and after the native teleport.
- `attachmentRemoved`: the actual removed child, old slot and transport-list membership. This keeps
  the native event firing behavior while working around its `eventSource` pointing at the parent.

The second event pass covers native events that already carry a related unit as `eventSource`, but
previously required awkward chained logic to inspect it:

- `killedAnyUnit`: killed unit reference, type, team, position, HP and building classification.
- `queuedUnitFinished`: produced unit details plus the completed queue action ID and quantity.
- `touchTargetSuccess`: the reached target's reference, type, team and position.
- `transportingNewUnit` and `transportUnloadedOrRemovedUnit`: passenger details and the carrier's
  used/maximum slots after the operation.
- `enteredTransport` and `leftTransport`: carrier details and its used/maximum slots as seen by the
  passenger event.
- `newMessage`: sender details and flags reporting whether native message tags/data were supplied.

The native `eventSource`, event tags and pre-existing message data remain intact; these additions
provide stable typed aliases and snapshots rather than replacing the original event behavior.

All values are added to the event's existing `eventData(...)` scope. Omitting the enhanced names
keeps native INI behavior and does not activate the synchronized requirement.

The machine-readable bilingual field catalog is stored at
`src/main/resources/ini_essentials/fields.csv`; enhanced native event values are stored in
`src/main/resources/ini_essentials/event-data.csv`. The generated spreadsheet in `docs/` is intended
for the same community workflow as the original Rusted Warfare Unit Modding Reference.

The workbook is generated rather than edited by hand. Its English and Chinese pages use the
original reference's five-column-first layout and matching section colors (`[core]` green,
action/hiddenAction orange, `[geometry_*]` high-contrast indigo), followed by extension metadata columns. Native event-data additions
live in a separate large catalog at the bottom instead of being mixed with regular INI sections.
Colored shortcuts jump to regular sections or that event catalog, whose own index links each event;
every section links back to its corresponding index.
Like the community 1.15 enhanced reference, these controls use transparent DrawingML hyperlink
overlays: hovering shows a link pointer and clicking jumps immediately without selecting a cell.

```text
python docs/generate_reference.py
python docs/generate_reference.py --check
```

`sync-schema.txt` is the canonical gameplay/protocol input for the multiplayer SHA-256. Descriptive
wording is deliberately excluded so documentation-only edits do not break compatibility.
