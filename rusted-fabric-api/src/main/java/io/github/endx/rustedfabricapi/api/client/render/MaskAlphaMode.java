package io.github.endx.rustedfabricapi.api.client.render;

/** Formula used to combine content and mask alpha. */
public enum MaskAlphaMode {
    /** contentAlpha * maskAlpha */
    MULTIPLY,
    /** min(contentAlpha, maskAlpha) */
    MIN,
    /** maskAlpha, retaining the content RGB channels */
    REPLACE
}
