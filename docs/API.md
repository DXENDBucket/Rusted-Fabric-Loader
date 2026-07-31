# Rusted Fabric API

## Compatibility status

Rusted Fabric API `0.1.x` is experimental and targets Rusted Warfare `1.15` with the mapping version recorded in `fabric.mod.json`. Event names and callback arguments should be treated as source-compatible within a `0.1.x` line where practical, but mapping corrections can still require signature changes.

Windows desktop remains the working Loader and ordinary Fabric-style Jar mod target. The active
Android direction is a PC-edition port that imports the user's Steam files and aims to execute the
same Fabric/Knot runtime in an ARM64 JVM. Native Android APK patching and Xposed hooks are frozen.

Mods can declare the game dependency exposed by the GameProvider:

```json
"depends": {
  "fabricloader": ">=0.18.1",
  "rusted_warfare": "1.15",
  "rustedfabricapi": ">=0.1.0"
}
```

## Loader lifecycle entrypoints

Two Rusted-specific Fabric entrypoints are available:

- `rustedfabricloader:classpath_ready`: the game Jar and libraries are on the launch classpath, before standard Fabric initializers run.
- `rustedfabricloader:before_game`: standard `main` and `client` initializers have completed, immediately before the game main method is invoked.

Implement `RustedFabricAPIEntrypoint` to receive a typed `RustedFabricAPIContext`. The context is an
immutable snapshot; its launch-argument array and capabilities are defensively copied.
The same base class also implements Android's `RustedFabricModEntrypoint`, so one entrypoint source
can be packaged for both platforms. See [`PORTABLE_MOD_BUILD.md`](PORTABLE_MOD_BUILD.md).
`contextVersion()` is currently `5`. Version 5 adds the shared game-session API and live `RFH1`
handshake capability. Version 4 added the canonical multiplayer manifest and the
`multiplayer.compat.v1` capability. Version 3 added `platform()`, `mappingProfileId()`,
`capabilities()`, `packageName()`, and `processName()`. The older `androidRuntime()` accessor remains
available.

## Windows and Android portability

> The native Android-APK backend is frozen. Current Android work targets the desktop-JVM port; until
> its platform adapters are complete, API release verification still runs on Windows Jar mods.

`rusted-fabric-api` contains the complete platform-neutral public surface: context, runtime holder,
events, helpers, sessions, and multiplayer contracts. Its classes are embedded in the Windows API
Jar and compiled into the Android loader DEX,
so a mod can use the same imports and listener source on both platforms:

```java
RuntimeLifecycleEvents.AFTER_ENGINE_INITIALIZATION.register(context -> {
    if (context.hasCapability("event.engine.init")) {
        // portable initialization logic
    }
});
```

`LOADER_READY` fires after enabled mods are loaded, and `GAME_READY` fires after the first successful
engine initialization. Both are exception-isolated and available on Windows and Android. Portable
multiplayer manifests, evaluation, and events live under `api.multiplayer` and
`MultiplayerCompatibilityEvents`; see [`MULTIPLAYER.md`](MULTIPLAYER.md).

`RustedFabricRuntime.currentSession()` and `GameSessionEvents` are also common API. They are active
for single-player as well as host/client play, so portable gameplay mods do not need separate
offline and online entrypoints.

The distributed binary is still platform-specific: Windows uses a Fabric Jar containing JVM class
files, while Android requires a DEX mod package. The provided Gradle convention builds both outputs
from one common source set. Put
Slick/LWJGL, desktop Mixins, Android UI/storage, and other platform APIs behind separate adapters.
Android entrypoint classes implement `RustedFabricModEntrypoint`; the `.javamod` v1 format and loading
rules are documented in [`ANDROID_MODS.md`](ANDROID_MODS.md).

The three Gradle modules are explicit build boundaries, not competing APIs:

- `rusted-fabric-api`: the public cross-platform API. It has no Fabric, Mixin, Android framework,
  Slick/LWJGL, or game implementation class dependency.
- `rusted-fabric-api-desktop`: Windows Fabric/Mixin hooks, named-to-official remapping, and the desktop
  RFH1 adapter. Its distributable Jar embeds `rusted-fabric-api` so users install one API Jar.
- `rusted-fabric-api-android`: the Android RFH1/mapping adapter shared by both local-patch and Xposed
  backends. Android packaging compiles it and `rusted-fabric-api` into DEX.

Mod source targets `rusted-fabric-api`; it uses a platform backend only for platform-specific
integration or build tooling. This separation prevents desktop dependencies from entering Android
while mechanically verifying that the public API remains portable.

Backend coverage is machine-readable in
`rusted-fabric-api/src/main/resources/rustedfabricapi/api-support-matrix.csv`. Each public event
group has a versioned capability key and a `full`, `partial`, or `unavailable` level for Windows,
Android local patch, and Android Xposed. `RustedFabricCapabilities` exposes the stable keys;
`ApiSupportMatrix.available(context, capability)` combines expected backend coverage with the
capabilities actually advertised by the running Loader. The build fails if a public event class or
capability row is missing.

## Event behavior

Events under `io.github.endx.rustedfabricapi.api.event` invoke listeners synchronously in registration order on the thread that reached the corresponding game method. They do not switch to a render, update, or network thread.

- Existing game-object events propagate listener exceptions to the intercepted game call. A listener should catch failures it can recover from.
- Permanent registration is intended for initialization time; runtime-toggleable features should
  keep and close a subscription handle.
- `register(listener)` remains the simple permanent-registration API. Use `subscribe(listener)`
  when a feature can be disabled at runtime; its idempotent `Registration` handle implements
  `AutoCloseable`. `unregister(listener)` and `listenerCount()` are also available.
- Both methods register in `RustedFabricEvent.DEFAULT_PHASE`. Named phases and explicit ordering
  are available when integrations need deterministic before/after relationships; listeners within
  one phase retain registration order.
- `BEFORE_*` callbacks returning `true` generally cancel the operation, but the callback interface remains the source of truth.
- `MODIFY_*` callbacks are chained in registration order; each listener receives the value produced by the previous listener.
- Game objects are commonly exposed as `Object`. This keeps the public API Jar namespace-neutral across named development and official runtime. Mods may cast to mapped game types when they are compiled and remapped through the supported pipeline.

`RuntimeLifecycleEvents` is the cross-platform exception: each listener is isolated, failures are
counted in `DispatchResult`, registrations can be unregistered, and no game or platform object is
exposed. The before/after engine initialization events are one-shot on both backends.

### Ordered event phases

Every `RustedFabricEvent` supports Fabric-style named phases without changing the behavior of
existing one-argument registrations. An API or compatibility mod can declare ordering edges and
then register a listener in its own phase:

```java
Identifier VALIDATE = Identifier.of("my_mod", "validate");
Identifier APPLY_COMPAT = Identifier.of("compat_mod", "apply");

UnitDamageEvents.BEFORE_DAMAGE.addPhaseOrdering(VALIDATE, APPLY_COMPAT);

LifecycleScope scope = LifecycleScope.create("my_mod:damage_rules");
scope.own(UnitDamageEvents.BEFORE_DAMAGE.subscribe(VALIDATE,
        (target, attacker, damage, projectile) -> shouldCancelDamage(target, damage)));
```

An ordering edge applies to the whole phase, remains declared while the phase is empty, and may be
added before or after its listeners. Multiple edges are resolved with a topological sort. A cycle
or a phase ordered before itself throws `IllegalArgumentException` and leaves the prior invoker
unchanged. Unrelated phases follow their earliest listener registration; the phase identifier is
the final stable tie-breaker. `phaseOrder()` and `listenerCount(phase)` expose read-only diagnostics.

`subscribe(phase, listener)` returns a `PhasedRegistration`, which exposes `phase()` and otherwise
has the same idempotent cleanup contract as an ordinary registration. Dispatch always uses an
immutable snapshot: adding or removing a listener from inside a callback affects the next dispatch,
never the callback loop already in progress. Phase IDs and listener ordering are local runtime
state and are not automatically part of a multiplayer content fingerprint.

## Desktop typed API

Ordinary Windows Fabric Jar mods should prefer the mapped API in `rusted-fabric-api-desktop` when
they need game objects. It provides IDE-visible `GameEngine`, `Team`, `Unit`, `Projectile`, and
`Command` types instead of requiring casts from `Object`:

```java
import io.github.endx.rustedfabricapi.api.client.event.ClientTickEvents;
import io.github.endx.rustedfabricapi.api.unit.Units;
import io.github.endx.rustedfabricapi.api.unit.event.UnitEvents;

ClientTickEvents.END_CLIENT_TICK.register(engine -> {
    if (engine != null && engine.isGameStarted) {
        int activeUnits = Units.alive().size();
    }
});

UnitEvents.AFTER_REGISTER.register(unit -> {
    float healthFraction = unit.maxHp > 0.0F ? unit.hp / unit.maxHp : 0.0F;
});
```

The first typed desktop layer contains:

- `api.client.RustedWarfareClient`: engine/team lookup and game-thread scheduling.
- `api.client.option.ClientOptions`, typed option keys, transactions, and `ClientOptionEvents`:
  validated access to 38 user-visible audio, display, interface, input, and replay preferences.
- `api.client.event.ClientLifecycleEvents`: one-shot typed engine initialization boundaries.
- `api.client.event.ClientTickEvents`: start/end update callbacks.
- `api.scheduler.GameTickScheduler`: delayed/repeating update-thread work advanced by native
  simulation ticks, with owner cancellation and map/session/global lifetimes.
- `api.client.Selection` and `api.client.event.SelectionEvents`: selection snapshots, mutations,
  and cancellable select/deselect/clear callbacks.
- `api.client.event.ClientRenderEvents`, `HudRenderEvents`, `WorldRenderEvents`,
  `HudDrawContext`, and `WorldDrawContext`: frame callbacks plus Paint-free screen/world text,
  primitives, images, clipping, coordinate conversion, and scoped transforms without leaking
  Slick/LWJGL implementation types.
- `api.client.Camera` and `CameraSnapshot`: immutable camera bounds/zoom queries, visibility tests,
  clamped movement, smooth zoom targets, and pointer-to-world conversion.
- `api.client.input.KeyBindings`, `ModKeyBinding`, `KeyBindingEvents`, and `ClientInputEvents`:
  namespace-scoped custom bindings, key-settings menu integration, saved-binding compatibility,
  binding edges, and immutable raw keyboard/pointer observations with logical/world coordinates.
- `api.client.screen.ClientScreens`, `UiDocumentSnapshot`, and `ScreenEvents`: LibRocket-free page,
  popup, and alert queries plus loaded/opened/closed, active-page, and topmost lifecycle callbacks.
- `api.client.message.ClientMessages` and `MessageEvents`: local system/chat history, alerts,
  dialogs, cancellable insertion, and history-clear boundaries.
- `api.client.warlog.WarLog` and `WarLogEvents`: local event-log text and positioned unit entries,
  forced display durations, unread-entry camera jumps, and cancellable insertion callbacks.
- `api.client.minimap.Minimaps`, `MinimapSnapshot`, and `MinimapEvents`: world/screen coordinate
  conversion, immutable layout state, transient markers/scan pulses, and cancellable callbacks.
- `api.audio.Sounds`, `SoundPlayback`, and `SoundEvents`: built-in/custom sound loading, interface/
  global/positional playback, mixer state, and cancellable high-level playback callbacks.
- `api.chat.Chats`, `ChatEvents`, and `api.chat.command.ChatCommands`: typed local/server chat,
  namespaced server commands usable by unmodified clients, quoted arguments, private replies, and
  cancellable send/receive/execute boundaries.
- `api.config.ModConfigFiles`, `ModConfigFile`, and `ConfigEvents`: mod-scoped files below Fabric's
  external config directory, bounded reads, UTF-8/properties helpers, safe paths, and atomic writes.
- `api.asset.ModResources`, `ModResourcePack`, and `ModResourceEvents`: safe read-only access to
  files bundled in ordinary mod Jars, plus content-addressed temporary extraction for native APIs.
- `api.asset.reload.ModResourceReloaders`: dependency-ordered prepare/apply listeners with native
  reload hooks, failure isolation, lifecycle events, and immutable per-listener reports.
- `api.asset.condition.ResourceConditions`: extensible JSON load predicates for optional-mod,
  registry-entry, and tag-aware resource compatibility without splitting a mod Jar.
- `api.datagen.ModDataGenerator`, `LanguageDataProvider`, and `RegistryTagDataProvider`:
  build-time, dependency-ordered generation of deterministic resources for ordinary mod Jars.
- `api.text.Translations` and `LanguageEvents`: namespace-scoped UTF-8 translation bundles,
  locale fallback, parameter formatting, cache invalidation, and native language-reload callbacks.
- `api.registry.ModRegistries`, `ModRegistry`, and `RegistryKey`: process-wide typed custom
  registries with stable identifiers/raw IDs, deterministic lifecycle events, and explicit freeze.
- `api.registry.tag.RegistryTags` and `RegistryTagReloaders`: transactional multi-mod tag
  contributions, nested references, cycle validation, and Fabric-style JSON discovery/reload.
- `api.service.ModServices`, `ServiceKey`, and `ServiceRegistry`: typed optional inter-mod services
  with deterministic provider selection and removable registrations.
- `api.lifecycle.LifecycleScope`: one reverse-order owner for event subscriptions, service
  registrations, reload listeners, and other `AutoCloseable` feature resources.
- `api.lobby.LobbyGameSetup`, `GameSetupSnapshot`, and `GameSetupEditor`: immutable lobby-rule
  snapshots and validated transactional host/proxy updates through the native synchronization path.
- `api.lobby.LobbyPlayers` and `LobbyPlayerEvents`: player/AI snapshots, connection lookup, kick,
  slot/alliance requests, automatic team layouts, pause control, and cancellable native boundaries.
- `api.effect.Effects` and `api.effect.event.EffectEvents`: pooled built-in effect creation,
  priority/visibility controls, cleanup, and typed finalized line/light callbacks.
- `api.save.Saves` and `api.save.event.SaveEvents`: path-safe local save/load/delete helpers and
  cancellable high-level file lifecycle callbacks.
- `api.replay.Replays`, `ReplaySnapshot`, and `ReplayEvents`: path-safe replay recording/playback/
  deletion, immutable header/progress state, and typed lifecycle callbacks.
- `api.stats.GameStatistics`, `TeamStatisticsSnapshot`, `StatisticMetric`, and `StatisticsEvents`:
  per-team kill/loss snapshots, post-game history values, and typed reset/update/kill callbacks.
- `api.mission.Missions`, `MissionSnapshot`, and `MissionTriggerEvents`: read-only mission/result/
  survival state, parsed trigger discovery, map-object points, and cancellable trigger activation.
- `api.unit.Units`, `api.unit.Teams`, and `TeamStateSnapshot`: typed unit/team snapshots,
  spatial queries, stable team identity/alliance/outcome/control state, economy, and relations.
- `api.unit.UnitSpawns`, `api.unit.event.UnitSpawnEvents`, and `UnitTeamEvents`: bookkeeping-safe
  live-unit creation/removal, cancellable API-driven spawns, and ownership-change callbacks.
- `api.unit.event.UnitEvents`: typed registration lifecycle.
- `api.unit.event.UnitDamageEvents`: cancellable damage/death callbacks and modifiable immunity or
  death-effect results.
- `api.unit.combat.CombatUnits`, `TurretSnapshot`, and `api.unit.combat.event.CombatEvents`:
  current-target and range checks, immutable turret configuration/runtime snapshots, normal
  warmup/reload firing, and cancellable or modifiable targeting/fire decisions.
- `api.unit.order.UnitOrders` and `UnitOrderSnapshot`: active/next/last waypoint access, immutable
  queue snapshots, attack-mode access, and direct deterministic queue operations for move, attack,
  repair, reclaim, guard, patrol, and follow behavior.
- `api.unit.repair.RepairReclaim` and `api.unit.repair.event.RepairReclaimEvents`: repair/reclaim
  capability and active-target queries, build/unbuild speed and resource prices, construction
  progress, nearest reclaim-resource lookup, and typed cancellable/modifiable lifecycle hooks.
- `api.unit.status.StatusEffects`, `StatusEffectSnapshot`, and `StatusEffectEvents`: immutable
  active-effect snapshots, stable effect classification, timed movement-speed effects, per-action
  or global action blocking, explicit removal, and add/update/expire/remove lifecycle callbacks.
- `api.unit.attribute.UnitVitals` and `UnitVitalsSnapshot`: health, shield, and energy snapshots and
  native runtime writes shared by built-in and custom units. `CustomUnitStats`, `UnitStatSnapshot`,
  and `UnitStatEvents` expose all 19 mapped custom-unit mutable stats, native side-effect-preserving
  writes, and observable native/metadata rebases.
- `UnitStatModifier` provides replaceable `namespace:path` modifiers for custom-unit metadata stats.
  Evaluation is deterministic: direct additions, baseline-scaled additions, then total multipliers.
  Runtime values (`HEALTH`, `SHIELD`, and `ENERGY`) can be set but are deliberately not modifier
  targets. Modifiers are runtime-owned and omitted from native saves; persist the effect's own state
  with `PersistentData` and reapply it after loading.
- `api.unit.type.UnitTypes` and `api.unit.type.event.UnitTypeEvents`: built-in/custom type discovery,
  replacement resolution, starting-position validation, and typed creation/validation callbacks.
- `api.custom.CustomUnits`, `CustomUnitRegistryEvents`, and `CustomUnitLifecycleEvents`: active and
  pending definition snapshots, lookup/creation/reload, registry phases, metadata conversion,
  custom-unit death, and removal.
- `api.custom.CustomUnitTriggers` and `CustomUnitTriggerEvents`: immediate configured-event
  dispatch, contextual queued dispatch, and cancellable before/after trigger or queue callbacks.
- `api.custom.attachment.Attachments` and `AttachmentEvents`: slot discovery and offsets, attached
  unit snapshots, parent/slot lookup, attach/detach calls, and mutation callbacks.
- `api.unit.tag.UnitTags` and `UnitTagEvents`: tag parsing/interning, immutable name snapshots,
  subset/intersection checks, team-index-safe runtime mutation, and cancellable replacement events.
- `api.unit.action.UnitActions`: native action discovery, visibility/availability checks, lookup by
  action ID, and synchronized targeted or non-targeted execution.
- `api.unit.action.JavaUnitActions`, `JavaUnitAction`, and `JavaUnitActionEvents`: permanent
  process-wide Java action registration, built-in/custom unit-type attachment by internal name,
  native immediate/map-target action-panel input, and cancellable synchronized per-unit callbacks.
- `api.unit.build.BuildQueues` and `api.unit.build.event.BuildQueueEvents`: immutable queue
  snapshots, current item/progress, queue mutation helpers, action application, activation,
  refund, completion, and produced-unit positioning callbacks.
- `api.unit.transport.TransportUnits` and `api.unit.transport.event.TransportEvents`: transport
  detection, immutable cargo snapshots, capacity/slot queries, validated loading and unloading,
  containment/attachment lookup, and cancellable or modifiable cargo lifecycle callbacks.
- `api.custom.action.CustomActionEffects` and `CustomActionEffectEvents`: target-safe execution and
  before/after interception for each mapped custom action effect without exposing Android
  `PointF` in listener signatures.
- `api.resource.Resources`: built-in/custom resource lookup, amounts, affordability, atomic payment,
  refunds, and team resource queries.
- `api.command.Commands` and `api.command.event.CommandEvents`: synchronized command creation,
  common orders, and cancellable issue callbacks.
- `api.projectile.event.ProjectileEvents`: typed creation, update, impact, explosion, and removal.
- `api.map.Maps`, `MapObjects`, `MapTiles`, and map events: current-map queries and typed TMX/current-map
  loading phases, tile/world conversion, visibility, exploration, fog reveal helpers, and immutable
  tile-layer plus custom object-group/point/region/property catalogs.
- `api.path.Pathfinding`, `PathQuery`, `PathRequestHandle`, and `PathEvents`: native blocking,
  terrain-only blocking, movement cost, clearance and connected-region queries; immutable
  asynchronous route requests; defensively copied results; and queue/one-shot solve callbacks.
- `api.world.GameWorld` and `WorldPoint`: current simulation state, timing, speed, network role,
  map bounds, and immutable world positions.
- `api.networking.ClientNetworking` and `ServerNetworking`: Fabric-style `namespace:path` channels,
  immutable binary payloads, client-to-host send, targeted host-to-client send, and Loader-only
  broadcast over the game's existing reliable connection.
- `api.networking.PacketBuffer`, `PacketCodec`, and `PacketCodecs`: bounded primitive/VarInt/string/
  UUID/array serialization and reusable typed channel payloads.
- `api.networking.Connections` and `api.networking.event.ConnectionEvents`: connection snapshots,
  Loader-peer filtering, accepted-player/ready/closing/removed lifecycle callbacks, and disconnects.
- `api.util.Identifier`: general validated Fabric-style `namespace:path` identifiers.
- `api.data.PersistentData`, `PersistentDataKey`, `PersistentDataCodec`, and
  `api.data.event.PersistentDataEvents`: versioned global/per-unit components, lazy migration,
  codec failure reporting, unknown-mod data retention, and automatic persistence through local
  saves, replay/save streams, and multiplayer resync snapshots.

Compile against `game-lib-named.jar` and the named desktop API Jar. Both the mod and API must then
be remapped to the official namespace for a normal game installation. The existing
`api.event` callbacks remain available as the namespace-neutral compatibility layer.

`rusted-fabric-api-desktop:build` also creates a named `-sources.jar` containing both the portable
and mapped desktop source trees, so IDE navigation and documentation work from a single dependency.

State-changing helpers such as `UnitSpawns.spawn`, `CustomUnits.create`, `BuildQueues.clear`,
`TransportUnits.tryLoad`, `Attachments.attach`, `UnitTags.set`, `CustomUnitTriggers.trigger`,
`CustomActionEffects.executeAt`, `UnitActions.issue`, and resource setters as well as
`CombatUnits.tryFire`, `UnitOrders` mutations, `RepairReclaim.setConstructionProgress`, all
`StatusEffects` and `CustomUnitStats` mutations, `UnitVitals` writes, `Saves.save/load/delete`, and
`Missions.activate` must run on the update
thread. Use
`RustedWarfareClient.execute` when starting from UI, file, or
network callbacks. Action helpers create normal game `Command` objects, so they retain the game's
multiplayer command path instead of directly changing unit orders.

`Pathfinding.submit` also runs on the update thread. Its `PathRequestHandle.future()` completes when
the native solver publishes the route; it does not block the update loop. Path results copy every
native node and its current tile-center world position. Pending Loader-owned futures complete
exceptionally when the map is replaced. In multiplayer, route-dependent simulation must submit the
same deterministic query on every participating peer.

For player-issued multiplayer orders, prefer `Commands.move`, `attack`, `attackMove`, `repair`,
`reclaim`, `guard`, `patrol`, `loadInto`, or `loadUp`. Direct `UnitOrders` mutations are intended for
deterministic simulation logic that executes on every peer; calling them on only one peer can
desynchronize a match.

## Java unit actions

An ordinary JAR mod can add a button to the native action panel without defining a custom-unit
action or adding its own mixin. Register the action during mod initialization and attach it to the
native internal type name; attachments are retained across custom-unit metadata reloads:

```java
JavaUnitAction report = JavaUnitAction.builder(
        "example:report_status", "Report status", "Runs once for each selected tank",
        context -> log("selected unit=" + context.unit()))
        .availableWhen(unit -> !unit.dead)
        .displayPriority(20.0F)
        .build();
JavaUnitActions.attach("tank", report);

JavaUnitAction mark = JavaUnitAction.builder(
        "example:mark", "Mark position", "Runs at a synchronized world point",
        context -> context.targetPoint().ifPresent(point -> mark(context.unit(), point)))
        .targetPointWhen((unit, point) -> point.x() >= 0.0F && point.y() >= 0.0F)
        .build();
JavaUnitActions.attach("tank", mark);

JavaUnitAction signal = JavaUnitAction.builder(
        "example:signal", "Signal", "Costs 5 credits per executing unit",
        context -> sendSignal(context.unit()))
        .textForUnit(unit -> Translations.translate("example:signal_name", Math.round(unit.hp)))
        .descriptionForUnit(unit -> Translations.translate("example:signal_description"))
        .creditCost(5)
        .cooldownMillis(3_000)
        .build();
JavaUnitActions.attach("tank", signal);
```

The action ID is passed through the game's normal `Command` path and resolved independently for
each selected unit. Therefore every simulation participant must load the same gameplay mod and
register the same IDs, attachments, conditions, listeners, and deterministic callbacks. This is a
required gameplay-mod feature, not a server-only or vanilla-client-compatible extension point.
Callbacks run on the update thread and exceptions propagate to the caller; do not read local UI,
wall-clock, file, or unsynchronized random state when changing gameplay. A cancelled, hidden,
unavailable, or locked Java action is consumed without executing its handler.

`targetPoint()` enters the game's native map cursor and accepts every finite point;
`targetPointWhen(...)` adds deterministic per-unit validation. The cursor is accepted when at least
one selected unit accepts the point, while command execution rechecks the predicate and invokes the
handler only for accepting units. The synchronized point is available through
`JavaUnitActionContext.targetPoint()`. Programmatic callers should use `UnitActions.issueAt(...)`
with a null unit target for these actions.

`creditCost(...)` uses the game's native price object, so the action panel displays the cost and
participates in local resource prediction. The synchronized execution path rechecks affordability
and atomically charges the complete integer-credit cost separately for every selected unit whose
handler would run. `BEFORE_EXECUTE` cancellation happens before payment; a handler or
`AFTER_EXECUTE` exception does not refund a completed payment. Conditions, target validators and
listeners must therefore be deterministic on every simulation participant.

`cooldownMillis(...)` uses the game's native per-unit action-block status and its deterministic
simulation millisecond clock. The action panel displays the native cooldown and refuses another
order while it remains active; `UnitActions.available(...)`, `UnitActions.canRun(...)`, and final
synchronized execution apply the same check. A positive cooldown begins after `BEFORE_EXECUTE`
accepts and payment succeeds, but before the handler runs. It is serialized with the unit, and can
be inspected with `cooldownMillis()`, `remainingCooldownMillis(unit)`, or
`isCoolingDown(unit)`. A handler exception does not remove a cooldown that already began.

`textForUnit(...)` and `descriptionForUnit(...)` run only when the native action panel/tooltip has
a concrete unit; otherwise the builder's static fallback strings are returned. These callbacks are
client presentation and may use `Translations`, but should remain fast because the UI can query
them every frame. `icon(ClientImage)` accepts an image loaded from a mod Jar with `ClientImages`
after the graphics engine is initialized. `icon(Supplier<ClientImage>)` supports lazy client-only
state and may return null on a headless participant. The action retains the wrapper rather than a
raw native image; a closed image safely becomes no icon. Because action registrations are
process-lifetime, keep an owned action icon open until shutdown and do not replace/close it while a
render call may still be using it.

This public form deliberately remains non-queued. Immediate and world-point actions with optional
integer-credit costs are supported; custom-resource costs, queued, unit-targeted, and build actions
remain closed until their native reservation, prediction, target-selection, and queue semantics can
be preserved end to end. The registry has no unregister operation because action IDs are part of
the multiplayer command protocol for the process lifetime.

## TMX map object data

`MapObjects.snapshot()` copies every loaded TMX object group into a `MapObjectCatalog`. It is safe to
retain across ticks and contains no native `MapObject`, `Properties`, or Android geometry objects.
Catalogs can query groups and objects case-insensitively by name or type, find objects with a custom
property, and perform axis-aligned point/intersection searches:

```java
MapObjectEvents.AFTER_LOAD.register(catalog -> {
    for (MapObjectSnapshot zone : catalog.ofType("capture_zone")) {
        int score = zone.properties().integer("score").orElse(1);
        WorldPoint center = zone.center();
    }
});
```

`MapProperties` preserves the TMX strings and offers non-throwing integer, finite-decimal, and
boolean parsing. Boolean values accept `true/false`, `yes/no`, `on/off`, and `1/0`. Snapshotting does
not mark native properties as consumed, so it does not hide the game's unused-property diagnostics.
Object coordinates are the final world coordinates after the game's map-scale conversion. Bounds
queries remain axis-aligned; `rotation()` is reported separately. Tile objects additionally expose
their GID, local tileset index, tileset name, and image source. Capture from the update thread or use
the immutable catalog supplied by `MapObjectEvents.AFTER_LOAD`.

`MapTiles` provides the corresponding read-only tile-layer view without returning mapped
`MapLayer` or `MapTile` instances. Layer metadata includes dimensions, visibility, ground/items
classification, non-atlas rendering and custom properties. Individual, ground, world-coordinate,
layer-stack, and bounded rectangular queries capture only the requested non-empty tiles:

```java
MapTiles.groundAtWorld(unit.x, unit.y).ifPresent(tile -> {
    if (tile.water() && !tile.waterBridge()) {
        log("unit is over water at " + tile.tileX() + "," + tile.tileY());
    }
    int customDamage = tile.properties().integer("damage").orElse(0);
});
```

A `MapTileSnapshot` includes world bounds/center, global and local tileset IDs, atlas/registered IDs,
water, bridge, lava, cliff, resource-pool and construction-blocking flags, extra pathing cost,
variant count, and tileset properties. `region(...)` validates that its complete rectangle belongs
to the selected layer and returns present tiles in row-major order. The older `Maps.tileAt(...)`
methods remain available as explicitly raw mapped escape hatches for operations not yet covered.

Register persistent keys once during mod initialization. Each key owns a current non-negative data
version; its decoder receives the version stored in the save and may migrate old payloads. Entries
are limited to 256 KiB and the complete Loader extension to 16 MiB. The extension is appended after
the native `<SAVE END>` marker: vanilla ignores it, old vanilla saves remain readable by Loader,
and raw entries for currently absent mods are preserved when the world is saved again. Persistent
data mutations that affect simulation must follow the same deterministic multiplayer rules as the
rest of the game state.

`UnitTypes.canSpawnStarting(...)` only checks whether a starting-unit preview can be placed at a
location. The old `UnitTypes.spawnStarting(...)` name is deprecated because it never created a
unit. Use `UnitSpawns` for actual creation with team accounting and building path-cost refresh:

```java
RustedWarfareClient.execute(() -> {
    Unit tank = UnitSpawns.spawn(UnitTypes.require("tank"), team, 400.0F, 600.0F);
    Teams.changeOwner(tank, anotherTeam);
});
```

Direct unit creation changes synchronized game state. In multiplayer it should normally be
performed by the host as part of deterministic mod logic; it is not automatically converted into
a vanilla command or replicated as a custom network message.

## Desktop client options

`ClientOptions` exposes a fixed typed catalog of 38 user-visible preferences and an immutable
`ClientOptionSnapshot`. It deliberately omits
UUIDs, network identity keys, ban policy, storage migration, mod-manager serialization, and other
internal settings. Each key declares its Java type, stable `rustedwarfare:*` ID, native name, value
validation, and whether applying it fully may require restarting the desktop client.

```java
ClientOptionUpdateResult result = ClientOptions.update(options -> options
        .set(ClientOptions.MASTER_VOLUME, 0.75F)
        .set(ClientOptions.SHOW_FPS, true)
        .set(ClientOptions.MINIMAP_ALLY_COLORS, true));

if (result.restartRequired()) {
    log("One or more options require a restart to apply completely");
}
```

The transaction validates every pending value before mutation, removes unchanged values, lets
`ClientOptionEvents.BEFORE_UPDATE` cancel the complete set, applies it atomically under the native
settings lock, and saves once. Pass `false` as the second `update` argument for a transient runtime
change. The result distinguishes cancellation, application, persistence request/success, and
restart requirements. Run mutations through `RustedWarfareClient.execute(...)` when called from a
worker thread.

`AFTER_NATIVE_DYNAMIC_CHANGE` observes supported fields changed through the game's normal
reflective UI/preferences path. `BEFORE_NATIVE_SAVE` and `AFTER_NATIVE_SAVE` observe all native
save calls, including the one generated by an API transaction, but cannot cancel them. Client
options are local presentation/preferences and must not be used as synchronized gameplay state.

## Desktop key bindings

Register custom bindings during the mod entrypoint. Registration is safe before `GameEngine`
exists: the API installs pending bindings when the native input registry is constructed, early
enough for the game's normal `[keys]` settings loader and key-binding screen.

```java
ModKeyBinding inspect = KeyBindings.register(
        "examplemod:inspect", "Inspect selection", "Example Mod", "CTRL+K");

KeyBindingEvents.PRESSED.register(binding -> {
    if (binding == inspect) {
        log("selected=" + Selection.snapshot().size());
    }
});
```

IDs use lowercase `namespace:path` syntax and duplicate registration is idempotent only when all
metadata matches. The label shown by the game is given a stable ID suffix so two mods cannot
silently share the same settings key. `isPressed()` is also available for tick-based polling.

`ClientInputEvents` exposes lower-level keyboard, mouse-button, movement, drag, and wheel callbacks
after the game has updated its own input state. `KeyboardInput` contains both the desktop callback
code and the translated game key code, printable character, repeat state, modifier snapshot, and
whether LibRocket UI was active. `PointerInput` contains physical callback coordinates, logical HUD
coordinates, movement delta, button/wheel data, and an optional world position:

```java
ClientInputEvents.MOUSE_PRESSED.register(input -> {
    if (!input.userInterfaceActive() && input.insideWorldViewport()) {
        WorldPoint clicked = input.worldPosition().orElseThrow();
    }
});

ClientInputEvents.KEY_PRESSED.register(input -> {
    boolean controlShift = input.modifiers().control() && input.modifiers().shift();
});
```

Raw input events are deliberately observational. Cancelling a low-level release callback could
leave the native key or touch state permanently pressed, so behavior cancellation remains on the
semantic command, selection, combat, and unit-action events. `InputKeys` provides stable desktop to
game-key translation, key names, current key state, and the current modifier snapshot.

## Desktop screen lifecycle

`ScreenEvents` observes the game's page stack without exposing `ElementDocument` or other
LibRocket implementation classes. Every `UiDocumentSnapshot` has a stable lifetime ID, a normalized
path, a `PAGE`, `POPUP`, or `ALERT` kind, safe primitive metadata, and modal fields such as title,
message, text-input presence, and back-button visibility.

```java
ScreenEvents.OPENED.register(document ->
        log("opened " + document.kind() + ": " + document.path()));

ScreenEvents.TOPMOST_CHANGED.register(change ->
        change.next().ifPresent(document -> log("now visible: " + document.path())));
```

`LOADED` applies to normal pages after native loading has completed but before they are shown.
`OPENED` and `CLOSED` include pages and both overlay kinds. `ACTIVE_PAGE_CHANGED` tracks the page
behind overlays, while `TOPMOST_CHANGED` tracks what the user currently sees. Replacement changes
contain both the previous and next snapshots.

`ClientScreens` provides read-only queries for the active page, popup, alert, and topmost document.
Its `back`, `reloadActivePage`, `clearHistory`, and `closeTopmostOverlay` methods use the game's
normal navigation paths and should be called from the update/UI thread. Arbitrary RML document
opening is intentionally not public yet because it requires a resource and script-isolation policy.

`ClientDialogs` covers the common interactive case without RML or script strings. A dialog has one
required primary button, an optional secondary button, optional text input, optional Enter-to-submit,
and an optional back/escape dismissal path. Buttons invoke a Java callback exactly once after the
native popup closes:

```java
DialogSpec rename = DialogSpec.builder("Rename unit", "Enter a display name")
        .textInput("")
        .primaryButton("Save")
        .secondaryButton("Cancel")
        .build();

ClientDialogs.show(rename, result -> {
    if (result.choice() == DialogChoice.PRIMARY) {
        String name = result.input().orElse("");
    }
});
```

The return value is empty when the game or another mod already owns the normal popup slot. The API
does not replace or queue behind that popup. A successful `DialogHandle` can check its open state or
dismiss only its own dialog. Dismissal does not report partially entered text; submitted empty text
is represented by a present `Optional<String>` containing `""`. Creation and handle dismissal must
run on the update/UI thread.

## HUD/world drawing and Jar images

Prefer `HudRenderEvents.AFTER_HUD` over the lower-level `AFTER_HUD_RENDER` event. It supplies one
frame-scoped `HudDrawContext` with screen dimensions, UI scale, delta, text measurement, text with
backgrounds, rectangles, lines, circles, whole/centered/rotated/scaled/region images, clipping, and
transforms. `DrawStyle` is immutable and hides the game's Android-compatible `Paint` class.

```java
DrawStyle label = DrawStyle.text(ArgbColor.WHITE, 16.0F);
DrawStyle panel = DrawStyle.fill(ArgbColor.argb(160, 0, 0, 0));

HudRenderEvents.AFTER_HUD.register((gameInterface, draw) -> {
    draw.drawTextWithBackground("Wave " + wave,
            12.0F, 24.0F, label, panel, 5.0F);
    draw.withClip(0.0F, 0.0F, 200.0F, 100.0F, clipped ->
            clipped.drawCircle(100.0F, 50.0F, 32.0F,
                    DrawStyle.stroke(ArgbColor.GREEN, 2.0F)));
});
```

Use `WorldRenderEvents.AFTER_WORLD` for range rings, targeting previews, path overlays, and labels
anchored to the map. It fires after terrain, units, effects, and mission overlays, but before the
HUD and minimap. The loader applies the native world viewport clip and restores graphics state even
when a listener fails. Geometry uses world units; stroke widths, text sizes, padding, and naturally
sized images use screen pixels and therefore remain readable while zooming:

```java
WorldRenderEvents.AFTER_WORLD.register(draw -> {
    WorldPoint center = draw.viewport().center();
    draw.drawCircle(center.x(), center.y(), 40.0F,
            DrawStyle.stroke(ArgbColor.argb(190, 80, 220, 255), 2.0F));

    ScreenPosition screen = draw.worldToScreen(center);
    WorldPoint roundTrip = draw.screenToWorld(screen.x(), screen.y());
});
```

`WorldViewport` is an immutable per-frame snapshot based on the renderer's pixel-snapped camera.
It provides world/screen conversion, zoom-aware length conversion, viewport bounds, and circle
visibility checks. `WorldDrawContext.screen()` is available when an overlay also needs an explicit
screen-space element while retaining the world viewport clip.

`withClip` and `transformed` restore native graphics state in `finally`, including when a listener
throws. Styles are converted to native paints at most once per context, and fonts are prepared only
when a style is actually used for text. Direct `graphics()` access remains as an escape hatch for
uncovered mapped operations and carries responsibility for restoring any changed state.

`ClientImages.load(ModResource)` reads PNG/JPEG-compatible image data directly from an ordinary
mod Jar through a named native stream. It returns `Optional.empty()` only when
`ClientImageEvents.BEFORE_LOAD` cancels the load. A returned `ClientImage` records size, source,
fallback, and ownership; close owned images when the mod no longer needs them. Closing is
idempotent and never releases engine-owned images wrapped with `ClientImage.borrowed(...)` or the
shared out-of-memory fallback. Load/create/release and drawing must run on the render/update thread.

## Camera, sound, and visual effects

Camera mutation and effect creation should run on the update/render thread. Native effect creation
can return no object when visibility rules or the effect pool reject it, so creation helpers return
`Optional<EffectInstance>` where appropriate.

```java
RustedWarfareClient.execute(() -> {
    Camera.centerAt(400.0F, 600.0F);
    Camera.setTargetZoom(1.25F);
    Effects.light(400.0F, 600.0F, 0.0F, 0xffffaa00);
    Sounds.playAt(Sounds.requireBuiltin("click"), 0.8F, 400.0F, 600.0F);
});

SoundEvents.BEFORE_PLAY.register((engine, playback) ->
        mutedSoundNames.contains(playback.sound().name));
```

`SoundEvents.BEFORE_PLAY` aggregates cancellation without skipping later listeners. It intercepts
the game's high-level interface, global, and positional paths, including sounds initiated by the
base game. `EffectEvents` currently exposes fully configured line, light, and attached-light
effects; lower-level allocation remains deliberately private because helper methods continue to
configure pooled instances after allocation.

## Saves, missions, and local messages

`Saves` accepts plain leaf names only and adds `.rwsave` automatically. It rejects path traversal,
directory separators, control characters, and Windows-reserved filename characters before the
game filesystem is reached. Saving retains the native temporary-file replacement path, while
loading uses the same normal-load option as the desktop save menu.

```java
RustedWarfareClient.execute(() -> {
    Saves.save("example checkpoint");
    MissionSnapshot mission = Missions.snapshot();
    ClientMessages.postSystem("Wave: " + mission.survivalWave());
});

SaveEvents.BEFORE_LOAD.register((manager, name) -> protectedNames.contains(name));
MissionTriggerEvents.AFTER_ACTIVATE.register((engine, trigger) -> log(trigger.id));
```

Mission mutation is intentionally narrow. Mapping names such as `MissionEngine.setWave` and
`GameSaver.setSaveCompression` are not exposed because bytecode analysis shows that they perform
different internal jobs (trigger-frame processing and autosave-timer reset respectively).
`Missions.activate` is provided because that method's trigger bookkeeping is verified; mission
state and win/loss results otherwise remain snapshot queries.

## Mod Jar resources and localization

`ModResources.forMod(modId)` opens a read-only resource pack backed by that Fabric mod container.
Paths are always relative to the Jar root and reject absolute paths, empty segments, traversal,
control characters, and drive prefixes before any I/O. `ModResource` supports streams, bounded
byte reads, UTF-8 text, and UTF-8 Java `Properties`:

```java
ModResourcePack resources = ModResources.forMod("examplemod");
String template = resources.resource("assets/examplemod/templates/default.ini").readUtf8();
List<ModResource> definitions = resources.find("assets/examplemod/definitions",
        path -> path.endsWith(".json"));
```

Fabric-container packs support deterministic recursive discovery; results are filtered and sorted
by the full Jar-relative path. `forClass(...)` is intended for direct library/test lookups and
reports `supportsDiscovery() == false`, because a general JVM class loader cannot safely enumerate
all of its resources.

`ModResources.forDirectory(...)` exposes the same read-only/discovery contract for an explicitly
selected development directory. Real-path checks prevent a symlinked file from escaping that root;
this is intended for generated fixtures and unpacked hot-reload assets, not arbitrary game files.

Register reusable data loaders through `ModResourceReloaders` instead of rebuilding runtime state
inside an arbitrary render or tick callback. Preparation for every schedulable listener completes
before any apply phase begins; declared dependencies control apply order:

```java
ModResourceReloaders.register("examplemod:rules", resources,
        new ModResourceReloader<Properties>() {
            public Properties prepare(ModResourcePack pack) throws Exception {
                return pack.resource("assets/examplemod/data/rules.properties")
                        .readPropertiesUtf8();
            }

            public void apply(Properties prepared) {
                activeRules = Rules.parse(prepared);
            }
        }, "examplemod:base_data");
```

Registered listeners run automatically after initial engine setup, after the native custom-unit
link graph is rebuilt, and after a language reload. `reloadAll(MANUAL)` provides an explicit hot-
reload path. Execution is synchronous on the native event/manual caller thread, so `prepare` should
perform bounded parsing and `apply` should publish already prepared state quickly. A preparation or
apply exception is captured in `ResourceReloadReport`; independent listeners continue, while
dependents become `BLOCKED`. Missing dependencies and dependency cycles are also reported without
executing affected listeners. `ModResourceReloadEvents.AFTER_RELOAD` is the central place for a mod
or development tool to log failures.

Reads into memory are capped at 128 MiB. When a native game API requires a real filesystem path,
`extractToCache()` writes a content-addressed copy atomically below the operating system temporary
directory. It does not unpack into the game, project, or Git repository. Equal content reuses the
same path; `ModResourceEvents` observes reads and can cancel API-mediated extraction.

Data-driven reloaders can use `ResourceConditions` to keep optional compatibility data in the same
Jar. A resource is unconditional when it has no `rustedfabric:load_conditions` member. When the
member is present, its array is evaluated in order and the first false condition skips that whole
resource during preparation:

```json
{
  "rustedfabric:load_conditions": [
    {
      "condition": "rustedfabric:all_mods_loaded",
      "values": ["economy_addon", "shared_library"]
    }
  ],
  "values": ["examplemod:addon_mode"]
}
```

Built-in condition IDs are `rustedfabric:true`, `false`, `all_mods_loaded`, `any_mod_loaded`,
`not`, `all`, `any`, `registry_contains`, and `tag_contains`. Boolean conditions nest through a
condition object in `value` (`not`) or an array in `values` (`all`/`any`). Registry lookup uses
stable IDs; `registry_contains` takes `registry` and `value`, while `tag_contains` additionally
takes `tag`. Missing mods, registries, entries, or tags evaluate false. Invalid JSON and unknown
condition types fail the reloader's prepare phase instead of silently loading incompatible data.
Registry and tag predicates observe the active snapshot before the current reloader's apply phase;
they cannot refer to a tag being created by the same pending JSON transaction.

Condition types are process-wide and may be extended during mod initialization:

```java
ResourceConditions.register("examplemod:feature_enabled", json -> {
    boolean enabled = json.get("enabled").getAsBoolean();
    return context -> enabled;
});
```

`ResourceConditionEvaluation` reports how many top-level conditions ran and identifies the first
rejection, which is useful for development diagnostics. `ResourceConditionContext.runtime()` uses
the live Fabric Loader and mod registries; its builder supplies an explicit immutable context for
offline validators and tests. The Registry Tag JSON reloader applies these conditions directly and
offers a context overload for tooling.

Translations use `assets/<namespace>/lang/*.properties` in UTF-8. Register a unique namespace once
during mod initialization and address keys as `namespace:path`:

```java
Translations.register("examplemod", ModResources.forMod("examplemod"));
String message = Translations.translate("examplemod:loader_ready", playerName);
```

For language `zh_cn`, files are merged in this order: `default.properties`, `en.properties`,
`zh.properties`, then `zh_cn.properties`; later values override earlier ones. Missing keys return
their identifier unless `translateOr(...)` supplies an explicit fallback. Parameters use
`MessageFormat` with the active locale. A native language reload clears the API cache and fires
`LanguageEvents.BEFORE_RELOAD` / `AFTER_RELOAD`. Closing the returned
`Translations.Registration` removes that namespace, which is useful for tests or controlled
runtime teardown.

## Jar data generation

`ModDataGenerator` is the build-time counterpart to the runtime resource APIs. It does not launch
the game and writes only below an explicit output directory. Providers write into private memory
first; dependencies run in topological order, duplicate paths are rejected, and no file is changed
when any provider fails, is missing a dependency, or participates in a dependency cycle. After all
providers succeed, paths are sorted and committed with per-file atomic replacement where supported.
Identical files are left untouched and reported separately from written files.

```java
ModDataGenerator generator = new ModDataGenerator(output, "examplemod");
generator.addProvider("examplemod:language",
        new LanguageDataProvider("examplemod", "zh_cn") {
            protected void generateTranslations(LanguageBuilder translations) {
                translations.add("loader_ready", "模组已加载");
            }
        });

generator.addProvider("examplemod:mode_tags",
        new RegistryTagDataProvider<Mode>(MODE_REGISTRY_KEY, "examplemod") {
            protected void generateTags(TagLookup<Mode> tags) {
                tags.tag("examplemod:interactive")
                        .condition(ResourceConditionJson.allModsLoaded("examplemod"))
                        .add(Identifier.parse("examplemod:survival"))
                        .addOptional(Identifier.parse("addon:optional_mode"));
            }
        }, "examplemod:language");

DataGenerationReport report = generator.run().requireSuccess();
```

`DataOutput` also supports arbitrary bytes, UTF-8 text, and pretty JSON, so a mod can implement its
own provider without extending an API base class. Paths use the same relative-path protections as
runtime resources, individual outputs are capped at 128 MiB, and provider/resource snapshots are
defensively copied. `ResourceConditionJson` creates built-in nested, mod, registry, and tag
conditions without hand-writing their JSON schema.

Keep build-only sources in a separate Gradle source set so they are not packaged into the runtime
mod. The example project exposes `runDatagen`, uses `build/generated/datagen` as an exclusive output
directory, clears that directory before an executed generation task to remove stale files, and adds
it to the main resource set. Core `ModDataGenerator` deliberately does not delete unplanned files
because callers may point it at a directory containing hand-written resources.

```groovy
sourceSets { datagen { java.srcDir 'src/datagen/java' } }
dependencies { datagenImplementation project(':rusted-fabric-api-desktop') }

tasks.register('runDatagen', JavaExec) {
    dependsOn datagenClasses
    classpath = sourceSets.datagen.runtimeClasspath
    mainClass.set('examplemod.ExampleDataGeneration')
    args layout.buildDirectory.dir('generated/datagen').get().asFile.absolutePath
}
```

## Optional inter-mod services and lifecycle scopes

`ModServices` is intended for optional runtime integration, where one mod publishes an interface
and another uses it only when a provider is installed. Put the service interface and its
`ServiceKey` in a small shared API artifact (or in the consumer's published API), so every mod sees
the same `Class` object. A provider can then register during its normal Fabric initializer:

```java
public interface EconomyBridge {
    ServiceKey<EconomyBridge> KEY =
            ServiceKey.of("economy_api:bridge", EconomyBridge.class);

    int credits(Object team);
}

LifecycleScope modScope = LifecycleScope.create("economy_provider");
modScope.own("economy bridge", ModServices.register(
        EconomyBridge.KEY,
        "my_economy_mod:default",
        100,
        team -> readCredits(team)));
```

Consumers do not need a hard dependency on a particular implementation:

```java
ModServices.find(EconomyBridge.KEY).ifPresent(bridge -> {
    int credits = bridge.credits(team);
});
```

Higher priority providers are selected first. Equal priorities are ordered by provider
`namespace:path`, never by mod load order, so `find`, `all`, and `entries` are deterministic.
Reusing a provider ID for the same service or reusing a service ID with another Java type is
rejected. `ServiceRegistration.close()` removes only that provider; callers that cache a selected
service must therefore define their own lifetime rules. Service objects are local runtime objects
and are never automatically serialized or included in multiplayer compatibility fingerprints.

`LifecycleScope` collects those removable registrations and closes them in reverse registration
order. Child scopes make independently toggleable features straightforward:

```java
LifecycleScope feature = modScope.child("optional_hud");
feature.own("tick listener", ClientTickEvents.END_CLIENT_TICK.subscribe(this::tickHud));
feature.onClose("clear cached HUD data", hudCache::clear);

// Disable just this feature. Calling close again is safe.
feature.close();
```

`close()` attempts every cleanup and throws one `LifecycleCloseException` containing all failures.
Code that wants to log or display failures without throwing can call `closeReport()` instead.
Both methods are idempotent; the report records attempted/succeeded counts and the label/cause for
each failed resource. `forget(resource)` transfers ownership back to the caller without closing it.

These services differ from persistent registries: use `ModServices` for replaceable behavior and
optional adapters, and `ModRegistries` for stable content IDs that can enter saves or packets.

## Mod-defined registries

`ModRegistries` is the root for extension objects shared between ordinary Java mods. A registry ID
is process-wide and carries a runtime value type, while each entry has its own `namespace:path` ID
and a stable insertion-ordered raw ID:

```java
RegistryKey<Mode> key = RegistryKey.of("examplemod:modes", Mode.class);
ModRegistry<Mode> modes = ModRegistries.create(key);

modes.events().AFTER_ENTRY_ADDED.register((registry, entry) ->
        log("registered " + entry.id() + " as " + entry.rawId()));
modes.register("examplemod:survival", new Mode("Survival"));
modes.freeze();
```

Duplicate IDs, reuse of the same value instance, incompatible lookup types, and registration after
freeze are rejected. Snapshots preserve insertion order and never expose mutable backing
collections. `BEFORE_FREEZE` may perform final registrations; after `freeze()` returns, IDs and raw
IDs are immutable. There is intentionally no unregister operation: registry entries can be stored
in saves or packets, so removing one at runtime would make numeric identities ambiguous. Registry
creation and entry registration should normally happen during mod initialization before gameplay.

Never persist or transmit `RegistryEntry.rawId()` without first proving an identical frozen layout.
Use stable-ID codecs for normal save/network payloads:

```java
PacketCodec<Mode> modeCodec = RegistryCodecs.value(modes);
ServerNetworking.registerGlobalReceiver(modeChannel, modeCodec,
        (engine, sender, channel, mode) -> applyMode(mode));
```

`modes.snapshot()` records the registry ID, runtime value type, frozen state, ordered IDs, a
content fingerprint, and a layout fingerprint. `local.compare(remote)` distinguishes an exact
layout, the same entries in a different raw-ID order, different entries, and a different registry.
Stable-ID codecs are safe for either identical entry set; `rawIdsCompatible()` becomes true only
for two frozen snapshots with the exact same layout. `RegistryCodecs.SNAPSHOT` can carry this check
over an existing Loader-only handshake or named channel. Unknown stable IDs fail decoding instead
of silently selecting another entry.

Each `ModRegistry` also owns a reloadable `tags()` manager. Contributions are keyed by a stable
contributor ID, so applying a new version replaces that contributor's previous data without
accumulating stale entries. All contributions are merged and resolved before commit; required
missing entries/tags or a `#tag` cycle reject the transaction without changing the active tags.
Optional values use `{ "id": "examplemod:optional", "required": false }`.

The JSON reloader discovers files at:

```text
data/<tag namespace>/tags/<registry namespace>/<registry path>/<tag path>.json
```

For registry `examplemod:modes`, the file
`data/examplemod/tags/examplemod/modes/interactive.json` defines tag
`examplemod:interactive`:

```json
{
  "rustedfabric:load_conditions": [
    {
      "condition": "rustedfabric:any_mod_loaded",
      "values": ["examplemod", "examplemod_compat"]
    }
  ],
  "replace": false,
  "values": [
    "examplemod:survival",
    "#examplemod:playable",
    { "id": "addon:optional_mode", "required": false }
  ]
}
```

Register it through the normal resource reload graph so its entries already exist before apply:

```java
RegistryTagJsonReloader<Mode> tags = RegistryTagReloaders.json(
        modes, "examplemod:mode_tags", "examplemod");
ModResourceReloaders.register("examplemod:mode_tags", resources, tags,
        "examplemod:base_data");
```

Tag membership is identity-based like registry values. Tags may change after the registry itself
is frozen; entry IDs/raw IDs remain immutable while grouping data can follow resource reloads.

## Mod configuration and chat commands

`ModConfigFiles.file(modId, relativePath)` creates a path-safe handle below Fabric Loader's
external `config/<modid>/` directory. It does not put user settings in the mod Jar or Git repository.
Reads are capped at 8 MiB; writes use a temporary file in the same directory and atomically replace
the destination where the filesystem supports it. UTF-8 text, raw bytes, and Java `Properties` are
supported. I/O remains explicit and reports `IOException` to the mod:

```java
ModConfigFile config = ModConfigFiles.file("examplemod", "settings.properties");
Properties values = config.readProperties();
values.setProperty("enabled", "true");
config.writeProperties(values, "Example Mod settings");
```

Paths are relative only. Traversal segments, empty segments, control characters, Windows-reserved
characters/device names, and paths longer than the API limit are rejected before filesystem access.
`ConfigEvents` observes or cancels API-mediated writes/deletes; direct `Files` calls are outside
those events.

`ChatCommands` registers server-side commands in mandatory `namespace:path` form. Players invoke
them using the game's normal `-`, `.`, or `_` command prefix. For example, a vanilla client can type
`.examplemod:status` when only the host has the mod installed:

```java
ChatCommands.register("examplemod:status", context -> {
    context.reply("tick=" + GameWorld.tick() + ", units=" + Units.alive().size());
    return 1;
});
```

The native server first tries to broadcast a chat line and then handles it as a command. The API
recognizes registered names before broadcast, hides the raw command line, and executes the handler
when the native command phase is reached. It does not replace or override built-in commands.
Handlers run synchronously on the server's native chat/network call thread; schedule game-state
mutation through `RustedWarfareClient.execute(...)` when thread ownership is uncertain. Arguments
support single/double quotes and backslash escaping. `context.reply(...)` targets only the sender,
while `context.error(...)` uses the native command-error path. `Chats` provides typed local send,
team send, host broadcast, targeted server message, and local-system-display helpers.

## Replays, statistics, war log, and minimap

`Replays` applies the same leaf-name/path-traversal policy as `Saves` and adds `.replay` when it is
omitted. Starting playback resets the current game exactly like the native replay menu, while
`stop()` closes either playback or recording streams. Use these mutations on the game thread.
`ReplayEvents` exposes cancellable record/play/delete boundaries plus non-cancellable stream-stop
notifications; a stop notification can also occur inside a subsequent record/play operation.

```java
RustedWarfareClient.execute(() -> {
    if (!Replays.isActive()) Replays.startRecording("modded match");
});

ReplayEvents.AFTER_PLAY.register((manager, name, success) ->
        log("replay " + name + " loaded=" + success));
```

`GameStatistics` deliberately returns immutable `TeamStatisticsSnapshot` values instead of the
engine's mutable counters. `historyValue(team, metric, gameTimeMillis)` reads the same four series
used by the post-game graph: income, army value, building value, and total value.

```java
TeamStatisticsSnapshot stats = GameStatistics.snapshot(team);
int economyAtTwoMinutes = GameStatistics.historyValue(
        team, StatisticMetric.INCOME, 120_000);
```

`WarLog` controls the local event log only; it does not send chat or network packets. Normal and
forced-duration text entries, unit-created/upgrade/damage entries, clearing, and the native
first-unread camera jump are available. `WarLogEvents` can cancel individual insertions.

`Minimaps` converts `WorldPoint` values to immutable `ScreenPoint` values and back, and can add the
four native marker kinds or scan pulses. Conversions return `Optional.empty()` while the mapping is
not ready or when a screen point is outside the minimap. Markers and war-log entries are client-side
presentation; synchronized gameplay must not depend on whether a listener cancels them.

## Lobby rules and player administration

`LobbyGameSetup.snapshot()` exposes only fields whose current mappings and native packet semantics
are confirmed: map source/path, starting credits, fog/reveal, AI difficulty, starting units, income
multiplier, nukes, shared control, team locking, and random seed. Unresolved obfuscated setup flags
are deliberately preserved internally but are not presented as public API.

Use `update(...)` as one transaction instead of mutating `NetworkEngine.gameSetup` directly:

```java
RustedWarfareClient.execute(() -> {
    if (LobbyGameSetup.canUpdate()) {
        LobbyGameSetup.update(editor -> editor
                .fogMode(LobbyFogMode.LINE_OF_SIGHT)
                .startingCredits(StartingCreditsPreset.MEDIUM_5000)
                .aiDifficulty(LobbyAiDifficulty.HARD)
                .incomeMultiplier(1.5F)
                .noNukes(true));
    }
});
```

The edit begins from the complete current native object, so unknown fields survive an update. It
validates all exposed values and custom starting-unit IDs before mutation. On a host it writes the
live setup and calls the native `applyGameSetup` path, which refreshes AI/player state and broadcasts
server info. On a native proxy controller the same call edits a copy and lets the game translate the
differences into normal lobby commands. An ordinary client cannot update rules. Changes are rejected
once game start has begun.

`LobbyGameSetupEvents.BEFORE_UPDATE` can cancel API-mediated transactions without partially writing
the live setup. `BEFORE_NATIVE_APPLY` and `AFTER_NATIVE_APPLY` observe the game's apply boundary but
are intentionally not cancellable: native UI code may have modified its live object before reaching
that method. `AFTER_UPDATE` reports whether the complete native call returned successfully.

`LobbyPlayers` snapshots active teams (including spectators by default) and maps a player team back
to its active connection. Host/proxy helpers use the game's existing request paths for kick,
zero-based slot move, and zero-based ally-team selection; `LobbyPlayers.SPECTATOR_SLOT` requests a
spectator slot. Hosts can additionally add AI, apply automatic team layouts, and pause a running
match. `LobbyPlayerEvents` can cancel those native administration boundaries, and
all listeners still run when aggregating a cancellation. These APIs send only native state and
commands, so they do not require client-side Loader installation.

## Team runtime state and economy

`Teams.snapshotState(team)` captures a stable view of the team's ID, ally group, player name,
AI/spectator/local-player classification, victory/defeat flags, manual and automatic shared-control
state, credits, unit counters, cap, and native income values. `Teams.snapshotStates(...)` captures
all active teams without exposing the native registry's mutable list.

```java
for (TeamStateSnapshot state : Teams.snapshotStates(true)) {
    System.out.println("team=" + state.teamId()
            + " ally=" + state.allianceGroup()
            + " credits=" + state.credits()
            + " sharing=" + state.isSharingControl());
}
```

`Teams.setCredits` validates finite values, chains `MODIFY_SET_CREDITS`, allows
`BEFORE_SET_CREDITS` cancellation, and then fires `AFTER_CREDITS_CHANGED`. The legacy convenience
method `Resources.setTeamCredits` delegates to the same boundary. `Teams.addCreditsAndRecordIncome`
uses the game's scaled native path; its actual old/new values are observed with source
`NATIVE_RECORDED_INCOME`. Direct native field assignments outside these paths cannot be observed.

Credit changes affect lockstep simulation. A gameplay mod must make the same deterministic change
on every participating peer; a server-only mod cannot safely grant credits only on the host while
unmodified clients simulate the match. `TeamStateEvents.OUTCOME_ANNOUNCED` is observational and
fires after native victory, defeat, or wiped-out announcements; it is deliberately not a blanket
interceptor for direct outcome-field writes.

## Named networking channels

Named channels are optional transport for features that actually need mod-to-mod communication.
They do not open another socket and are never sent to a connection that did not complete the RFH1
Loader handshake. This means a server-only or optional mod can still accept vanilla clients; use
`ServerNetworking.canSend(connection)` before sending any payload. Optional features can inspect
`ServerNetworking.peerManifest(connection)` / `isModPresent(...)` (or the client-side equivalents)
before assuming that the corresponding mod exists remotely.
The Windows Loader advertises `NetworkingCapabilities.NAMED_CHANNELS` (`network.channels.v1`);
other backends do not advertise it.

```java
ChannelId status = ChannelId.of("examplemod", "status");

ServerNetworking.registerGlobalReceiver(status, (engine, sender, channel, payload) -> {
    String message = payload.utf8();
});

if (ServerNetworking.canSend(connection)) {
    ServerNetworking.send(engine, connection, status, PacketPayload.utf8("ready"));
}
```

For reusable typed payloads, define a codec once and use the overloads on both sides:

```java
PacketCodec<String> statusCodec = PacketCodecs.UTF8;
ClientNetworking.registerGlobalReceiver(status, statusCodec,
        (engine, connection, channel, message) -> log(message));
ClientNetworking.send(engine, status, statusCodec, "ready");
```

`PacketBuffer` also supplies big-endian primitives, signed VarInt/VarLong round trips, strict UTF-8,
channel IDs, general `Identifier` values, UUIDs, and length-prefixed byte arrays. Typed decoders reject unread trailing bytes,
which catches mismatched protocol versions early instead of silently desynchronizing the payload.

Channels allow one global receiver per side. IDs are limited to lowercase `namespace:path` form,
payloads are copied on entry and capped at 256 KiB, and corrupt envelopes are rejected before a mod
callback runs. `ServerNetworking.broadcast` filters its recipients instead of using the game's raw
broadcast method, preventing unknown custom packets from reaching vanilla players.

## Game-thread scheduling

`GameThreadScheduler` queues work for the next mapped update or render phase. It is intended for
mod callbacks that start on a file, UI, or network thread but need to touch game/render state:

```java
GameThreadScheduler.onNextUpdate(() -> updateGameState())
        .exceptionally(failure -> {
            log(failure);
            return null;
        });
```

Tasks retain submission order. A task failure completes only its returned `CompletableFuture`
exceptionally and does not prevent later tasks from running. Work submitted while a phase is being
drained runs in the following phase, preventing unbounded same-frame loops. Check
`RustedFabricCapabilities.GAME_LIFECYCLE` before using the scheduler. Windows advertises it; the
old Android bridges are retained only as unsupported historical scaffolding.

For gameplay delays and repeating simulation work, use the desktop `GameTickScheduler`. Unlike the
phase queue above, it advances only when `GameEngine.currentTick` changes, so pausing or receiving a
duplicate render/update callback does not consume delay. A zero- or one-tick delay means the next
eligible scheduler phase. Larger native-tick jumps consume the corresponding delay but a repeating
task runs at most once in one scheduler phase, avoiding catch-up bursts:

```java
ScheduledGameTask pulse = GameTickScheduler.repeat(
        "examplemod:economy_pulse", 60, 60, GameTaskScope.SESSION,
        () -> updateEconomy());

GameTickScheduler.schedule(
        "examplemod:spawn_notice", 1, GameTaskScope.MAP,
        () -> ClientMessages.postSystem("Map started"));
```

Tasks retain submission order and always run on the desktop update thread immediately before
`ClientTickEvents.END_CLIENT_TICK`. Scheduling from inside an executing task cannot re-enter the
current dispatch. An ordinary exception fails only that task, completes its defensive completion
future exceptionally, and does not skip later tasks. Repeating tasks stop after failure.
`ScheduledGameTask` exposes state, execution count, remaining delay, last failure, and idempotent
cancellation. `TickExecutionReport` records each phase's executed/failed sequence IDs.

`MAP` tasks are cancelled immediately after a replacement map has loaded successfully, before the
public loaded-map callbacks; tasks created by those callbacks therefore belong to the new map.
`SESSION` tasks survive map replacement and are cancelled by `GameSessionEvents.SESSION_ENDED`.
`GLOBAL` tasks require explicit cancellation. Stable owner IDs support `cancelOwner` and owner-level
inspection, while `beginMap` and tick execution are backend bridge methods rather than mod lifecycle
entrypoints. Scheduled tasks are runtime-only and are not serialized into saves or replays.

Scheduling synchronized gameplay logic on only one multiplayer peer can desynchronize the match.
Either schedule the same task from the same deterministic event on every participating Loader peer,
or keep the task presentation-only. Server-only tasks are safe when they affect only server-side
services or issue normal replicated/vanilla-compatible commands.

## Projectile development API

`ProjectileEvents` exposes mapped creation, per-frame update, impact/explosion, removal-request, and
final removal boundaries. Update events are high-frequency and listeners should remain lightweight.
`Projectiles.snapshot(projectile)` returns an immutable `ProjectileSnapshot` containing the common
position, source/target, lifetime, speed, direct/area damage, ballistic, impact, and removal state:

```java
ProjectileEvents.AFTER_PROJECTILE_CREATED.subscribe((projectile, source) -> {
    ProjectileSnapshot state = Projectiles.snapshot(projectile);
    log("projectile=" + state.id() + " damage=" + state.directDamage());
});
```

`BEFORE_PROJECTILE_IMPACT` and `AFTER_PROJECTILE_IMPACT` include one immutable
`ProjectileImpactSnapshot`. It classifies unit, ground, fixed-position, or unspecified impacts and
captures the impact and target coordinates, target unit, unit/terrain collision flags, and contact
radius at the boundary. On exact explosion-capable backends the order is before-impact,
before-explosion, original game logic, after-explosion, then after-impact.

The snapshot distinguishes the named runtime, PC official namespace, and Android 1.15 official
field layout without exposing a compile-time game class dependency. The lifecycle event capability
is full on Windows and the Android local-patch backend. Xposed supports creation, update, and
removal, but is marked partial because method hooks cannot observe Android 1.15's inlined explosion
basic block; use the local-patch backend when exact explosion callbacks are required.

`Projectiles.requestRemoval(projectile)` retains the game's deferred-removal behavior.
`Projectiles.removeImmediately(projectile)` instead calls the mapped game-object removal entrypoint
synchronously; use it from an explosion callback when the projectile must not survive until its next
update. Normal backend removal events still apply.

## Unit and team development API

For ordinary desktop Fabric Jar mods, `Units`, `UnitView`, `Teams`, and `TeamView` are the preferred
surface for common unit logic. They hide named/official member names and avoid a compile-time game
class dependency. A view is live: every accessor reads the current game object, while list-returning
queries are immutable snapshots of membership.

```java
UnitLifecycleEvents.registerAfterUnitAdded(unit -> {
    if (unit.alive() && unit.healthFraction() < 0.25f) {
        unit.setHealth(unit.maxHealth() * 0.25f);
    }
});

List<UnitView> nearby = Units.within(worldX, worldY, 200f);
List<UnitView> friendly = Units.forTeam(playerTeam);
```

The stable view includes object identity and position, health/shield/energy/ammo, team, death and
registration state, building/flying/underwater flags, movement type, recent damager and containing
unit. `Units.active()`, `alive()`, `matching(...)`, `forTeam(...)`, `within(...)`, and `byId(...)`
cover the usual query patterns. Explicit operations currently include health, direction,
construction progress, team changes, and removal. Call them only from game-thread callbacks or work
scheduled through `GameThreadScheduler`.

Raw `Object` lifecycle events remain available for compatibility and uncommon mapped operations.
`registerAfterUnitAdded`, `subscribeAfterUnitAdded`, `registerBeforeUnitRemoved`, and
`subscribeBeforeUnitRemoved` provide the type-safe view adapters. Check
`RustedFabricCapabilities.GAME_UNITS` when a mod may run on an older Loader.

## Unit damage development API

`UnitDamageEvents` provides cancellable pre-damage and post-damage callbacks. The post callback
reports both the requested amount and the amount returned by the game. Windows exposes the full
damage, immunity, death-sequence, and death-effect surface. Android 1.15 R8 distributes
`applyDamage` across eight concrete implementations; both Android backends hook all eight exact
targets and expose the pre/post damage pair. They also expose the modifiable death-effect result for
14 mapped unit implementations and cancellable before/after complete-death callbacks for custom
units. Android remains `partial`: the shared immunity method and complete non-custom death
sequences do not yet have equally reliable mapping anchors.

`CustomUnitRuntimeSnapshot.capture(unit)` promotes the high-confidence v0.84 construction/runtime
mapping into the same stable style. It exposes active/revert metadata build-queue-effect gates,
`whenBuilding_cannotMove` runtime state, first CREATED/COMPLETE_AND_ACTIVE pending state,
auto-trigger cooldown, and the previous leg-animation base transform. Android's strict mapping does
not yet identify the leg-base X/Y fields, so `hasLastLegBasePosition()` is false and those two values
are `Float.NaN` there; height and direction remain available. These are snapshots, not live mutable
wrappers, so values remain consistent for the duration of a callback.

## API layers

- `api.event`: public experimental event surface for mods.
- `rusted-fabric-api`: all cross-platform public contracts, events, helpers, sessions, and multiplayer
  APIs, with no Fabric, Xposed, Android framework, Slick, or game implementation class dependency.
- `rusted-fabric-api-desktop`: public mapped Windows API, internal Windows hooks, and remapping
  support.
- `rusted-fabric-api-android`: internal Android mapping and network transport support.
- `api.asset`, `api.ini`, and `api.logic`: higher-level experimental helpers backed by current mappings.
- `api.diagnostic`: development diagnostics. Output and reflected member coverage are not a stable compatibility contract. Mapping v1.1 includes `PlatformRuntimeDiagnostics` for operating-system, platform-extension, and file-change-engine state.
- `api.util.RustedReflection`: low-level compatibility support; prefer higher-level APIs when one exists.
- `mixin`: internal implementation. Mods must not reference these classes.

## Mixin failure policy

The API uses only named Mixin sources. Official-runtime targets and selectors are generated by `remapJarToOfficial`. The configuration is required and every injection currently has `require = 1`, so a target drift fails during startup instead of silently disabling an API event.

Run `gradlew.bat check` before installing a build. It validates the Mixin source/config inventory and runs dependency-free API contract checks.
