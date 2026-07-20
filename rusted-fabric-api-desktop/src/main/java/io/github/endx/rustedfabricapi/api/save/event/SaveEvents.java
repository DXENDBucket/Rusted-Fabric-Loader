package io.github.endx.rustedfabricapi.api.save.event;

import io.github.endx.rustedfabricapi.api.event.RustedFabricEvent;
import rustedwarfare.save.GameSaver;

/** Typed high-level local save-file lifecycle. */
public final class SaveEvents {
    public static final RustedFabricEvent<BeforeSave> BEFORE_SAVE =
            RustedFabricEvent.create(listeners -> (manager, name, automatic) -> {
                boolean cancelled = false;
                for (BeforeSave listener : listeners) {
                    cancelled |= listener.beforeSave(manager, name, automatic);
                }
                return cancelled;
            });
    public static final RustedFabricEvent<AfterSave> AFTER_SAVE =
            RustedFabricEvent.create(listeners -> (manager, name, automatic) -> {
                for (AfterSave listener : listeners) listener.afterSave(manager, name, automatic);
            });
    public static final RustedFabricEvent<BeforeLoad> BEFORE_LOAD =
            RustedFabricEvent.create(listeners -> (manager, name) -> {
                boolean cancelled = false;
                for (BeforeLoad listener : listeners) cancelled |= listener.beforeLoad(manager, name);
                return cancelled;
            });
    public static final RustedFabricEvent<AfterLoad> AFTER_LOAD =
            RustedFabricEvent.create(listeners -> (manager, name, success) -> {
                for (AfterLoad listener : listeners) listener.afterLoad(manager, name, success);
            });
    public static final RustedFabricEvent<BeforeDelete> BEFORE_DELETE =
            RustedFabricEvent.create(listeners -> (manager, name) -> {
                boolean cancelled = false;
                for (BeforeDelete listener : listeners) cancelled |= listener.beforeDelete(manager, name);
                return cancelled;
            });
    public static final RustedFabricEvent<AfterDelete> AFTER_DELETE =
            RustedFabricEvent.create(listeners -> (manager, name, success) -> {
                for (AfterDelete listener : listeners) listener.afterDelete(manager, name, success);
            });

    private SaveEvents() {
    }

    @FunctionalInterface
    public interface BeforeSave {
        boolean beforeSave(GameSaver manager, String name, boolean automatic);
    }

    @FunctionalInterface
    public interface AfterSave {
        void afterSave(GameSaver manager, String name, boolean automatic);
    }

    @FunctionalInterface
    public interface BeforeLoad {
        boolean beforeLoad(GameSaver manager, String name);
    }

    @FunctionalInterface
    public interface AfterLoad {
        void afterLoad(GameSaver manager, String name, boolean success);
    }

    @FunctionalInterface
    public interface BeforeDelete {
        boolean beforeDelete(GameSaver manager, String name);
    }

    @FunctionalInterface
    public interface AfterDelete {
        void afterDelete(GameSaver manager, String name, boolean success);
    }
}
