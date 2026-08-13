package io.github.endx.rustedfabricapi.api.client.event;

import io.github.endx.rustedfabricapi.api.client.render.ClientImage;
import io.github.endx.rustedfabricapi.api.event.RustedFabricEvent;

/** Client-only presentation hooks for configured custom-unit action icons. */
public final class CustomActionIconEvents {
    /**
     * Lets a mod replace a configured action's icon without changing its synchronized behavior.
     * The first listener returning a non-null, open image wins.
     */
    public static final RustedFabricEvent<Override> OVERRIDE =
            RustedFabricEvent.create(listeners -> (actionId, actionName) -> {
                for (Override listener : listeners) {
                    ClientImage image = listener.icon(actionId, actionName);
                    if (image != null && !image.isClosed()) return image;
                }
                return null;
            });

    private CustomActionIconEvents() { }

    @FunctionalInterface
    public interface Override {
        /** Returns null to keep the native icon and allow later listeners to handle the action. */
        ClientImage icon(String actionId, String configuredActionName);
    }
}
