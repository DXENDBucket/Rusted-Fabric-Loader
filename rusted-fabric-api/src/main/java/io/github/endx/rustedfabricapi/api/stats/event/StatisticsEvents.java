package io.github.endx.rustedfabricapi.api.stats.event;

import io.github.endx.rustedfabricapi.api.event.RustedFabricEvent;
import rustedwarfare.stats.StatsEngine;
import rustedwarfare.stats.StatsEventDispatcher;
import rustedwarfare.unit.Unit;

/** Typed observation events for native match-statistics updates. */
public final class StatisticsEvents {
    public static final RustedFabricEvent<EngineEvent> AFTER_RESET = engineEvent();
    /** Fires after the native periodic history-update method, whether or not its timer sampled. */
    public static final RustedFabricEvent<EngineEvent> AFTER_PERIODIC_UPDATE = engineEvent();
    public static final RustedFabricEvent<EngineEvent> AFTER_HISTORY_FINALIZED = engineEvent();
    public static final RustedFabricEvent<UnitKilled> BEFORE_UNIT_KILLED = unitKilled();
    public static final RustedFabricEvent<UnitKilled> AFTER_UNIT_KILLED = unitKilled();

    private StatisticsEvents() {
    }

    private static RustedFabricEvent<EngineEvent> engineEvent() {
        return RustedFabricEvent.create(listeners -> manager -> {
            for (EngineEvent listener : listeners) listener.onStatistics(manager);
        });
    }

    private static RustedFabricEvent<UnitKilled> unitKilled() {
        return RustedFabricEvent.create(listeners -> (dispatcher, killed, attacker) -> {
            for (UnitKilled listener : listeners) listener.onUnitKilled(dispatcher, killed, attacker);
        });
    }

    @FunctionalInterface
    public interface EngineEvent {
        void onStatistics(StatsEngine manager);
    }

    @FunctionalInterface
    public interface UnitKilled {
        /** The attacker may be {@code null} for environmental or unattributed deaths. */
        void onUnitKilled(StatsEventDispatcher dispatcher, Unit killed, Unit attacker);
    }
}
