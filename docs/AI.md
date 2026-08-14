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

```java
AiControllers.Handle handle = AiControllers.assign(
        aiTeam,
        Identifier.of("example_ai", "main"),
        context -> {
            AiWorldSnapshot world = context.world();
            // Strategic/operational/tactical planners run at their own deterministic cadences.
            // Issue grouped orders through context.orders().
            return AiTickDecision.REPLACE_NATIVE;
        });
```

Controllers execute on the simulation thread. Multiplayer peers must run the same controller code
and make identical decisions. Do not use wall-clock time, unordered collection iteration, local UI
state, or random sources that are not synchronized by the game.

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

- Unit capability/role resolvers for built-in and Java/INI units.
- Threat and influence grids computed from complete world state.
- Task-force ownership, staging, retreat, reinforcement, and target reservations.
- Economy/build planning with extractor, factory, defense, and tech budgets.
- Air-control assessment and counter-composition planning.
- Deterministic strategic, operational, and tactical cadence helpers.
- Debug snapshots suitable for a profiler-style in-game AI page.

Sources used for the initial doctrine include the community [Multiplayer Basic Guide](https://indiefaq.com/guides/659-rusted-warfare-rts.html), the [Artillery unit notes](https://rustedwarfare.fandom.com/wiki/Artillery), the Steam [Basic Defense Guide](https://steamcommunity.com/sharedfiles/filedetails/?id=1449760671), and the Chinese community's [technical strategy compilation](https://www.taptap.cn/moment/15221798324931855).
