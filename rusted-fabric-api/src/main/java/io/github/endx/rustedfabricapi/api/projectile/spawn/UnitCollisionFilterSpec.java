package io.github.endx.rustedfabricapi.api.projectile.spawn;

import rustedwarfare.custom.CustomTagList;
import rustedwarfare.unit.MovementType;

import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

/** Optional deterministic replacement for the native ground-only unit contact scan. */
public final class UnitCollisionFilterSpec {
    private static final UnitCollisionFilterSpec NATIVE = new Builder(false).buildInternal();

    private final boolean enabled;
    private final Set<UnitCollisionLayer> layers;
    private final Set<MovementType> movementTypes;
    private final float minHeight;
    private final float maxHeight;
    private final CustomTagList requiredTags;
    private final CustomTagList forbiddenTags;
    private final boolean includeTransported;

    private UnitCollisionFilterSpec(Builder builder) {
        enabled = builder.enabled;
        layers = Collections.unmodifiableSet(EnumSet.copyOf(builder.layers));
        movementTypes = Collections.unmodifiableSet(
                new LinkedHashSet<MovementType>(builder.movementTypes));
        minHeight = finite(builder.minHeight, "minHeight");
        maxHeight = finite(builder.maxHeight, "maxHeight");
        if (maxHeight < minHeight) {
            throw new IllegalArgumentException("maxHeight must be >= minHeight");
        }
        requiredTags = builder.requiredTags;
        forbiddenTags = builder.forbiddenTags;
        includeTransported = builder.includeTransported;
    }

    public static UnitCollisionFilterSpec nativeGroundOnly() { return NATIVE; }
    public static Builder builder() { return new Builder(true); }

    public boolean enabled() { return enabled; }
    public Set<UnitCollisionLayer> layers() { return layers; }
    public Set<MovementType> movementTypes() { return movementTypes; }
    public float minHeight() { return minHeight; }
    public float maxHeight() { return maxHeight; }
    public CustomTagList requiredTags() { return requiredTags; }
    public CustomTagList forbiddenTags() { return forbiddenTags; }
    public boolean includeTransported() { return includeTransported; }

    private static float finite(float value, String name) {
        if (!Float.isFinite(value)) throw new IllegalArgumentException(name + " must be finite");
        return value;
    }

    public static final class Builder {
        private final boolean enabled;
        private final EnumSet<UnitCollisionLayer> layers =
                EnumSet.of(UnitCollisionLayer.GROUND);
        private final Set<MovementType> movementTypes = new LinkedHashSet<MovementType>();
        private float minHeight = -Float.MAX_VALUE;
        private float maxHeight = Float.MAX_VALUE;
        private CustomTagList requiredTags;
        private CustomTagList forbiddenTags;
        private boolean includeTransported;

        private Builder(boolean enabled) { this.enabled = enabled; }

        public Builder layers(Set<UnitCollisionLayer> value) {
            Objects.requireNonNull(value, "layers");
            if (value.isEmpty()) throw new IllegalArgumentException("layers must not be empty");
            layers.clear(); layers.addAll(value); return this;
        }
        public Builder movementTypes(Set<MovementType> value) {
            Objects.requireNonNull(value, "movementTypes");
            movementTypes.clear(); movementTypes.addAll(value); return this;
        }
        public Builder heightRange(float min, float max) {
            minHeight = min; maxHeight = max; return this;
        }
        public Builder requiredTags(CustomTagList value) { requiredTags = value; return this; }
        public Builder forbiddenTags(CustomTagList value) { forbiddenTags = value; return this; }
        public Builder includeTransported(boolean value) { includeTransported = value; return this; }
        public UnitCollisionFilterSpec build() { return new UnitCollisionFilterSpec(this); }
        private UnitCollisionFilterSpec buildInternal() { return new UnitCollisionFilterSpec(this); }
    }
}
