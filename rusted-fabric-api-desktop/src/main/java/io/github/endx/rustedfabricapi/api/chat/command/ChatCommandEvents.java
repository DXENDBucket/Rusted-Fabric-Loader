package io.github.endx.rustedfabricapi.api.chat.command;

import io.github.endx.rustedfabricapi.api.event.RustedFabricEvent;

/** Observation events around registered custom command handlers. */
public final class ChatCommandEvents {
    public static final RustedFabricEvent<BeforeExecute> BEFORE_EXECUTE =
            RustedFabricEvent.create(listeners -> context -> {
                boolean cancelled = false;
                for (BeforeExecute listener : listeners) cancelled |= listener.beforeExecute(context);
                return cancelled;
            });
    public static final RustedFabricEvent<AfterExecute> AFTER_EXECUTE =
            RustedFabricEvent.create(listeners -> (context, result, failure) -> {
                for (AfterExecute listener : listeners) listener.afterExecute(context, result, failure);
            });

    private ChatCommandEvents() {
    }

    @FunctionalInterface
    public interface BeforeExecute {
        /** Cancelling still consumes and hides the command. */
        boolean beforeExecute(ChatCommandContext context);
    }

    @FunctionalInterface
    public interface AfterExecute {
        void afterExecute(ChatCommandContext context, int result, RuntimeException failure);
    }
}
