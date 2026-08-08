package io.github.endx.rustedfabricapi.api.config;

import io.github.endx.rustedfabricapi.api.event.RustedFabricEvent;

/** Lifecycle events for files accessed through {@link ModConfigFile}. */
public final class ConfigEvents {
    public static final RustedFabricEvent<BeforeMutation> BEFORE_WRITE = beforeMutation();
    public static final RustedFabricEvent<AfterMutation> AFTER_WRITE = afterMutation();
    public static final RustedFabricEvent<AfterRead> AFTER_READ =
            RustedFabricEvent.create(listeners -> (file, present, byteCount) -> {
                for (AfterRead listener : listeners) listener.afterRead(file, present, byteCount);
            });
    public static final RustedFabricEvent<BeforeMutation> BEFORE_DELETE = beforeMutation();
    public static final RustedFabricEvent<AfterMutation> AFTER_DELETE = afterMutation();

    private ConfigEvents() {
    }

    private static RustedFabricEvent<BeforeMutation> beforeMutation() {
        return RustedFabricEvent.create(listeners -> file -> {
            boolean cancelled = false;
            for (BeforeMutation listener : listeners) cancelled |= listener.beforeMutation(file);
            return cancelled;
        });
    }

    private static RustedFabricEvent<AfterMutation> afterMutation() {
        return RustedFabricEvent.create(listeners -> (file, success) -> {
            for (AfterMutation listener : listeners) listener.afterMutation(file, success);
        });
    }

    @FunctionalInterface
    public interface BeforeMutation {
        boolean beforeMutation(ModConfigFile file);
    }

    @FunctionalInterface
    public interface AfterMutation {
        void afterMutation(ModConfigFile file, boolean success);
    }

    @FunctionalInterface
    public interface AfterRead {
        void afterRead(ModConfigFile file, boolean present, int byteCount);
    }
}
