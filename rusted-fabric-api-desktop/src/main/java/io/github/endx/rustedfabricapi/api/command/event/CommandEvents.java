package io.github.endx.rustedfabricapi.api.command.event;

import io.github.endx.rustedfabricapi.api.event.RustedFabricEvent;
import rustedwarfare.command.Command;

/** Strongly typed events around {@link Command#issueCommand()}. */
public final class CommandEvents {
    public static final RustedFabricEvent<BeforeIssue> BEFORE_ISSUE =
            RustedFabricEvent.create(listeners -> command -> {
                boolean cancelled = false;
                for (BeforeIssue listener : listeners) {
                    cancelled |= listener.beforeIssue(command);
                }
                return cancelled;
            });

    public static final RustedFabricEvent<AfterIssue> AFTER_ISSUE =
            RustedFabricEvent.create(listeners -> command -> {
                for (AfterIssue listener : listeners) {
                    listener.afterIssue(command);
                }
            });

    private CommandEvents() {
    }

    @FunctionalInterface
    public interface BeforeIssue {
        /** @return {@code true} to cancel the command before the game processes it. */
        boolean beforeIssue(Command command);
    }

    @FunctionalInterface
    public interface AfterIssue {
        void afterIssue(Command command);
    }
}
