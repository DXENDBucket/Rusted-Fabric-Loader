package io.github.endx.rustedfabricapi.api.datagen.provider;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import com.google.gson.JsonObject;
import io.github.endx.rustedfabricapi.api.registry.tag.RegistryTagValue;
import io.github.endx.rustedfabricapi.api.util.Identifier;

/** Fluent builder for one generated registry-tag resource. */
public final class RegistryTagBuilder<T> {
    private final Identifier id;
    private final ArrayList<RegistryTagValue> values = new ArrayList<RegistryTagValue>();
    private final Set<String> uniqueValues = new LinkedHashSet<String>();
    private final ArrayList<JsonObject> conditions = new ArrayList<JsonObject>();
    private boolean replace;

    RegistryTagBuilder(Identifier id) { this.id = id; }

    public Identifier id() { return id; }

    public RegistryTagBuilder<T> replace(boolean value) {
        replace = value;
        return this;
    }

    public RegistryTagBuilder<T> add(Identifier entry) {
        return addValue(RegistryTagValue.entry(entry, true));
    }

    public RegistryTagBuilder<T> addOptional(Identifier entry) {
        return addValue(RegistryTagValue.entry(entry, false));
    }

    public RegistryTagBuilder<T> addTag(Identifier tag) {
        return addValue(RegistryTagValue.tag(tag, true));
    }

    public RegistryTagBuilder<T> addOptionalTag(Identifier tag) {
        return addValue(RegistryTagValue.tag(tag, false));
    }

    public RegistryTagBuilder<T> condition(JsonObject condition) {
        conditions.add(Objects.requireNonNull(condition, "condition").deepCopy());
        return this;
    }

    public boolean replaces() { return replace; }

    public List<RegistryTagValue> values() {
        return Collections.unmodifiableList(new ArrayList<RegistryTagValue>(values));
    }

    public List<JsonObject> conditions() {
        ArrayList<JsonObject> copy = new ArrayList<JsonObject>(conditions.size());
        for (JsonObject condition : conditions) copy.add(condition.deepCopy());
        return Collections.unmodifiableList(copy);
    }

    private RegistryTagBuilder<T> addValue(RegistryTagValue value) {
        RegistryTagValue checked = Objects.requireNonNull(value, "value");
        String key = (checked.tagReference() ? "#" : "") + checked.id();
        if (!uniqueValues.add(key)) {
            throw new IllegalArgumentException("Duplicate value " + key + " in tag " + id);
        }
        values.add(checked);
        return this;
    }
}
