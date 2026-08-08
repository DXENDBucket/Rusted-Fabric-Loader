package io.github.endx.rustedfabricapi.api.client.option.event;

import io.github.endx.rustedfabricapi.api.client.option.ClientOptionChange;
import io.github.endx.rustedfabricapi.api.client.option.ClientOptionChangeSet;
import io.github.endx.rustedfabricapi.api.client.option.ClientOptionUpdateResult;
import io.github.endx.rustedfabricapi.api.event.RustedFabricEvent;
import rustedwarfare.core.SettingsEngine;

/** Transaction, native dynamic-change, and persistence boundaries for client options. */
public final class ClientOptionEvents {
    public static final RustedFabricEvent<BeforeUpdate> BEFORE_UPDATE =
            RustedFabricEvent.create(listeners -> (settings, changes) -> {
                boolean cancelled = false;
                for (BeforeUpdate listener : listeners) {
                    cancelled |= listener.beforeUpdate(settings, changes);
                }
                return cancelled;
            });

    public static final RustedFabricEvent<AfterUpdate> AFTER_UPDATE =
            RustedFabricEvent.create(listeners -> (settings, result) -> {
                for (AfterUpdate listener : listeners) listener.afterUpdate(settings, result);
            });

    /** Observes supported settings changed through the game's reflective UI/config path. */
    public static final RustedFabricEvent<NativeDynamicChange> AFTER_NATIVE_DYNAMIC_CHANGE =
            RustedFabricEvent.create(listeners -> (settings, change) -> {
                for (NativeDynamicChange listener : listeners) {
                    listener.afterChange(settings, change);
                }
            });

    /** Observational boundary; native settings saves are intentionally not cancellable. */
    public static final RustedFabricEvent<NativeSaveStart> BEFORE_NATIVE_SAVE =
            RustedFabricEvent.create(listeners -> settings -> {
                for (NativeSaveStart listener : listeners) listener.beforeSave(settings);
            });

    public static final RustedFabricEvent<NativeSaveComplete> AFTER_NATIVE_SAVE =
            RustedFabricEvent.create(listeners -> (settings, successful) -> {
                for (NativeSaveComplete listener : listeners) {
                    listener.afterSave(settings, successful);
                }
            });

    private ClientOptionEvents() {
    }

    @FunctionalInterface public interface BeforeUpdate {
        boolean beforeUpdate(SettingsEngine settings, ClientOptionChangeSet changes);
    }
    @FunctionalInterface public interface AfterUpdate {
        void afterUpdate(SettingsEngine settings, ClientOptionUpdateResult result);
    }
    @FunctionalInterface public interface NativeDynamicChange {
        void afterChange(SettingsEngine settings, ClientOptionChange<?> change);
    }
    @FunctionalInterface public interface NativeSaveStart {
        void beforeSave(SettingsEngine settings);
    }
    @FunctionalInterface public interface NativeSaveComplete {
        void afterSave(SettingsEngine settings, boolean successful);
    }
}
