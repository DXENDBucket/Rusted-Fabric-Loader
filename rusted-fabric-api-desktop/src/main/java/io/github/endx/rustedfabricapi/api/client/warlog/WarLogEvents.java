package io.github.endx.rustedfabricapi.api.client.warlog;

import io.github.endx.rustedfabricapi.api.event.RustedFabricEvent;
import rustedwarfare.ui.WarLogInterface;
import rustedwarfare.unit.Unit;

/** Typed interception points for local war-log entries. */
public final class WarLogEvents {
    /** Duration is {@code -1} for a normal, non-forced text entry. */
    public static final RustedFabricEvent<BeforeText> BEFORE_TEXT =
            RustedFabricEvent.create(listeners -> (log, text, durationMillis) -> {
                boolean cancelled = false;
                for (BeforeText listener : listeners) {
                    cancelled |= listener.beforeText(log, text, durationMillis);
                }
                return cancelled;
            });
    public static final RustedFabricEvent<AfterText> AFTER_TEXT =
            RustedFabricEvent.create(listeners -> (log, text, durationMillis) -> {
                for (AfterText listener : listeners) listener.afterText(log, text, durationMillis);
            });
    public static final RustedFabricEvent<BeforeUnitEntry> BEFORE_UNIT_ENTRY =
            RustedFabricEvent.create(listeners -> (log, kind, unit) -> {
                boolean cancelled = false;
                for (BeforeUnitEntry listener : listeners) {
                    cancelled |= listener.beforeUnitEntry(log, kind, unit);
                }
                return cancelled;
            });
    public static final RustedFabricEvent<AfterUnitEntry> AFTER_UNIT_ENTRY =
            RustedFabricEvent.create(listeners -> (log, kind, unit) -> {
                for (AfterUnitEntry listener : listeners) listener.afterUnitEntry(log, kind, unit);
            });
    public static final RustedFabricEvent<Clear> BEFORE_CLEAR = clearEvent();
    public static final RustedFabricEvent<Clear> AFTER_CLEAR = clearEvent();

    private WarLogEvents() {
    }

    private static RustedFabricEvent<Clear> clearEvent() {
        return RustedFabricEvent.create(listeners -> log -> {
            for (Clear listener : listeners) listener.onClear(log);
        });
    }

    @FunctionalInterface
    public interface BeforeText {
        boolean beforeText(WarLogInterface log, String text, int durationMillis);
    }

    @FunctionalInterface
    public interface AfterText {
        void afterText(WarLogInterface log, String text, int durationMillis);
    }

    @FunctionalInterface
    public interface BeforeUnitEntry {
        boolean beforeUnitEntry(WarLogInterface log, WarLogEntryKind kind, Unit unit);
    }

    @FunctionalInterface
    public interface AfterUnitEntry {
        void afterUnitEntry(WarLogInterface log, WarLogEntryKind kind, Unit unit);
    }

    @FunctionalInterface
    public interface Clear {
        void onClear(WarLogInterface log);
    }
}
