package io.github.endx.rustedfabricapi.api.fog;

import io.github.endx.rustedfabricapi.api.geometry.GeometryMask;

@FunctionalInterface
public interface FogMaskProvider {
    /** Returns the current world-space mask, or null to skip this update. */
    GeometryMask currentMask();
}
