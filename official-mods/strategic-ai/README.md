# Strategic AI

Experimental whole-team AI replacement built on Rusted Fabric API. Installing the mod makes it
claim every otherwise-unassigned native AI team. It currently provides a deterministic first-pass
loop for resource expansion, initial production infrastructure, unit production, terrain-aware
rallying, and attack movement.

Building placement uses the game's synchronized native build-order command. Stock placement
actions are deliberately not identified through `UnitAction.isBuildAction()`, because that flag
means queued unit production and is false for mines, factories, and other placed buildings. The
economy loop reserves pending sites briefly, takes the nearest reachable free resource, avoids
duplicating a factory while its cached world snapshot catches up, and keeps combat factories
running while one suitable producer fills a builder shortage.

Against an outranged armed building, eligible units use ordinary movement to enter a native
target-specific one-way-fire band and then rely on automatic fire. Shorter-ranged units keep a
direct attack order and act as the screen instead of forcing every unit through the same command.

Menu-background AI teams are ignored. Real-match strategic snapshots are cached for three
simulation seconds and their refresh phases are staggered by team, while lightweight force
positioning remains responsive at a half-second cadence.

The controller is intentionally omniscient, matching the original game AI. It treats land, hover,
water, over-cliff, over-cliff-water, and air reachability separately, so mountains split land plans
without incorrectly blocking units that can cross them.

The live situation layer refreshes once per simulation second. Team roles, the active land front,
and resource ownership are reconsidered on a slower hysteretic cycle, or immediately after the
assigned frontline loses its base, so short-lived influence changes do not make allied roles
oscillate. Established battles can pull the plan toward an active reachable land front instead of
leaving every army tied to the opening corridor forever.

Combat control also has a per-unit reaction layer. Critically damaged units under current fire
withdraw independently, recently attacked units answer a target they can actually engage, and any
unit with a target-specific range advantage uses ordinary movement to hold a one-way-fire band
against mobile units as well as buildings. Retreat and reaction leases are deliberately short so
the strategic group controller can absorb the unit again instead of permanently detaching it.

Land forces now assemble on a route-aligned rally line before committing, while units already
fighting beyond that line remain part of the vanguard instead of being recalled. Reinforcements
join in batches, and each movement domain keeps a short shared target lease that favours reachable,
dangerous, unfinished or weakened forward defenses. This avoids both one-at-a-time feeding and
the old tendency to retarget a different tower on every planning pulse.

An independent live combat layer runs roughly five times per simulation second. It spatially
indexes the current battlefield, selects an immediately relevant target for each engaged unit,
and can override a stale group objective. Equal- or longer-ranged units work the edge of their
target-specific firing range; outranged units rush only when local combat strength and a real
speed advantage make closing viable, otherwise they disengage from enemy reach. The index is
rebuilt linearly per pass so this does not degrade into an all-units-by-all-enemies scan.

Team production now enforces its assigned domain: the frontline and economy/tech positions retain
land production while mobile-support positions own the air plan. Dual-purpose gunships no longer
count as air-superiority aircraft merely because they can shoot upward. Under parity or air
disadvantage an upgraded air factory selects the best equal-credit air-superiority investment and
banks for it, rather than spending the upgrade budget on another light interceptor. Health,
shield, target-specific air DPS, range, pursuit speed, and price all enter this comparison, so a
slow paper-stat winner no longer displaces a faster interceptor that wins the actual squad fight.
Land production still commits to coherent batches, but an exact unit type above roughly 45% of a
developed ground force receives a growing planning penalty and triggers an early replan. This
prevents one efficient T1 chassis from permanently monopolising every land factory without forcing
random one-of-each production. Absolute combat quality and cost efficiency use diminishing-return
curves, leaving range, tech, current front mode, and force composition enough weight to choose the
right batch instead of always selecting the cheapest efficient chassis.
Factory-slot throughput is scored separately as combat power multiplied by native queue build
speed. A higher-tier unit can therefore be preferred when it converts one factory's time into
combat power faster, even if its credit efficiency is only equal or slightly worse.
Additional factories are budgeted from the selected unit's full-queue credit burn per second.
Current income funds sustained capacity; only credits above a role-specific reserve fund a bounded
30--55 second burst. Frontline positions can expand capacity more aggressively during assault or
attrition, while economy/tech positions preserve a longer reserve horizon. A large temporary bank
therefore no longer causes factories that the current economy cannot meaningfully feed.

A lost forward tower race is a persistent operation instead of a generic retreat. Up to four
frontline builders leave the predicted enemy-tower range together, regroup on the home side, build
one safe replacement together, and keep repairing it. The attrition production plan concurrently
favours ground units able to fire beyond the primary enemy defense, allowing the repaired tower to
screen artillery pressure.

This is synchronized simulation behavior and is declared `required` in multiplayer. Set the JVM
property `-Drusted.fabric.strategicAi.enabled=false` to keep the mod installed without claiming AI
teams during development.
