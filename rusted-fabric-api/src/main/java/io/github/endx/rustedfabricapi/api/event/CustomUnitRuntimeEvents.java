package io.github.endx.rustedfabricapi.api.event;

public final class CustomUnitRuntimeEvents {
    private CustomUnitRuntimeEvents() {
    }

    public static final RustedFabricEvent<BeforeCustomActionExecute> BEFORE_CUSTOM_ACTION_EXECUTE =
            RustedFabricEvent.create(listeners -> (unit, action, targetPoint, targetUnit, recursionDepth) -> {
                boolean cancelled = false;
                for (BeforeCustomActionExecute listener : listeners) {
                    cancelled |= listener.beforeCustomActionExecute(unit, action, targetPoint, targetUnit, recursionDepth);
                }
                return cancelled;
            });

    public static final RustedFabricEvent<AfterCustomActionExecute> AFTER_CUSTOM_ACTION_EXECUTE =
            RustedFabricEvent.create(listeners -> (unit, action, targetPoint, targetUnit, recursionDepth, result) -> {
                for (AfterCustomActionExecute listener : listeners) {
                    listener.afterCustomActionExecute(unit, action, targetPoint, targetUnit, recursionDepth, result);
                }
            });

    public static final RustedFabricEvent<BeforeCustomActionEffectExecute> BEFORE_CUSTOM_ACTION_EFFECT_EXECUTE =
            RustedFabricEvent.create(listeners -> (effect, unit, action, targetPoint, targetUnit, recursionDepth) -> {
                boolean cancelled = false;
                for (BeforeCustomActionEffectExecute listener : listeners) {
                    cancelled |= listener.beforeCustomActionEffectExecute(effect, unit, action, targetPoint, targetUnit, recursionDepth);
                }
                return cancelled;
            });

    public static final RustedFabricEvent<AfterCustomActionEffectExecute> AFTER_CUSTOM_ACTION_EFFECT_EXECUTE =
            RustedFabricEvent.create(listeners -> (effect, unit, action, targetPoint, targetUnit, recursionDepth, result) -> {
                for (AfterCustomActionEffectExecute listener : listeners) {
                    listener.afterCustomActionEffectExecute(effect, unit, action, targetPoint, targetUnit, recursionDepth, result);
                }
            });

    public static final RustedFabricEvent<BeforeCustomUnitConvert> BEFORE_CUSTOM_UNIT_CONVERT =
            RustedFabricEvent.create(listeners -> (unit, action, targetPoint, targetUnit, recursionDepth) -> {
                boolean cancelled = false;
                for (BeforeCustomUnitConvert listener : listeners) {
                    cancelled |= listener.beforeCustomUnitConvert(unit, action, targetPoint, targetUnit, recursionDepth);
                }
                return cancelled;
            });

    public static final RustedFabricEvent<AfterCustomUnitConvert> AFTER_CUSTOM_UNIT_CONVERT =
            RustedFabricEvent.create(listeners -> (unit, action, targetPoint, targetUnit, recursionDepth) -> {
                for (AfterCustomUnitConvert listener : listeners) {
                    listener.afterCustomUnitConvert(unit, action, targetPoint, targetUnit, recursionDepth);
                }
            });

    public static final RustedFabricEvent<BeforeTurretFireAtTarget> BEFORE_TURRET_FIRE_AT_TARGET =
            RustedFabricEvent.create(listeners -> (unit, targetUnit, turretIndex) -> {
                boolean cancelled = false;
                for (BeforeTurretFireAtTarget listener : listeners) {
                    cancelled |= listener.beforeTurretFireAtTarget(unit, targetUnit, turretIndex);
                }
                return cancelled;
            });

    public static final RustedFabricEvent<AfterProjectileCreatedFromTemplate> AFTER_PROJECTILE_CREATED_FROM_TEMPLATE =
            RustedFabricEvent.create(listeners -> (projectile, targetUnit, turretIndex, template, x, y, height, direction) -> {
                for (AfterProjectileCreatedFromTemplate listener : listeners) {
                    listener.afterProjectileCreatedFromTemplate(projectile, targetUnit, turretIndex, template, x, y, height, direction);
                }
            });

    public static final RustedFabricEvent<AfterProjectileTemplateApplied> AFTER_PROJECTILE_TEMPLATE_APPLIED =
            RustedFabricEvent.create(listeners -> (projectile, targetUnit, turretIndex, template, x, y, height, direction) -> {
                for (AfterProjectileTemplateApplied listener : listeners) {
                    listener.afterProjectileTemplateApplied(projectile, targetUnit, turretIndex, template, x, y, height, direction);
                }
            });

    public static final RustedFabricEvent<BeforeFireProjectileAtGround> BEFORE_FIRE_PROJECTILE_AT_GROUND =
            RustedFabricEvent.create(listeners -> (unit, targetUnit, x, y, turretIndex, template, projectileCount) -> {
                boolean cancelled = false;
                for (BeforeFireProjectileAtGround listener : listeners) {
                    cancelled |= listener.beforeFireProjectileAtGround(unit, targetUnit, x, y, turretIndex, template, projectileCount);
                }
                return cancelled;
            });

    public static final RustedFabricEvent<BeforeResourceCostPaid> BEFORE_RESOURCE_COST_PAID =
            RustedFabricEvent.create(listeners -> (resourceAmount, unit, operation) -> {
                boolean cancelled = false;
                for (BeforeResourceCostPaid listener : listeners) {
                    cancelled |= listener.beforeResourceCostPaid(resourceAmount, unit, operation);
                }
                return cancelled;
            });

    public static final RustedFabricEvent<AfterMutableStatsApplied> AFTER_MUTABLE_STATS_APPLIED =
            RustedFabricEvent.create(listeners -> (writerElement, unit) -> {
                for (AfterMutableStatsApplied listener : listeners) {
                    listener.afterMutableStatsApplied(writerElement, unit);
                }
            });

    public static final RustedFabricEvent<AfterCustomUnitTransportLoad> AFTER_CUSTOM_UNIT_TRANSPORT_LOAD =
            RustedFabricEvent.create(listeners -> (unit, transportedUnit) -> {
                for (AfterCustomUnitTransportLoad listener : listeners) {
                    listener.afterCustomUnitTransportLoad(unit, transportedUnit);
                }
            });

    public static final RustedFabricEvent<AfterCustomUnitTransportUnload> AFTER_CUSTOM_UNIT_TRANSPORT_UNLOAD =
            RustedFabricEvent.create(listeners -> (unit, transportedUnit) -> {
                for (AfterCustomUnitTransportUnload listener : listeners) {
                    listener.afterCustomUnitTransportUnload(unit, transportedUnit);
                }
            });

    public static final RustedFabricEvent<AfterCustomUnitKilled> AFTER_CUSTOM_UNIT_KILLED =
            RustedFabricEvent.create(listeners -> unit -> {
                for (AfterCustomUnitKilled listener : listeners) {
                    listener.afterCustomUnitKilled(unit);
                }
            });

    public static final RustedFabricEvent<AfterCustomUnitRemoved> AFTER_CUSTOM_UNIT_REMOVED =
            RustedFabricEvent.create(listeners -> unit -> {
                for (AfterCustomUnitRemoved listener : listeners) {
                    listener.afterCustomUnitRemoved(unit);
                }
            });

    public static final RustedFabricEvent<AfterBuildQueueItemComplete> AFTER_BUILD_QUEUE_ITEM_COMPLETE =
            RustedFabricEvent.create(listeners -> (unit, queueItem) -> {
                for (AfterBuildQueueItemComplete listener : listeners) {
                    listener.afterBuildQueueItemComplete(unit, queueItem);
                }
            });

    @FunctionalInterface
    public interface BeforeCustomActionExecute {
        boolean beforeCustomActionExecute(Object unit, Object action, Object targetPoint, Object targetUnit, int recursionDepth);
    }

    @FunctionalInterface
    public interface AfterCustomActionExecute {
        void afterCustomActionExecute(Object unit, Object action, Object targetPoint, Object targetUnit, int recursionDepth, boolean result);
    }

    @FunctionalInterface
    public interface BeforeCustomActionEffectExecute {
        boolean beforeCustomActionEffectExecute(Object effect, Object unit, Object action, Object targetPoint, Object targetUnit, int recursionDepth);
    }

    @FunctionalInterface
    public interface AfterCustomActionEffectExecute {
        void afterCustomActionEffectExecute(Object effect, Object unit, Object action, Object targetPoint, Object targetUnit, int recursionDepth, boolean result);
    }

    @FunctionalInterface
    public interface BeforeCustomUnitConvert {
        boolean beforeCustomUnitConvert(Object unit, Object action, Object targetPoint, Object targetUnit, int recursionDepth);
    }

    @FunctionalInterface
    public interface AfterCustomUnitConvert {
        void afterCustomUnitConvert(Object unit, Object action, Object targetPoint, Object targetUnit, int recursionDepth);
    }

    @FunctionalInterface
    public interface BeforeTurretFireAtTarget {
        boolean beforeTurretFireAtTarget(Object unit, Object targetUnit, int turretIndex);
    }

    @FunctionalInterface
    public interface AfterProjectileCreatedFromTemplate {
        void afterProjectileCreatedFromTemplate(Object projectile, Object targetUnit, int turretIndex, Object template, float x, float y, float height, float direction);
    }

    @FunctionalInterface
    public interface AfterProjectileTemplateApplied {
        void afterProjectileTemplateApplied(Object projectile, Object targetUnit, int turretIndex, Object template, float x, float y, float height, float direction);
    }

    @FunctionalInterface
    public interface BeforeFireProjectileAtGround {
        boolean beforeFireProjectileAtGround(Object unit, Object targetUnit, float x, float y, int turretIndex, Object template, int projectileCount);
    }

    @FunctionalInterface
    public interface BeforeResourceCostPaid {
        boolean beforeResourceCostPaid(Object resourceAmount, Object unit, String operation);
    }

    @FunctionalInterface
    public interface AfterMutableStatsApplied {
        void afterMutableStatsApplied(Object writerElement, Object unit);
    }

    @FunctionalInterface
    public interface AfterCustomUnitTransportLoad {
        void afterCustomUnitTransportLoad(Object unit, Object transportedUnit);
    }

    @FunctionalInterface
    public interface AfterCustomUnitTransportUnload {
        void afterCustomUnitTransportUnload(Object unit, Object transportedUnit);
    }

    @FunctionalInterface
    public interface AfterCustomUnitKilled {
        void afterCustomUnitKilled(Object unit);
    }

    @FunctionalInterface
    public interface AfterCustomUnitRemoved {
        void afterCustomUnitRemoved(Object unit);
    }

    @FunctionalInterface
    public interface AfterBuildQueueItemComplete {
        void afterBuildQueueItemComplete(Object unit, Object queueItem);
    }
}
