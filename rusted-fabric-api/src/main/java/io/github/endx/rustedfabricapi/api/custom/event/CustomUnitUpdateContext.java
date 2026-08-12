package io.github.endx.rustedfabricapi.api.custom.event;

import io.github.endx.rustedfabricapi.api.custom.CustomUnitHandle;
import rustedwarfare.custom.CustomUnit;

import java.util.Objects;

/** Portable deterministic update context for one native custom-unit instance. */
public final class CustomUnitUpdateContext {
    private final CustomUnitHandle unit;
    private final float deltaFrames;

    public CustomUnitUpdateContext(CustomUnit unit, float deltaFrames) {
        this.unit = CustomUnitHandle.of(Objects.requireNonNull(unit, "unit"));
        this.deltaFrames = deltaFrames;
    }

    public CustomUnitHandle unit() { return unit; }
    public float deltaFrames() { return deltaFrames; }
}
