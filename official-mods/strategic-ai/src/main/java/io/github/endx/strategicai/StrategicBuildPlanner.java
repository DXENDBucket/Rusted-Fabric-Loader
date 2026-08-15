package io.github.endx.strategicai;

import io.github.endx.rustedfabricapi.api.ai.AiMovementDomain;
import io.github.endx.rustedfabricapi.api.ai.AiResourceControl;
import io.github.endx.rustedfabricapi.api.ai.AiStrategicMapSnapshot;
import io.github.endx.rustedfabricapi.api.ai.AiStrategicResource;
import io.github.endx.rustedfabricapi.api.ai.AiTerrainCell;
import io.github.endx.rustedfabricapi.api.ai.AiTickContext;
import io.github.endx.rustedfabricapi.api.ai.AiUnitCapabilities;
import io.github.endx.rustedfabricapi.api.ai.AiUnitTypeCapabilities;
import io.github.endx.rustedfabricapi.api.game.UnitView;
import io.github.endx.rustedfabricapi.api.unit.action.UnitActions;
import io.github.endx.rustedfabricapi.api.unit.type.UnitTypes;
import rustedwarfare.unit.OrderableUnit;
import rustedwarfare.unit.Unit;
import rustedwarfare.unit.UnitType;
import rustedwarfare.unit.action.UnitAction;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Handles resource expansion, first production infrastructure, and factory queues. */
final class StrategicBuildPlanner {
    private static final long PLACEMENT_RESERVATION_CYCLES = 6L;
    private final Map<Long, Long> resourceReservations = new HashMap<Long, Long>();
    private long combatProducerReservationUntil;
    private boolean announcedEconomy;
    private int announcedOrders;

    void update(AiTickContext context, AiStrategicMapSnapshot situation, long cycle) {
        if (!announcedEconomy) {
            announcedEconomy = true;
            announceEconomy(context, situation);
        }
        resourceReservations.entrySet().removeIf(entry -> entry.getValue() <= cycle);
        List<Builder> builders = builders(situation);
        if (!builders.isEmpty()) {
            if (!claimResource(context, situation, builders, cycle)) {
                ensureCombatProduction(context, situation, builders, cycle);
            }
        }
        queueUnits(context, situation, cycle);
    }

    private boolean claimResource(AiTickContext context,
            AiStrategicMapSnapshot situation, List<Builder> builders, long cycle) {
        ResourceClaim best = null;
        for (AiStrategicResource resource : situation.resources()) {
            if (resource.control() != AiResourceControl.UNCLAIMED) continue;
            long siteKey = resourceKey(resource);
            if (resourceReservations.containsKey(siteKey)) continue;
            for (Builder builder : builders) {
                if (!resource.reachable(builder.capabilities.movementDomain())) continue;
                UnitAction action = resourceExtractorAction(builder.unit);
                if (action == null) continue;
                float dx = builder.capabilities.unit().x() - resource.site().center().x();
                float dy = builder.capabilities.unit().y() - resource.site().center().y();
                float distanceSquared = dx * dx + dy * dy;
                if (best == null || distanceSquared < best.distanceSquared
                        || distanceSquared == best.distanceSquared
                        && siteKey < best.siteKey) {
                    best = new ResourceClaim(resource, builder, action,
                            siteKey, distanceSquared);
                }
            }
        }
        if (best == null) return false;
        context.orders().build(Collections.singletonList(
                        best.builder.capabilities.unit()),
                best.resource.site().center().x(), best.resource.site().center().y(), best.action);
        resourceReservations.put(best.siteKey, cycle + PLACEMENT_RESERVATION_CYCLES);
        return true;
    }

    private void ensureCombatProduction(AiTickContext context,
            AiStrategicMapSnapshot situation, List<Builder> builders, long cycle) {
        if (hasCombatProducer(situation.world().own())) {
            combatProducerReservationUntil = 0L;
            return;
        }
        if (combatProducerReservationUntil > cycle) return;
        for (Builder builder : builders) {
            List<UnitAction> actions = availableBuildActions(builder.unit, true, false);
            actions.removeIf(action -> !declaresCombatProduction(action.getBuildUnitType()));
            actions.sort(BUILD_ACTION_ORDER);
            for (UnitAction action : actions) {
                UnitType type = action.getBuildUnitType();
                BuildPoint point = findBuildPoint(context, situation, builder, type);
                if (point == null) continue;
                context.orders().build(Collections.singletonList(
                        builder.capabilities.unit()), point.x, point.y, action);
                combatProducerReservationUntil = cycle + PLACEMENT_RESERVATION_CYCLES;
                return;
            }
        }
    }

    private void queueUnits(AiTickContext context,
            AiStrategicMapSnapshot situation, long cycle) {
        if (context.team().maxUnitCount() > 0
                && context.team().totalUnitCountIncludingQueued()
                >= context.team().maxUnitCount()) return;
        int builderCount = 0;
        boolean builderAlreadyQueued = false;
        for (UnitView unit : situation.world().own()) {
            if (!unit.alive()) continue;
            if (AiUnitCapabilities.capture(unit).builder()) builderCount++;
            if (!unit.building() || !(unit.raw() instanceof Unit)) continue;
            Unit raw = (Unit) unit.raw();
            for (UnitAction action : UnitActions.forUnit(raw)) {
                UnitType type = action.getBuildUnitType();
                if (action.isBuildAction() && type != null
                        && AiUnitTypeCapabilities.capture(type).builder()
                        && action.getDisplayQueueCount(raw, true) > 0) {
                    builderAlreadyQueued = true;
                }
            }
        }
        boolean builderRequestSatisfied = builderCount >= 2 || builderAlreadyQueued;
        for (UnitView view : situation.world().own()) {
            if (!view.alive()) continue;
            if (!view.building() || !(view.raw() instanceof OrderableUnit)) continue;
            Unit raw = (Unit) view.raw();
            List<UnitAction> candidates = availableBuildActions(raw, false, false);
            candidates.removeIf(action -> action.getDisplayQueueCount(raw, true) > 0);
            List<UnitAction> preferred = new ArrayList<UnitAction>();
            if (!builderRequestSatisfied) {
                for (UnitAction action : candidates) {
                    if (AiUnitTypeCapabilities.capture(action.getBuildUnitType()).builder()) {
                        preferred.add(action);
                    }
                }
            }
            boolean queuingBuilder = !preferred.isEmpty();
            if (preferred.isEmpty()) {
                for (UnitAction action : candidates) {
                    if (AiUnitTypeCapabilities.capture(
                            action.getBuildUnitType()).mobileCombatUnit()) {
                        preferred.add(action);
                    }
                }
            }
            if (preferred.isEmpty()) {
                for (UnitAction action : candidates) {
                    if (!AiUnitTypeCapabilities.capture(action.getBuildUnitType()).builder()) {
                        preferred.add(action);
                    }
                }
            }
            if (preferred.isEmpty()) continue;
            preferred.sort(BUILD_ACTION_ORDER);
            int selected = Math.floorMod((int) (cycle + view.id()), preferred.size());
            UnitAction action = preferred.get(selected);
            UnitActions.issue(context.rawTeam(),
                    Collections.singletonList((OrderableUnit) raw), action);
            if (announcedOrders++ < 4) {
                System.out.println("[Strategic AI] Team " + context.team().id()
                        + " queued " + safe(action.getActionIdString())
                        + " -> " + safe(action.getBuildUnitType().getInternalName())
                        + " at " + safe(raw.r().getInternalName()));
            }
            if (queuingBuilder) builderRequestSatisfied = true;
        }
    }

    private static void announceEconomy(AiTickContext context,
            AiStrategicMapSnapshot situation) {
        System.out.println("[Strategic AI] Team " + context.team().id()
                + " economy: credits=" + (long) context.team().credits()
                + ", units=" + situation.world().own().size());
        for (UnitView view : situation.world().own()) {
            if (!(view.raw() instanceof Unit)) continue;
            Unit raw = (Unit) view.raw();
            List<UnitAction> all = UnitActions.forUnit(raw);
            int production = 0;
            int runnable = 0;
            for (UnitAction action : all) {
                if (!action.isBuildAction() || action.getBuildUnitType() == null
                        || action.getBuildUnitType().isBuilding()) continue;
                production++;
                if (UnitActions.canRun(raw, action)) runnable++;
            }
            System.out.println("[Strategic AI] Team " + context.team().id()
                    + " unit " + safe(raw.r() != null ? raw.r().getInternalName() : null)
                    + ": actions=" + all.size() + ", production=" + production
                    + ", runnableProduction=" + runnable
                    + ", orderable=" + (raw instanceof OrderableUnit));
        }
    }

    private static long resourceKey(AiStrategicResource resource) {
        return ((long) resource.site().tileX() << 32)
                ^ (resource.site().tileY() & 0xffffffffL);
    }

    private static List<Builder> builders(AiStrategicMapSnapshot situation) {
        ArrayList<Builder> result = new ArrayList<Builder>();
        for (UnitView unit : situation.world().own()) {
            if (!unit.alive()) continue;
            AiUnitCapabilities capabilities = AiUnitCapabilities.capture(unit);
            if (capabilities.builder() && capabilities.movable()
                    && capabilities.orderable() && capabilities.idle()) {
                result.add(new Builder((Unit) unit.raw(), capabilities));
            }
        }
        result.sort(Comparator.comparingLong(value -> value.capabilities.unit().id()));
        return result;
    }

    private static UnitAction resourceExtractorAction(Unit builder) {
        List<UnitAction> actions = availableBuildActions(builder, true, true);
        return actions.isEmpty() ? null : actions.get(0);
    }

    private static List<UnitAction> availableBuildActions(Unit unit,
            boolean buildings, boolean resourceOnly) {
        ArrayList<UnitAction> result = new ArrayList<UnitAction>();
        for (UnitAction action : UnitActions.available(unit)) {
            UnitType type = action.getBuildUnitType();
            if (type == null || type.isBuilding() != buildings) continue;
            if (buildings ? !UnitActions.isBuildingPlacement(action)
                    : !action.isBuildAction()) continue;
            if (buildings && type.isPlaceOnlyOnResourcePool() != resourceOnly) continue;
            result.add(action);
        }
        result.sort(BUILD_ACTION_ORDER);
        return result;
    }

    private static boolean hasCombatProducer(List<UnitView> units) {
        for (UnitView unit : units) {
            if (!unit.alive()) continue;
            if (unit.building() && unit.raw() instanceof Unit
                    && offersMobileCombat((Unit) unit.raw())) return true;
        }
        return false;
    }

    private static boolean offersMobileCombat(Unit producer) {
        for (UnitAction action : UnitActions.forUnit(producer)) {
            UnitType type = action.getBuildUnitType();
            if (action.isBuildAction() && type != null && !type.isBuilding()
                    && AiUnitTypeCapabilities.capture(type).mobileCombatUnit()) return true;
        }
        return false;
    }

    private static boolean declaresCombatProduction(UnitType building) {
        if (building == null || !building.isBuilding()) return false;
        for (UnitAction action : UnitActions.forType(building,
                Math.max(1, building.getTechLevel()))) {
            UnitType product = action.getBuildUnitType();
            if (action.isBuildAction() && product != null && !product.isBuilding()
                    && AiUnitTypeCapabilities.capture(product).mobileCombatUnit()) return true;
        }
        return false;
    }

    private static BuildPoint findBuildPoint(AiTickContext context,
            AiStrategicMapSnapshot situation, Builder builder, UnitType type) {
        UnitView view = builder.capabilities.unit();
        int tileWidth = situation.terrain().tileWidth();
        int tileHeight = situation.terrain().tileHeight();
        AiTerrainCell origin = situation.terrain().cellAtWorld(view.x(), view.y());
        for (int radius = 4; radius <= 14; radius += 2) {
            for (int dy = -radius; dy <= radius; dy++) {
                for (int dx = -radius; dx <= radius; dx++) {
                    if (Math.abs(dx) != radius && Math.abs(dy) != radius) continue;
                    float x = view.x() + dx * tileWidth;
                    float y = view.y() + dy * tileHeight;
                    AiTerrainCell target = situation.terrain().cellAtWorld(x, y);
                    AiMovementDomain domain = builder.capabilities.movementDomain();
                    if (!situation.terrain().sameRegion(origin, target, domain)) continue;
                    if (UnitTypes.canSpawnStarting(type, x, y, 0.0F, 0.0F,
                            context.rawTeam())) return new BuildPoint(x, y);
                }
            }
        }
        return null;
    }

    private static final Comparator<UnitAction> BUILD_ACTION_ORDER = Comparator
            .comparingInt(UnitAction::getCreditCost)
            .thenComparing(action -> {
                UnitType type = action.getBuildUnitType();
                return type != null ? safe(type.getInternalName()) : "";
            })
            .thenComparing(action -> safe(action.getActionIdString()));

    private static String safe(String value) {
        return value != null ? value : "";
    }

    private static final class Builder {
        final Unit unit;
        final AiUnitCapabilities capabilities;

        Builder(Unit unit, AiUnitCapabilities capabilities) {
            this.unit = unit;
            this.capabilities = capabilities;
        }
    }

    private static final class BuildPoint {
        final float x;
        final float y;

        BuildPoint(float x, float y) {
            this.x = x;
            this.y = y;
        }
    }

    private static final class ResourceClaim {
        final AiStrategicResource resource;
        final Builder builder;
        final UnitAction action;
        final long siteKey;
        final float distanceSquared;

        ResourceClaim(AiStrategicResource resource, Builder builder, UnitAction action,
                long siteKey, float distanceSquared) {
            this.resource = resource;
            this.builder = builder;
            this.action = action;
            this.siteKey = siteKey;
            this.distanceSquared = distanceSquared;
        }
    }
}
