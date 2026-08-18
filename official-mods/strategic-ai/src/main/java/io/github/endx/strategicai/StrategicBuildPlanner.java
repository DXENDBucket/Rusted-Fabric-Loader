package io.github.endx.strategicai;

import io.github.endx.rustedfabricapi.api.ai.AiMovementDomain;
import io.github.endx.rustedfabricapi.api.ai.AiResourceControl;
import io.github.endx.rustedfabricapi.api.ai.AiStrategicMapSnapshot;
import io.github.endx.rustedfabricapi.api.ai.AiStrategicResource;
import io.github.endx.rustedfabricapi.api.ai.AiTerrainCell;
import io.github.endx.rustedfabricapi.api.ai.AiTerrainRouteMap;
import io.github.endx.rustedfabricapi.api.ai.AiTickContext;
import io.github.endx.rustedfabricapi.api.ai.AiUnitCapabilities;
import io.github.endx.rustedfabricapi.api.ai.AiUnitTypeCapabilities;
import io.github.endx.rustedfabricapi.api.game.UnitView;
import io.github.endx.rustedfabricapi.api.unit.action.UnitActions;
import io.github.endx.rustedfabricapi.api.unit.type.UnitTypes;
import io.github.endx.rustedfabricapi.api.world.GameWorld;
import io.github.endx.rustedfabricapi.api.world.WorldPoint;
import rustedwarfare.unit.OrderableUnit;
import rustedwarfare.unit.Unit;
import rustedwarfare.unit.UnitType;
import rustedwarfare.unit.action.QueueableUnitAction;
import rustedwarfare.unit.action.UnitAction;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

/** Handles resource expansion, first production infrastructure, and factory queues. */
final class StrategicBuildPlanner {
    private static final long RESOURCE_RESERVATION_CYCLES = 30L;
    private static final long BUILDING_RESERVATION_CYCLES = 40L;
    private static final long RECOVERY_BUILD_RESERVATION_CYCLES = 8L;
    private static final long FRONTLINE_BUILDER_RETREAT_CYCLES = 14L;
    private static final float PRIMARY_BASE_RESOURCE_RADIUS = 700.0F;
    private static final int FRONTLINE_BUILDER_TARGET = 3;
    private final Map<Long, Long> resourceReservations = new HashMap<Long, Long>();
    private final Map<Long, Long> buildingReservations = new HashMap<Long, Long>();
    private final java.util.HashSet<Long> frontlineBuilderIds =
            new java.util.HashSet<Long>();
    private final Map<Long, Long> frontlineBuilderRetreatUntil =
            new HashMap<Long, Long>();
    private final AirSuperiorityGate airSuperiorityGate = new AirSuperiorityGate();
    private final EnumMap<BaseLayoutGeometry.District, Long> primaryDistrictReservations =
            new EnumMap<BaseLayoutGeometry.District, Long>(BaseLayoutGeometry.District.class);
    private BaseLayoutPlan primaryBase;
    private long frontierDefenseReservationUntil;
    private long frontierDefenseTargetKey = Long.MIN_VALUE;
    private boolean forceLockFortification;
    private long abandonedForwardTowerId = Long.MIN_VALUE;
    private boolean contestRecoveryActive;
    private long contestRecoveryTargetKey = Long.MIN_VALUE;
    private long contestRecoveryEnemyTowerId = Long.MIN_VALUE;
    private WorldPoint contestRecoveryPoint;
    private UnitType contestRecoveryTowerType;
    private long contestRecoveryBuildUntil;
    private String focusFactoryId;
    private UnitType focusUnitType;
    private long productionPlanUntil;
    private StrategicFrontState.Mode productionPlanFrontMode;
    private StrategicProductionDoctrine.AirBalance productionPlanAirBalance;
    private int productionPlanFocusedCount;
    private boolean announcedEconomy;
    private int announcedOrders;
    private String announcedCapacityPlan;
    private long economicInvestmentReservationUntil;

    void onStrategicReplan() {
        primaryBase = null;
        focusFactoryId = null;
        focusUnitType = null;
        productionPlanUntil = 0L;
        productionPlanAirBalance = null;
        productionPlanFrontMode = null;
        productionPlanFocusedCount = 0;
        primaryDistrictReservations.clear();
        frontlineBuilderRetreatUntil.clear();
        frontierDefenseReservationUntil = 0L;
        frontierDefenseTargetKey = Long.MIN_VALUE;
        forceLockFortification = false;
        abandonedForwardTowerId = Long.MIN_VALUE;
        contestRecoveryActive = false;
        contestRecoveryTargetKey = Long.MIN_VALUE;
        contestRecoveryEnemyTowerId = Long.MIN_VALUE;
        contestRecoveryPoint = null;
        contestRecoveryTowerType = null;
        contestRecoveryBuildUntil = 0L;
        economicInvestmentReservationUntil = 0L;
    }

    void update(AiTickContext context, AiStrategicMapSnapshot situation, long cycle,
            StrategicResourceCampaign resourceCampaign, StrategicTeamPlan teamPlan,
            StrategicFrontState frontState) {
        if (!announcedEconomy) {
            announcedEconomy = true;
            announceEconomy(context, situation);
        }
        resourceReservations.entrySet().removeIf(entry -> entry.getValue() <= cycle);
        buildingReservations.entrySet().removeIf(entry -> entry.getValue() <= cycle);
        primaryDistrictReservations.entrySet().removeIf(entry -> entry.getValue() <= cycle);
        frontlineBuilderRetreatUntil.entrySet().removeIf(entry -> entry.getValue() <= cycle);
        List<UnitView> currentOwn = context.world().own();
        retainLiveFrontlineBuilders(currentOwn);
        ensurePrimaryBase(situation, currentOwn, context.world().enemies(), teamPlan);
        boolean leadsFrontline = teamPlan.leadsFrontline();
        if (!leadsFrontline) {
            frontlineBuilderIds.clear();
            frontlineBuilderRetreatUntil.clear();
        }
        List<Builder> allBuilders = builders(currentOwn, false);
        List<Builder> builders = builders(currentOwn, true);
        if (leadsFrontline) {
            assignFrontlineBuilders(allBuilders, resourceCampaign, teamPlan);
            evaluateForwardTowerContest(context, situation, resourceCampaign, currentOwn);
            withdrawThreatenedFrontlineBuilders(context, situation, allBuilders, cycle);
        }
        builders.removeIf(builder -> frontlineBuilderRetreatUntil.containsKey(
                builder.capabilities.unit().id()));
        List<Builder> frontBuilders = selectBuilders(builders, true);
        List<Builder> baseBuilders = selectBuilders(builders, false);
        if (leadsFrontline) {
            beginLostTowerRecovery(situation, resourceCampaign,
                    selectBuilders(allBuilders, true), currentOwn);
        }
        refreshProductionPlan(context, situation, currentOwn, cycle,
                teamPlan, frontState);
        boolean frontHandled = leadsFrontline && handleContestRecovery(context, situation,
                selectBuilders(allBuilders, true), cycle, resourceCampaign, currentOwn);
        if (!frontHandled && leadsFrontline && !frontBuilders.isEmpty()) {
            frontHandled = ensureFrontierMaintenance(context, frontBuilders,
                    resourceCampaign, currentOwn)
                    || ensureFrontierFortification(context, situation, frontBuilders,
                    cycle, resourceCampaign)
                    || claimCampaignResource(context, frontBuilders, cycle, resourceCampaign)
                    || ensureFrontierDefense(context, situation, frontBuilders, cycle,
                    resourceCampaign, currentOwn)
                    || recoverOrStageFrontlineBuilders(context, frontBuilders,
                    situation, resourceCampaign, teamPlan, currentOwn);
        }
        if (!baseBuilders.isEmpty()) {
            int producers = countFocusedCombatProducers(currentOwn, teamPlan.ownRole());
            if (producers == 0) {
                ensureRoleProduction(context, situation, baseBuilders, cycle,
                        teamPlan, resourceCampaign, currentOwn);
            } else if ((leadsFrontline || !claimCampaignResource(
                            context, baseBuilders, cycle, resourceCampaign))
                    && !claimResource(context, situation, baseBuilders, cycle, teamPlan)
                    && !ensureStrategicDefenses(context, situation, baseBuilders, baseBuilders,
                            cycle, teamPlan, frontState, resourceCampaign,
                            currentOwn, producers)
                    && !ensureEconomicInvestment(context, situation, baseBuilders, cycle,
                            teamPlan, currentOwn)) {
                ensureProductionCapacity(context, situation, baseBuilders, cycle,
                        teamPlan, frontState, resourceCampaign, currentOwn, producers);
            }
        }
        queueUnits(context, situation, cycle, teamPlan, frontState, resourceCampaign);
    }

    private boolean claimCampaignResource(AiTickContext context, List<Builder> builders,
            long cycle, StrategicResourceCampaign campaign) {
        if (!campaign.allowsBuild() || campaign.target() == null
                || campaign.target().control() != AiResourceControl.UNCLAIMED) return false;
        AiStrategicResource resource = campaign.target();
        long siteKey = resourceKey(resource);
        if (resourceReservations.containsKey(siteKey)) return true;
        ResourceClaim best = null;
        for (Builder builder : builders) {
            if (!resource.reachable(builder.capabilities.movementDomain())) continue;
            UnitAction action = resourceExtractorAction(builder.unit);
            if (action == null) continue;
            float dx = builder.capabilities.unit().x() - resource.site().center().x();
            float dy = builder.capabilities.unit().y() - resource.site().center().y();
            float distanceSquared = dx * dx + dy * dy;
            if (best == null || distanceSquared < best.distanceSquared
                    || distanceSquared == best.distanceSquared
                    && builder.capabilities.unit().id()
                    < best.builder.capabilities.unit().id()) {
                best = new ResourceClaim(resource, builder, action, siteKey, distanceSquared);
            }
        }
        if (best == null) return false;
        context.orders().build(Collections.singletonList(best.builder.capabilities.unit()),
                resource.site().center().x(), resource.site().center().y(), best.action);
        resourceReservations.put(siteKey, cycle + RESOURCE_RESERVATION_CYCLES);
        campaign.markBuildOrdered();
        System.out.println("[Strategic AI] Team " + context.team().id()
                + " ordered frontier extractor at " + resource.site().tileX()
                + "," + resource.site().tileY());
        return true;
    }

    private boolean claimResource(AiTickContext context,
            AiStrategicMapSnapshot situation, List<Builder> builders, long cycle,
            StrategicTeamPlan teamPlan) {
        ResourceClaim best = null;
        if (primaryBase == null) return false;
        float maximumDistanceSquared = PRIMARY_BASE_RESOURCE_RADIUS
                * PRIMARY_BASE_RESOURCE_RADIUS;
        for (AiStrategicResource resource : situation.resources()) {
            if (resource.control() != AiResourceControl.UNCLAIMED) continue;
            if (!teamPlan.ownsLocalResource(resource)) continue;
            if (primaryBase.anchor().distanceSquared(resource.site().center())
                    > maximumDistanceSquared) continue;
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
        resourceReservations.put(best.siteKey, cycle + RESOURCE_RESERVATION_CYCLES);
        return true;
    }

    private void queueUnits(AiTickContext context,
            AiStrategicMapSnapshot situation, long cycle,
            StrategicTeamPlan teamPlan, StrategicFrontState frontState,
            StrategicResourceCampaign resourceCampaign) {
        if (context.team().maxUnitCount() > 0
                && context.team().totalUnitCountIncludingQueued()
                >= context.team().maxUnitCount()) return;
        int builderCount = 0;
        float builderReadiness = 0.0F;
        boolean builderAlreadyQueued = false;
        Map<String, Integer> queuedCombatByType = new HashMap<String, Integer>();
        for (UnitView unit : context.world().own()) {
            if (!unit.alive()) continue;
            if (AiUnitCapabilities.capture(unit).builder()) {
                builderCount++;
                builderReadiness += Math.max(0.25F, Math.min(1.0F, unit.healthFraction()));
            }
            if (!unit.building() || !(unit.raw() instanceof Unit)) continue;
            Unit raw = (Unit) unit.raw();
            for (UnitAction action : UnitActions.forUnit(raw)) {
                UnitType type = action.getBuildUnitType();
                if (action.isBuildAction() && type != null
                        && AiUnitTypeCapabilities.capture(type).builder()
                        && action.getDisplayQueueCount(raw, true) > 0) {
                    builderAlreadyQueued = true;
                }
                int queued = action.getDisplayQueueCount(raw, true);
                if (action.isBuildAction() && type != null && queued > 0
                        && AiUnitTypeCapabilities.capture(type).mobileCombatUnit()) {
                    queuedCombatByType.merge(safe(type.getInternalName()).toLowerCase(
                            java.util.Locale.ROOT), Integer.valueOf(queued), Integer::sum);
                }
            }
        }
        int desiredBuilders = teamPlan.leadsFrontline()
                && resourceCampaign.active() ? 4 : teamPlan.leadsFrontline() ? 3 : 2;
        boolean builderRequestSatisfied = builderCount >= desiredBuilders
                && builderReadiness >= desiredBuilders - 0.20F
                || builderAlreadyQueued;
        for (UnitView view : context.world().own()) {
            if (!view.alive()) continue;
            if (!view.building() || !(view.raw() instanceof OrderableUnit)) continue;
            Unit raw = (Unit) view.raw();
            if (hasQueuedUpgrade(raw)) continue;
            UnitAction upgrade = offersMobileCombat(raw) ? availableUpgrade(raw) : null;
            boolean focusFactory = isFocusFactory(raw.r());
            boolean needsFocusUpgrade = focusFactory && focusUnitType != null
                    && !offersUnit(raw, focusUnitType)
                    && upgrade != null
                    && context.team().credits() >= Math.max(1, upgrade.getCreditCost()) * 1.10D;
            if (upgrade != null && (needsFocusUpgrade
                    || StrategicProductionDoctrine.shouldUpgrade(
                    upgrade, teamPlan.ownRole(), teamPlan.primaryMobileSupport(),
                    situation, frontState, cycle + view.id()))) {
                UnitActions.issue(context.rawTeam(),
                        Collections.singletonList((OrderableUnit) raw), upgrade);
                if (announcedOrders++ < 8) {
                    System.out.println("[Strategic AI] Team " + context.team().id()
                            + " queued factory upgrade "
                            + safe(upgrade.getActionIdString()) + " at "
                            + safe(raw.r().getInternalName()));
                }
                continue;
            }
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
            UnitAction action;
            if (queuingBuilder) {
                preferred.sort(BUILD_ACTION_ORDER);
                action = preferred.get(0);
            } else {
                UnitAction focused = focusFactory
                        ? actionProducing(preferred, focusUnitType) : null;
                boolean focusUnavailable = focusFactory && focusUnitType != null
                        && !offersUnit(raw, focusUnitType);
                boolean focusAffordable = focused != null
                        && context.team().credits() >= Math.max(
                        focused.getCreditCost(), focusUnitType.getBuildCostCredits());
                if (focused != null && focusAffordable) {
                    action = focused;
                } else if (focusUnavailable || focused != null) {
                    // Preserve credits for the required upgrade or the chosen high-tier unit.
                    action = null;
                } else if (focusFactory) {
                    action = StrategicProductionDoctrine.chooseFocusedFallback(
                            preferred, focusUnitType, context.team().credits());
                } else if (offersMobileCombat(raw)) {
                    // An off-doctrine factory remains idle instead of independently creating
                    // a mixed army. Capacity planning will build the selected factory family.
                    action = null;
                } else {
                    action = StrategicProductionDoctrine.choose(preferred,
                            teamPlan.ownRole(), situation, frontState,
                            context.world().own(), queuedCombatByType,
                            cycle + view.id());
                }
            }
            if (action == null) continue;
            UnitActions.issue(context.rawTeam(),
                    Collections.singletonList((OrderableUnit) raw), action);
            if (announcedOrders++ < 4) {
                System.out.println("[Strategic AI] Team " + context.team().id()
                        + " queued " + safe(action.getActionIdString())
                        + " -> " + safe(action.getBuildUnitType().getInternalName())
                        + " at " + safe(raw.r().getInternalName()));
            }
            if (queuingBuilder) builderRequestSatisfied = true;
            else queuedCombatByType.merge(safe(action.getBuildUnitType().getInternalName())
                            .toLowerCase(java.util.Locale.ROOT), Integer.valueOf(1),
                    Integer::sum);
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

    private void ensurePrimaryBase(AiStrategicMapSnapshot situation,
            List<UnitView> own, List<UnitView> enemies, StrategicTeamPlan teamPlan) {
        if (primaryBase != null && primaryBase.anchorAlive(own)) return;
        UnitView anchor = null;
        for (UnitView unit : own) {
            if (!unit.alive() || !unit.building()) continue;
            if (anchor == null || unit.maxHealth() > anchor.maxHealth()
                    || unit.maxHealth() == anchor.maxHealth() && unit.id() < anchor.id()) {
                anchor = unit;
            }
        }
        if (anchor == null) {
            primaryBase = null;
            return;
        }
        WorldPoint anchorPoint = new WorldPoint(anchor.x(), anchor.y());
        WorldPoint front = teamPlan.preferredFrontierPoint();
        if (front == null) front = situation.primaryFront().orElseGet(() -> enemyCenter(
                enemies, situation, anchorPoint));
        primaryBase = new BaseLayoutPlan(anchor.id(), anchorPoint, front);
        System.out.println("[Strategic AI] Base layout anchored at unit " + anchor.id()
                + " facing " + (int) front.x() + "," + (int) front.y());
    }

    private void refreshProductionPlan(AiTickContext context,
            AiStrategicMapSnapshot situation, List<UnitView> own, long cycle,
            StrategicTeamPlan teamPlan, StrategicFrontState frontState) {
        StrategicFrontState.Mode mode = frontState != null ? frontState.mode() : null;
        StrategicProductionDoctrine.AirBalance currentAirBalance = airSuperiorityGate.update(
                StrategicProductionDoctrine.assessAirBalance(situation));
        int currentFocusCount = focusUnitType != null
                ? countLiveType(own, safe(focusUnitType.getInternalName())) : 0;
        boolean focusConcentrationIncreased = focusUnitType != null
                && focusTypeSaturated(own, focusUnitType)
                && currentFocusCount > productionPlanFocusedCount;
        if (focusFactoryId != null && focusUnitType != null
                && cycle < productionPlanUntil && mode == productionPlanFrontMode
                && currentAirBalance == productionPlanAirBalance
                && !focusConcentrationIncreased) return;
        java.util.LinkedHashMap<String, FactoryPlanCandidate> factories =
                new java.util.LinkedHashMap<String, FactoryPlanCandidate>();
        ArrayList<FactoryPortfolioPolicy.Profile> existingPortfolio =
                new ArrayList<FactoryPortfolioPolicy.Profile>();
        for (UnitView view : own) {
            if (!view.alive() || !(view.raw() instanceof Unit)) continue;
            Unit raw = (Unit) view.raw();
            AiUnitCapabilities live = AiUnitCapabilities.capture(view);
            if (live.builder() && live.movable()) {
                for (UnitAction action : UnitActions.forUnit(raw)) {
                    UnitType factory = action.getBuildUnitType();
                    if (factory == null || !factory.isBuilding()
                            || !UnitActions.isBuildingPlacement(action)
                            || factory.isPlaceOnlyOnResourcePool()
                            || !declaresCombatProduction(factory)) continue;
                    addFactoryCandidate(factories, factory,
                            Math.max(action.getCreditCost(), factory.getBuildCostCredits()), false);
                }
            }
            if (view.building() && raw.r() != null && declaresCombatProduction(raw.r())) {
                addFactoryCandidate(factories, raw.r(), raw.r().getBuildCostCredits(), true);
                existingPortfolio.addAll(factoryProfiles(raw.r(), teamPlan.ownRole()));
            }
        }
        FactoryPlanCandidate best = null;
        for (FactoryPlanCandidate factory : factories.values()) {
            selectFactoryProduct(factory, teamPlan.ownRole(), situation,
                    frontState, context.team().credits(), own);
            if (factory.unit == null) continue;
            if (!factory.existing) {
                factory.portfolioNovelty = FactoryPortfolioPolicy.novelty(
                        factoryProfiles(factory.factory, teamPlan.ownRole()),
                        existingPortfolio);
                factory.score += factory.portfolioNovelty * 7.5D;
            }
            System.out.println("[Strategic AI] Team " + context.team().id()
                    + " factory candidate=" + safe(factory.factory.getInternalName())
                    + " main=" + safe(factory.unit.getInternalName())
                    + " existing=" + factory.existing
                    + " unitScore=" + String.format(java.util.Locale.ROOT,
                    "%.2f", factory.unitScore)
                    + " portfolioNovelty=" + String.format(java.util.Locale.ROOT,
                    "%.2f", factory.portfolioNovelty)
                    + " total=" + String.format(java.util.Locale.ROOT,
                    "%.2f", factory.score));
            if (best == null || factory.score > best.score
                    || factory.score == best.score
                    && safe(factory.factory.getInternalName()).compareToIgnoreCase(
                    safe(best.factory.getInternalName())) < 0) best = factory;
        }
        if (best == null) return;
        focusFactoryId = safe(best.factory.getInternalName());
        focusUnitType = best.unit;
        productionPlanUntil = cycle + 48L;
        productionPlanFrontMode = mode;
        productionPlanAirBalance = currentAirBalance;
        productionPlanFocusedCount = countLiveType(
                own, safe(focusUnitType.getInternalName()));
        System.out.println("[Strategic AI] Team " + context.team().id()
                + " production plan factory=" + focusFactoryId
                + " main=" + safe(focusUnitType.getInternalName())
                + " score=" + String.format(java.util.Locale.ROOT,
                "%.2f", best.score)
                + " portfolioNovelty=" + String.format(java.util.Locale.ROOT,
                "%.2f", best.portfolioNovelty)
                + " until=" + productionPlanUntil);
    }

    private static void addFactoryCandidate(
            Map<String, FactoryPlanCandidate> candidates,
            UnitType factory, int cost, boolean existing) {
        String id = safe(factory.getInternalName()).toLowerCase(java.util.Locale.ROOT);
        FactoryPlanCandidate value = candidates.get(id);
        if (value == null) {
            candidates.put(id, new FactoryPlanCandidate(factory, cost, existing));
        } else if (existing) {
            value.existing = true;
        }
    }

    private static List<FactoryPortfolioPolicy.Profile> factoryProfiles(
            UnitType factory, TeamPositionDoctrine.Role role) {
        ArrayList<FactoryPortfolioPolicy.Profile> result =
                new ArrayList<FactoryPortfolioPolicy.Profile>();
        java.util.HashSet<String> seen = new java.util.HashSet<String>();
        for (UnitAction action : supportedTechActions(factory, 4)) {
            UnitType product = action.getBuildUnitType();
            if (!action.isBuildAction() || product == null || product.isBuilding()) continue;
            AiUnitTypeCapabilities capabilities = AiUnitTypeCapabilities.capture(product);
            if (!capabilities.mobileCombatUnit()
                    || StrategicProductionDoctrine.isReconType(capabilities)
                    || !roleAcceptsProduct(role, capabilities)
                    || !seen.add(safe(capabilities.typeId()).toLowerCase(
                    java.util.Locale.ROOT))) continue;
            result.add(FactoryPortfolioPolicy.profile(capabilities));
        }
        return result;
    }

    private static void selectFactoryProduct(FactoryPlanCandidate factory,
            TeamPositionDoctrine.Role role, AiStrategicMapSnapshot situation,
            StrategicFrontState frontState, double credits, List<UnitView> liveOwn) {
        java.util.LinkedHashMap<String, UnitType> products =
                new java.util.LinkedHashMap<String, UnitType>();
        for (UnitAction action : supportedTechActions(factory.factory, 4)) {
            UnitType product = action.getBuildUnitType();
            if (!action.isBuildAction() || product == null || product.isBuilding()) continue;
            AiUnitTypeCapabilities capabilities = AiUnitTypeCapabilities.capture(product);
            if (!capabilities.mobileCombatUnit()
                    || StrategicProductionDoctrine.isReconType(capabilities)) continue;
            products.putIfAbsent(safe(product.getInternalName()).toLowerCase(
                    java.util.Locale.ROOT), product);
        }
        StrategicProductionDoctrine.AirBalance airBalance =
                StrategicProductionDoctrine.assessAirBalance(situation);
        if (role == TeamPositionDoctrine.Role.MOBILE_SUPPORT
                && airBalance != StrategicProductionDoctrine.AirBalance.SUPERIORITY) {
            UnitType strongest = null;
            double strongestValue = 0.0D;
            for (UnitType product : products.values()) {
                AiUnitTypeCapabilities capabilities = AiUnitTypeCapabilities.capture(product);
                double value = StrategicProductionDoctrine.airSuperiorityValue(capabilities);
                if (value > strongestValue || value == strongestValue && value > 0.0D
                        && (strongest == null || safe(product.getInternalName())
                        .compareToIgnoreCase(safe(strongest.getInternalName())) < 0)) {
                    strongest = product;
                    strongestValue = value;
                }
            }
            if (strongest != null) {
                factory.unit = strongest;
                factory.unitScore = plannedUnitValue(
                        AiUnitTypeCapabilities.capture(strongest), role,
                        frontState, airBalance, credits, liveOwn) + 16.0D;
                factory.score = factory.unitScore
                        - Math.max(0, factory.cost) * 0.00016D
                        + (factory.existing ? 0.22D : 0.0D);
                return;
            }
        }
        for (UnitType product : products.values()) {
            AiUnitTypeCapabilities capabilities = AiUnitTypeCapabilities.capture(product);
            if (!roleAcceptsProduct(role, capabilities)) continue;
            double score = plannedUnitValue(capabilities, role, frontState,
                    airBalance, credits, liveOwn);
            if (factory.unit == null || score > factory.unitScore
                    || score == factory.unitScore
                    && safe(product.getInternalName()).compareToIgnoreCase(
                    safe(factory.unit.getInternalName())) < 0) {
                factory.unit = product;
                factory.unitScore = score;
            }
        }
        if (factory.unit != null) {
            factory.score = factory.unitScore
                    - Math.max(0, factory.cost) * 0.00016D
                    + (factory.existing ? 0.22D : 0.0D);
        }
    }

    private static double plannedUnitValue(AiUnitTypeCapabilities capabilities,
            TeamPositionDoctrine.Role role, StrategicFrontState frontState,
            StrategicProductionDoctrine.AirBalance airBalance, double credits,
            List<UnitView> liveOwn) {
        double durability = Math.max(1.0D, capabilities.maximumHealth()
                + capabilities.maximumShield());
        boolean sustainedFight = frontState != null
                && (frontState.mode() == StrategicFrontState.Mode.ATTRITION
                || frontState.mode() == StrategicFrontState.Mode.MUSTER);
        double dps = Math.max(0.04D, sustainedFight
                ? capabilities.estimatedSustainedDps() * 0.78D
                + capabilities.estimatedInitialDps() * 0.22D
                : capabilities.estimatedEngagementDps(180.0F));
        double power = Math.sqrt(durability * dps);
        double cost = Math.max(1.0D, capabilities.creditCost());
        double efficiency = power * 100.0D / cost;
        double throughput = ProductionValuePolicy.productionThroughput(
                power, capabilities.buildSpeed());
        double score = ProductionValuePolicy.balancedCombatValue(
                power, efficiency, throughput)
                + capabilities.techLevel() * 0.38D;
        if (capabilities.areaWeapon()) {
            double crowdControl = Math.log1p(capabilities.estimatedAreaDps()) * 0.42D
                    + Math.min(2.6D, capabilities.maximumAreaDamageRadius() / 42.0D);
            if (frontState != null && (frontState.mode() == StrategicFrontState.Mode.ASSAULT
                    || frontState.mode() == StrategicFrontState.Mode.MUSTER)) {
                crowdControl *= 1.18D;
            }
            score += crowdControl;
        }
        boolean air = capabilities.movementDomain() == AiMovementDomain.AIR;
        if (role == TeamPositionDoctrine.Role.MOBILE_SUPPORT) {
            score += air ? 6.0D : -6.0D;
            if (airBalance != StrategicProductionDoctrine.AirBalance.SUPERIORITY) {
                score += capabilities.airToAirSpecialist() ? 7.0D : -5.0D;
            }
        } else {
            score += air ? -4.5D : 1.2D;
            if (!air) {
                int groundTotal = groundCombatCount(liveOwn);
                int sameType = countLiveType(liveOwn, capabilities.typeId());
                score -= ProductionValuePolicy.exactTypeSaturationPenalty(
                        sameType, groundTotal);
            }
            if (role == TeamPositionDoctrine.Role.FRONTLINE) {
                score += Math.min(3.5D, capabilities.maximumAttackRange() / 95.0D);
                if (frontState != null
                        && frontState.mode() == StrategicFrontState.Mode.ATTRITION) {
                    score += capabilities.maximumAttackRange() / 70.0D;
                    score += Math.log1p(durability) * 0.28D;
                    UnitView defense = frontState.primaryDefense();
                    if (defense != null && defense.raw() instanceof Unit) {
                        float towerRange = AiUnitTypeCapabilities.capture(
                                ((Unit) defense.raw()).r()).maximumAttackRange();
                        float margin = capabilities.maximumAttackRange() - towerRange;
                        score += capabilities.maximumAttackRange() / 40.0D
                                + (margin >= 5.0F ? 12.0D
                                : -Math.min(10.0D, -margin / 20.0D));
                    }
                    if (capabilities.maximumWarmupTime() > 0.0F) {
                        score += Math.log1p(capabilities.estimatedSustainedDps()
                                / Math.max(0.01F, capabilities.estimatedInitialDps())) * 1.4D;
                        if (capabilities.retainsWarmupAfterFiring()) score += 0.65D;
                    }
                }
                if (credits < cost * 1.35D) score -= 3.0D;
            } else if (role == TeamPositionDoctrine.Role.ECONOMY_TECH) {
                score += Math.log1p(power) * 0.55D + capabilities.techLevel() * 0.65D;
            }
        }
        return score;
    }

    private static boolean focusTypeSaturated(List<UnitView> own, UnitType focused) {
        AiUnitTypeCapabilities capabilities = AiUnitTypeCapabilities.capture(focused);
        if (capabilities.movementDomain() == AiMovementDomain.AIR) return false;
        return ProductionValuePolicy.exactTypeSaturated(
                countLiveType(own, capabilities.typeId()), groundCombatCount(own));
    }

    private static int groundCombatCount(List<UnitView> units) {
        int result = 0;
        for (UnitView unit : units) {
            AiUnitTypeCapabilities capabilities = type(unit);
            if (capabilities != null && capabilities.mobileCombatUnit()
                    && capabilities.movementDomain() != AiMovementDomain.AIR) result++;
        }
        return result;
    }

    private static int countLiveType(List<UnitView> units, String typeId) {
        int result = 0;
        for (UnitView unit : units) {
            AiUnitTypeCapabilities capabilities = type(unit);
            if (capabilities != null && safe(capabilities.typeId())
                    .equalsIgnoreCase(safe(typeId))) result++;
        }
        return result;
    }

    private static AiUnitTypeCapabilities type(UnitView unit) {
        if (unit == null || !(unit.raw() instanceof Unit)) return null;
        UnitType type = ((Unit) unit.raw()).r();
        return type != null ? AiUnitTypeCapabilities.capture(type) : null;
    }

    private static boolean roleAcceptsProduct(TeamPositionDoctrine.Role role,
            AiUnitTypeCapabilities capabilities) {
        boolean air = capabilities.movementDomain() == AiMovementDomain.AIR;
        if (role == TeamPositionDoctrine.Role.MOBILE_SUPPORT) return air;
        if (role == TeamPositionDoctrine.Role.FRONTLINE
                || role == TeamPositionDoctrine.Role.ECONOMY_TECH) {
            return isLandCombatDomain(capabilities.movementDomain());
        }
        return true;
    }

    private boolean isFocusFactory(UnitType type) {
        return type != null && focusFactoryId != null
                && focusFactoryId.equalsIgnoreCase(safe(type.getInternalName()));
    }

    private static boolean offersUnit(Unit producer, UnitType product) {
        if (producer == null || product == null) return false;
        for (UnitAction action : UnitActions.available(producer)) {
            if (action.getBuildUnitType() == product && action.isBuildAction()) return true;
        }
        return false;
    }

    private static UnitAction actionProducing(List<UnitAction> actions, UnitType product) {
        if (product == null) return null;
        for (UnitAction action : actions) {
            if (action.getBuildUnitType() == product) return action;
        }
        return null;
    }

    private static WorldPoint enemyCenter(List<UnitView> enemies,
            AiStrategicMapSnapshot situation, WorldPoint fallbackAnchor) {
        float x = 0.0F;
        float y = 0.0F;
        int count = 0;
        for (UnitView enemy : enemies) {
            if (!enemy.alive() || !enemy.building()) continue;
            x += enemy.x();
            y += enemy.y();
            count++;
        }
        if (count > 0) return new WorldPoint(x / count, y / count);
        float mapX = situation.terrain().mapWidthTiles() * situation.terrain().tileWidth() * 0.5F;
        float mapY = situation.terrain().mapHeightTiles() * situation.terrain().tileHeight() * 0.5F;
        if (Math.abs(mapX - fallbackAnchor.x()) + Math.abs(mapY - fallbackAnchor.y()) < 1.0F) {
            mapX += 1.0F;
        }
        return new WorldPoint(mapX, mapY);
    }

    private boolean ensurePrimaryBuilding(AiTickContext context,
            AiStrategicMapSnapshot situation, List<Builder> builders, long cycle,
            BaseLayoutGeometry.District district, Predicate<UnitType> acceptedType) {
        if (primaryBase == null) return false;
        if (primaryDistrictReservations.containsKey(district)) return true;
        boolean ordered = ensurePlannedBuildingAt(context, situation, builders, cycle,
                primaryBase, district, acceptedType, "primary");
        if (ordered) primaryDistrictReservations.put(
                district, cycle + BUILDING_RESERVATION_CYCLES);
        return ordered;
    }

    private boolean ensureRoleProduction(AiTickContext context,
            AiStrategicMapSnapshot situation, List<Builder> builders, long cycle,
            StrategicTeamPlan teamPlan, StrategicResourceCampaign campaign,
            List<UnitView> own) {
        Predicate<UnitType> rolePreferred = preferredProducer(teamPlan.ownRole());
        Predicate<UnitType> preferred = focusFactoryId != null
                ? type -> focusFactoryId.equalsIgnoreCase(safe(type.getInternalName()))
                : rolePreferred;
        BaseLayoutPlan frontline = frontlineProductionLayout(teamPlan, campaign, own);
        if (frontline != null) {
            if (preferred != null && ensurePlannedBuildingAt(context, situation, builders,
                    cycle, frontline, BaseLayoutGeometry.District.PRODUCTION,
                    preferred, "frontline")) return true;
            if (ensurePlannedBuildingAt(context, situation, builders, cycle,
                    frontline, BaseLayoutGeometry.District.PRODUCTION,
                    StrategicBuildPlanner::declaresCombatProduction,
                    "frontline")) return true;
        }
        if (preferred != null && ensurePrimaryBuilding(context, situation, builders,
                cycle, BaseLayoutGeometry.District.PRODUCTION, preferred)) return true;
        return ensurePrimaryBuilding(context, situation, builders, cycle,
                BaseLayoutGeometry.District.PRODUCTION,
                StrategicBuildPlanner::declaresCombatProduction);
    }

    private void ensureProductionCapacity(AiTickContext context,
            AiStrategicMapSnapshot situation, List<Builder> builders, long cycle,
            StrategicTeamPlan teamPlan, StrategicFrontState frontState,
            StrategicResourceCampaign campaign,
            List<UnitView> own, int producers) {
        double credits = context.team().credits();
        AiUnitTypeCapabilities focus = focusUnitType != null
                ? AiUnitTypeCapabilities.capture(focusUnitType) : null;
        if (focus == null) return;
        double unitCost = Math.max(1.0D, focus.creditCost());
        double burnPerSecond = ProductionValuePolicy.creditBurnPerSecond(
                unitCost, focus.buildSpeed());
        double reserveFactor = teamPlan.ownRole() == TeamPositionDoctrine.Role.ECONOMY_TECH
                ? 2.0D : teamPlan.leadsFrontline() ? 1.25D : 1.55D;
        double reserve = Math.max(1200.0D, unitCost * reserveFactor);
        double horizon = productionBurstHorizon(teamPlan, frontState);
        int desired = ProductionValuePolicy.sustainableProducerTarget(
                context.team().incomeRate(), credits, reserve,
                burnPerSecond, horizon, 4);
        String capacityPlan = safe(focus.typeId()) + ':' + desired;
        if (!capacityPlan.equals(announcedCapacityPlan)) {
            announcedCapacityPlan = capacityPlan;
            System.out.println("[Strategic AI] Team " + context.team().id()
                    + " production capacity main=" + safe(focus.typeId())
                    + " income=" + context.team().incomeRate()
                    + " burnPerFactory=" + String.format(java.util.Locale.ROOT,
                    "%.1f", burnPerSecond) + "/s bank=" + (long) credits
                    + " reserve=" + (long) reserve + " desired=" + desired);
        }
        // Do not spend the reserve that was deliberately kept for the selected production
        // rhythm merely because a builder happens to expose another factory action.
        if (credits < reserve) return;
        if (producers < desired) {
            ensureRoleProduction(context, situation, builders, cycle,
                    teamPlan, campaign, own);
        }
    }

    private static double productionBurstHorizon(StrategicTeamPlan plan,
            StrategicFrontState frontState) {
        if (plan.leadsFrontline()) {
            if (frontState != null && (frontState.mode() == StrategicFrontState.Mode.ASSAULT
                    || frontState.mode() == StrategicFrontState.Mode.ATTRITION)) return 30.0D;
            return 40.0D;
        }
        if (plan.ownRole() == TeamPositionDoctrine.Role.MOBILE_SUPPORT) return 42.0D;
        if (plan.ownRole() == TeamPositionDoctrine.Role.ECONOMY_TECH) return 55.0D;
        return 45.0D;
    }

    private BaseLayoutPlan frontlineProductionLayout(StrategicTeamPlan teamPlan,
            StrategicResourceCampaign campaign, List<UnitView> own) {
        if (!teamPlan.leadsFrontline() || campaign.point() == null
                || !hasStaticDefenseNear(own, campaign.point(), 330.0F)) return null;
        WorldPoint home = primaryBase != null ? primaryBase.anchor() : teamPlan.ownAnchor();
        if (home == null) return null;
        WorldPoint front = extendAway(home, campaign.point(), 300.0F);
        return new BaseLayoutPlan(-resourceKey(campaign.target()), campaign.point(), front);
    }

    private boolean recoverOrStageFrontlineBuilders(AiTickContext context,
            List<Builder> builders, AiStrategicMapSnapshot situation,
            StrategicResourceCampaign campaign,
            StrategicTeamPlan teamPlan, List<UnitView> own) {
        if (!teamPlan.leadsFrontline() || campaign.point() == null) return false;
        WorldPoint home = primaryBase != null ? primaryBase.anchor() : teamPlan.ownAnchor();
        if (home == null) return false;
        boolean fortified = hasStaticDefenseNear(own, campaign.point(), 330.0F);
        // Losing one contest tower is not a retreat-to-base order. Builders hold a
        // second, lock-capable line close enough to keep contesting the resource.
        float setback = fortified ? 105.0F : 145.0F;
        WorldPoint staging = situationRoutePoint(
                situation.terrain().routesFrom(home, AiMovementDomain.LAND),
                campaign.point(), setback,
                ForceCoordinationGeometry.advance(campaign.point(), home, setback));
        UnitView enemyTower = closestStaticDefense(situation.world().enemies(),
                campaign.point().x(), campaign.point().y(), 430.0F);
        if (enemyTower != null && enemyTower.raw() instanceof Unit
                && enemyTower.constructionProgress() >= 0.82F) {
            AiUnitTypeCapabilities enemyType = AiUnitTypeCapabilities.capture(
                    ((Unit) enemyTower.raw()).r());
            float danger = enemyType.maximumAttackRange() + 42.0F;
            if (distance(staging.x(), staging.y(), enemyTower.x(), enemyTower.y())
                    < danger) {
                staging = safeContestFallback(situation, home, enemyTower,
                        enemyType.maximumAttackRange());
            }
        }
        int nearby = 0;
        float frontRadiusSquared = 300.0F * 300.0F;
        for (UnitView unit : own) {
            if (!unit.alive()) continue;
            AiUnitCapabilities capabilities = AiUnitCapabilities.capture(unit);
            if (!capabilities.builder() || !capabilities.movable()) continue;
            float dx = unit.x() - campaign.point().x();
            float dy = unit.y() - campaign.point().y();
            if (dx * dx + dy * dy <= frontRadiusSquared) nearby++;
        }
        int wanted = FRONTLINE_BUILDER_TARGET;
        ArrayList<UnitView> moving = new ArrayList<UnitView>();
        for (Builder builder : builders) {
            if (fortified && nearby + moving.size() >= wanted) break;
            UnitView unit = builder.capabilities.unit();
            float dx = unit.x() - staging.x();
            float dy = unit.y() - staging.y();
            if (dx * dx + dy * dy <= 45.0F * 45.0F) continue;
            moving.add(unit);
            frontlineBuilderIds.add(unit.id());
            if (moving.size() >= wanted) break;
        }
        if (moving.isEmpty()) return false;
        context.orders().move(moving, staging.x(), staging.y());
        return true;
    }

    private static WorldPoint extendAway(WorldPoint home, WorldPoint point, float distance) {
        float dx = point.x() - home.x();
        float dy = point.y() - home.y();
        float length = (float) Math.hypot(dx, dy);
        if (length < 0.001F) return new WorldPoint(point.x() + distance, point.y());
        return new WorldPoint(point.x() + dx / length * distance,
                point.y() + dy / length * distance);
    }

    private static Predicate<UnitType> preferredProducer(TeamPositionDoctrine.Role role) {
        if (role == TeamPositionDoctrine.Role.MOBILE_SUPPORT) {
            return type -> declaresProductionDomain(type, AiMovementDomain.AIR);
        }
        return null;
    }

    private boolean ensurePlannedBuildingAt(AiTickContext context,
            AiStrategicMapSnapshot situation, List<Builder> builders, long cycle,
            BaseLayoutPlan layout, BaseLayoutGeometry.District district,
            Predicate<UnitType> acceptedType, String layoutName) {
        ArrayList<BuildChoice> choices = new ArrayList<BuildChoice>();
        for (Builder builder : builders) {
            for (UnitAction action : availableBuildActions(builder.unit, true, false)) {
                UnitType type = action.getBuildUnitType();
                if (acceptedType.test(type)) choices.add(new BuildChoice(builder, action));
            }
        }
        choices.sort(Comparator.comparingInt((BuildChoice value) -> value.action.getCreditCost())
                .thenComparing(value -> safe(value.action.getBuildUnitType().getInternalName()))
                .thenComparingLong(value -> value.builder.capabilities.unit().id()));
        for (BuildChoice choice : choices) {
            BuildPoint point = findBuildPoint(context, situation, choice.builder,
                    choice.action.getBuildUnitType(), layout, district);
            if (point == null) continue;
            context.orders().build(Collections.singletonList(
                    choice.builder.capabilities.unit()), point.x, point.y, choice.action);
            buildingReservations.put(point.reservationKey,
                    cycle + BUILDING_RESERVATION_CYCLES);
            System.out.println("[Strategic AI] Planned " + layoutName + " "
                    + district.name().toLowerCase()
                    + " building " + safe(choice.action.getBuildUnitType().getInternalName())
                    + " at " + (int) point.x + "," + (int) point.y);
            return true;
        }
        return false;
    }

    private boolean ensureFrontierDefense(AiTickContext context,
            AiStrategicMapSnapshot situation, List<Builder> builders, long cycle,
            StrategicResourceCampaign campaign, List<UnitView> own) {
        if (!campaign.active() || campaign.phase() != StrategicResourceCampaign.Phase.HOLD
                || campaign.point() == null) return false;
        long targetKey = resourceKey(campaign.target());
        if (frontierDefenseTargetKey != targetKey) {
            frontierDefenseTargetKey = targetKey;
            frontierDefenseReservationUntil = 0L;
        }
        if (hasStaticDefenseNear(own, campaign.point(), 300.0F)) return false;
        if (cycle < frontierDefenseReservationUntil) return true;
        WorldPoint front = primaryBase != null ? primaryBase.front()
                : new WorldPoint(campaign.point().x() + 1.0F, campaign.point().y());
        BaseLayoutPlan outpost = new BaseLayoutPlan(-targetKey, campaign.point(), front);
        boolean ordered = ensurePlannedBuildingAt(context, situation, builders, cycle,
                outpost, BaseLayoutGeometry.District.DEFENSE,
                StrategicBuildPlanner::isStaticDefense, "frontier");
        if (ordered) frontierDefenseReservationUntil = cycle + BUILDING_RESERVATION_CYCLES;
        return ordered;
    }

    private boolean ensureStrategicDefenses(AiTickContext context,
            AiStrategicMapSnapshot situation, List<Builder> baseBuilders,
            List<Builder> allBuilders, long cycle, StrategicTeamPlan teamPlan,
            StrategicFrontState frontState, StrategicResourceCampaign campaign,
            List<UnitView> own, int producers) {
        if (primaryBase == null) return false;
        double seconds = gameSeconds();
        boolean airDisadvantage = productionPlanAirBalance
                == StrategicProductionDoctrine.AirBalance.DISADVANTAGE;
        WorldPoint forwardPoint = campaign.active() ? campaign.point() : null;
        WorldPoint productionCenter = productionClusterCenter(own);

        if (airDisadvantage && forwardPoint != null && teamPlan.leadsFrontline()) {
            int current = countDefenseNear(own, forwardPoint, 350.0F, true);
            int desired = DefensiveInvestmentPolicy.desiredAntiAir(
                    true, true, seconds, producers);
            if (current < desired && ensureForwardDefenseType(context, situation,
                    allBuilders, cycle, forwardPoint, true, true)) return true;
        }
        if (airDisadvantage) {
            int current = countDefenseNear(own, productionCenter, 440.0F, true);
            int desired = DefensiveInvestmentPolicy.desiredAntiAir(
                    true, false, seconds, producers);
            if (current < desired && ensureBaseDefenseType(context, situation,
                    baseBuilders, cycle, true, true, own, teamPlan, frontState)) return true;
        }

        int wantedForward = DefensiveInvestmentPolicy.desiredForwardGround(
                teamPlan.leadsFrontline(), seconds, context.team().displayIncomeRate());
        if (forwardPoint != null && wantedForward > 0
                && countDefenseNear(own, forwardPoint, 350.0F, false) < wantedForward
                && ensureForwardDefenseType(context, situation, allBuilders,
                cycle, forwardPoint, false, false)) return true;

        int wantedBase = DefensiveInvestmentPolicy.desiredBaseGround(
                seconds, producers, context.team().displayIncomeRate());
        return wantedBase > 0
                && countDefenseNear(own, productionCenter, 440.0F, false) < wantedBase
                && ensureBaseDefenseType(context, situation, baseBuilders,
                cycle, false, false, own, teamPlan, frontState);
    }

    private boolean ensureForwardDefenseType(AiTickContext context,
            AiStrategicMapSnapshot situation, List<Builder> builders, long cycle,
            WorldPoint objective, boolean antiAir, boolean urgent) {
        UnitType selected = selectDefenseType(context, builders, antiAir, urgent, true);
        if (selected == null) return false;
        WorldPoint home = primaryBase.anchor();
        AiTerrainRouteMap routes = situation.terrain().routesFrom(home, AiMovementDomain.LAND);
        WorldPoint anchor = situationRoutePoint(routes, objective, 255.0F,
                ForceCoordinationGeometry.advance(objective, home, 255.0F));
        BaseLayoutPlan conservative = new BaseLayoutPlan(
                -placementKey(situation, objective.x(), objective.y()),
                anchor, objective);
        boolean ordered = ensurePlannedBuildingAt(context, situation, builders, cycle,
                conservative, BaseLayoutGeometry.District.DEFENSE,
                type -> type == selected, antiAir ? "forward anti-air" : "forward line");
        if (ordered) frontierDefenseReservationUntil = cycle + BUILDING_RESERVATION_CYCLES;
        return ordered;
    }

    private boolean ensureBaseDefenseType(AiTickContext context,
            AiStrategicMapSnapshot situation, List<Builder> builders, long cycle,
            boolean antiAir, boolean urgent, List<UnitView> own,
            StrategicTeamPlan teamPlan, StrategicFrontState frontState) {
        UnitType selected = selectDefenseType(context, builders, antiAir, urgent, false);
        if (selected == null || primaryBase == null) return false;
        if (primaryDistrictReservations.containsKey(BaseLayoutGeometry.District.DEFENSE)) {
            return true;
        }
        WorldPoint center = productionClusterCenter(own);
        AiUnitTypeCapabilities capabilities = AiUnitTypeCapabilities.capture(selected);
        WorldPoint threat;
        if (capabilities.canAttackAir()) {
            threat = closestEnemyBuildingPoint(situation.world().enemies(), center);
        } else {
            threat = frontState != null ? frontState.point() : null;
            if (threat == null) threat = teamPlan.preferredFrontierPoint();
            if (threat == null) threat = situation.primaryFront().orElse(primaryBase.front());
        }
        BaseLayoutPlan perimeter = productionClusterLayout(own, threat);
        boolean ordered = ensurePlannedBuildingAt(context, situation, builders, cycle,
                perimeter, BaseLayoutGeometry.District.DEFENSE,
                type -> type == selected, antiAir ? "base anti-air perimeter"
                        : "base factory perimeter");
        if (ordered) primaryDistrictReservations.put(BaseLayoutGeometry.District.DEFENSE,
                cycle + BUILDING_RESERVATION_CYCLES);
        return ordered;
    }

    private WorldPoint productionClusterCenter(List<UnitView> own) {
        float x = 0.0F;
        float y = 0.0F;
        int count = 0;
        for (UnitView view : own) {
            if (!view.alive() || !view.building() || !(view.raw() instanceof Unit)) continue;
            Unit raw = (Unit) view.raw();
            if (raw.r() == null || !declaresCombatProduction(raw.r())) continue;
            x += view.x();
            y += view.y();
            count++;
        }
        return count > 0 ? new WorldPoint(x / count, y / count)
                : primaryBase.anchor();
    }

    private BaseLayoutPlan productionClusterLayout(List<UnitView> own, WorldPoint threat) {
        WorldPoint center = productionClusterCenter(own);
        WorldPoint front = threat != null ? threat : primaryBase.front();
        return new BaseLayoutPlan(primaryBase.anchorUnitId(), center, front);
    }

    private static WorldPoint closestEnemyBuildingPoint(List<UnitView> enemies,
            WorldPoint center) {
        UnitView closest = null;
        float best = Float.POSITIVE_INFINITY;
        for (UnitView enemy : enemies) {
            if (!enemy.alive() || !enemy.building()) continue;
            float distance = center.distanceSquared(new WorldPoint(enemy.x(), enemy.y()));
            if (distance < best || distance == best
                    && (closest == null || enemy.id() < closest.id())) {
                closest = enemy;
                best = distance;
            }
        }
        return closest != null ? new WorldPoint(closest.x(), closest.y()) : center;
    }

    private static UnitType selectDefenseType(AiTickContext context,
            List<Builder> builders, boolean antiAir, boolean urgent, boolean frontline) {
        UnitType selected = null;
        double best = Double.NEGATIVE_INFINITY;
        java.util.HashSet<UnitType> seen = new java.util.HashSet<UnitType>();
        for (Builder builder : builders) {
            for (UnitAction action : availableBuildActions(builder.unit, true, false)) {
                UnitType type = action.getBuildUnitType();
                if (!seen.add(type) || !isStaticDefense(type)) continue;
                AiUnitTypeCapabilities capabilities = AiUnitTypeCapabilities.capture(type);
                if (antiAir ? !capabilities.canAttackAir() : !capabilities.canAttackGround()) {
                    continue;
                }
                double cost = Math.max(1, Math.max(action.getCreditCost(),
                        capabilities.creditCost()));
                if (!DefensiveInvestmentPolicy.canAfford(context.team().credits(),
                        context.team().displayIncomeRate(), cost, urgent, frontline)) continue;
                double dps = antiAir ? capabilities.estimatedAirDps()
                        : capabilities.estimatedGroundDps();
                double durability = capabilities.maximumHealth()
                        + capabilities.maximumShield();
                double score = Math.sqrt(Math.max(1.0D, durability)
                        * Math.max(0.02D, dps))
                        + capabilities.maximumAttackRange() * 0.16D;
                score /= Math.sqrt(cost);
                if (antiAir && !capabilities.canAttackGround()) score += 0.22D;
                if (score > best || score == best && selected != null
                        && safe(type.getInternalName()).compareToIgnoreCase(
                        safe(selected.getInternalName())) < 0) {
                    selected = type;
                    best = score;
                }
            }
        }
        return selected;
    }

    private static int countDefenseNear(List<UnitView> units, WorldPoint point,
            float radius, boolean antiAir) {
        int result = 0;
        float radiusSquared = radius * radius;
        for (UnitView view : units) {
            if (!view.alive() || !view.building() || !(view.raw() instanceof Unit)) continue;
            UnitType type = ((Unit) view.raw()).r();
            if (!isStaticDefense(type)) continue;
            AiUnitTypeCapabilities capabilities = AiUnitTypeCapabilities.capture(type);
            if (antiAir ? !capabilities.canAttackAir() : !capabilities.canAttackGround()) continue;
            float dx = view.x() - point.x();
            float dy = view.y() - point.y();
            if (dx * dx + dy * dy <= radiusSquared) result++;
        }
        return result;
    }

    private boolean ensureEconomicInvestment(AiTickContext context,
            AiStrategicMapSnapshot situation, List<Builder> builders, long cycle,
            StrategicTeamPlan teamPlan, List<UnitView> own) {
        if (cycle < economicInvestmentReservationUntil) return false;
        double seconds = gameSeconds();
        double multiplier = context.team().effectiveIncomeMultiplier();
        double effectiveIncome = context.team().displayIncomeRate();
        boolean economyRole = teamPlan.ownRole() == TeamPositionDoctrine.Role.ECONOMY_TECH;

        EconomicUpgrade bestUpgrade = null;
        for (UnitView view : own) {
            if (!view.alive() || !view.building() || !(view.raw() instanceof OrderableUnit)) {
                continue;
            }
            Unit raw = (Unit) view.raw();
            if (!isEconomicBuilding(raw, view.creditGenerationPerSecond())
                    || hasQueuedUpgrade(raw)) continue;
            UnitAction upgrade = availableUpgrade(raw);
            if (upgrade == null) continue;
            double delta = expectedUpgradeIncomeDelta(view, raw, upgrade);
            double cost = Math.max(1, upgrade.getCreditCost());
            double buildSeconds = actionBuildTimeSeconds(upgrade);
            if (!EconomicInvestmentPolicy.shouldInvest(
                    EconomicInvestmentPolicy.Kind.RESOURCE_UPGRADE,
                    seconds, context.team().credits(), effectiveIncome,
                    cost, delta, multiplier, buildSeconds, economyRole)) continue;
            double payback = EconomicInvestmentPolicy.paybackSeconds(
                    cost, delta, multiplier, buildSeconds);
            if (bestUpgrade == null || payback < bestUpgrade.payback
                    || payback == bestUpgrade.payback && view.id() < bestUpgrade.view.id()) {
                bestUpgrade = new EconomicUpgrade(view, (OrderableUnit) raw,
                        upgrade, payback, delta);
            }
        }
        if (bestUpgrade != null) {
            UnitActions.issue(context.rawTeam(), Collections.singletonList(
                    bestUpgrade.unit), bestUpgrade.action);
            economicInvestmentReservationUntil = cycle + 8L;
            System.out.println("[Strategic AI] Team " + context.team().id()
                    + " queued economic upgrade "
                    + safe(bestUpgrade.action.getActionIdString())
                    + " payback=" + Math.round(bestUpgrade.payback) + "s"
                    + " delta=" + String.format(java.util.Locale.ROOT,
                    "%.1f", bestUpgrade.delta * multiplier) + "/s"
                    + " multiplier=" + String.format(java.util.Locale.ROOT,
                    "%.2f", multiplier));
            return true;
        }

        int manufacturerLimit = EconomicInvestmentPolicy.manufacturerLimit(
                seconds, effectiveIncome, economyRole);
        int manufacturers = 0;
        for (UnitView view : own) {
            if (!view.alive() || !(view.raw() instanceof Unit)) continue;
            Unit raw = (Unit) view.raw();
            if (isResourceManufacturer(raw.r())) manufacturers++;
        }
        if (manufacturers >= manufacturerLimit || builders.isEmpty()) return false;

        EconomicBuild bestBuild = null;
        java.util.HashSet<UnitType> seen = new java.util.HashSet<UnitType>();
        for (Builder builder : builders) {
            for (UnitAction action : availableBuildActions(builder.unit, true, false)) {
                UnitType type = action.getBuildUnitType();
                if (!seen.add(type) || !isResourceManufacturer(type)) continue;
                AiUnitTypeCapabilities capabilities = AiUnitTypeCapabilities.capture(type);
                double rate = capabilities.creditGenerationPerSecond();
                double cost = Math.max(1, Math.max(action.getCreditCost(),
                        capabilities.creditCost()));
                double buildSeconds = capabilities.nominalBuildTimeSeconds();
                if (!EconomicInvestmentPolicy.shouldInvest(
                        EconomicInvestmentPolicy.Kind.RESOURCE_MANUFACTURER,
                        seconds, context.team().credits(), effectiveIncome,
                        cost, rate, multiplier, buildSeconds, economyRole)) continue;
                double payback = EconomicInvestmentPolicy.paybackSeconds(
                        cost, rate, multiplier, buildSeconds);
                if (bestBuild == null || payback < bestBuild.payback) {
                    bestBuild = new EconomicBuild(type, payback, rate);
                }
            }
        }
        if (bestBuild == null) return false;
        final UnitType selected = bestBuild.type;
        boolean ordered = ensurePrimaryBuilding(context, situation, builders, cycle,
                BaseLayoutGeometry.District.SUPPORT, type -> type == selected);
        if (ordered) {
            economicInvestmentReservationUntil = cycle + BUILDING_RESERVATION_CYCLES;
            System.out.println("[Strategic AI] Team " + context.team().id()
                    + " ordered resource manufacturer " + safe(selected.getInternalName())
                    + " payback=" + Math.round(bestBuild.payback) + "s"
                    + " output=" + String.format(java.util.Locale.ROOT,
                    "%.1f", bestBuild.rate * multiplier) + "/s");
        }
        return ordered;
    }

    private boolean ensureFrontierMaintenance(AiTickContext context,
            List<Builder> builders, StrategicResourceCampaign campaign,
            List<UnitView> own) {
        if (!campaign.active() || campaign.point() == null) return false;
        UnitView damaged = null;
        float worstCondition = 0.985F;
        float radiusSquared = 330.0F * 330.0F;
        for (UnitView unit : own) {
            if (!unit.alive() || !unit.building() || !(unit.raw() instanceof Unit)) continue;
            if (unit.id() == abandonedForwardTowerId) continue;
            UnitType type = ((Unit) unit.raw()).r();
            if (!isStaticDefense(type)) continue;
            float dx = unit.x() - campaign.point().x();
            float dy = unit.y() - campaign.point().y();
            if (dx * dx + dy * dy > radiusSquared) continue;
            float fraction = unit.healthFraction();
            float condition = Math.min(fraction, unit.constructionProgress());
            if (condition < worstCondition || condition == worstCondition
                    && (damaged == null || unit.id() < damaged.id())) {
                damaged = unit;
                worstCondition = condition;
            }
        }
        if (damaged == null) return false;
        final UnitView repairTarget = damaged;
        ArrayList<Builder> repairers = new ArrayList<Builder>(builders);
        repairers.sort(Comparator.comparingDouble((Builder builder) -> {
            UnitView view = builder.capabilities.unit();
            float dx = view.x() - repairTarget.x();
            float dy = view.y() - repairTarget.y();
            return dx * dx + dy * dy;
        }).thenComparingLong(builder -> builder.capabilities.unit().id()));
        ArrayList<UnitView> units = new ArrayList<UnitView>();
        for (int index = 0; index < Math.min(
                FRONTLINE_BUILDER_TARGET, repairers.size()); index++) {
            units.add(repairers.get(index).capabilities.unit());
        }
        if (units.isEmpty()) return false;
        context.orders().repair(units, repairTarget);
        System.out.println("[Strategic AI] Team " + context.team().id()
                + " repairing forward defense " + damaged.id()
                + " condition=" + Math.round(worstCondition * 100.0F) + "%");
        return true;
    }

    private void evaluateForwardTowerContest(AiTickContext context,
            AiStrategicMapSnapshot situation, StrategicResourceCampaign campaign,
            List<UnitView> own) {
        if (contestRecoveryActive) return;
        if (!campaign.active() || campaign.point() == null) return;
        UnitView tower = closestIncompleteStaticDefense(
                own, campaign.point(), 340.0F);
        if (tower == null || tower.id() == abandonedForwardTowerId) return;
        UnitView enemyTower = closestStaticDefense(
                situation.world().enemies(), tower.x(), tower.y(), 430.0F);
        if (enemyTower == null || !(enemyTower.raw() instanceof Unit)) return;
        AiUnitTypeCapabilities enemyType = AiUnitTypeCapabilities.capture(
                ((Unit) enemyTower.raw()).r());
        float dx = enemyTower.x() - tower.x();
        float dy = enemyTower.y() - tower.y();
        float distance = (float) Math.hypot(dx, dy);
        if (distance > enemyType.maximumAttackRange() + 24.0F) return;

        float ownProgress = tower.constructionProgress();
        float enemyProgress = enemyTower.constructionProgress();
        int ownBuilders = nearbyBuilders(own, tower.x(), tower.y(), 210.0F);
        int enemyBuilders = nearbyBuilders(situation.world().enemies(),
                enemyTower.x(), enemyTower.y(), 210.0F);
        boolean completedEnemyControls = enemyProgress >= 0.98F && ownProgress < 0.86F;
        boolean losesProgressRace = enemyProgress > ownProgress + 0.10F
                && enemyBuilders >= Math.max(1, ownBuilders);
        boolean collapsing = tower.healthFraction() + 0.16F < ownProgress
                && ownProgress < 0.90F;
        if (!completedEnemyControls && !losesProgressRace && !collapsing) return;

        WorldPoint home = primaryBase != null ? primaryBase.anchor() : null;
        if (home == null) return;
        WorldPoint fallback = safeContestFallback(situation, home, enemyTower,
                enemyType.maximumAttackRange());
        abandonedForwardTowerId = tower.id();
        forceLockFortification = true;
        contestRecoveryActive = true;
        contestRecoveryTargetKey = resourceKey(campaign.target());
        contestRecoveryEnemyTowerId = enemyTower.id();
        contestRecoveryPoint = fallback;
        contestRecoveryTowerType = tower.raw() instanceof Unit
                ? ((Unit) tower.raw()).r() : null;
        contestRecoveryBuildUntil = 0L;
        frontierDefenseReservationUntil = 0L;
        campaign.retryFortification();
        System.out.println("[Strategic AI] Team " + context.team().id()
                + " abandoning losing contest tower " + tower.id()
                + " ownProgress=" + Math.round(ownProgress * 100.0F) + "%"
                + " enemyProgress=" + Math.round(enemyProgress * 100.0F) + "%"
                + " builders=" + ownBuilders + ":" + enemyBuilders
                + "; preparing resource lock at "
                + (int) fallback.x() + "," + (int) fallback.y());
    }

    private boolean handleContestRecovery(AiTickContext context,
            AiStrategicMapSnapshot situation, List<Builder> builders, long cycle,
            StrategicResourceCampaign campaign, List<UnitView> own) {
        if (!contestRecoveryActive) return false;
        if (!campaign.active() || campaign.point() == null
                || resourceKey(campaign.target()) != contestRecoveryTargetKey) {
            contestRecoveryActive = false;
            contestRecoveryTargetKey = Long.MIN_VALUE;
            contestRecoveryEnemyTowerId = Long.MIN_VALUE;
            contestRecoveryPoint = null;
            contestRecoveryTowerType = null;
            contestRecoveryBuildUntil = 0L;
            abandonedForwardTowerId = Long.MIN_VALUE;
            forceLockFortification = false;
            return false;
        }
        UnitView enemyTower = unitById(situation.world().enemies(),
                contestRecoveryEnemyTowerId);
        if (enemyTower == null || !enemyTower.alive()
                || !(enemyTower.raw() instanceof Unit)) {
            // The dangerous tower disappeared; the original forward line can be used again.
            contestRecoveryActive = false;
            contestRecoveryTargetKey = Long.MIN_VALUE;
            contestRecoveryEnemyTowerId = Long.MIN_VALUE;
            contestRecoveryPoint = null;
            contestRecoveryTowerType = null;
            contestRecoveryBuildUntil = 0L;
            abandonedForwardTowerId = Long.MIN_VALUE;
            forceLockFortification = false;
            return false;
        }
        AiUnitTypeCapabilities enemyType = AiUnitTypeCapabilities.capture(
                ((Unit) enemyTower.raw()).r());
        WorldPoint home = primaryBase != null ? primaryBase.anchor() : null;
        if (home == null) return true;
        contestRecoveryPoint = safeContestFallback(situation, home, enemyTower,
                enemyType.maximumAttackRange());

        List<Builder> frontline = recoveryFrontlineBuilders(
                builders, campaign.point(), enemyTower,
                enemyType.maximumAttackRange(), FRONTLINE_BUILDER_TARGET);
        if (frontline.isEmpty()) return true;
        java.util.LinkedHashMap<Long, UnitView> evacuation =
                new java.util.LinkedHashMap<Long, UnitView>();
        for (Builder builder : frontline) {
            UnitView unit = builder.capabilities.unit();
            evacuation.put(unit.id(), unit);
            frontlineBuilderIds.add(unit.id());
        }
        float frontSweepRange = Math.max(560.0F,
                enemyType.maximumAttackRange() + 250.0F);
        for (Builder builder : builders) {
            UnitView unit = builder.capabilities.unit();
            if (distance(unit.x(), unit.y(), enemyTower.x(), enemyTower.y())
                    < enemyType.maximumAttackRange() + 95.0F
                    || distance(unit.x(), unit.y(), campaign.point().x(),
                    campaign.point().y()) < frontSweepRange
                    && (frontlineBuilderIds.contains(unit.id())
                    || builder.capabilities.idle())) {
                evacuation.put(unit.id(), unit);
                frontlineBuilderIds.add(unit.id());
            }
        }
        ArrayList<UnitView> participants = new ArrayList<UnitView>(evacuation.values());
        boolean allOutsideEnemyRange = true;
        boolean allStaged = true;
        float dangerRange = enemyType.maximumAttackRange() + 38.0F;
        for (UnitView unit : participants) {
            if (distance(unit.x(), unit.y(), enemyTower.x(), enemyTower.y()) < dangerRange) {
                allOutsideEnemyRange = false;
            }
            if (distance(unit.x(), unit.y(),
                    contestRecoveryPoint.x(), contestRecoveryPoint.y()) > 88.0F) {
                allStaged = false;
            }
        }
        if (!allOutsideEnemyRange || !allStaged) {
            context.orders().move(participants,
                    contestRecoveryPoint.x(), contestRecoveryPoint.y());
            return true;
        }

        UnitView replacement = closestStaticDefense(own,
                contestRecoveryPoint.x(), contestRecoveryPoint.y(), 150.0F);
        if (replacement != null && replacement.id() != abandonedForwardTowerId) {
            if (replacement.constructionProgress() < 0.98F
                    || replacement.healthFraction() < 0.985F) {
                context.orders().repair(participants, replacement);
                return true;
            }
            contestRecoveryActive = false;
            contestRecoveryTargetKey = Long.MIN_VALUE;
            forceLockFortification = false;
            contestRecoveryBuildUntil = 0L;
            campaign.markFortificationOrdered();
            return false;
        }
        if (cycle < contestRecoveryBuildUntil) return true;

        BuildChoice choice = recoveryTowerChoice(frontline, contestRecoveryTowerType);
        if (choice == null) return true;
        BuildPoint point = findRecoveryTowerPoint(context, situation, choice.builder,
                choice.action.getBuildUnitType(), contestRecoveryPoint,
                enemyTower, enemyType.maximumAttackRange());
        if (point == null) return true;
        List<UnitView> buildersForTower = buildParticipants(frontline,
                choice.action.getBuildUnitType(), FRONTLINE_BUILDER_TARGET);
        if (buildersForTower.isEmpty()) return true;
        context.orders().build(buildersForTower, point.x, point.y, choice.action);
        for (UnitView builder : buildersForTower) frontlineBuilderIds.add(builder.id());
        buildingReservations.put(point.reservationKey,
                cycle + BUILDING_RESERVATION_CYCLES);
        contestRecoveryBuildUntil = cycle + RECOVERY_BUILD_RESERVATION_CYCLES;
        campaign.markFortificationOrdered();
        System.out.println("[Strategic AI] Team " + context.team().id()
                + " rebuilding safe front tower with " + buildersForTower.size()
                + " builders at " + (int) point.x + "," + (int) point.y);
        return true;
    }

    /**
     * A front builder that has actually taken fire gets a short retreat lease. This is deliberately
     * separate from ordinary idle planning: an unfinished safe build keeps its builders, while a
     * builder under live fire is not ordered back and forth on consecutive planning pulses.
     */
    private void withdrawThreatenedFrontlineBuilders(AiTickContext context,
            AiStrategicMapSnapshot situation, List<Builder> builders, long cycle) {
        WorldPoint home = primaryBase != null ? primaryBase.anchor() : null;
        if (home == null) return;
        for (Builder builder : builders) {
            UnitView unit = builder.capabilities.unit();
            if (!frontlineBuilderIds.contains(unit.id())) continue;
            boolean freshlyHit = unit.recentDamager(2.75F).isPresent();
            Long until = frontlineBuilderRetreatUntil.get(unit.id());
            if (freshlyHit) {
                until = cycle + FRONTLINE_BUILDER_RETREAT_CYCLES;
                frontlineBuilderRetreatUntil.put(unit.id(), until);
            }
            if (until == null || until <= cycle) continue;
            // Reissue only after another hit or after the retreat order has completed. Avoids
            // resetting the path every strategic tick while still preventing idle hesitation.
            if (freshlyHit || builder.capabilities.idle()) {
                WorldPoint position = new WorldPoint(unit.x(), unit.y());
                WorldPoint retreat = passableRoutePointAfter(
                        situation.terrain().routesFrom(position, AiMovementDomain.LAND),
                        home, 260.0F).orElse(home);
                context.orders().move(Collections.singletonList(unit),
                        retreat.x(), retreat.y());
            }
        }
    }

    private void beginLostTowerRecovery(AiStrategicMapSnapshot situation,
            StrategicResourceCampaign campaign, List<Builder> builders,
            List<UnitView> own) {
        if (contestRecoveryActive || !campaign.active()
                || campaign.point() == null || builders.isEmpty()) return;
        UnitView enemyTower = closestStaticDefense(situation.world().enemies(),
                campaign.point().x(), campaign.point().y(), 430.0F);
        if (enemyTower == null || !(enemyTower.raw() instanceof Unit)) return;
        AiUnitTypeCapabilities enemyType = AiUnitTypeCapabilities.capture(
                ((Unit) enemyTower.raw()).r());
        float distanceToFront = distance(enemyTower.x(), enemyTower.y(),
                campaign.point().x(), campaign.point().y());
        boolean controlsFront = distanceToFront
                <= enemyType.maximumAttackRange() + 45.0F;
        boolean established = enemyTower.constructionProgress() >= 0.82F;
        boolean ownLineReady = hasCompletedStaticDefenseNear(
                own, campaign.point(), 340.0F);
        if (!campaign.fortificationLost()
                && (!established || !controlsFront || ownLineReady)) return;
        WorldPoint home = primaryBase != null ? primaryBase.anchor() : null;
        if (home == null) return;
        BuildChoice choice = recoveryTowerChoice(closestFrontlineBuilders(
                builders, campaign.point(), FRONTLINE_BUILDER_TARGET), null);
        if (choice == null) return;
        contestRecoveryActive = true;
        contestRecoveryTargetKey = resourceKey(campaign.target());
        contestRecoveryEnemyTowerId = enemyTower.id();
        contestRecoveryPoint = safeContestFallback(situation, home,
                enemyTower, enemyType.maximumAttackRange());
        contestRecoveryTowerType = choice.action.getBuildUnitType();
        contestRecoveryBuildUntil = 0L;
        forceLockFortification = true;
        System.out.println("[Strategic AI] Enemy front tower established before own line; "
                + "redirecting every exposed builder to "
                + (int) contestRecoveryPoint.x() + "," + (int) contestRecoveryPoint.y());
    }

    private static WorldPoint safeContestFallback(AiStrategicMapSnapshot situation,
            WorldPoint home, UnitView enemyTower, float enemyRange) {
        AiTerrainRouteMap routes = situation.terrain().routesFrom(home, AiMovementDomain.LAND);
        WorldPoint enemy = new WorldPoint(enemyTower.x(), enemyTower.y());
        for (float setback = enemyRange + 90.0F;
                setback <= enemyRange + 570.0F; setback += 80.0F) {
            java.util.Optional<WorldPoint> candidate =
                    passableRoutePointBefore(routes, enemy, setback);
            if (candidate.isPresent()
                    && distance(candidate.get().x(), candidate.get().y(),
                    enemyTower.x(), enemyTower.y()) >= enemyRange + 38.0F) {
                return candidate.get();
            }
        }
        AiTerrainCell homeCell = situation.terrain().cellAtWorld(home.x(), home.y());
        return homeCell != null
                ? homeCell.representativePoint(AiMovementDomain.LAND).orElse(home) : home;
    }

    private static List<Builder> closestFrontlineBuilders(List<Builder> builders,
            WorldPoint front, int maximum) {
        ArrayList<Builder> result = new ArrayList<Builder>(builders);
        result.sort(Comparator.comparingDouble((Builder builder) -> {
            UnitView unit = builder.capabilities.unit();
            float dx = unit.x() - front.x();
            float dy = unit.y() - front.y();
            return dx * dx + dy * dy;
        }).thenComparingLong(builder -> builder.capabilities.unit().id()));
        if (result.size() > maximum) result.subList(maximum, result.size()).clear();
        return result;
    }

    private List<Builder> recoveryFrontlineBuilders(List<Builder> builders,
            WorldPoint front, UnitView enemyTower, float enemyRange, int maximum) {
        ArrayList<Builder> eligible = new ArrayList<Builder>();
        ArrayList<Builder> idleReserve = new ArrayList<Builder>();
        float sweep = Math.max(560.0F, enemyRange + 250.0F);
        for (Builder builder : builders) {
            UnitView unit = builder.capabilities.unit();
            boolean assigned = frontlineBuilderIds.contains(unit.id());
            boolean exposed = distance(unit.x(), unit.y(), enemyTower.x(), enemyTower.y())
                    < enemyRange + 95.0F;
            boolean nearFront = distance(unit.x(), unit.y(), front.x(), front.y()) < sweep;
            if (assigned || exposed || nearFront && builder.capabilities.idle()) {
                eligible.add(builder);
            } else if (builder.capabilities.idle()) {
                idleReserve.add(builder);
            }
        }
        Comparator<Builder> byFront = Comparator.comparingDouble((Builder builder) -> {
            UnitView unit = builder.capabilities.unit();
            float dx = unit.x() - front.x();
            float dy = unit.y() - front.y();
            return dx * dx + dy * dy;
        }).thenComparingLong(builder -> builder.capabilities.unit().id());
        eligible.sort(byFront);
        idleReserve.sort(byFront);
        for (Builder builder : idleReserve) {
            if (eligible.size() >= maximum) break;
            eligible.add(builder);
        }
        if (eligible.size() > maximum) eligible.subList(maximum, eligible.size()).clear();
        return eligible;
    }

    private static BuildChoice recoveryTowerChoice(List<Builder> builders,
            UnitType preferredType) {
        ArrayList<BuildChoice> choices = new ArrayList<BuildChoice>();
        for (Builder builder : builders) {
            for (UnitAction action : availableBuildActions(builder.unit, true, false)) {
                if (!isStaticDefense(action.getBuildUnitType())) continue;
                choices.add(new BuildChoice(builder, action));
            }
        }
        choices.sort(Comparator
                .comparingInt((BuildChoice value) -> value.action.getBuildUnitType()
                        == preferredType ? 0 : 1)
                .thenComparingInt(value -> value.action.getCreditCost())
                .thenComparing(value -> safe(value.action.getBuildUnitType().getInternalName()))
                .thenComparingLong(value -> value.builder.capabilities.unit().id()));
        return choices.isEmpty() ? null : choices.get(0);
    }

    private BuildPoint findRecoveryTowerPoint(AiTickContext context,
            AiStrategicMapSnapshot situation, Builder builder, UnitType tower,
            WorldPoint desired, UnitView enemyTower, float enemyRange) {
        int tileWidth = situation.terrain().tileWidth();
        int tileHeight = situation.terrain().tileHeight();
        int[][] offsets = {{0, 0}, {-1, 0}, {1, 0}, {0, -1}, {0, 1},
                {-2, 0}, {2, 0}, {0, -2}, {0, 2}, {-1, -1}, {1, -1},
                {-1, 1}, {1, 1}, {-3, 0}, {3, 0}};
        AiTerrainCell origin = situation.terrain().cellAtWorld(
                builder.capabilities.unit().x(), builder.capabilities.unit().y());
        ArrayList<WorldPoint> bases = new ArrayList<WorldPoint>();
        bases.add(desired);
        WorldPoint home = primaryBase != null ? primaryBase.anchor()
                : new WorldPoint(builder.capabilities.unit().x(),
                builder.capabilities.unit().y());
        AiTerrainRouteMap routes = situation.terrain().routesFrom(home, AiMovementDomain.LAND);
        for (float fartherHome : new float[]{90.0F, 180.0F, 270.0F}) {
            java.util.Optional<WorldPoint> base = passableRoutePointBefore(
                    routes, desired, fartherHome);
            if (base.isPresent()) bases.add(base.get());
        }
        for (WorldPoint base : bases) {
            for (int[] offset : offsets) {
                float x = base.x() + offset[0] * tileWidth;
                float y = base.y() + offset[1] * tileHeight;
                if (distance(x, y, enemyTower.x(), enemyTower.y())
                        < enemyRange + 38.0F) continue;
                long key = placementKey(situation, x, y);
                if (buildingReservations.containsKey(key)) continue;
                AiTerrainCell target = situation.terrain().cellAtWorld(x, y);
                if (target == null
                        || target.passableFraction(AiMovementDomain.LAND) < 0.12F
                        || !target.representativePoint(AiMovementDomain.LAND).isPresent()
                        || !situation.terrain().sameRegion(origin, target,
                        builder.capabilities.movementDomain())) continue;
                if (UnitTypes.canSpawnStarting(tower, x, y, 0.0F, 0.0F,
                        context.rawTeam())) return new BuildPoint(x, y, key);
            }
        }
        return null;
    }

    private static List<UnitView> buildParticipants(List<Builder> builders,
            UnitType tower, int maximum) {
        ArrayList<UnitView> result = new ArrayList<UnitView>();
        for (Builder builder : builders) {
            for (UnitAction action : availableBuildActions(builder.unit, true, false)) {
                if (action.getBuildUnitType() != tower) continue;
                result.add(builder.capabilities.unit());
                break;
            }
            if (result.size() >= maximum) break;
        }
        return result;
    }

    private static UnitView unitById(List<UnitView> units, long id) {
        for (UnitView unit : units) {
            if (unit.id() == id) return unit;
        }
        return null;
    }

    private static float distance(float x1, float y1, float x2, float y2) {
        return (float) Math.hypot(x1 - x2, y1 - y2);
    }

    private boolean ensureFrontierFortification(AiTickContext context,
            AiStrategicMapSnapshot situation, List<Builder> builders, long cycle,
            StrategicResourceCampaign campaign) {
        if (campaign.point() == null
                || !campaign.allowsFortification() && !forceLockFortification) return false;
        if (campaign.fortificationLost()) {
            frontierDefenseReservationUntil = 0L;
            forceLockFortification = true;
        }
        long targetKey = resourceKey(campaign.target());
        if (frontierDefenseTargetKey != targetKey) {
            frontierDefenseTargetKey = targetKey;
            frontierDefenseReservationUntil = 0L;
        }
        if (cycle < frontierDefenseReservationUntil) return true;
        ArrayList<BuildChoice> choices = new ArrayList<BuildChoice>();
        for (Builder builder : builders) {
            for (UnitAction action : availableBuildActions(builder.unit, true, false)) {
                UnitType type = action.getBuildUnitType();
                if (isStaticDefense(type)
                        && AiUnitTypeCapabilities.capture(type).canAttackGround()
                        && DefensiveInvestmentPolicy.canAfford(
                        context.team().credits(), context.team().displayIncomeRate(),
                        Math.max(action.getCreditCost(), type.getBuildCostCredits()),
                        false, true)) {
                    choices.add(new BuildChoice(builder, action));
                }
            }
        }
        choices.sort(Comparator.comparingInt((BuildChoice value) -> value.action.getCreditCost())
                .thenComparing(value -> safe(value.action.getBuildUnitType().getInternalName()))
                .thenComparingLong(value -> value.builder.capabilities.unit().id()));
        for (BuildChoice choice : choices) {
            ForwardTowerPoint point = findForwardTowerPoint(context, situation,
                    choice.builder, choice.action.getBuildUnitType(), campaign.point());
            if (point == null) continue;
            List<UnitView> participants = forwardBuildParticipants(
                    builders, choice.action.getBuildUnitType());
            context.orders().build(participants, point.x, point.y, choice.action);
            buildingReservations.put(point.reservationKey,
                    cycle + BUILDING_RESERVATION_CYCLES);
            frontierDefenseReservationUntil = cycle + BUILDING_RESERVATION_CYCLES;
            campaign.markFortificationOrdered();
            System.out.println("[Strategic AI] Planned conservative forward defense "
                    + safe(choice.action.getBuildUnitType().getInternalName())
                    + " mode=route-safe-line"
                    + " at " + (int) point.x + "," + (int) point.y);
            forceLockFortification = false;
            return true;
        }
        return false;
    }

    private List<UnitView> forwardBuildParticipants(List<Builder> builders,
            UnitType tower) {
        ArrayList<UnitView> result = new ArrayList<UnitView>();
        for (Builder builder : builders) {
            boolean canBuild = false;
            for (UnitAction action : availableBuildActions(builder.unit, true, false)) {
                if (action.getBuildUnitType() == tower) {
                    canBuild = true;
                    break;
                }
            }
            if (canBuild) {
                UnitView unit = builder.capabilities.unit();
                result.add(unit);
                frontlineBuilderIds.add(unit.id());
            }
            if (result.size() >= FRONTLINE_BUILDER_TARGET) break;
        }
        return result;
    }

    private ForwardTowerPoint findForwardTowerPoint(AiTickContext context,
            AiStrategicMapSnapshot situation, Builder builder, UnitType tower,
            WorldPoint resource) {
        WorldPoint home = primaryBase != null ? primaryBase.anchor()
                : new WorldPoint(builder.capabilities.unit().x(),
                builder.capabilities.unit().y());
        float towerRange = Math.max(90.0F,
                AiUnitTypeCapabilities.capture(tower).maximumAttackRange());
        float setback = Math.max(230.0F, towerRange + 70.0F);
        AiTerrainRouteMap routes = situation.terrain().routesFrom(home, AiMovementDomain.LAND);
        WorldPoint base = passableRoutePointBefore(routes, resource, setback)
                .orElseGet(() -> ForwardTowerGeometry.placement(
                        home, resource, towerRange, 0.0F, 0.0F));
        float dx = resource.x() - base.x();
        float dy = resource.y() - base.y();
        float length = (float) Math.hypot(dx, dy);
        if (length < 1.0F) { dx = 1.0F; dy = 0.0F; }
        else { dx /= length; dy /= length; }
        float[] lateral = {0.0F, -42.0F, 42.0F, -78.0F, 78.0F, -118.0F, 118.0F};
        // Search toward home first. A failed race must make the next line safer, not closer.
        float[] radial = {0.0F, -48.0F, -96.0F, -144.0F, 48.0F};
        AiTerrainCell origin = situation.terrain().cellAtWorld(
                builder.capabilities.unit().x(), builder.capabilities.unit().y());
        for (float radialOffset : radial) {
            for (float lateralOffset : lateral) {
                float x = base.x() + dx * radialOffset - dy * lateralOffset;
                float y = base.y() + dy * radialOffset + dx * lateralOffset;
                if (enemyTowerControls(situation.world().enemies(), x, y, 18.0F)) continue;
                long key = placementKey(situation, x, y);
                if (buildingReservations.containsKey(key)) continue;
                AiTerrainCell target = situation.terrain().cellAtWorld(x, y);
                if (target == null
                        || target.passableFraction(AiMovementDomain.LAND) < 0.12F
                        || !target.representativePoint(AiMovementDomain.LAND).isPresent()) continue;
                if (!situation.terrain().sameRegion(origin, target,
                        builder.capabilities.movementDomain())) continue;
                if (UnitTypes.canSpawnStarting(tower, x, y, 0.0F, 0.0F,
                        context.rawTeam())) return new ForwardTowerPoint(x, y, key);
            }
        }
        return null;
    }

    private static java.util.Optional<WorldPoint> passableRoutePointBefore(
            AiTerrainRouteMap routes, WorldPoint resource, float distanceBefore) {
        List<WorldPoint> path = routes.pathTo(resource);
        if (path.isEmpty()) return java.util.Optional.empty();
        float remaining = Math.max(0.0F, distanceBefore);
        for (int index = path.size() - 1; index > 0; index--) {
            WorldPoint to = path.get(index);
            WorldPoint from = path.get(index - 1);
            float segment = (float) Math.sqrt(to.distanceSquared(from));
            if (segment >= remaining) return java.util.Optional.of(from);
            remaining -= segment;
        }
        return java.util.Optional.of(path.get(0));
    }

    private static java.util.Optional<WorldPoint> passableRoutePointAfter(
            AiTerrainRouteMap routes, WorldPoint destination, float distanceAfter) {
        List<WorldPoint> path = routes.pathTo(destination);
        if (path.isEmpty()) return java.util.Optional.empty();
        float remaining = Math.max(0.0F, distanceAfter);
        for (int index = 1; index < path.size(); index++) {
            WorldPoint from = path.get(index - 1);
            WorldPoint to = path.get(index);
            float segment = (float) Math.sqrt(to.distanceSquared(from));
            if (segment >= remaining) return java.util.Optional.of(to);
            remaining -= segment;
        }
        return java.util.Optional.of(path.get(path.size() - 1));
    }

    private static WorldPoint situationRoutePoint(AiTerrainRouteMap routes,
            WorldPoint destination, float distanceBefore, WorldPoint fallback) {
        return passableRoutePointBefore(routes, destination, distanceBefore).orElse(fallback);
    }

    private static boolean enemyTowerControls(List<UnitView> enemies,
            float x, float y, float safetyMargin) {
        for (UnitView enemy : enemies) {
            if (!enemy.alive() || !enemy.building() || !(enemy.raw() instanceof Unit)) continue;
            AiUnitTypeCapabilities capabilities = AiUnitTypeCapabilities.capture(
                    ((Unit) enemy.raw()).r());
            if (!capabilities.attacker()) continue;
            float range = capabilities.maximumAttackRange() + safetyMargin;
            float dx = enemy.x() - x;
            float dy = enemy.y() - y;
            if (dx * dx + dy * dy <= range * range) return true;
        }
        return false;
    }

    private static List<Builder> builders(List<UnitView> own, boolean idleOnly) {
        ArrayList<Builder> result = new ArrayList<Builder>();
        for (UnitView unit : own) {
            if (!unit.alive()) continue;
            AiUnitCapabilities capabilities = AiUnitCapabilities.capture(unit);
            if (capabilities.builder() && capabilities.movable()
                    && capabilities.orderable() && (!idleOnly || capabilities.idle())) {
                result.add(new Builder((Unit) unit.raw(), capabilities));
            }
        }
        result.sort(Comparator.comparingLong(value -> value.capabilities.unit().id()));
        return result;
    }

    private void assignFrontlineBuilders(List<Builder> builders,
            StrategicResourceCampaign campaign, StrategicTeamPlan teamPlan) {
        if (frontlineBuilderIds.size() >= FRONTLINE_BUILDER_TARGET) return;
        WorldPoint front = campaign.point() != null
                ? campaign.point() : teamPlan.preferredFrontierPoint();
        if (front == null) return;
        ArrayList<Builder> candidates = new ArrayList<Builder>();
        for (Builder builder : builders) {
            UnitView unit = builder.capabilities.unit();
            if (!frontlineBuilderIds.contains(unit.id())
                    && builder.capabilities.idle()) candidates.add(builder);
        }
        candidates.sort(Comparator.comparingDouble((Builder builder) -> {
            UnitView unit = builder.capabilities.unit();
            float dx = unit.x() - front.x();
            float dy = unit.y() - front.y();
            return dx * dx + dy * dy;
        }).thenComparingLong(builder -> builder.capabilities.unit().id()));
        for (Builder builder : candidates) {
            if (frontlineBuilderIds.size() >= FRONTLINE_BUILDER_TARGET) break;
            frontlineBuilderIds.add(builder.capabilities.unit().id());
        }
    }

    private List<Builder> selectBuilders(List<Builder> builders, boolean frontline) {
        ArrayList<Builder> selected = new ArrayList<Builder>();
        for (Builder builder : builders) {
            if (frontlineBuilderIds.contains(builder.capabilities.unit().id()) == frontline) {
                selected.add(builder);
            }
        }
        return selected;
    }

    private void retainLiveFrontlineBuilders(List<UnitView> own) {
        java.util.HashSet<Long> live = new java.util.HashSet<Long>();
        for (UnitView unit : own) {
            if (!unit.alive()) continue;
            AiUnitCapabilities capabilities = AiUnitCapabilities.capture(unit);
            if (capabilities.builder() && capabilities.movable()) live.add(unit.id());
        }
        frontlineBuilderIds.retainAll(live);
    }

    private static UnitAction resourceExtractorAction(Unit builder) {
        List<UnitAction> actions = availableBuildActions(builder, true, true);
        return actions.isEmpty() ? null : actions.get(0);
    }

    private static UnitAction availableUpgrade(Unit unit) {
        ArrayList<UnitAction> upgrades = new ArrayList<UnitAction>();
        for (UnitAction action : UnitActions.available(unit)) {
            if (UnitActions.isUpgradeAction(action)
                    && action.getDisplayQueueCount(unit, true) <= 0) upgrades.add(action);
        }
        upgrades.sort(Comparator.comparingInt(UnitAction::getCreditCost)
                .thenComparing(action -> safe(action.getActionIdString())));
        return upgrades.isEmpty() ? null : upgrades.get(0);
    }

    private static boolean hasQueuedUpgrade(Unit unit) {
        for (UnitAction action : UnitActions.forUnit(unit)) {
            if (UnitActions.isUpgradeAction(action)
                    && action.getDisplayQueueCount(unit, true) > 0) return true;
        }
        return false;
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

    private int countFocusedCombatProducers(List<UnitView> units,
            TeamPositionDoctrine.Role role) {
        int count = 0;
        for (UnitView unit : units) {
            if (!unit.alive()) continue;
            if (primaryBase != null && unit.id() == primaryBase.anchorUnitId()) continue;
            if (!unit.building() || !(unit.raw() instanceof Unit)) continue;
            Unit raw = (Unit) unit.raw();
            if (!offersMobileCombat(raw)) continue;
            if (focusFactoryId != null) {
                if (focusFactoryId.equalsIgnoreCase(safe(raw.r() != null
                        ? raw.r().getInternalName() : null))) count++;
                continue;
            }
            for (UnitAction action : UnitActions.forUnit(raw)) {
                UnitType product = action.getBuildUnitType();
                if (!action.isBuildAction() || product == null || product.isBuilding()) continue;
                AiUnitTypeCapabilities capabilities = AiUnitTypeCapabilities.capture(product);
                if (capabilities.mobileCombatUnit()
                        && roleAcceptsProduct(role, capabilities)) {
                    count++;
                    break;
                }
            }
        }
        return count;
    }

    private static boolean hasStaticDefenseNear(List<UnitView> units,
            WorldPoint point, float radius) {
        float radiusSquared = radius * radius;
        for (UnitView unit : units) {
            if (!unit.alive() || !unit.building()) continue;
            if (unit.raw() instanceof Unit && offersMobileCombat((Unit) unit.raw())) continue;
            if (!AiUnitCapabilities.capture(unit).attacker()) continue;
            if (unit.constructionProgress() < 0.98F) continue;
            float dx = unit.x() - point.x();
            float dy = unit.y() - point.y();
            if (dx * dx + dy * dy <= radiusSquared) return true;
        }
        return false;
    }

    private static boolean hasCompletedStaticDefenseNear(List<UnitView> units,
            WorldPoint point, float radius) {
        float radiusSquared = radius * radius;
        for (UnitView unit : units) {
            if (!unit.alive() || !unit.building() || unit.constructionProgress() < 0.98F
                    || !(unit.raw() instanceof Unit)
                    || !isStaticDefense(((Unit) unit.raw()).r())) continue;
            float dx = unit.x() - point.x();
            float dy = unit.y() - point.y();
            if (dx * dx + dy * dy <= radiusSquared) return true;
        }
        return false;
    }

    private static UnitView closestIncompleteStaticDefense(List<UnitView> units,
            WorldPoint point, float radius) {
        UnitView best = null;
        float bestDistance = radius * radius;
        for (UnitView unit : units) {
            if (!unit.alive() || !unit.building() || unit.constructionProgress() >= 0.98F
                    || !(unit.raw() instanceof Unit)
                    || !isStaticDefense(((Unit) unit.raw()).r())) continue;
            float dx = unit.x() - point.x();
            float dy = unit.y() - point.y();
            float distance = dx * dx + dy * dy;
            if (distance <= bestDistance && (best == null || distance < bestDistance
                    || unit.id() < best.id())) {
                best = unit;
                bestDistance = distance;
            }
        }
        return best;
    }

    private static UnitView closestStaticDefense(List<UnitView> units,
            float x, float y, float radius) {
        UnitView best = null;
        float bestDistance = radius * radius;
        for (UnitView unit : units) {
            if (!unit.alive() || !unit.building() || !(unit.raw() instanceof Unit)
                    || !isStaticDefense(((Unit) unit.raw()).r())) continue;
            float dx = unit.x() - x;
            float dy = unit.y() - y;
            float distance = dx * dx + dy * dy;
            if (distance <= bestDistance && (best == null || distance < bestDistance
                    || unit.id() < best.id())) {
                best = unit;
                bestDistance = distance;
            }
        }
        return best;
    }

    private static int nearbyBuilders(List<UnitView> units,
            float x, float y, float radius) {
        int count = 0;
        float squared = radius * radius;
        for (UnitView unit : units) {
            if (!unit.alive()) continue;
            AiUnitCapabilities capabilities = AiUnitCapabilities.capture(unit);
            if (!capabilities.builder() || !capabilities.movable()) continue;
            float dx = unit.x() - x;
            float dy = unit.y() - y;
            if (dx * dx + dy * dy <= squared) count++;
        }
        return count;
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

    private static boolean declaresProductionDomain(UnitType building,
            AiMovementDomain domain) {
        if (building == null || !building.isBuilding()) return false;
        for (UnitAction action : UnitActions.forType(building,
                Math.max(1, building.getTechLevel()))) {
            UnitType product = action.getBuildUnitType();
            if (action.isBuildAction() && product != null && !product.isBuilding()) {
                AiUnitTypeCapabilities capabilities = AiUnitTypeCapabilities.capture(product);
                if (capabilities.mobileCombatUnit()
                        && capabilities.movementDomain() == domain) return true;
            }
        }
        return false;
    }

    private static boolean declaresNonReconGroundProduction(UnitType building) {
        if (building == null || !building.isBuilding()) return false;
        for (UnitAction action : supportedTechActions(building, 4)) {
            UnitType product = action.getBuildUnitType();
            if (!action.isBuildAction() || product == null || product.isBuilding()) continue;
            AiUnitTypeCapabilities capabilities = AiUnitTypeCapabilities.capture(product);
            if (capabilities.mobileCombatUnit()
                    && !StrategicProductionDoctrine.isReconType(capabilities)
                    && isLandCombatDomain(capabilities.movementDomain())) return true;
        }
        return false;
    }

    private static boolean isLandCombatDomain(AiMovementDomain domain) {
        return domain == AiMovementDomain.LAND || domain == AiMovementDomain.HOVER
                || domain == AiMovementDomain.OVER_CLIFF
                || domain == AiMovementDomain.OVER_CLIFF_WATER;
    }

    /** Enumerates only action tables that the concrete unit type actually supports. */
    private static List<UnitAction> supportedTechActions(UnitType type, int requestedMaximum) {
        ArrayList<UnitAction> actions = new ArrayList<UnitAction>();
        for (int tech = 1; tech <= requestedMaximum; tech++) {
            try {
                actions.addAll(UnitActions.forType(type, tech));
            } catch (RuntimeException failure) {
                String message = failure.getMessage();
                if (message != null && message.startsWith("Tech level:")
                        && message.contains("greater than maxTechLevel")) break;
                throw failure;
            }
        }
        return actions;
    }

    private static boolean isStaticDefense(UnitType building) {
        return building != null && building.isBuilding()
                && AiUnitTypeCapabilities.capture(building).attacker()
                && !declaresCombatProduction(building);
    }

    private static boolean isEconomicBuilding(Unit unit, float currentRate) {
        return unit != null && unit.r() != null && currentRate > 0.0F
                && (isResourceManufacturer(unit.r())
                || unit.r().isPlaceOnlyOnResourcePool());
    }

    private static boolean isResourceManufacturer(UnitType type) {
        if (type == null || !type.isBuilding() || type.isPlaceOnlyOnResourcePool()) return false;
        AiUnitTypeCapabilities capabilities = AiUnitTypeCapabilities.capture(type);
        if (capabilities.creditGenerationPerSecond() <= 0.0F) return false;
        String id = safe(type.getInternalName()).toLowerCase(java.util.Locale.ROOT);
        return capabilities.harvester() || id.contains("fabricator")
                || id.contains("resource") || id.contains("extractor");
    }

    private static double expectedUpgradeIncomeDelta(UnitView view, Unit raw,
            UnitAction upgrade) {
        double current = Math.max(0.0D, view.creditGenerationPerSecond());
        UnitType target = upgrade.getBuildUnitType();
        if (target != null) {
            double targetRate = AiUnitTypeCapabilities.capture(target)
                    .creditGenerationPerSecond();
            if (targetRate > current) return targetRate - current;
        }
        String id = safe(raw.r() != null ? raw.r().getInternalName() : null)
                .toLowerCase(java.util.Locale.ROOT);
        int tech = Math.max(1, view.techLevel());
        if (id.contains("extractor")) {
            double next = tech <= 1 ? 12.0D : tech == 2 ? 18.0D : current;
            if (next > current) return next - current;
        }
        if (id.contains("fabricator")) {
            double next = tech <= 1 ? 7.0D : tech == 2 ? 14.0D : current;
            if (next > current) return next - current;
        }
        // Custom economic upgrades do not always expose their conversion target. Use a
        // conservative lower-bound estimate instead of assuming a dramatic tier jump.
        return Math.max(1.0D, current * (tech <= 1 ? 0.55D : 0.42D));
    }

    private static double actionBuildTimeSeconds(UnitAction action) {
        if (!(action instanceof QueueableUnitAction)) return 0.0D;
        float speed = ((QueueableUnitAction) action).getBuildSpeed();
        return Float.isFinite(speed) && speed > 0.0F
                ? 1.0D / (speed * 60.0D) : 0.0D;
    }

    private static double gameSeconds() {
        return Math.max(0, GameWorld.gameTimeMillis()) / 1000.0D;
    }

    private BuildPoint findBuildPoint(AiTickContext context,
            AiStrategicMapSnapshot situation, Builder builder, UnitType type,
            BaseLayoutPlan layout, BaseLayoutGeometry.District district) {
        UnitView view = builder.capabilities.unit();
        int tileWidth = situation.terrain().tileWidth();
        int tileHeight = situation.terrain().tileHeight();
        AiTerrainCell origin = situation.terrain().cellAtWorld(view.x(), view.y());
        int[][] nudges = {{0, 0}, {-1, 0}, {1, 0}, {0, -1}, {0, 1},
                {-2, 0}, {2, 0}, {0, -2}, {0, 2}, {-1, -1}, {1, -1}, {-1, 1}, {1, 1}};
        ArrayList<WorldPoint> slots = new ArrayList<WorldPoint>(layout.slots(district));
        if (district == BaseLayoutGeometry.District.DEFENSE) {
            slots.sort(Comparator.comparingDouble((WorldPoint point) ->
                    -defenseTerrainScore(situation, layout, point)));
        }
        for (WorldPoint slot : slots) {
            for (int[] nudge : nudges) {
                float x = slot.x() + nudge[0] * tileWidth;
                float y = slot.y() + nudge[1] * tileHeight;
                long reservationKey = placementKey(situation, x, y);
                if (buildingReservations.containsKey(reservationKey)) continue;
                AiTerrainCell target = situation.terrain().cellAtWorld(x, y);
                AiMovementDomain domain = builder.capabilities.movementDomain();
                if (!situation.terrain().sameRegion(origin, target, domain)) continue;
                if (UnitTypes.canSpawnStarting(type, x, y, 0.0F, 0.0F,
                        context.rawTeam())) {
                    return new BuildPoint(x, y, reservationKey);
                }
            }
        }
        return null;
    }

    private static double defenseTerrainScore(AiStrategicMapSnapshot situation,
            BaseLayoutPlan layout, WorldPoint point) {
        AiTerrainCell cell = situation.terrain().cellAtWorld(point.x(), point.y());
        if (cell == null) return Double.NEGATIVE_INFINITY;
        double placement = DefensiveInvestmentPolicy.placementPriority(
                layout.anchor(), layout.front(), point,
                situation.terrain().worldWidth(), situation.terrain().worldHeight());
        return placement + cell.landChokeScore() * 0.45F
                + cell.passableFraction(AiMovementDomain.LAND) * 0.2F
                - cell.buildingBlockedFraction() * 0.15F;
    }

    private static long placementKey(AiStrategicMapSnapshot situation, float x, float y) {
        int tileX = Math.round(x / Math.max(1, situation.terrain().tileWidth()));
        int tileY = Math.round(y / Math.max(1, situation.terrain().tileHeight()));
        return ((long) tileX << 32) ^ (tileY & 0xffffffffL);
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

    private static class BuildPoint {
        final float x;
        final float y;
        final long reservationKey;

        BuildPoint(float x, float y, long reservationKey) {
            this.x = x;
            this.y = y;
            this.reservationKey = reservationKey;
        }
    }

    private static final class ForwardTowerPoint extends BuildPoint {
        ForwardTowerPoint(float x, float y, long reservationKey) {
            super(x, y, reservationKey);
        }
    }

    private static final class BuildChoice {
        final Builder builder;
        final UnitAction action;

        BuildChoice(Builder builder, UnitAction action) {
            this.builder = builder;
            this.action = action;
        }
    }

    private static final class EconomicUpgrade {
        final UnitView view;
        final OrderableUnit unit;
        final UnitAction action;
        final double payback;
        final double delta;

        EconomicUpgrade(UnitView view, OrderableUnit unit, UnitAction action,
                double payback, double delta) {
            this.view = view;
            this.unit = unit;
            this.action = action;
            this.payback = payback;
            this.delta = delta;
        }
    }

    private static final class EconomicBuild {
        final UnitType type;
        final double payback;
        final double rate;

        EconomicBuild(UnitType type, double payback, double rate) {
            this.type = type;
            this.payback = payback;
            this.rate = rate;
        }
    }

    private static final class FactoryPlanCandidate {
        final UnitType factory;
        final int cost;
        boolean existing;
        UnitType unit;
        double unitScore = Double.NEGATIVE_INFINITY;
        double score = Double.NEGATIVE_INFINITY;
        double portfolioNovelty;

        FactoryPlanCandidate(UnitType factory, int cost, boolean existing) {
            this.factory = factory;
            this.cost = cost;
            this.existing = existing;
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
