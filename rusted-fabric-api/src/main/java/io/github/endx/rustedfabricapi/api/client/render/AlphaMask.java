package io.github.endx.rustedfabricapi.api.client.render;

/** Supplies mask alpha in caller-defined local coordinates. */
@FunctionalInterface
public interface AlphaMask {
    /** Returns alpha; values outside zero through one are clamped by the compositor. */
    float alphaAt(float x, float y);
}
