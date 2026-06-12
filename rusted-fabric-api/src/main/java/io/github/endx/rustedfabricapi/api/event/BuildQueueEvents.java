package io.github.endx.rustedfabricapi.api.event;

public final class BuildQueueEvents {
    private BuildQueueEvents() {
    }

    public static final RustedFabricEvent<BeforeQueueActionApply> BEFORE_QUEUE_ACTION_APPLY =
            RustedFabricEvent.create(listeners -> (queue, action, front, targetPoint, targetUnit) -> {
                boolean cancelled = false;
                for (BeforeQueueActionApply listener : listeners) {
                    cancelled |= listener.beforeQueueActionApply(queue, action, front, targetPoint, targetUnit);
                }
                return cancelled;
            });

    public static final RustedFabricEvent<AfterQueueActionApply> AFTER_QUEUE_ACTION_APPLY =
            RustedFabricEvent.create(listeners -> (queue, action, front, targetPoint, targetUnit, queueItem) -> {
                for (AfterQueueActionApply listener : listeners) {
                    listener.afterQueueActionApply(queue, action, front, targetPoint, targetUnit, queueItem);
                }
            });

    public static final RustedFabricEvent<BeforeQueueItemActivate> BEFORE_QUEUE_ITEM_ACTIVATE =
            RustedFabricEvent.create(listeners -> (queue, queueItem) -> {
                for (BeforeQueueItemActivate listener : listeners) {
                    listener.beforeQueueItemActivate(queue, queueItem);
                }
            });

    public static final RustedFabricEvent<AfterQueueItemActivate> AFTER_QUEUE_ITEM_ACTIVATE =
            RustedFabricEvent.create(listeners -> (queue, queueItem) -> {
                for (AfterQueueItemActivate listener : listeners) {
                    listener.afterQueueItemActivate(queue, queueItem);
                }
            });

    public static final RustedFabricEvent<BeforeQueueClearAndRefund> BEFORE_QUEUE_CLEAR_AND_REFUND =
            RustedFabricEvent.create(listeners -> (queue, refund) -> {
                boolean cancelled = false;
                for (BeforeQueueClearAndRefund listener : listeners) {
                    cancelled |= listener.beforeQueueClearAndRefund(queue, refund);
                }
                return cancelled;
            });

    public static final RustedFabricEvent<AfterQueueClearAndRefund> AFTER_QUEUE_CLEAR_AND_REFUND =
            RustedFabricEvent.create(listeners -> (queue, refund) -> {
                for (AfterQueueClearAndRefund listener : listeners) {
                    listener.afterQueueClearAndRefund(queue, refund);
                }
            });

    public static final RustedFabricEvent<BeforeQueueItemRefund> BEFORE_QUEUE_ITEM_REFUND =
            RustedFabricEvent.create(listeners -> (queue, queueItem) -> {
                boolean cancelled = false;
                for (BeforeQueueItemRefund listener : listeners) {
                    cancelled |= listener.beforeQueueItemRefund(queue, queueItem);
                }
                return cancelled;
            });

    public static final RustedFabricEvent<AfterQueueItemRefund> AFTER_QUEUE_ITEM_REFUND =
            RustedFabricEvent.create(listeners -> (queue, queueItem) -> {
                for (AfterQueueItemRefund listener : listeners) {
                    listener.afterQueueItemRefund(queue, queueItem);
                }
            });

    public static final RustedFabricEvent<AfterQueueItemComplete> AFTER_QUEUE_ITEM_COMPLETE =
            RustedFabricEvent.create(listeners -> (queue, queueItem, spacing, useRallyPoint, spawnYOffset, producedUnit) -> {
                for (AfterQueueItemComplete listener : listeners) {
                    listener.afterQueueItemComplete(queue, queueItem, spacing, useRallyPoint, spawnYOffset, producedUnit);
                }
            });

    public static final RustedFabricEvent<AfterNewlyProducedUnitPositioned> AFTER_NEWLY_PRODUCED_UNIT_POSITIONED =
            RustedFabricEvent.create(listeners -> (queue, unit, spacing, useRallyPoint) -> {
                for (AfterNewlyProducedUnitPositioned listener : listeners) {
                    listener.afterNewlyProducedUnitPositioned(queue, unit, spacing, useRallyPoint);
                }
            });

    public static final RustedFabricEvent<BeforeHostBuildQueueItemComplete> BEFORE_HOST_BUILD_QUEUE_ITEM_COMPLETE =
            RustedFabricEvent.create(listeners -> (host, queueItem) -> {
                boolean cancelled = false;
                for (BeforeHostBuildQueueItemComplete listener : listeners) {
                    cancelled |= listener.beforeHostBuildQueueItemComplete(host, queueItem);
                }
                return cancelled;
            });

    public static final RustedFabricEvent<AfterHostBuildQueueItemComplete> AFTER_HOST_BUILD_QUEUE_ITEM_COMPLETE =
            RustedFabricEvent.create(listeners -> (host, queueItem) -> {
                for (AfterHostBuildQueueItemComplete listener : listeners) {
                    listener.afterHostBuildQueueItemComplete(host, queueItem);
                }
            });

    public static final RustedFabricEvent<AfterHostBuildQueueItemActivate> AFTER_HOST_BUILD_QUEUE_ITEM_ACTIVATE =
            RustedFabricEvent.create(listeners -> (host, queueItem) -> {
                for (AfterHostBuildQueueItemActivate listener : listeners) {
                    listener.afterHostBuildQueueItemActivate(host, queueItem);
                }
            });

    public static final RustedFabricEvent<ModifyHostBuildQueueItemRefundable> MODIFY_HOST_BUILD_QUEUE_ITEM_REFUNDABLE =
            RustedFabricEvent.create(listeners -> (host, queueItem, vanillaResult) -> {
                Boolean result = Boolean.valueOf(vanillaResult);
                for (ModifyHostBuildQueueItemRefundable listener : listeners) {
                    Boolean override = listener.modifyHostBuildQueueItemRefundable(host, queueItem, result.booleanValue());
                    if (override != null) {
                        result = override;
                    }
                }
                return result;
            });

    @FunctionalInterface
    public interface BeforeQueueActionApply {
        boolean beforeQueueActionApply(Object queue, Object action, boolean front, Object targetPoint, Object targetUnit);
    }

    @FunctionalInterface
    public interface AfterQueueActionApply {
        void afterQueueActionApply(Object queue, Object action, boolean front, Object targetPoint, Object targetUnit,
                                   Object queueItem);
    }

    @FunctionalInterface
    public interface BeforeQueueItemActivate {
        void beforeQueueItemActivate(Object queue, Object queueItem);
    }

    @FunctionalInterface
    public interface AfterQueueItemActivate {
        void afterQueueItemActivate(Object queue, Object queueItem);
    }

    @FunctionalInterface
    public interface BeforeQueueClearAndRefund {
        boolean beforeQueueClearAndRefund(Object queue, boolean refund);
    }

    @FunctionalInterface
    public interface AfterQueueClearAndRefund {
        void afterQueueClearAndRefund(Object queue, boolean refund);
    }

    @FunctionalInterface
    public interface BeforeQueueItemRefund {
        boolean beforeQueueItemRefund(Object queue, Object queueItem);
    }

    @FunctionalInterface
    public interface AfterQueueItemRefund {
        void afterQueueItemRefund(Object queue, Object queueItem);
    }

    @FunctionalInterface
    public interface AfterQueueItemComplete {
        void afterQueueItemComplete(Object queue, Object queueItem, float spacing, boolean useRallyPoint,
                                    float spawnYOffset, Object producedUnit);
    }

    @FunctionalInterface
    public interface AfterNewlyProducedUnitPositioned {
        void afterNewlyProducedUnitPositioned(Object queue, Object unit, float spacing, boolean useRallyPoint);
    }

    @FunctionalInterface
    public interface BeforeHostBuildQueueItemComplete {
        boolean beforeHostBuildQueueItemComplete(Object host, Object queueItem);
    }

    @FunctionalInterface
    public interface AfterHostBuildQueueItemComplete {
        void afterHostBuildQueueItemComplete(Object host, Object queueItem);
    }

    @FunctionalInterface
    public interface AfterHostBuildQueueItemActivate {
        void afterHostBuildQueueItemActivate(Object host, Object queueItem);
    }

    @FunctionalInterface
    public interface ModifyHostBuildQueueItemRefundable {
        Boolean modifyHostBuildQueueItemRefundable(Object host, Object queueItem, boolean currentResult);
    }
}
