package io.github.endx.rustedfabricapi.api.datagen.provider;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.github.endx.rustedfabricapi.api.asset.condition.ResourceConditions;
import io.github.endx.rustedfabricapi.api.datagen.DataOutput;
import io.github.endx.rustedfabricapi.api.datagen.DataProvider;
import io.github.endx.rustedfabricapi.api.registry.RegistryKey;
import io.github.endx.rustedfabricapi.api.registry.tag.RegistryTagValue;
import io.github.endx.rustedfabricapi.api.util.Identifier;

/** Base provider for the Registry Tag JSON layout consumed at runtime. */
public abstract class RegistryTagDataProvider<T> implements DataProvider {
    private final RegistryKey<T> registry;
    private final String tagNamespace;

    protected RegistryTagDataProvider(RegistryKey<T> registry, String tagNamespace) {
        this.registry = Objects.requireNonNull(registry, "registry");
        this.tagNamespace = Identifier.of(Objects.requireNonNull(tagNamespace, "tagNamespace"),
                "tag").namespace();
    }

    public RegistryKey<T> registry() { return registry; }

    public String tagNamespace() { return tagNamespace; }

    @Override public final void generate(DataOutput output) {
        TagCollection tags = new TagCollection();
        generateTags(tags);
        ArrayList<RegistryTagBuilder<T>> ordered =
                new ArrayList<RegistryTagBuilder<T>>(tags.values());
        Collections.sort(ordered, Comparator.comparing(tag -> tag.id().toString()));
        for (RegistryTagBuilder<T> tag : ordered) {
            output.writeJson(resourcePath(tag.id()), toJson(tag));
        }
    }

    protected abstract void generateTags(TagLookup<T> tags);

    public interface TagLookup<T> {
        RegistryTagBuilder<T> tag(Identifier id);

        default RegistryTagBuilder<T> tag(String id) { return tag(Identifier.parse(id)); }
    }

    private String resourcePath(Identifier id) {
        return "data/" + tagNamespace + "/tags/" + registry.id().namespace() + '/'
                + registry.id().path() + '/' + id.path() + ".json";
    }

    private JsonObject toJson(RegistryTagBuilder<T> tag) {
        JsonObject result = new JsonObject();
        if (!tag.conditions().isEmpty()) {
            JsonArray conditions = new JsonArray();
            for (JsonObject condition : tag.conditions()) conditions.add(condition);
            result.add(ResourceConditions.CONDITIONS_MEMBER, conditions);
        }
        result.addProperty("replace", tag.replaces());
        JsonArray values = new JsonArray();
        for (RegistryTagValue value : tag.values()) {
            String id = (value.tagReference() ? "#" : "") + value.id();
            if (value.required()) {
                values.add(id);
            } else {
                JsonObject optional = new JsonObject();
                optional.addProperty("id", id);
                optional.addProperty("required", false);
                values.add(optional);
            }
        }
        result.add("values", values);
        return result;
    }

    private final class TagCollection implements TagLookup<T> {
        private final Map<Identifier, RegistryTagBuilder<T>> tags =
                new LinkedHashMap<Identifier, RegistryTagBuilder<T>>();

        @Override public RegistryTagBuilder<T> tag(Identifier id) {
            Identifier checked = Objects.requireNonNull(id, "id");
            if (!tagNamespace.equals(checked.namespace())) {
                throw new IllegalArgumentException("Tag belongs to namespace "
                        + checked.namespace() + ", expected " + tagNamespace);
            }
            return tags.computeIfAbsent(checked, RegistryTagBuilder<T>::new);
        }

        List<RegistryTagBuilder<T>> values() {
            return new ArrayList<RegistryTagBuilder<T>>(tags.values());
        }
    }
}
