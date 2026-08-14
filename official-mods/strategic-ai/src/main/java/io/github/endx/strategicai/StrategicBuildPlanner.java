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
import java.util.List;

/** Handles resource expansion, first production infrastructure, and factory queues. */
final class StrategicBuildPlanner {
    void update(AiTickContext context, AiStrategicMapSnapshot situation, long cycle) {
        List<Builder> builders = builders(situation);
        if (!builders.isEmpty()) {
            if (!claimResource(context, situation, builders)) {
                ensureCombatProduction(context, situation, builders);
            }
        }
        queueUnits(context, situation, cycle);
    }

    private boolean claimResource(AiTickContext context,
            AiStrategicMapSnapshot situation, List<Builder> builders) {
        for (AiStrategicResource resource : situation.resources()) {
            if (resource.control() != AiResourceControl.UNCLAIMED) continue;
            for (Builder builder : builders) {
                if (!resource.reachable(builder.capabilities.movementDomain())) continue;
                UnitAction action = resourceExtractorAction(builder.unit);
                if (action == null) continue;
                contextUnitAction(context, builder.unit, action,
                        resource.site().center().x(), resource.site().center().y());
                return true;
            }
        }
        return false;
    }

    private void ensureCombatProduction(AiTickContext context,
            AiStrategicMapSnapshot situation, List<Builder> builders) {
        if (hasCombatProducer(situation.world().own())) return;
        for (Builder builder : builders) {
            List<UnitAction> actions = availableBuildActions(builder.unit, true, false);
            actions.removeIf(action -> !declaresCombatProduction(action.getBuildUnitType()));
            actions.sort(BUILD_ACTION_ORDER);
            for (UnitAction action : actions) {
                UnitType type = action.getBuildUnitType();
                BuildPoint point = findBuildPoint(context, situation, builder, type);
                if (point == null) continue;
                contextUnitAction(context, builder.unit, action, point.x, point.y);
                return;
            }
        }
    }

    private void queueUnits(AiTickContext context,
            AiStrategicMapSnapshot situation, long cycle) {
        int builderCount = 0;
        for (UnitView unit : situation.world().own()) {
            if (AiUnitCapabilities.capture(unit).builder()) builderCount++;
        }
        boolean needBuilder = builderCount < 2;
        for (UnitView view : situation.world().own()) {
            if (!view.building() || !(view.raw() instanceof OrderableUnit)) continue;
            Unit raw = (Unit) view.raw();
            List<UnitAction> candidates = availableBuildActions(raw, false, false);
            candidates.removeIf(action -> action.getDisplayQueueCount(raw, true) > 0);
            List<UnitAction> preferred = new ArrayList<UnitAction>();
            for (UnitAction action : candidates) {
                AiUnitTypeCapabilities type = AiUnitTypeCapabilities.capture(
                        action.getBuildUnitType());
                if (needBuilder ? type.builder() : type.mobileCombatUnit()) preferred.add(action);
            }
            if (preferred.isEmpty() && !needBuilder) {
                for (UnitAction action : candidates) {
                    if (!AiUnitTypeCapabilities.capture(action.getBuildUnitType()).builder()) {
                        preferred.add(action);
                    }
                }
            }
            if (preferred.isEmpty()) continue;
            preferred.sort(BUILD_ACTION_ORDER);
            int selected = Math.floorMod((int) (cycle + view.id()), preferred.size());
            UnitActions.issue(context.rawTeam(),
                    Collections.singletonList((OrderableUnit) raw), preferred.get(selected));
        }
    }

    private static List<Builder> builders(AiStrategicMapSnapshot situation) {
        ArrayList<Builder> result = new ArrayList<Builder>();
        for (UnitView unit : situation.world().own()) {
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
            if (!action.isBuildAction()) continue;
            UnitType type = action.getBuildUnitType();
            if (type == null || type.isBuilding() != buildings) continue;
            if (buildings && type.isPlaceOnlyOnResourcePool() != resourceOnly) continue;
            result.add(action);
        }
        result.sort(BUILD_ACTION_ORDER);
        return result;
    }

    private static boolean hasCombatProducer(List<UnitView> units) {
        for (UnitView unit : units) {
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

    private static void contextUnitAction(AiTickContext context, Unit unit,
            UnitAction action, float x, float y) {
        UnitActions.issueAt(context.rawTeam(),
                Collections.singletonList((OrderableUnit) unit), action, x, y, null);
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
}
