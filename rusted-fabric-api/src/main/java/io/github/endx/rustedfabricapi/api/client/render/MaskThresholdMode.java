package io.github.endx.rustedfabricapi.api.client.render;

/** Treatment applied after a mask-alpha threshold. */
public enum MaskThresholdMode {
    /** Discard values below the threshold and retain the others. */
    KEEP,
    /** Convert accepted values to fully opaque. */
    BINARY,
    /** Remap threshold..1 continuously onto 0..1. */
    NORMALIZE
}
