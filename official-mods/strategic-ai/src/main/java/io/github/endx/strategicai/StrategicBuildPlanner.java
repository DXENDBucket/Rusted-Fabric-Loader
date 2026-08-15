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
import io.github.endx.rustedfabricapi.api.world.WorldPoint;
import rustedwarfare.unit.OrderableUnit;
import rustedwarfare.unit.Unit;
import rustedwarfare.unit.UnitType;
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
    private static final float PRIMARY_BASE_RESOURCE_RADIUS = 700.0F;
    private final Map<Long, Long> resourceReservations = new HashMap<Long, Long>();
    private final Map<Long, Long> buildingReservations = new HashMap<Long, Long>();
    private final EnumMap<BaseLayoutGeometry.District, Long> primaryDistrictReservations =
            new EnumMap<BaseLayoutGeometry.District, Long>(BaseLayoutGeometry.District.class);
    private BaseLayoutPlan primaryBase;
    private long frontierDefenseReservationUntil;
    private long frontierDefenseTargetKey = Long.MIN_VALUE;
    private boolean forceLockFortification;
    private long abandonedForwardTowerId = Long.MIN_VALUE;
    private String focusFactoryId;
    private UnitType focusUnitType;
    private long productionPlanUntil;
    private StrategicFrontState.Mode productionPlanFrontMode;
    private boolean announcedEconomy;
    private int announcedOrders;

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
        List<UnitView> currentOwn = context.world().own();
        ensurePrimaryBase(situation, currentOwn, context.world().enemies(), teamPlan);
        evaluateForwardTowerContest(context, situation, resourceCampaign, currentOwn);
        List<Builder> builders = builders(currentOwn);
        refreshProductionPlan(context, situation, currentOwn, cycle,
                teamPlan, frontState);
        if (!builders.isEmpty()) {
            List<Builder> baseBuilders = buildersForBase(
                    builders, resourceCampaign, teamPlan);
            int producers = countDedicatedCombatProducers(currentOwn);
            if (ensureFrontierMaintenance(context, builders, resourceCampaign,
                    currentOwn)) {
                // A forward builder repairs the contested tower instead of idling beside it.
            } else if (ensureFrontierFortification(context, situation, builders, cycle,
                    resourceCampaign)) {
                // A suitable front-position opener intentionally takes priority over a home factory.
            } else if (recoverOrStageFrontlineBuilders(context, builders,
                    situation, resourceCampaign, teamPlan, currentOwn)) {
                // Frontline builders regroup behind a failed tower or join the established outpost.
            } else if (producers == 0) {
                ensureRoleProduction(context, situation, baseBuilders, cycle,
                        teamPlan, resourceCampaign, currentOwn);
            } else if (!claimCampaignResource(context, builders, cycle, resourceCampaign)
                    && !ensureFrontierDefense(context, situation, builders, cycle,
                            resourceCampaign, currentOwn)
                    && !claimResource(context, situation, baseBuilders, cycle, teamPlan)) {
                ensureProductionCapacity(context, situation, baseBuilders, cycle,
                        teamPlan, resourceCampaign, currentOwn, producers);
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
                if (focused != null) {
                    action = focused;
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
        if (focusFactoryId != null && focusUnitType != null
                && cycle < productionPlanUntil && mode == productionPlanFrontMode) return;
        java.util.LinkedHashMap<String, FactoryPlanCandidate> factories =
                new java.util.LinkedHashMap<String, FactoryPlanCandidate>();
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
            }
        }
        FactoryPlanCandidate best = null;
        for (FactoryPlanCandidate factory : factories.values()) {
            selectFactoryProduct(factory, teamPlan.ownRole(), situation,
                    frontState, context.team().credits());
            if (factory.unit == null) continue;
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
        System.out.println("[Strategic AI] Team " + context.team().id()
                + " production plan factory=" + focusFactoryId
                + " main=" + safe(focusUnitType.getInternalName())
                + " score=" + String.format(java.util.Locale.ROOT,
                "%.2f", best.score) + " until=" + productionPlanUntil);
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

    private static void selectFactoryProduct(FactoryPlanCandidate factory,
            TeamPositionDoctrine.Role role, AiStrategicMapSnapshot situation,
            StrategicFrontState frontState, double credits) {
        java.util.LinkedHashMap<String, UnitType> products =
                new java.util.LinkedHashMap<String, UnitType>();
        for (int tech = 1; tech <= 4; tech++) {
            try {
                for (UnitAction action : UnitActions.forType(factory.factory, tech)) {
                    UnitType product = action.getBuildUnitType();
                    if (!action.isBuildAction() || product == null || product.isBuilding()) continue;
                    AiUnitTypeCapabilities capabilities = AiUnitTypeCapabilities.capture(product);
                    if (!capabilities.mobileCombatUnit()
                            || StrategicProductionDoctrine.isReconType(capabilities)) continue;
                    products.putIfAbsent(safe(product.getInternalName()).toLowerCase(
                            java.util.Locale.ROOT), product);
                }
            } catch (RuntimeException ignored) {
                // Some custom factories expose fewer synthetic tech-level action lists.
            }
        }
        StrategicProductionDoctrine.AirBalance airBalance =
                StrategicProductionDoctrine.assessAirBalance(situation);
        for (UnitType product : products.values()) {
            AiUnitTypeCapabilities capabilities = AiUnitTypeCapabilities.capture(product);
            double score = plannedUnitValue(capabilities, role, frontState,
                    airBalance, credits);
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
            StrategicProductionDoctrine.AirBalance airBalance, double credits) {
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
        double score = Math.log1p(power) * 1.65D + efficiency * 38.0D
                + capabilities.techLevel() * 0.38D;
        boolean air = capabilities.movementDomain() == AiMovementDomain.AIR;
        if (role == TeamPositionDoctrine.Role.MOBILE_SUPPORT) {
            score += air ? 6.0D : -6.0D;
            if (airBalance != StrategicProductionDoctrine.AirBalance.SUPERIORITY) {
                score += capabilities.airToAirSpecialist() ? 7.0D : -5.0D;
            }
        } else {
            score += air ? -4.5D : 1.2D;
            if (role == TeamPositionDoctrine.Role.FRONTLINE) {
                score += Math.min(3.5D, capabilities.maximumAttackRange() / 95.0D);
                if (frontState != null
                        && frontState.mode() == StrategicFrontState.Mode.ATTRITION) {
                    score += capabilities.maximumAttackRange() / 70.0D;
                    score += Math.log1p(durability) * 0.28D;
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
            StrategicTeamPlan teamPlan, StrategicResourceCampaign campaign,
            List<UnitView> own, int producers) {
        double credits = context.team().credits();
        int desired = credits >= 18000.0D ? 4 : credits >= 8500.0D ? 3
                : credits >= 3200.0D ? 2 : 1;
        int combat = 0;
        for (UnitView unit : own) {
            if (unit.alive() && !unit.building()
                    && AiUnitCapabilities.capture(unit).mobileCombatUnit()) combat++;
        }
        desired = Math.max(desired, Math.min(4, 1 + combat / 12));
        if (teamPlan.leadsFrontline() && credits >= 2400.0D) desired = Math.max(2, desired);
        if (producers < desired) {
            ensureRoleProduction(context, situation, builders, cycle,
                    teamPlan, campaign, own);
        }
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
        int wanted = 3;
        ArrayList<UnitView> moving = new ArrayList<UnitView>();
        for (Builder builder : builders) {
            if (fortified && nearby + moving.size() >= wanted) break;
            UnitView unit = builder.capabilities.unit();
            float dx = unit.x() - staging.x();
            float dy = unit.y() - staging.y();
            if (dx * dx + dy * dy <= 45.0F * 45.0F) continue;
            moving.add(unit);
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
        if (role == TeamPositionDoctrine.Role.ECONOMY_TECH) {
            return type -> declaresProductionDomain(type, AiMovementDomain.OVER_CLIFF);
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
        for (int index = 0; index < Math.min(3, repairers.size()); index++) {
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
        float ownRange = tower.raw() instanceof Unit
                ? Math.max(90.0F, AiUnitTypeCapabilities.capture(
                ((Unit) tower.raw()).r()).maximumAttackRange()) : 150.0F;
        WorldPoint fallback = pathAwareTowerPlacement(situation, home, campaign.point(),
                ownRange, true, 0.0F, 0.0F);
        ArrayList<UnitView> withdrawing = new ArrayList<UnitView>();
        for (UnitView unit : own) {
            AiUnitCapabilities capabilities = AiUnitCapabilities.capture(unit);
            if (!unit.alive() || !capabilities.builder() || !capabilities.movable()) continue;
            float bx = unit.x() - tower.x();
            float by = unit.y() - tower.y();
            if (bx * bx + by * by <= 260.0F * 260.0F) withdrawing.add(unit);
            if (withdrawing.size() >= 3) break;
        }
        if (!withdrawing.isEmpty()) {
            context.orders().move(withdrawing, fallback.x(), fallback.y());
        }
        abandonedForwardTowerId = tower.id();
        forceLockFortification = true;
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

    private boolean ensureFrontierFortification(AiTickContext context,
            AiStrategicMapSnapshot situation, List<Builder> builders, long cycle,
            StrategicResourceCampaign campaign) {
        if (!campaign.allowsFortification() || campaign.point() == null) return false;
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
                if (isStaticDefense(action.getBuildUnitType())) {
                    choices.add(new BuildChoice(builder, action));
                }
            }
        }
        choices.sort(Comparator.comparingInt((BuildChoice value) -> value.action.getCreditCost())
                .thenComparing(value -> safe(value.action.getBuildUnitType().getInternalName()))
                .thenComparingLong(value -> value.builder.capabilities.unit().id()));
        for (BuildChoice choice : choices) {
            ForwardTowerPoint point = findForwardTowerPoint(context, situation,
                    choice.builder, choice.action.getBuildUnitType(), campaign.point(), builders);
            if (point == null) continue;
            List<UnitView> participants = forwardBuildParticipants(
                    builders, choice.action.getBuildUnitType());
            context.orders().build(participants, point.x, point.y, choice.action);
            buildingReservations.put(point.reservationKey,
                    cycle + BUILDING_RESERVATION_CYCLES);
            frontierDefenseReservationUntil = cycle + BUILDING_RESERVATION_CYCLES;
            campaign.markFortificationOrdered();
            System.out.println("[Strategic AI] Planned forward-opening defense "
                    + safe(choice.action.getBuildUnitType().getInternalName())
                    + " mode=" + (point.lockMode ? "lock" : "contest")
                    + " at " + (int) point.x + "," + (int) point.y);
            forceLockFortification = false;
            return true;
        }
        return false;
    }

    private static List<UnitView> forwardBuildParticipants(List<Builder> builders,
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
            if (canBuild) result.add(builder.capabilities.unit());
            if (result.size() >= 3) break;
        }
        return result;
    }

    private ForwardTowerPoint findForwardTowerPoint(AiTickContext context,
            AiStrategicMapSnapshot situation, Builder builder, UnitType tower,
            WorldPoint resource, List<Builder> ownBuilders) {
        WorldPoint home = primaryBase != null ? primaryBase.anchor()
                : new WorldPoint(builder.capabilities.unit().x(),
                builder.capabilities.unit().y());
        float towerRange = Math.max(90.0F,
                AiUnitTypeCapabilities.capture(tower).maximumAttackRange());
        WorldPoint contestPoint = pathAwareTowerPlacement(
                situation, home, resource, towerRange, false, 0.0F, 0.0F);
        boolean lock = forceLockFortification
                || enemyWinsBuilderRace(situation, resource, ownBuilders, tower)
                || enemyTowerControls(situation.world().enemies(),
                contestPoint.x(), contestPoint.y(), 25.0F);
        float[] lateral = {0.0F, -42.0F, 42.0F, -78.0F, 78.0F};
        float[] radial = {0.0F, -24.0F, 24.0F, -48.0F, 48.0F};
        AiTerrainCell origin = situation.terrain().cellAtWorld(
                builder.capabilities.unit().x(), builder.capabilities.unit().y());
        for (float radialOffset : radial) {
            for (float lateralOffset : lateral) {
                WorldPoint candidate = pathAwareTowerPlacement(situation, home, resource,
                        towerRange, lock, radialOffset, lateralOffset);
                float x = candidate.x();
                float y = candidate.y();
                if (lock) {
                    float toResource = (float) Math.hypot(x - resource.x(), y - resource.y());
                    if (toResource > towerRange - 18.0F) continue;
                }
                if (enemyTowerControls(situation.world().enemies(), x, y, 18.0F)) continue;
                long key = placementKey(situation, x, y);
                if (buildingReservations.containsKey(key)) continue;
                AiTerrainCell target = situation.terrain().cellAtWorld(x, y);
                if (!situation.terrain().sameRegion(origin, target,
                        builder.capabilities.movementDomain())) continue;
                if (UnitTypes.canSpawnStarting(tower, x, y, 0.0F, 0.0F,
                        context.rawTeam())) return new ForwardTowerPoint(x, y, key, lock);
            }
        }
        return null;
    }

    private static WorldPoint pathAwareTowerPlacement(AiStrategicMapSnapshot situation,
            WorldPoint home, WorldPoint resource, float towerRange,
            boolean lockMode, float radialOffset, float lateralOffset) {
        AiTerrainRouteMap routes = situation.terrain().routesFrom(
                home, AiMovementDomain.LAND);
        List<WorldPoint> path = routes.pathTo(resource);
        if (path.size() < 2) {
            return ForwardTowerGeometry.placement(home, resource, towerRange,
                    lockMode, radialOffset, lateralOffset);
        }
        WorldPoint end = path.get(path.size() - 1);
        WorldPoint previous = path.get(path.size() - 2);
        float dx = end.x() - previous.x();
        float dy = end.y() - previous.y();
        float length = (float) Math.hypot(dx, dy);
        if (length < 1.0F) { dx = 1.0F; dy = 0.0F; }
        else { dx /= length; dy /= length; }
        float baseForward = lockMode
                ? -Math.min(towerRange - 28.0F, towerRange * 0.72F) : 55.0F;
        float x = resource.x() + dx * (baseForward + radialOffset) - dy * lateralOffset;
        float y = resource.y() + dy * (baseForward + radialOffset) + dx * lateralOffset;
        AiTerrainCell cell = situation.terrain().cellAtWorld(x, y);
        return cell != null ? cell.representativePoint(AiMovementDomain.LAND)
                .orElse(new WorldPoint(x, y)) : new WorldPoint(x, y);
    }

    private static WorldPoint situationRoutePoint(AiTerrainRouteMap routes,
            WorldPoint destination, float distanceBefore, WorldPoint fallback) {
        return routes.pointBefore(destination, distanceBefore).orElse(fallback);
    }

    private static boolean enemyWinsBuilderRace(AiStrategicMapSnapshot situation,
            WorldPoint point, List<Builder> ownBuilders, UnitType tower) {
        Unit target = UnitTypes.createUnregisteredPrototype(tower);
        float own = Float.POSITIVE_INFINITY;
        for (Builder builder : ownBuilders) {
            own = Math.min(own, builderCompletionTime(situation,
                    builder.capabilities.unit(), builder.capabilities, point, target));
        }
        float enemy = Float.POSITIVE_INFINITY;
        for (UnitView unit : situation.world().enemies()) {
            AiUnitCapabilities capabilities = AiUnitCapabilities.capture(unit);
            if (!capabilities.builder() || !capabilities.movable()) continue;
            enemy = Math.min(enemy, builderCompletionTime(
                    situation, unit, capabilities, point, target));
        }
        return Float.isFinite(enemy) && (!Float.isFinite(own) || own > enemy * 1.06F + 18.0F);
    }

    private static float builderCompletionTime(AiStrategicMapSnapshot situation,
            UnitView builder, AiUnitCapabilities capabilities,
            WorldPoint point, Unit target) {
        float route = routeCost(situation, builder, point,
                capabilities.movementDomain());
        if (!Float.isFinite(route)) return Float.POSITIVE_INFINITY;
        float speed = Math.max(0.1F, capabilities.movementSpeed());
        float construction = 1000.0F;
        if (builder.raw() instanceof OrderableUnit) {
            try {
                float progress = ((OrderableUnit) builder.raw())
                        .getBuildProgressSpeedForTarget(target);
                if (Float.isFinite(progress) && progress > 0.0F) {
                    construction = 1.0F / progress;
                }
            } catch (RuntimeException ignored) {
                // Keep the conservative fallback when a custom builder requires live target state.
            }
        }
        return route / speed + construction;
    }

    private static float routeCost(AiStrategicMapSnapshot situation, UnitView unit,
            WorldPoint point, AiMovementDomain domain) {
        java.util.OptionalDouble cost = situation.terrain().routesFrom(
                new WorldPoint(unit.x(), unit.y()), domain).costTo(point);
        return cost.isPresent() ? (float) cost.getAsDouble() : Float.POSITIVE_INFINITY;
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

    private static List<Builder> builders(List<UnitView> own) {
        ArrayList<Builder> result = new ArrayList<Builder>();
        for (UnitView unit : own) {
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

    private static List<Builder> buildersForBase(List<Builder> builders,
            StrategicResourceCampaign campaign, StrategicTeamPlan teamPlan) {
        if (!teamPlan.leadsFrontline() || !campaign.active()
                || campaign.point() == null || builders.size() <= 1) return builders;
        ArrayList<Builder> byFrontDistance = new ArrayList<Builder>(builders);
        WorldPoint front = campaign.point();
        byFrontDistance.sort(Comparator.comparingDouble((Builder builder) -> {
            UnitView unit = builder.capabilities.unit();
            float dx = unit.x() - front.x();
            float dy = unit.y() - front.y();
            return dx * dx + dy * dy;
        }).thenComparingLong(builder -> builder.capabilities.unit().id()));
        java.util.HashSet<Long> reserved = new java.util.HashSet<Long>();
        for (int index = 0; index < Math.min(3, byFrontDistance.size()); index++) {
            UnitView unit = byFrontDistance.get(index).capabilities.unit();
            float dx = unit.x() - front.x();
            float dy = unit.y() - front.y();
            if (dx * dx + dy * dy <= 520.0F * 520.0F) reserved.add(unit.id());
        }
        ArrayList<Builder> result = new ArrayList<Builder>();
        for (Builder builder : builders) {
            if (!reserved.contains(builder.capabilities.unit().id())) result.add(builder);
        }
        return result;
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

    private int countDedicatedCombatProducers(List<UnitView> units) {
        int count = 0;
        for (UnitView unit : units) {
            if (!unit.alive()) continue;
            if (primaryBase != null && unit.id() == primaryBase.anchorUnitId()) continue;
            if (unit.building() && unit.raw() instanceof Unit
                    && offersMobileCombat((Unit) unit.raw())) count++;
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

    private static boolean isStaticDefense(UnitType building) {
        return building != null && building.isBuilding()
                && AiUnitTypeCapabilities.capture(building).attacker()
                && !declaresCombatProduction(building);
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
                    -defenseTerrainScore(situation, point)));
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

    private static float defenseTerrainScore(AiStrategicMapSnapshot situation,
            WorldPoint point) {
        AiTerrainCell cell = situation.terrain().cellAtWorld(point.x(), point.y());
        if (cell == null) return Float.NEGATIVE_INFINITY;
        return cell.landChokeScore() * 0.65F
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
        final boolean lockMode;

        ForwardTowerPoint(float x, float y, long reservationKey, boolean lockMode) {
            super(x, y, reservationKey);
            this.lockMode = lockMode;
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

    private static final class FactoryPlanCandidate {
        final UnitType factory;
        final int cost;
        boolean existing;
        UnitType unit;
        double unitScore = Double.NEGATIVE_INFINITY;
        double score = Double.NEGATIVE_INFINITY;

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
