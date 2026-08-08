package io.github.endx.rustedfabricapi.api.registry.tag;

import java.util.Objects;

import io.github.endx.rustedfabricapi.api.util.Identifier;

/** One required or optional direct-entry/tag reference in a tag definition. */
public final class RegistryTagValue {
    private final Identifier id;
    private final boolean tagReference;
    private final boolean required;

    private RegistryTagValue(Identifier id, boolean tagReference, boolean required) {
        this.id = Objects.requireNonNull(id, "id");
        this.tagReference = tagReference;
        this.required = required;
    }

    public static RegistryTagValue entry(Identifier id, boolean required) {
        return new RegistryTagValue(id, false, required);
    }

    public static RegistryTagValue tag(Identifier id, boolean required) {
        return new RegistryTagValue(id, true, required);
    }

    public static RegistryTagValue parse(String value, boolean required) {
        Objects.requireNonNull(value, "value");
        boolean tag = value.startsWith("#");
        String id = tag ? value.substring(1) : value;
        return new RegistryTagValue(Identifier.parse(id), tag, required);
    }

    public Identifier id() { return id; }

    public boolean tagReference() { return tagReference; }

    public boolean required() { return required; }

    @Override public String toString() {
        return (tagReference ? "#" : "") + id + (required ? "" : "?");
    }
}
