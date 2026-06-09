package io.github.endx.rustedfabricapi.api.event;

public final class ResourceRuntimeEvents {
    public static final RustedFabricEvent<BeforeResourceAmountSubtract> BEFORE_RESOURCE_AMOUNT_SUBTRACT =
            RustedFabricEvent.create(listeners -> (resourceAmount, unit, scale, scaled, operation) -> {
                boolean cancelled = false;
                for (BeforeResourceAmountSubtract listener : listeners) {
                    cancelled |= listener.beforeResourceAmountSubtract(resourceAmount, unit, scale, scaled, operation);
                }
                return cancelled;
            });

    public static final RustedFabricEvent<AfterResourceAmountSubtract> AFTER_RESOURCE_AMOUNT_SUBTRACT =
            RustedFabricEvent.create(listeners -> (resourceAmount, unit, scale, scaled, operation) -> {
                for (AfterResourceAmountSubtract listener : listeners) {
                    listener.afterResourceAmountSubtract(resourceAmount, unit, scale, scaled, operation);
                }
            });

    public static final RustedFabricEvent<BeforeResourceAmountAdd> BEFORE_RESOURCE_AMOUNT_ADD =
            RustedFabricEvent.create(listeners -> (resourceAmount, unit, scale, scaled, operation) -> {
                boolean cancelled = false;
                for (BeforeResourceAmountAdd listener : listeners) {
                    cancelled |= listener.beforeResourceAmountAdd(resourceAmount, unit, scale, scaled, operation);
                }
                return cancelled;
            });

    public static final RustedFabricEvent<AfterResourceAmountAdd> AFTER_RESOURCE_AMOUNT_ADD =
            RustedFabricEvent.create(listeners -> (resourceAmount, unit, scale, scaled, operation) -> {
                for (AfterResourceAmountAdd listener : listeners) {
                    listener.afterResourceAmountAdd(resourceAmount, unit, scale, scaled, operation);
                }
            });

    public static final RustedFabricEvent<BeforeTakeResourcesCollect> BEFORE_TAKE_RESOURCES_COLLECT =
            RustedFabricEvent.create(listeners -> (effect, unit, action, targetPoint, targetUnit, recursionDepth) -> {
                boolean cancelled = false;
                for (BeforeTakeResourcesCollect listener : listeners) {
                    cancelled |= listener.beforeTakeResourcesCollect(effect, unit, action, targetPoint, targetUnit, recursionDepth);
                }
                return cancelled;
            });

    public static final RustedFabricEvent<AfterTakeResourcesCollect> AFTER_TAKE_RESOURCES_COLLECT =
            RustedFabricEvent.create(listeners -> (effect, unit, action, targetPoint, targetUnit, recursionDepth, result) -> {
                for (AfterTakeResourcesCollect listener : listeners) {
                    listener.afterTakeResourcesCollect(effect, unit, action, targetPoint, targetUnit, recursionDepth, result);
                }
            });

    public static final RustedFabricEvent<BeforeResourceConversion> BEFORE_RESOURCE_CONVERSION =
            RustedFabricEvent.create(listeners -> (effect, unit, action, targetPoint, targetUnit, recursionDepth) -> {
                boolean cancelled = false;
                for (BeforeResourceConversion listener : listeners) {
                    cancelled |= listener.beforeResourceConversion(effect, unit, action, targetPoint, targetUnit, recursionDepth);
                }
                return cancelled;
            });

    public static final RustedFabricEvent<AfterResourceConversion> AFTER_RESOURCE_CONVERSION =
            RustedFabricEvent.create(listeners -> (effect, unit, action, targetPoint, targetUnit, recursionDepth, result) -> {
                for (AfterResourceConversion listener : listeners) {
                    listener.afterResourceConversion(effect, unit, action, targetPoint, targetUnit, recursionDepth, result);
                }
            });

    public static final RustedFabricEvent<ResourceAvailabilityCheck> RESOURCE_AVAILABILITY_CHECK =
            RustedFabricEvent.create(listeners -> (resourceAmount, unit, scale, scaled, operation, currentResult) -> {
                boolean result = currentResult;
                for (ResourceAvailabilityCheck listener : listeners) {
                    result = listener.resourceAvailabilityCheck(resourceAmount, unit, scale, scaled, operation, result);
                }
                return result;
            });

    public static final RustedFabricEvent<BeforeResourceReserve> BEFORE_RESOURCE_RESERVE =
            RustedFabricEvent.create(listeners -> (resourceAmount, unit, lagHiding, operation) -> {
                boolean cancelled = false;
                for (BeforeResourceReserve listener : listeners) {
                    cancelled |= listener.beforeResourceReserve(resourceAmount, unit, lagHiding, operation);
                }
                return cancelled;
            });

    public static final RustedFabricEvent<AfterResourceReserve> AFTER_RESOURCE_RESERVE =
            RustedFabricEvent.create(listeners -> (resourceAmount, unit, lagHiding, operation, result) -> {
                for (AfterResourceReserve listener : listeners) {
                    listener.afterResourceReserve(resourceAmount, unit, lagHiding, operation, result);
                }
            });

    private ResourceRuntimeEvents() {
    }

    @FunctionalInterface
    public interface BeforeResourceAmountSubtract {
        boolean beforeResourceAmountSubtract(Object resourceAmount, Object unit, double scale, boolean scaled, String operation);
    }

    @FunctionalInterface
    public interface AfterResourceAmountSubtract {
        void afterResourceAmountSubtract(Object resourceAmount, Object unit, double scale, boolean scaled, String operation);
    }

    @FunctionalInterface
    public interface BeforeResourceAmountAdd {
        boolean beforeResourceAmountAdd(Object resourceAmount, Object unit, double scale, boolean scaled, String operation);
    }

    @FunctionalInterface
    public interface AfterResourceAmountAdd {
        void afterResourceAmountAdd(Object resourceAmount, Object unit, double scale, boolean scaled, String operation);
    }

    @FunctionalInterface
    public interface BeforeTakeResourcesCollect {
        boolean beforeTakeResourcesCollect(Object effect, Object unit, Object action, Object targetPoint, Object targetUnit, int recursionDepth);
    }

    @FunctionalInterface
    public interface AfterTakeResourcesCollect {
        void afterTakeResourcesCollect(Object effect, Object unit, Object action, Object targetPoint, Object targetUnit, int recursionDepth, boolean result);
    }

    @FunctionalInterface
    public interface BeforeResourceConversion {
        boolean beforeResourceConversion(Object effect, Object unit, Object action, Object targetPoint, Object targetUnit, int recursionDepth);
    }

    @FunctionalInterface
    public interface AfterResourceConversion {
        void afterResourceConversion(Object effect, Object unit, Object action, Object targetPoint, Object targetUnit, int recursionDepth, boolean result);
    }

    @FunctionalInterface
    public interface ResourceAvailabilityCheck {
        boolean resourceAvailabilityCheck(Object resourceAmount, Object unit, double scale, boolean scaled, String operation, boolean currentResult);
    }

    @FunctionalInterface
    public interface BeforeResourceReserve {
        boolean beforeResourceReserve(Object resourceAmount, Object unit, boolean lagHiding, String operation);
    }

    @FunctionalInterface
    public interface AfterResourceReserve {
        void afterResourceReserve(Object resourceAmount, Object unit, boolean lagHiding, String operation, boolean result);
    }
}
