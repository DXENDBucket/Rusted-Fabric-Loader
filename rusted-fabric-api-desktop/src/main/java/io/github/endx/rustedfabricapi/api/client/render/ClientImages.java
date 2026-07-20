package io.github.endx.rustedfabricapi.api.client.render;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import java.util.Optional;

import io.github.endx.rustedfabricapi.api.asset.ModResource;
import io.github.endx.rustedfabricapi.api.client.RustedWarfareClient;
import io.github.endx.rustedfabricapi.api.client.render.event.ClientImageEvents;
import rustedwarfare.client.render.GameImage;
import rustedwarfare.core.GameEngine;
import rustedwarfare.io.NamedInputStream;
import rustedwarfare.render.GraphicsEngine;

/** Loads native render images directly from ordinary mod-Jar resources. */
public final class ClientImages {
    private ClientImages() {
    }

    public static Optional<ClientImage> load(ModResource resource) throws IOException {
        return load(resource, true);
    }

    /** Returns empty when a before-load listener cancels the operation. */
    public static Optional<ClientImage> load(ModResource resource, boolean smooth)
            throws IOException {
        ModResource checked = Objects.requireNonNull(resource, "resource");
        GraphicsEngine graphics = graphics();
        if (ClientImageEvents.BEFORE_LOAD.invoker().beforeLoad(checked, smooth)) {
            return Optional.empty();
        }
        ClientImage result = null;
        boolean success = false;
        try (InputStream raw = checked.open();
                NamedInputStream input = new NamedInputStream(raw, checked.toString())) {
            GameImage image = graphics.loadImageFromStream(input, false);
            if (image == null) throw new IOException("Native image loader returned null: " + checked);
            boolean fallback = image.isOutOfMemoryFallback();
            if (!fallback) {
                image.setName(checked.toString());
                image.setSmooth(smooth);
            }
            result = new ClientImage(image, checked, !fallback, fallback);
            success = true;
            return Optional.of(result);
        } finally {
            ClientImageEvents.AFTER_LOAD.invoker().afterLoad(checked, result, success);
        }
    }

    public static ClientImage create(int width, int height, boolean alpha) {
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("image dimensions must be positive");
        }
        GameImage image = graphics().createImage(width, height, alpha);
        if (image == null) throw new IllegalStateException("Native image creation returned null");
        boolean fallback = image.isOutOfMemoryFallback();
        return new ClientImage(image, null, !fallback, fallback);
    }

    private static GraphicsEngine graphics() {
        GameEngine engine = RustedWarfareClient.requireEngine();
        if (engine.renderGraphicsEngine == null) {
            throw new IllegalStateException("Render graphics engine is not initialized");
        }
        return engine.renderGraphicsEngine;
    }
}
