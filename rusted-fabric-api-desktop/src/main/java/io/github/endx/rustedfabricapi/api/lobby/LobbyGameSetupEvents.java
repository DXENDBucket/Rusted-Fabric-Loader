package io.github.endx.rustedfabricapi.api.lobby;

import io.github.endx.rustedfabricapi.api.event.RustedFabricEvent;
import rustedwarfare.network.NetworkEngine;

/** API-mediated update events plus observation of the native apply boundary. */
public final class LobbyGameSetupEvents {
    public static final RustedFabricEvent<BeforeUpdate> BEFORE_UPDATE =
            RustedFabricEvent.create(listeners -> (network, current, requested) -> {
                boolean cancelled = false;
                for (BeforeUpdate listener : listeners) {
                    cancelled |= listener.beforeUpdate(network, current, requested);
                }
                return cancelled;
            });
    public static final RustedFabricEvent<AfterUpdate> AFTER_UPDATE =
            RustedFabricEvent.create(listeners -> (network, previous, requested, success) -> {
                for (AfterUpdate listener : listeners) {
                    listener.afterUpdate(network, previous, requested, success);
                }
            });
    public static final RustedFabricEvent<NativeApply> BEFORE_NATIVE_APPLY = nativeApply();
    public static final RustedFabricEvent<NativeApply> AFTER_NATIVE_APPLY = nativeApply();

    private LobbyGameSetupEvents() {
    }

    private static RustedFabricEvent<NativeApply> nativeApply() {
        return RustedFabricEvent.create(listeners -> (network, requested) -> {
            for (NativeApply listener : listeners) listener.onApply(network, requested);
        });
    }

    @FunctionalInterface
    public interface BeforeUpdate {
        boolean beforeUpdate(NetworkEngine network, GameSetupSnapshot current,
                GameSetupSnapshot requested);
    }

    @FunctionalInterface
    public interface AfterUpdate {
        void afterUpdate(NetworkEngine network, GameSetupSnapshot previous,
                GameSetupSnapshot requested, boolean success);
    }

    @FunctionalInterface
    public interface NativeApply {
        void onApply(NetworkEngine network, GameSetupSnapshot requested);
    }
}
