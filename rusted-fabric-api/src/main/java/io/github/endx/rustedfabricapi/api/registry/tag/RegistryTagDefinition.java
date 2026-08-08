package io.github.endx.rustedfabricapi.api.registry.tag;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import io.github.endx.rustedfabricapi.api.util.Identifier;

/** Unresolved tag contribution, equivalent to one Fabric-style tag JSON file. */
public final class RegistryTagDefinition {
    private final Identifier tagId;
    private final boolean replace;
    private final List<RegistryTagValue> values;

    private RegistryTagDefinition(Identifier tagId, boolean replace,
            List<RegistryTagValue> values) {
        this.tagId = Objects.requireNonNull(tagId, "tagId");
        ArrayList<RegistryTagValue> copy = new ArrayList<RegistryTagValue>(
                Objects.requireNonNull(values, "values"));
        for (RegistryTagValue value : copy) Objects.requireNonNull(value, "tag value");
        this.replace = replace;
        this.values = Collections.unmodifiableList(copy);
    }

    public static RegistryTagDefinition of(Identifier tagId, boolean replace,
            List<RegistryTagValue> values) {
        return new RegistryTagDefinition(tagId, replace, values);
    }

    public Identifier tagId() { return tagId; }

    public boolean replace() { return replace; }

    public List<RegistryTagValue> values() { return values; }

    @Override public String toString() {
        return "RegistryTagDefinition{" + tagId + ", replace=" + replace
                + ", values=" + values + '}';
    }
}
