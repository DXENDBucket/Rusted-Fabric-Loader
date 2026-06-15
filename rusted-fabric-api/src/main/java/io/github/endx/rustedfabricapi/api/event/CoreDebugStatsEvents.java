package io.github.endx.rustedfabricapi.api.event;

public final class CoreDebugStatsEvents {
    public static final RustedFabricEvent<BeforeStatsEngineLifecycle> BEFORE_STATS_ENGINE_RESET =
            RustedFabricEvent.create(listeners -> statsEngine -> {
                boolean cancelled = false;
                for (BeforeStatsEngineLifecycle listener : listeners) {
                    cancelled |= listener.beforeStatsEngineLifecycle(statsEngine);
                }
                return cancelled;
            });

    public static final RustedFabricEvent<AfterStatsEngineLifecycle> AFTER_STATS_ENGINE_RESET =
            RustedFabricEvent.create(listeners -> statsEngine -> {
                for (AfterStatsEngineLifecycle listener : listeners) {
                    listener.afterStatsEngineLifecycle(statsEngine);
                }
            });

    public static final RustedFabricEvent<BeforeStatsEngineLifecycle> BEFORE_PERIODIC_STATS_SNAPSHOT =
            RustedFabricEvent.create(listeners -> statsEngine -> {
                boolean cancelled = false;
                for (BeforeStatsEngineLifecycle listener : listeners) {
                    cancelled |= listener.beforeStatsEngineLifecycle(statsEngine);
                }
                return cancelled;
            });

    public static final RustedFabricEvent<AfterStatsEngineLifecycle> AFTER_PERIODIC_STATS_SNAPSHOT =
            RustedFabricEvent.create(listeners -> statsEngine -> {
                for (AfterStatsEngineLifecycle listener : listeners) {
                    listener.afterStatsEngineLifecycle(statsEngine);
                }
            });

    public static final RustedFabricEvent<BeforeStatsEngineLifecycle> BEFORE_FINALIZE_STATS_HISTORY =
            RustedFabricEvent.create(listeners -> statsEngine -> {
                boolean cancelled = false;
                for (BeforeStatsEngineLifecycle listener : listeners) {
                    cancelled |= listener.beforeStatsEngineLifecycle(statsEngine);
                }
                return cancelled;
            });

    public static final RustedFabricEvent<AfterStatsEngineLifecycle> AFTER_FINALIZE_STATS_HISTORY =
            RustedFabricEvent.create(listeners -> statsEngine -> {
                for (AfterStatsEngineLifecycle listener : listeners) {
                    listener.afterStatsEngineLifecycle(statsEngine);
                }
            });

    public static final RustedFabricEvent<BeforeStatsHistorySnapshot> BEFORE_STATS_HISTORY_SNAPSHOT =
            RustedFabricEvent.create(listeners -> (statsEngine, frame, flagA, flagB) -> {
                boolean cancelled = false;
                for (BeforeStatsHistorySnapshot listener : listeners) {
                    cancelled |= listener.beforeStatsHistorySnapshot(statsEngine, frame, flagA, flagB);
                }
                return cancelled;
            });

    public static final RustedFabricEvent<AfterStatsHistorySnapshot> AFTER_STATS_HISTORY_SNAPSHOT =
            RustedFabricEvent.create(listeners -> (statsEngine, frame, flagA, flagB) -> {
                for (AfterStatsHistorySnapshot listener : listeners) {
                    listener.afterStatsHistorySnapshot(statsEngine, frame, flagA, flagB);
                }
            });

    public static final RustedFabricEvent<BeforeStatsUnitKilledNotification> BEFORE_NOTIFY_UNIT_KILLED =
            RustedFabricEvent.create(listeners -> (dispatcher, killedUnit, attackerUnit) -> {
                for (BeforeStatsUnitKilledNotification listener : listeners) {
                    listener.beforeStatsUnitKilledNotification(dispatcher, killedUnit, attackerUnit);
                }
            });

    public static final RustedFabricEvent<AfterStatsUnitKilledNotification> AFTER_NOTIFY_UNIT_KILLED =
            RustedFabricEvent.create(listeners -> (dispatcher, killedUnit, attackerUnit) -> {
                for (AfterStatsUnitKilledNotification listener : listeners) {
                    listener.afterStatsUnitKilledNotification(dispatcher, killedUnit, attackerUnit);
                }
            });

    private CoreDebugStatsEvents() {
    }

    @FunctionalInterface
    public interface BeforeStatsEngineLifecycle {
        boolean beforeStatsEngineLifecycle(Object statsEngine);
    }

    @FunctionalInterface
    public interface AfterStatsEngineLifecycle {
        void afterStatsEngineLifecycle(Object statsEngine);
    }

    @FunctionalInterface
    public interface BeforeStatsHistorySnapshot {
        boolean beforeStatsHistorySnapshot(Object statsEngine, int frame, boolean flagA, boolean flagB);
    }

    @FunctionalInterface
    public interface AfterStatsHistorySnapshot {
        void afterStatsHistorySnapshot(Object statsEngine, int frame, boolean flagA, boolean flagB);
    }

    @FunctionalInterface
    public interface BeforeStatsUnitKilledNotification {
        void beforeStatsUnitKilledNotification(Object dispatcher, Object killedUnit, Object attackerUnit);
    }

    @FunctionalInterface
    public interface AfterStatsUnitKilledNotification {
        void afterStatsUnitKilledNotification(Object dispatcher, Object killedUnit, Object attackerUnit);
    }
}
