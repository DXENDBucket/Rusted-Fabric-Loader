package io.github.endx.rustedfabricapi.api.unit.repair.event;

import io.github.endx.rustedfabricapi.api.event.RustedFabricEvent;
import rustedwarfare.custom.CustomTagList;
import rustedwarfare.custom.resource.ResourceAmount;
import rustedwarfare.unit.OrderableUnit;
import rustedwarfare.unit.Unit;
import rustedwarfare.unit.UnitOrder;

/** Strongly typed repair, reclaim, construction and resource-delta events. */
public final class RepairReclaimEvents {
    public static final RustedFabricEvent<BeforeOrderUpdate> BEFORE_ORDER_UPDATE =
            RustedFabricEvent.create(listeners -> (unit, delta, order) -> {
                boolean cancelled = false;
                for (BeforeOrderUpdate listener : listeners) {
                    cancelled |= listener.beforeUpdate(unit, delta, order);
                }
                return cancelled;
            });
    public static final RustedFabricEvent<AfterOrderUpdate> AFTER_ORDER_UPDATE =
            RustedFabricEvent.create(listeners -> (unit, delta, order) -> {
                for (AfterOrderUpdate listener : listeners) listener.afterUpdate(unit, delta, order);
            });
    public static final RustedFabricEvent<ModifyTargetDecision> MODIFY_CAN_REPAIR = targetDecisionEvent();
    public static final RustedFabricEvent<ModifyTargetDecision> MODIFY_CAN_RECLAIM = targetDecisionEvent();
    public static final RustedFabricEvent<ModifySpeed> MODIFY_BUILD_PROGRESS_SPEED = speedEvent();
    public static final RustedFabricEvent<ModifySpeed> MODIFY_UNBUILD_SPEED = speedEvent();
    public static final RustedFabricEvent<ModifyTargetResourceAmount> MODIFY_BUILD_PRICE =
            targetResourceAmountEvent();
    public static final RustedFabricEvent<ModifyUnitResourceAmount> MODIFY_BASE_RECLAIM_PRICE =
            unitResourceAmountEvent();
    public static final RustedFabricEvent<ModifyUnitResourceAmount> MODIFY_RECLAIM_PRICE_OVERRIDE =
            unitResourceAmountEvent();
    public static final RustedFabricEvent<ModifyUnitResourceAmount> MODIFY_REPAIR_RECLAIM_RESOURCE_DELTA =
            unitResourceAmountEvent();
    public static final RustedFabricEvent<BeforeConstructionProgressSet> BEFORE_CONSTRUCTION_PROGRESS_SET =
            RustedFabricEvent.create(listeners -> (unit, progress) -> {
                boolean cancelled = false;
                for (BeforeConstructionProgressSet listener : listeners) {
                    cancelled |= listener.beforeSet(unit, progress);
                }
                return cancelled;
            });
    public static final RustedFabricEvent<AfterConstructionProgressSet> AFTER_CONSTRUCTION_PROGRESS_SET =
            RustedFabricEvent.create(listeners -> (unit, progress) -> {
                for (AfterConstructionProgressSet listener : listeners) listener.afterSet(unit, progress);
            });
    public static final RustedFabricEvent<ModifyNearestResourceTarget> MODIFY_NEAREST_RESOURCE_TARGET =
            RustedFabricEvent.create(listeners -> (searcher, x, y, range, tags, current) -> {
                Unit result = current;
                for (ModifyNearestResourceTarget listener : listeners) {
                    Unit replacement = listener.modify(searcher, x, y, range, tags, result);
                    if (replacement != null) result = replacement;
                }
                return result;
            });

    private RepairReclaimEvents() {
    }

    private static RustedFabricEvent<ModifyTargetDecision> targetDecisionEvent() {
        return RustedFabricEvent.create(listeners -> (unit, target, current) -> {
            Boolean result = Boolean.valueOf(current);
            for (ModifyTargetDecision listener : listeners) {
                Boolean replacement = listener.modify(unit, target, result.booleanValue());
                if (replacement != null) result = replacement;
            }
            return result;
        });
    }

    private static RustedFabricEvent<ModifySpeed> speedEvent() {
        return RustedFabricEvent.create(listeners -> (unit, target, current) -> {
            Float result = Float.valueOf(current);
            for (ModifySpeed listener : listeners) {
                Float replacement = listener.modify(unit, target, result.floatValue());
                if (replacement != null) result = replacement;
            }
            return result;
        });
    }

    private static RustedFabricEvent<ModifyTargetResourceAmount> targetResourceAmountEvent() {
        return RustedFabricEvent.create(listeners -> (unit, target, current) -> {
            ResourceAmount result = current;
            for (ModifyTargetResourceAmount listener : listeners) {
                ResourceAmount replacement = listener.modify(unit, target, result);
                if (replacement != null) result = replacement;
            }
            return result;
        });
    }

    private static RustedFabricEvent<ModifyUnitResourceAmount> unitResourceAmountEvent() {
        return RustedFabricEvent.create(listeners -> (unit, current) -> {
            ResourceAmount result = current;
            for (ModifyUnitResourceAmount listener : listeners) {
                ResourceAmount replacement = listener.modify(unit, result);
                if (replacement != null) result = replacement;
            }
            return result;
        });
    }

    @FunctionalInterface
    public interface BeforeOrderUpdate {
        boolean beforeUpdate(OrderableUnit unit, float delta, UnitOrder order);
    }

    @FunctionalInterface
    public interface AfterOrderUpdate {
        void afterUpdate(OrderableUnit unit, float delta, UnitOrder order);
    }

    @FunctionalInterface
    public interface ModifyTargetDecision {
        /** Return {@code null} to retain {@code currentResult}. */
        Boolean modify(OrderableUnit unit, Unit target, boolean currentResult);
    }

    @FunctionalInterface
    public interface ModifySpeed {
        /** Return {@code null} to retain {@code currentSpeed}. */
        Float modify(OrderableUnit unit, Unit target, float currentSpeed);
    }

    @FunctionalInterface
    public interface ModifyTargetResourceAmount {
        /** Return {@code null} to retain {@code current}. */
        ResourceAmount modify(OrderableUnit unit, Unit target, ResourceAmount current);
    }

    @FunctionalInterface
    public interface ModifyUnitResourceAmount {
        /** Return {@code null} to retain {@code current}. */
        ResourceAmount modify(Unit unit, ResourceAmount current);
    }

    @FunctionalInterface
    public interface BeforeConstructionProgressSet {
        boolean beforeSet(Unit unit, float progress);
    }

    @FunctionalInterface
    public interface AfterConstructionProgressSet {
        void afterSet(Unit unit, float progress);
    }

    @FunctionalInterface
    public interface ModifyNearestResourceTarget {
        /** Return {@code null} to retain {@code currentTarget}. */
        Unit modify(OrderableUnit searcher, float x, float y, float range,
                    CustomTagList requiredTags, Unit currentTarget);
    }
}
