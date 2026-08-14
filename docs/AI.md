# Java AI development

Rusted Fabric's AI API deliberately exposes the complete live simulation. It does not emulate
fog-of-war knowledge: an AI controller can inspect all active enemy units just as the native AI
does. Difficulty should come from doctrine, planning cadence, reaction delay, and ordinary economy
rules rather than maintaining a second visibility system.

## Initial API boundary

`AiControllers.assign(team, owner, controller)` claims one exact `AiTeam`. Unassigned teams keep
the original game AI. A controller may return `PASS` while it is only observing or augmenting the
native implementation, then return `REPLACE_NATIVE` once it owns the complete decision loop.
Assignments are exclusive so two mods cannot silently issue competing plans for the same team.

`AiTickContext.world()` is a lazily captured, stable-ID-sorted membership snapshot with `own`,
`allies`, `enemies`, and `neutral` groups. Its `UnitView` objects remain live. `context.orders()`
uses the normal synchronized command path and rejects units not owned by that AI team.

`AiTickContext.strategicMap()` adds the first situation-assessment layer. The static terrain half is
cached for the current map; the dynamic team, influence, front, and resource summaries are captured
once per tick context. Controllers normally consume this at their slower strategic cadence rather
than on every native AI update.

```java
AiControllers.Handle handle = AiControllers.assign(
        aiTeam,
        Identifier.of("example_ai", "main"),
        context -> {
            AiWorldSnapshot world = context.world();
            AiStrategicMapSnapshot situation = context.strategicMap();
            // Strategic/operational/tactical planners run at their own deterministic cadences.
            // Issue grouped orders through context.orders().
            return AiTickDecision.REPLACE_NATIVE;
        });
```

Controllers execute on the simulation thread. Multiplayer peers must run the same controller code
and make identical decisions. Do not use wall-clock time, unordered collection iteration, local UI
state, or random sources that are not synchronized by the game.

## Strategic map and player distribution

`AiTerrainMapSnapshot` divides the map into deterministic 12-by-12-tile strategic cells while
retaining the exact coordinates of resource pools. Each `AiTerrainCell` reports water, lava,
explicit cliff/mountain, large cliff-or-tree blocker, and building-blocked fractions. It also uses
the game's native path-cost maps to expose passable fractions and connected regions for land,
hover, water, over-cliff, over-cliff-water, and air movement. `landChokeScore()` marks constrained
land corridors as a planning hint.

Mountains therefore affect decisions structurally rather than cosmetically. Land influence cannot
cross disconnected mountain regions, a mine across such a region is not reported as land-reachable,
and a land front is only formed where the opposing land regions can actually meet. Hover,
over-cliff, naval, and air conclusions remain independent. Resource objectives expose all reachable
movement domains through `reachableDomains()` and `reachable(domain)`.

The dynamic half provides:

- `AiTeamPresence`: each player's unit/building counts, health, movement mix, centroid, building
  centroid, densest-cluster anchor, and spread radius.
- `AiInfluenceCell`: exact local unit counts, coarse friendly/enemy influence, control state,
  frontline score, and the movement domain that formed that front.
- `AiStrategicResource`: current occupant and ownership, nearby pressure, per-domain reachability,
  and a normalized capture, lock-down, deny, defend, or support suggestion.
- `primaryFront()`: the current strongest meeting point, suitable as a first staging hint.

These objective kinds and scores are advisory inputs. They do not issue orders or reserve a mine,
and later economy/task-force planners may override them based on available builders, matchup, and
team strategy. The assessment deliberately uses the complete map and does not simulate fog.

## Experimental Strategic AI mod

The optional official `strategic_ai` mod is the first real consumer of this API. On the first update
of each otherwise-unclaimed native AI team, it assigns a per-team controller and returns
`REPLACE_NATIVE`. Its current deterministic loop can order an idle builder onto an unclaimed
resource pool, construct an available low-cost combat factory when the team has none, keep short
factory queues supplied, group idle non-builder combat units by movement domain, and attack-move
toward lock-down resources, the primary front, or a reachable enemy position.

This is an integration baseline, not the finished tactical AI. It does not yet understand damage
types, target-domain restrictions, tower/artillery counters, retreat thresholds, production
budgets, or allied player specialization. Because it changes synchronized simulation decisions,
its multiplayer mode is `required`. During development it can be left installed but disabled with
`-Drusted.fabric.strategicAi.enabled=false`.

## Tactical model to build next

The first controller should organize units into task forces rather than permanent map lanes. The
shared `AiForceRole` vocabulary starts with frontline, ranged support, artillery, air superiority,
mobile anti-air, raider, engineer, repair, transport, economy, and static-defense roles. A unit can
serve more than one role, and a task force should be rebuilt when enemy composition changes.

The intended counter loop is:

1. Static defenses hold extractors and staging positions.
2. Artillery and other siege units outrange and break concentrated defenses.
3. Mobile or air forces hunt exposed artillery.
4. Interceptors and mobile anti-air protect the frontline and siege group.
5. After a breach, one group attacks economy/production while another screens reinforcements.

For team games, the same roles can be lifted to player-level assignments: the forward player
holds contested extractors and narrow approaches; air and mech specialists keep mobile reserves
instead of duplicating the same front; siege support concentrates against the currently selected
defense; and the closest suitable force answers raids. These are preferences, not exclusive
lanes—an air player still contributes mobile anti-air and a frontline player must preserve a screen
for allied artillery.

This follows recurring community guidance: early air scouting reveals air investment, artillery
needs a screen while attacking towers, and excessive static-defense spending gives up map control.
The API does not hard-code these decisions; it supplies the deterministic control and observation
boundary on which a dedicated AI mod can implement them.

## Planned follow-up layers

- Role and weapon-profile resolvers layered over the available unit/type capability snapshots.
- Capability-aware threat ranges and damage pressure layered over the current presence grid.
- Task-force ownership, staging, retreat, reinforcement, and target reservations.
- Economy/build budgets for defense, technology, unit composition, and reserve credits.
- Air-control assessment and counter-composition planning.
- Deterministic strategic, operational, and tactical cadence helpers.
- Debug snapshots suitable for a profiler-style in-game AI page.

Sources used for the initial doctrine include the community [Multiplayer Basic Guide](https://indiefaq.com/guides/659-rusted-warfare-rts.html), the [Artillery unit notes](https://rustedwarfare.fandom.com/wiki/Artillery), the Steam [Basic Defense Guide](https://steamcommunity.com/sharedfiles/filedetails/?id=1449760671), and the Chinese community's [technical strategy compilation](https://www.taptap.cn/moment/15221798324931855).
