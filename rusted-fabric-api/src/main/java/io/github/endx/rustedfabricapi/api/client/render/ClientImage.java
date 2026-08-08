package io.github.endx.rustedfabricapi.api.client.render;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import io.github.endx.rustedfabricapi.api.asset.ModResource;
import io.github.endx.rustedfabricapi.api.client.render.event.ClientImageEvents;
import rustedwarfare.client.render.GameImage;

/** Ownership-aware wrapper around a native game image. */
public final class ClientImage implements AutoCloseable {
    private final GameImage image;
    private final ModResource source;
    private final int width;
    private final int height;
    private final boolean ownsNativeImage;
    private final boolean fallback;
    private final AtomicBoolean closed = new AtomicBoolean();

    ClientImage(GameImage image, ModResource source, boolean ownsNativeImage, boolean fallback) {
        this.image = Objects.requireNonNull(image, "image");
        this.source = source;
        this.width = image.getWidth();
        this.height = image.getHeight();
        this.ownsNativeImage = ownsNativeImage;
        this.fallback = fallback;
    }

    /** Wraps an engine-owned image without assuming ownership of its native data. */
    public static ClientImage borrowed(GameImage image) {
        GameImage checked = Objects.requireNonNull(image, "image");
        return new ClientImage(checked, null, false, checked.isOutOfMemoryFallback());
    }

    public int width() { return width; }
    public int height() { return height; }
    public Optional<ModResource> source() { return Optional.ofNullable(source); }
    public boolean ownsNativeImage() { return ownsNativeImage; }
    public boolean isFallback() { return fallback; }
    public boolean isClosed() { return closed.get(); }

    /** Raw mapped image access for APIs not yet covered by {@link HudDrawContext}. */
    public GameImage nativeImage() { return requireOpen(); }

    public ClientImage smooth(boolean smooth) {
        requireOpen().setSmooth(smooth);
        return this;
    }

    GameImage requireOpen() {
        if (closed.get()) throw new IllegalStateException("Client image has been released");
        return image;
    }

    /** Releases only images owned by this wrapper; borrowed and shared fallback data remain intact. */
    @Override
    public void close() {
        if (!closed.compareAndSet(false, true)) return;
        boolean nativeReleased = ownsNativeImage && !fallback;
        if (nativeReleased) image.releaseImageData();
        ClientImageEvents.AFTER_RELEASE.invoker().afterRelease(this, nativeReleased);
    }

    @Override
    public String toString() {
        return "ClientImage{" + width + 'x' + height
                + ", source=" + source
                + ", owned=" + ownsNativeImage
                + ", fallback=" + fallback
                + ", closed=" + closed + '}';
    }
}
