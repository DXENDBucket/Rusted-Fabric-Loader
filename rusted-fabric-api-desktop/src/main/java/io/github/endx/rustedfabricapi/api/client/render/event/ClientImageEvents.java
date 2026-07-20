package io.github.endx.rustedfabricapi.api.client.render.event;

import io.github.endx.rustedfabricapi.api.asset.ModResource;
import io.github.endx.rustedfabricapi.api.client.render.ClientImage;
import io.github.endx.rustedfabricapi.api.event.RustedFabricEvent;

/** Mod-resource image loading and wrapper-release lifecycle. */
public final class ClientImageEvents {
    public static final RustedFabricEvent<BeforeLoad> BEFORE_LOAD =
            RustedFabricEvent.create(listeners -> (resource, smooth) -> {
                boolean cancelled = false;
                for (BeforeLoad listener : listeners) {
                    cancelled |= listener.beforeLoad(resource, smooth);
                }
                return cancelled;
            });
    public static final RustedFabricEvent<AfterLoad> AFTER_LOAD =
            RustedFabricEvent.create(listeners -> (resource, image, successful) -> {
                for (AfterLoad listener : listeners) {
                    listener.afterLoad(resource, image, successful);
                }
            });
    public static final RustedFabricEvent<AfterRelease> AFTER_RELEASE =
            RustedFabricEvent.create(listeners -> (image, nativeReleased) -> {
                for (AfterRelease listener : listeners) {
                    listener.afterRelease(image, nativeReleased);
                }
            });

    private ClientImageEvents() {
    }

    @FunctionalInterface public interface BeforeLoad {
        boolean beforeLoad(ModResource resource, boolean smooth);
    }
    @FunctionalInterface public interface AfterLoad {
        void afterLoad(ModResource resource, ClientImage image, boolean successful);
    }
    @FunctionalInterface public interface AfterRelease {
        void afterRelease(ClientImage image, boolean nativeReleased);
    }
}
