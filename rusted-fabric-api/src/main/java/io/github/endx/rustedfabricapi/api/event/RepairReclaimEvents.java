package io.github.endx.rustedfabricapi.api.event;

public final class RepairReclaimEvents {
    private RepairReclaimEvents() {
    }

    public static final RustedFabricEvent<BeforeRepairReclaimOrderUpdate> BEFORE_REPAIR_RECLAIM_ORDER_UPDATE =
            RustedFabricEvent.create(listeners -> (unit, delta, waypoint, waypointState) -> {
                boolean cancelled = false;
                for (BeforeRepairReclaimOrderUpdate listener : listeners) {
                    cancelled |= listener.beforeRepairReclaimOrderUpdate(unit, delta, waypoint, waypointState);
                }
                return cancelled;
            });

    public static final RustedFabricEvent<AfterRepairReclaimOrderUpdate> AFTER_REPAIR_RECLAIM_ORDER_UPDATE =
            RustedFabricEvent.create(listeners -> (unit, delta, waypoint, waypointState) -> {
                for (AfterRepairReclaimOrderUpdate listener : listeners) {
                    listener.afterRepairReclaimOrderUpdate(unit, delta, waypoint, waypointState);
                }
            });

    public static final RustedFabricEvent<ModifyCanRepairTarget> MODIFY_CAN_REPAIR_TARGET =
            RustedFabricEvent.create(listeners -> (unit, target, vanillaResult) -> {
                Boolean result = Boolean.valueOf(vanillaResult);
                for (ModifyCanRepairTarget listener : listeners) {
                    Boolean override = listener.modifyCanRepairTarget(unit, target, result.booleanValue());
                    if (override != null) {
                        result = override;
                    }
                }
                return result;
            });

    public static final RustedFabricEvent<ModifyCanReclaimUnitTarget> MODIFY_CAN_RECLAIM_UNIT_TARGET =
            RustedFabricEvent.create(listeners -> (unit, target, vanillaResult) -> {
                Boolean result = Boolean.valueOf(vanillaResult);
                for (ModifyCanReclaimUnitTarget listener : listeners) {
                    Boolean override = listener.modifyCanReclaimUnitTarget(unit, target, result.booleanValue());
                    if (override != null) {
                        result = override;
                    }
                }
                return result;
            });

    public static final RustedFabricEvent<ModifyBuildProgressSpeed> MODIFY_BUILD_PROGRESS_SPEED =
            RustedFabricEvent.create(listeners -> (unit, target, vanillaSpeed) -> {
                Float result = Float.valueOf(vanillaSpeed);
                for (ModifyBuildProgressSpeed listener : listeners) {
                    Float override = listener.modifyBuildProgressSpeed(unit, target, result.floatValue());
                    if (override != null) {
                        result = override;
                    }
                }
                return result;
            });

    public static final RustedFabricEvent<ModifyUnbuildSpeed> MODIFY_UNBUILD_SPEED =
            RustedFabricEvent.create(listeners -> (unit, target, vanillaSpeed) -> {
                Float result = Float.valueOf(vanillaSpeed);
                for (ModifyUnbuildSpeed listener : listeners) {
                    Float override = listener.modifyUnbuildSpeed(unit, target, result.floatValue());
                    if (override != null) {
                        result = override;
                    }
                }
                return result;
            });

    public static final RustedFabricEvent<ModifyBuildPriceForTarget> MODIFY_BUILD_PRICE_FOR_TARGET =
            RustedFabricEvent.create(listeners -> (unit, target, currentPrice) -> {
                Object result = currentPrice;
                for (ModifyBuildPriceForTarget listener : listeners) {
                    Object override = listener.modifyBuildPriceForTarget(unit, target, result);
                    if (override != null) {
                        result = override;
                    }
                }
                return result;
            });

    public static final RustedFabricEvent<ModifyBaseReclaimPrice> MODIFY_BASE_RECLAIM_PRICE =
            RustedFabricEvent.create(listeners -> (unit, currentPrice) -> {
                Object result = currentPrice;
                for (ModifyBaseReclaimPrice listener : listeners) {
                    Object override = listener.modifyBaseReclaimPrice(unit, result);
                    if (override != null) {
                        result = override;
                    }
                }
                return result;
            });

    public static final RustedFabricEvent<ModifyReclaimPriceOverride> MODIFY_RECLAIM_PRICE_OVERRIDE =
            RustedFabricEvent.create(listeners -> (unit, currentPrice) -> {
                Object result = currentPrice;
                for (ModifyReclaimPriceOverride listener : listeners) {
                    Object override = listener.modifyReclaimPriceOverride(unit, result);
                    if (override != null) {
                        result = override;
                    }
                }
                return result;
            });

    public static final RustedFabricEvent<ModifySimilarResourcesTag> MODIFY_SIMILAR_RESOURCES_TAG =
            RustedFabricEvent.create(listeners -> (unit, currentTags) -> {
                Object result = currentTags;
                for (ModifySimilarResourcesTag listener : listeners) {
                    Object override = listener.modifySimilarResourcesTag(unit, result);
                    if (override != null) {
                        result = override;
                    }
                }
                return result;
            });

    public static final RustedFabricEvent<BeforeConstructionProgressSet> BEFORE_CONSTRUCTION_PROGRESS_SET =
            RustedFabricEvent.create(listeners -> (unit, progress) -> {
                boolean cancelled = false;
                for (BeforeConstructionProgressSet listener : listeners) {
                    cancelled |= listener.beforeConstructionProgressSet(unit, progress);
                }
                return cancelled;
            });

    public static final RustedFabricEvent<AfterConstructionProgressSet> AFTER_CONSTRUCTION_PROGRESS_SET =
            RustedFabricEvent.create(listeners -> (unit, progress) -> {
                for (AfterConstructionProgressSet listener : listeners) {
                    listener.afterConstructionProgressSet(unit, progress);
                }
            });

    public static final RustedFabricEvent<BeforeActiveResourceDeltaRefresh> BEFORE_ACTIVE_RESOURCE_DELTA_REFRESH =
            RustedFabricEvent.create(listeners -> unit -> {
                for (BeforeActiveResourceDeltaRefresh listener : listeners) {
                    listener.beforeActiveResourceDeltaRefresh(unit);
                }
            });

    public static final RustedFabricEvent<AfterActiveResourceDeltaRefresh> AFTER_ACTIVE_RESOURCE_DELTA_REFRESH =
            RustedFabricEvent.create(listeners -> unit -> {
                for (AfterActiveResourceDeltaRefresh listener : listeners) {
                    listener.afterActiveResourceDeltaRefresh(unit);
                }
            });

    public static final RustedFabricEvent<ModifyBuildQueueResourceDelta> MODIFY_BUILD_QUEUE_RESOURCE_DELTA =
            RustedFabricEvent.create(listeners -> (unit, currentDelta) -> {
                Object result = currentDelta;
                for (ModifyBuildQueueResourceDelta listener : listeners) {
                    Object override = listener.modifyBuildQueueResourceDelta(unit, result);
                    if (override != null) {
                        result = override;
                    }
                }
                return result;
            });

    public static final RustedFabricEvent<ModifyQueuedActionResourceDelta> MODIFY_QUEUED_ACTION_RESOURCE_DELTA =
            RustedFabricEvent.create(listeners -> (unit, currentDelta) -> {
                Object result = currentDelta;
                for (ModifyQueuedActionResourceDelta listener : listeners) {
                    Object override = listener.modifyQueuedActionResourceDelta(unit, result);
                    if (override != null) {
                        result = override;
                    }
                }
                return result;
            });

    public static final RustedFabricEvent<ModifyRepairReclaimResourceDelta> MODIFY_REPAIR_RECLAIM_RESOURCE_DELTA =
            RustedFabricEvent.create(listeners -> (unit, currentDelta) -> {
                Object result = currentDelta;
                for (ModifyRepairReclaimResourceDelta listener : listeners) {
                    Object override = listener.modifyRepairReclaimResourceDelta(unit, result);
                    if (override != null) {
                        result = override;
                    }
                }
                return result;
            });

    public static final RustedFabricEvent<ModifyNearestReclaimResourceTarget> MODIFY_NEAREST_RECLAIM_RESOURCE_TARGET =
            RustedFabricEvent.create(listeners -> (searcher, x, y, range, requiredTags, currentTarget) -> {
                Object result = currentTarget;
                for (ModifyNearestReclaimResourceTarget listener : listeners) {
                    Object override = listener.modifyNearestReclaimResourceTarget(searcher, x, y, range, requiredTags, result);
                    if (override != null) {
                        result = override;
                    }
                }
                return result;
            });

    @FunctionalInterface
    public interface BeforeRepairReclaimOrderUpdate {
        boolean beforeRepairReclaimOrderUpdate(Object unit, float delta, Object waypoint, Object waypointState);
    }

    @FunctionalInterface
    public interface AfterRepairReclaimOrderUpdate {
        void afterRepairReclaimOrderUpdate(Object unit, float delta, Object waypoint, Object waypointState);
    }

    @FunctionalInterface
    public interface ModifyCanRepairTarget {
        Boolean modifyCanRepairTarget(Object unit, Object target, boolean currentResult);
    }

    @FunctionalInterface
    public interface ModifyCanReclaimUnitTarget {
        Boolean modifyCanReclaimUnitTarget(Object unit, Object target, boolean currentResult);
    }

    @FunctionalInterface
    public interface ModifyBuildProgressSpeed {
        Float modifyBuildProgressSpeed(Object unit, Object target, float currentSpeed);
    }

    @FunctionalInterface
    public interface ModifyUnbuildSpeed {
        Float modifyUnbuildSpeed(Object unit, Object target, float currentSpeed);
    }

    @FunctionalInterface
    public interface ModifyBuildPriceForTarget {
        Object modifyBuildPriceForTarget(Object unit, Object target, Object currentPrice);
    }

    @FunctionalInterface
    public interface ModifyBaseReclaimPrice {
        Object modifyBaseReclaimPrice(Object unit, Object currentPrice);
    }

    @FunctionalInterface
    public interface ModifyReclaimPriceOverride {
        Object modifyReclaimPriceOverride(Object unit, Object currentPrice);
    }

    @FunctionalInterface
    public interface ModifySimilarResourcesTag {
        Object modifySimilarResourcesTag(Object unit, Object currentTags);
    }

    @FunctionalInterface
    public interface BeforeConstructionProgressSet {
        boolean beforeConstructionProgressSet(Object unit, float progress);
    }

    @FunctionalInterface
    public interface AfterConstructionProgressSet {
        void afterConstructionProgressSet(Object unit, float progress);
    }

    @FunctionalInterface
    public interface BeforeActiveResourceDeltaRefresh {
        void beforeActiveResourceDeltaRefresh(Object unit);
    }

    @FunctionalInterface
    public interface AfterActiveResourceDeltaRefresh {
        void afterActiveResourceDeltaRefresh(Object unit);
    }

    @FunctionalInterface
    public interface ModifyBuildQueueResourceDelta {
        Object modifyBuildQueueResourceDelta(Object unit, Object currentDelta);
    }

    @FunctionalInterface
    public interface ModifyQueuedActionResourceDelta {
        Object modifyQueuedActionResourceDelta(Object unit, Object currentDelta);
    }

    @FunctionalInterface
    public interface ModifyRepairReclaimResourceDelta {
        Object modifyRepairReclaimResourceDelta(Object unit, Object currentDelta);
    }

    @FunctionalInterface
    public interface ModifyNearestReclaimResourceTarget {
        Object modifyNearestReclaimResourceTarget(Object searcher, float x, float y, float range,
                                                  Object requiredTags, Object currentTarget);
    }
}
