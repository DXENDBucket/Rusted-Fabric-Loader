# Strategic AI

Experimental whole-team AI replacement built on Rusted Fabric API. Installing the mod makes it
claim every otherwise-unassigned native AI team. It currently provides a deterministic first-pass
loop for resource expansion, initial production infrastructure, unit production, terrain-aware
rallying, and attack movement.

The controller is intentionally omniscient, matching the original game AI. It treats land, hover,
water, over-cliff, over-cliff-water, and air reachability separately, so mountains split land plans
without incorrectly blocking units that can cross them.

This is synchronized simulation behavior and is declared `required` in multiplayer. Set the JVM
property `-Drusted.fabric.strategicAi.enabled=false` to keep the mod installed without claiming AI
teams during development.
