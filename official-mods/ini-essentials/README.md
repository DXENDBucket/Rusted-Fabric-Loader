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

Automatic actions can override the native unit-wide trigger cooldown inside their own section:

```ini
[hiddenAction_regenerate]
autoTrigger: if self.hp < self.maxHp
autoTriggerCooldownTime: 0.5s
autoTriggerCooldownTime_allowDangerousHighCPU: false
addResources: hp=1
```

The timer belongs to this unit instance and this Action. When at least one Action opts in, every
other `autoTrigger` on the same unit keeps the `[core] autoTriggerCooldownTime` duration as its own
independent fallback instead of sharing the original single latch. The native limits remain: zero
to two seconds, and values below five simulation frames require the dangerous-CPU opt-in in the
same Action. These fields apply to `autoTrigger`, not `autoTriggerOnEvent`.

Camera action effects are available in both visible and hidden custom actions:

```ini
[action_focus]
cameraCenterOn: self.getOffsetRelative(x=memory.cameraOffsetX,y=100)
cameraTargetZoom: clamp(memory.zoom,0.5,3)
cameraStopMovement: memory.lockCamera
```

`cameraCenterAt`, `cameraCenterBy`, and `cameraCenterOn` are mutually exclusive inside one action.
Their coordinates, offsets and zoom are runtime numeric expressions; `cameraStopMovement` is a
runtime LogicBoolean. `cameraCenterOn` accepts any native UnitReference, including chained
references and marker-producing expressions such as `self.getOffsetRelative(y=100)`. The legacy
`self`, `target`, and `actionTarget` tokens remain compatible. Its trailing world-axis X/Y offsets
are optional together and default to zero; `getOffsetRelative` instead rotates its local offset
with the referenced unit. The
effects run only on the client locally controlling the acting unit's team; they do not alter
deterministic simulation state or another player's camera. A missing unit/action target makes the
corresponding contextual move a safe no-op.

## Live unit context

Native `self.hasActiveWaypoint` remains the preferred existence check, including its optional
`type` filter. INI Essentials adds the active waypoint values that the native expression language
does not otherwise expose:

```ini
self.hasActiveWaypoint
self.hasActiveWaypoint(type='attackMove')
self.activeWaypointType == 'attackMove'
self.activeWaypointX
self.activeWaypointY
self.activeWaypointRelativeX
self.activeWaypointRelativeY
self.isSelectedByLocalPlayer
```

Absolute coordinates use the active order's live native target position. Relative X is the
unit-local lateral/right axis and relative Y is its forward axis, matching
`self.getOffsetRelative(x=...,y=...)`. With no active waypoint, the type is `none`, absolute
coordinates fall back to the unit position, and relative coordinates are zero. The selection value
belongs to the local client and must only control presentation such as Decals or Overlays; it must
not decide synchronized gameplay behavior.

## Independent CustomProjectile assets

`class: CustomProjectile` files define a reusable native projectile template without registering a
dummy unit. The definition ID is always namespaced, and ordinary unit actions refer to one named
pattern after the final `/`:

```ini
[core]
class: CustomProjectile
name: example:plasma_fan
schemaVersion: 1
@memory phase: float
@memory splitDone: bool

[projectile]
directDamage: 20
life: 180
speed: 5
effectOnCreate: CUSTOM:launch
trailEffect: CUSTOM:trail
explodeEffect: CUSTOM:impact

[collision]
collideWithUnits: true
collideWithTerrain: true
contactCollisionRadius: 4
terrainTransitionFrom: land
terrainTransitionTo: water
unitCollisionLayers: ground,air,underwater
unitCollisionMovementTypes: land,air,hover
unitCollisionMinHeight: -5
unitCollisionMaxHeight: 30
unitCollisionWithoutTags: intangible

[effect_launch]
life: 12

[effect_trail]
life: 18

[effect_impact]
life: 30

[pattern_main]
type: fan
aimMode: direction
count: clamp(memory.shots,1,30)
sweepAngle: 60
originOffsetY: 18

[motion]
speed: 5+memory.phase
turnSpeed: 2+projectile.age*0.05
dx: sin(projectile.age*3)*1.5
dy: cos(projectile.age*3)*1.5
# offsetX/offsetY instead place the projectile relative to its own spawn point.

[lifecycle]
onUpdate: accelerate
onImpact: split

[hiddenAction_accelerate]
ifCondition: memory.phase<3
setMemory: phase=memory.phase+0.02

[action_split]
ifCondition: not memory.splitDone
setMemory: splitDone=true
emitProjectilePattern: example:fragment/main
```

```ini
[action_fireFan]
spawnCustomProjectile: example:plasma_fan

[turret_main]
projectilePattern: example:plasma_fan/main
projectilePatternRule_antiAir_pattern: example:anti_air/main
projectilePatternRule_antiAir_ifCondition: eventSource.isFlying
```

`single`, `fan`, `ring`, and `line` are deterministic same-tick layouts. A ring defaults to a full
360-degree sweep without duplicating its endpoint; a smaller sweep includes both ends. Numeric
pattern values are evaluated when the action runs or the turret fires, with the firing unit as
`self`, so memory, resources, and extended math functions are available. When `centerDirection` is
omitted, actions use the unit direction and turrets use the live native turret aim angle. One
expansion creates only its real native projectiles—there is no
invisible parent projectile—and `count` has a hard limit of 1024. `direction` works without an
action target; `point` and `unit` require the corresponding action or native turret target.

`[collision]` opts into the game's own contact collision. Its three values are evaluated once when
the firing action or turret shot is resolved, with the firing unit as `self`; memory, resources,
and math expressions are therefore available. `contactCollisionRadius` is added to a contacted
unit's native collision radius. `collideWithTerrain` explodes when the projectile's current map
tile is blocked in the native `hover` pathing layer: this includes map bounds, impassable terrain,
and blocking building/object costs, but ordinary land and water tiles normally do not count. Unit
eligibility, impact, damage, and friendly-fire behavior remain native.

`terrainTransitionFrom` and `terrainTransitionTo` are an optional paired rule for exact ground-tile
kind transitions such as `land -> water`; supported kinds are `any`, `land`, `water`,
`waterBridge`, `lava`, `cliff`, `resourcePool`, and `outOfBounds`. This samples the ground tile at
deterministic update boundaries and is separate from the native hover-blocked collision above.
`water`, `waterBridge`, `resourcePool`, and ordinary `land` are distinct categories, so for example
`waterBridge -> water` does not also match bridge-to-bridge movement.

Writing any `unitCollision*` filter switches unit contact from the native ground-only scan to the
extended deterministic scan. `unitCollisionLayers` classifies each live target as exactly one of
`ground`, `air`, or `underwater` (underwater takes priority), while
`unitCollisionMovementTypes` can additionally restrict native movement types. Absolute target
height bounds, required/forbidden runtime tags, and transported-unit inclusion are available; the
numeric/boolean values are resolved once at firing. A matched unit is assigned as the native
impact target, so ordinary damage, friendly-fire, effects, and removal remain in charge afterward.

Native `[effect_NAME]` sections are accepted and can be referenced from `[projectile]` with the
ordinary `CUSTOM:NAME` syntax, including `effectOnCreate`, `trailEffect`, `explodeEffect`, and
`explodeEffectOnShield`. Native `[decal_NAME]` sections are also accepted. Their layer is drawn at
the live projectile world position, while `self`, visibility expressions, team context, and
orientation still use the firing `CustomUnit`; this keeps native Decal syntax useful without
pretending a projectile is itself a unit.

Each spawned instance owns its declared `@memory` values, initialized to zero/false. `[motion]`
evaluates `speed`, `turnSpeed`, `dx`, and `dy` before native movement on every tick. `offsetX` and
`offsetY` are different: after the tick they place the projectile at a dynamic position relative
to its own spawn point, which supports parametric paths without integrating velocity. Expressions
keep the firing unit as `self` and can additionally read `projectile.age`, position, direction,
speed, velocity, and spawn-relative offsets.

`[lifecycle]` can bind `onCreate`, `onUpdate`, `onImpact`, and `onRemove` to local
`[action_NAME]` or `[hiddenAction_NAME]` sections. The supported projectile-safe subset is
`ifCondition`, `setMemory`, `setSpeed`, `setTurnSpeed`, `setDx`, `setDy`, `setOffsetX`,
`setOffsetY`, and `emitProjectilePattern`. Impact/removal bindings run once. Child emission starts
at the live projectile position and is capped at ten recursion levels; local actions also accept
`spawnCustomProjectile` with the same default-main shorthand. Ordinary unit actions can
use `spawnCustomProjectile: namespace:path` (default `main`) or the compatible
`emitProjectilePattern: namespace:path/pattern` form.

The `[projectile]`, `[effect_NAME]`, and `[decal_NAME]` sections accept their ordinary native fields
except deferred links to
other projectile names (`spawnProjectilesOnCreate`, `spawnProjectilesOnExplode`, and
`spawnProjectilesOnEndOfLife`), which remain rejected in favor of bounded lifecycle emission.
`projectilePattern` uses a precise weave inside
the native firing method: native projectile selection and `onShoot` run first, only projectile
allocation/template initialization is replaced, and the native muzzle effects, sound, recoil,
shot counter, and post-fire state continue once afterward. It does not use the older method-entry
cancellation event.

For turrets, `projectilePatternRule_NAME_pattern` plus one or more of `_ifCondition`,
`_ifTargetWithTags`, and `_ifTargetWithoutTags` selects the first matching pattern in declaration
order, then falls back to `projectilePattern`. Conditions use the same `self`/`eventSource` context
as ordinary projectile rules. When the unconditional `projectilePattern` exists, the turret may
omit native `projectile:`; INI Essentials installs a private zero-damage placeholder only to satisfy
the original parser, and the exact firing weave replaces it before initialization. A rule-only
turret still needs a native `projectile:` as its no-match fallback.

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
`anchor` accepts the same native UnitReference expressions as `cameraCenterOn`, so fog can follow
`self.customTarget1`, a parent/transport unit, a queried nearby unit, or a relative marker. The
special `actionTarget` token remains available for actions that contain only a target point.

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
yOffsetAbsolute: 28
width: clamp(screenWidth()*0.55,280,640)
height: 26
value: self.hp
maxValue: self.maxHp
color: #B71C1C
text: Boss %{self.hp}/%{self.maxHp}
textColor: #FFFFFF
```

`instanceCondition` filters units before selection. `instanceMode` can keep `all`, `first`, `last`,
`highestPriority`, `lowestPriority`, `nearestToCamera`, or `farthestFromCamera`; all single-instance
modes use the native unit ID as a deterministic tie-break, and `maxInstances` applies afterward.
`fogVisibility` defaults to `visible`, preventing an enemy overlay from exposing units still hidden
by fog.

Multi-instance layouts use `indexMode: compact|stable|explicit`. Compact indices close gaps every
frame, stable indices remain attached to a unit for its lifetime, and explicit layout evaluates the
dynamic `slot` field. `columns`, `spacingX`, and `spacingY` provide a grid. Dynamic fields and text
can read `overlayIndex()`, `overlayStableIndex()`, `overlayCount()`, `overlayRow()`,
`overlayColumn()`, `overlaySlot()`, `overlayUnitId()`, `screenWidth()`, `screenHeight()`, and
`uiScale()`. These context functions return zero outside overlay evaluation.

`layer: afterHud` is the default and draws above the native interface. `layer: beforeHud` draws
between the world and native HUD, which is useful for panels that native controls should cover.
`scale`, `scaleX`, and `scaleY` remain Overlay-wide fields because they apply equally to bars,
text, and images. `dirOffset` uses the same name as Decal. All four are runtime expressions applied
around the overlay center. Negative axis scales mirror an element. Bars can fill `leftToRight`, `rightToLeft`,
`topToBottom`, or `bottomToTop` through `barDirection`.

For Decal-style image definitions, `type: image` may be omitted: the presence of `image` selects
the image primitive. Overlay also uses Decal's `xOffsetAbsolute`, `yOffsetAbsolute`, and
`onlyWhileAlive` names directly instead of defining parallel aliases.

Image overlays can use Decal-compatible atlas field names while keeping `frame` dynamic:

```ini
[overlay_bossIcon]
type: image
layer: beforeHud
anchor: topCenter
image: boss_icons.png
total_frames: 4
frame_width: 64
frame_height: 64
frame_verticalOrdering: false
frame: memory.phase
scale: 1.25
dirOffset: memory.hudAngle
```

When explicit frame dimensions are omitted, a multi-frame image is split into one horizontal row,
or one vertical column when `frame_verticalOrdering: true`. Explicit dimensions allow a grid;
out-of-range runtime frame values are clamped.

## Decal and Overlay alpha masks

An image Overlay can use another image Overlay as an aligned alpha mask. The source Overlay keeps
its own anchor, offset, size, scale, rotation, frame, dynamic alpha, and unit context; the compositor
maps those screen transforms back onto the content image. A referenced source is a mask-only
definition by default and is not drawn normally unless it sets `maskRender: true`:

```ini
[overlay_bossShape]
type: image
anchor: topCenter
image: boss_bar_shape.png
width: 520
height: 38

[overlay_bossTexture]
type: image
anchor: topCenter
image: animated_energy.png
width: 760
height: 80
mask: bossShape
maskAlphaThreshold: 0.08
maskThresholdMode: normalize
maskAlphaMode: multiply
```

The same field family extends native Decals while leaving their original layer, visibility, frame,
team-color and placement pass in control:

```ini
[decal_hullShape]
layer: onTop
image: hull_mask.png

[decal_hullTexture]
layer: onTop
image: large_texture.png
mask: hullShape
maskAlphaThreshold: memory.damageMaskCutoff
maskInvert: memory.showOutside
maskThresholdMode: keep
maskAlphaMode: min
```

`maskThresholdMode` is `keep`, `binary`, or `normalize`. `maskAlphaMode` is `multiply`
(`contentAlpha*maskAlpha`), `min`, or `replace`; RGB always comes from the content image.
`maskUsesSourceAlpha: false` ignores the source definition's own alpha while retaining its texture
alpha. Threshold and inversion support runtime expressions.

`maskGeometry: NAME` replaces the image source with a `[geometry_NAME]` binary mask. Overlay
geometry uses the content Overlay's unscaled local coordinates; Decal geometry uses the content
Decal's unscaled local pixel coordinates. A masked native Decal currently requires one `image` on
both definitions; `imageStack`, leg-end anchors, and turret anchors are rejected rather than being
rendered with misleading alignment. Ordinary Decals without mask fields remain entirely native.

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
The compact top-level menu switches between `CustomUnitMetadata` and `CustomProjectile`; each class
has its own section shortcuts, so projectile asset fields are not mixed into the ordinary unit
section index. The separate event catalog at the bottom has its own per-event index, and every
section links back to the index for its class.
Like the community 1.15 enhanced reference, these controls use transparent DrawingML hyperlink
overlays: hovering shows a link pointer and clicking jumps immediately without selecting a cell.

```text
python docs/generate_reference.py
python docs/generate_reference.py --check
```

`sync-schema.txt` is the canonical gameplay/protocol input for the multiplayer SHA-256. Descriptive
wording is deliberately excluded so documentation-only edits do not break compatibility.
