package io.github.endx.rustedfabricapi.api.event;

public final class CommandEvents {
    public static final RustedFabricEvent<BeforeCommandIssue> BEFORE_COMMAND_ISSUE =
            RustedFabricEvent.create(listeners -> command -> {
                boolean cancelled = false;
                for (BeforeCommandIssue listener : listeners) {
                    cancelled |= listener.beforeCommandIssue(command);
                }
                return cancelled;
            });

    public static final RustedFabricEvent<AfterCommandIssue> AFTER_COMMAND_ISSUE =
            RustedFabricEvent.create(listeners -> command -> {
                for (AfterCommandIssue listener : listeners) {
                    listener.afterCommandIssue(command);
                }
            });

    private CommandEvents() {
    }

    @FunctionalInterface
    public interface BeforeCommandIssue {
        boolean beforeCommandIssue(Object command);
    }

    @FunctionalInterface
    public interface AfterCommandIssue {
        void afterCommandIssue(Object command);
    }
}
