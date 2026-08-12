package io.github.endx.rustedfabricapi.api.custom.event;

import io.github.endx.rustedfabricapi.api.custom.CustomUnitHandle;
import rustedwarfare.custom.CustomUnit;
import rustedwarfare.custom.CustomUnitMetadata;

/** Namespace-neutral snapshot of an INI custom unit metadata application or conversion. */
public final class CustomUnitMetadataApplyContext {
    private final CustomUnitHandle unit;
    private final String oldTypeName;
    private final String newTypeName;
    private final boolean conversion;
    private final boolean initial;

    CustomUnitMetadataApplyContext(CustomUnit unit, CustomUnitMetadata oldMetadata,
                                   CustomUnitMetadata newMetadata,
                                   boolean conversion, boolean initial) {
        this.unit = CustomUnitHandle.of(unit);
        this.oldTypeName = oldMetadata == null ? null : oldMetadata.getInternalName();
        this.newTypeName = newMetadata == null ? null : newMetadata.getInternalName();
        this.conversion = conversion;
        this.initial = initial;
    }

    public CustomUnitHandle unit() { return unit; }
    public String oldTypeName() { return oldTypeName; }
    public String newTypeName() { return newTypeName; }
    public boolean conversion() { return conversion; }
    public boolean initial() { return initial; }
}
