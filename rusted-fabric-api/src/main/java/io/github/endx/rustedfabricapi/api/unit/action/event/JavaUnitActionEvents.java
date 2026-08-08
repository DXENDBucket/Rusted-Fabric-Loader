package io.github.endx.rustedfabricapi.api.unit.action.event;

import io.github.endx.rustedfabricapi.api.event.RustedFabricEvent;
import io.github.endx.rustedfabricapi.api.unit.action.JavaUnitActionContext;

/** Observable and cancellable execution boundary for registered Java actions. */
public final class JavaUnitActionEvents {
    public static final RustedFabricEvent<BeforeExecute> BEFORE_EXECUTE =
            RustedFabricEvent.create(listeners -> context -> {
                boolean cancelled = false;
                for (BeforeExecute listener : listeners) cancelled |= listener.beforeExecute(context);
                return cancelled;
            });
    public static final RustedFabricEvent<AfterExecute> AFTER_EXECUTE =
            RustedFabricEvent.create(listeners -> context -> {
                for (AfterExecute listener : listeners) listener.afterExecute(context);
            });

    private JavaUnitActionEvents() {
    }

    @FunctionalInterface
    public interface BeforeExecute {
        boolean beforeExecute(JavaUnitActionContext context);
    }

    @FunctionalInterface
    public interface AfterExecute {
        void afterExecute(JavaUnitActionContext context);
    }
}
