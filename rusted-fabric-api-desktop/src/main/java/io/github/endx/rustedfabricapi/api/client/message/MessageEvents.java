package io.github.endx.rustedfabricapi.api.client.message;

import io.github.endx.rustedfabricapi.api.event.RustedFabricEvent;
import rustedwarfare.ui.MessageInterface;
import rustedwarfare.ui.MessageLine;

/** Events for the local in-game chat/system-message history. */
public final class MessageEvents {
    public static final RustedFabricEvent<BeforeAdd> BEFORE_ADD =
            RustedFabricEvent.create(listeners -> (history, sender, message) -> {
                boolean cancelled = false;
                for (BeforeAdd listener : listeners) {
                    cancelled |= listener.beforeAdd(history, sender, message);
                }
                return cancelled;
            });
    public static final RustedFabricEvent<AfterAdd> AFTER_ADD =
            RustedFabricEvent.create(listeners -> (history, sender, message, line) -> {
                for (AfterAdd listener : listeners) {
                    listener.afterAdd(history, sender, message, line);
                }
            });
    public static final RustedFabricEvent<Clear> BEFORE_CLEAR = clearEvent();
    public static final RustedFabricEvent<Clear> AFTER_CLEAR = clearEvent();

    private MessageEvents() {
    }

    private static RustedFabricEvent<Clear> clearEvent() {
        return RustedFabricEvent.create(listeners -> history -> {
            for (Clear listener : listeners) listener.onClear(history);
        });
    }

    @FunctionalInterface
    public interface BeforeAdd {
        boolean beforeAdd(MessageInterface history, String sender, String message);
    }

    @FunctionalInterface
    public interface AfterAdd {
        void afterAdd(MessageInterface history, String sender, String message, MessageLine line);
    }

    @FunctionalInterface
    public interface Clear {
        void onClear(MessageInterface history);
    }
}
