package io.github.endx.rustedfabricapi.api.client.render;

import java.util.Objects;
import java.util.function.Consumer;

import rustedwarfare.framework.GameObject;
import rustedwarfare.render.GraphicsEngine;

/**
 * Frame-scoped access to a native world-layer render boundary.
 *
 * <p>The supplied graphics engine already has Rusted Warfare's world zoom transform and viewport
 * clip applied. Native render helpers such as the Decal renderer can therefore be called directly.
 * The visible-object view retains the game's render order and is valid only during the callback.</p>
 */
public final class WorldLayerDrawContext {
    private final GraphicsEngine graphics;
    private final WorldViewport viewport;
    private final float delta;
    private final GameObject[] visibleObjects;
    private final int visibleObjectCount;

    public WorldLayerDrawContext(GraphicsEngine graphics, WorldViewport viewport, float delta,
                                 GameObject[] visibleObjects, int visibleObjectCount) {
        this.graphics = Objects.requireNonNull(graphics, "graphics");
        this.viewport = Objects.requireNonNull(viewport, "viewport");
        this.delta = delta;
        this.visibleObjects = Objects.requireNonNull(visibleObjects, "visibleObjects");
        this.visibleObjectCount = Math.max(0,
                Math.min(visibleObjectCount, visibleObjects.length));
    }

    public GraphicsEngine graphics() { return graphics; }
    public WorldViewport viewport() { return viewport; }
    public float delta() { return delta; }
    public int visibleObjectCount() { return visibleObjectCount; }

    /** Returns one object in the native visible-world render order. */
    public GameObject visibleObject(int index) {
        if (index < 0 || index >= visibleObjectCount) {
            throw new IndexOutOfBoundsException("visible object index: " + index);
        }
        return visibleObjects[index];
    }

    /** Visits visible objects without allocating a per-frame collection. */
    public void forEachVisibleObject(Consumer<? super GameObject> consumer) {
        Objects.requireNonNull(consumer, "consumer");
        for (int index = 0; index < visibleObjectCount; index++) {
            GameObject object = visibleObjects[index];
            if (object != null) consumer.accept(object);
        }
    }
}
