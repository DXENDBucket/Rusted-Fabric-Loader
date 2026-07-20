package io.github.endx.rustedfabricapi.api.unit.build.event;

import io.github.endx.rustedfabricapi.api.event.RustedFabricEvent;
import rustedwarfare.unit.Unit;
import rustedwarfare.unit.action.UnitAction;
import rustedwarfare.unit.build.BuildQueueHost;
import rustedwarfare.unit.build.BuildQueueItem;
import rustedwarfare.unit.build.FactoryQueueManager;

/** Strongly typed factory/build-queue lifecycle events. */
public final class BuildQueueEvents {
    public static final RustedFabricEvent<BeforeActionApply> BEFORE_ACTION_APPLY =
            RustedFabricEvent.create(listeners ->
                    (queue, action, front, targetX, targetY, hasTargetPoint, target) -> {
                boolean cancelled = false;
                for (BeforeActionApply listener : listeners) {
                    cancelled |= listener.beforeActionApply(queue, action, front,
                            targetX, targetY, hasTargetPoint, target);
                }
                return cancelled;
            });

    public static final RustedFabricEvent<AfterActionApply> AFTER_ACTION_APPLY =
            RustedFabricEvent.create(listeners ->
                    (queue, action, front, targetX, targetY, hasTargetPoint, target, item) -> {
                for (AfterActionApply listener : listeners) {
                    listener.afterActionApply(queue, action, front,
                            targetX, targetY, hasTargetPoint, target, item);
                }
            });

    public static final RustedFabricEvent<QueueItem> BEFORE_ITEM_ACTIVATE = queueItemEvent();
    public static final RustedFabricEvent<QueueItem> AFTER_ITEM_ACTIVATE = queueItemEvent();
    public static final RustedFabricEvent<BeforeClear> BEFORE_CLEAR =
            RustedFabricEvent.create(listeners -> (queue, refund) -> {
                boolean cancelled = false;
                for (BeforeClear listener : listeners) {
                    cancelled |= listener.beforeClear(queue, refund);
                }
                return cancelled;
            });
    public static final RustedFabricEvent<AfterClear> AFTER_CLEAR =
            RustedFabricEvent.create(listeners -> (queue, refund) -> {
                for (AfterClear listener : listeners) {
                    listener.afterClear(queue, refund);
                }
            });
    public static final RustedFabricEvent<BeforeQueueItem> BEFORE_ITEM_REFUND = beforeQueueItemEvent();
    public static final RustedFabricEvent<QueueItem> AFTER_ITEM_REFUND = queueItemEvent();

    public static final RustedFabricEvent<AfterComplete> AFTER_ITEM_COMPLETE =
            RustedFabricEvent.create(listeners ->
                    (queue, item, spacing, rally, yOffset, producedUnit) -> {
                        for (AfterComplete listener : listeners) {
                            listener.afterComplete(queue, item, spacing, rally, yOffset, producedUnit);
                        }
                    });

    public static final RustedFabricEvent<AfterPositioned> AFTER_PRODUCED_UNIT_POSITIONED =
            RustedFabricEvent.create(listeners -> (queue, unit, spacing, rally) -> {
                for (AfterPositioned listener : listeners) {
                    listener.afterPositioned(queue, unit, spacing, rally);
                }
            });

    public static final RustedFabricEvent<BeforeHostItem> BEFORE_HOST_ITEM_COMPLETE = beforeHostEvent();
    public static final RustedFabricEvent<HostItem> AFTER_HOST_ITEM_COMPLETE = hostEvent();
    public static final RustedFabricEvent<HostItem> AFTER_HOST_ITEM_ACTIVATE = hostEvent();
    public static final RustedFabricEvent<ModifyHostRefundable> MODIFY_HOST_ITEM_REFUNDABLE =
            RustedFabricEvent.create(listeners -> (host, item, current) -> {
                Boolean result = Boolean.valueOf(current);
                for (ModifyHostRefundable listener : listeners) {
                    Boolean replacement = listener.modify(host, item, result.booleanValue());
                    if (replacement != null) {
                        result = replacement;
                    }
                }
                return result;
            });

    private BuildQueueEvents() {
    }

    private static RustedFabricEvent<QueueItem> queueItemEvent() {
        return RustedFabricEvent.create(listeners -> (queue, item) -> {
            for (QueueItem listener : listeners) {
                listener.onQueueItem(queue, item);
            }
        });
    }

    private static RustedFabricEvent<BeforeQueueItem> beforeQueueItemEvent() {
        return RustedFabricEvent.create(listeners -> (queue, item) -> {
            boolean cancelled = false;
            for (BeforeQueueItem listener : listeners) {
                cancelled |= listener.beforeQueueItem(queue, item);
            }
            return cancelled;
        });
    }

    private static RustedFabricEvent<BeforeHostItem> beforeHostEvent() {
        return RustedFabricEvent.create(listeners -> (host, item) -> {
            boolean cancelled = false;
            for (BeforeHostItem listener : listeners) {
                cancelled |= listener.beforeHostItem(host, item);
            }
            return cancelled;
        });
    }

    private static RustedFabricEvent<HostItem> hostEvent() {
        return RustedFabricEvent.create(listeners -> (host, item) -> {
            for (HostItem listener : listeners) {
                listener.onHostItem(host, item);
            }
        });
    }

    @FunctionalInterface
    public interface BeforeActionApply {
        boolean beforeActionApply(FactoryQueueManager queue, UnitAction action, boolean front,
                                  float targetX, float targetY, boolean hasTargetPoint,
                                  Unit targetUnit);
    }

    @FunctionalInterface
    public interface AfterActionApply {
        void afterActionApply(FactoryQueueManager queue, UnitAction action, boolean front,
                              float targetX, float targetY, boolean hasTargetPoint,
                              Unit targetUnit, BuildQueueItem item);
    }

    @FunctionalInterface
    public interface QueueItem {
        void onQueueItem(FactoryQueueManager queue, BuildQueueItem item);
    }

    @FunctionalInterface
    public interface BeforeQueueItem {
        boolean beforeQueueItem(FactoryQueueManager queue, BuildQueueItem item);
    }

    @FunctionalInterface
    public interface BeforeClear {
        boolean beforeClear(FactoryQueueManager queue, boolean refund);
    }

    @FunctionalInterface
    public interface AfterClear {
        void afterClear(FactoryQueueManager queue, boolean refund);
    }

    @FunctionalInterface
    public interface AfterComplete {
        void afterComplete(FactoryQueueManager queue, BuildQueueItem item, float spacing,
                           boolean useRallyPoint, float spawnYOffset, Unit producedUnit);
    }

    @FunctionalInterface
    public interface AfterPositioned {
        void afterPositioned(FactoryQueueManager queue, Unit unit, float spacing,
                             boolean useRallyPoint);
    }

    @FunctionalInterface
    public interface BeforeHostItem {
        boolean beforeHostItem(BuildQueueHost host, BuildQueueItem item);
    }

    @FunctionalInterface
    public interface HostItem {
        void onHostItem(BuildQueueHost host, BuildQueueItem item);
    }

    @FunctionalInterface
    public interface ModifyHostRefundable {
        /** Return {@code null} to retain {@code currentValue}. */
        Boolean modify(BuildQueueHost host, BuildQueueItem item, boolean currentValue);
    }
}
