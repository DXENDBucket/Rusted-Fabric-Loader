package io.github.endx.rustedfabricapi.api.registry.tag;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.endx.rustedfabricapi.api.asset.ModResource;
import io.github.endx.rustedfabricapi.api.asset.ModResourcePack;
import io.github.endx.rustedfabricapi.api.asset.condition.ResourceConditionContext;
import io.github.endx.rustedfabricapi.api.asset.condition.ResourceConditions;
import io.github.endx.rustedfabricapi.api.asset.reload.ModResourceReloader;
import io.github.endx.rustedfabricapi.api.registry.ModRegistry;
import io.github.endx.rustedfabricapi.api.util.Identifier;

/** Fabric-style conditional JSON tag loader bound to one registry and contributor. */
public final class RegistryTagJsonReloader<T>
        implements ModResourceReloader<List<RegistryTagDefinition>>, AutoCloseable {
    private final ModRegistry<T> registry;
    private final Identifier contributor;
    private final String namespace;
    private final String prefix;
    private final ResourceConditionContext conditionContext;

    RegistryTagJsonReloader(ModRegistry<T> registry, Identifier contributor, String namespace) {
        this(registry, contributor, namespace, ResourceConditionContext.runtime());
    }

    RegistryTagJsonReloader(ModRegistry<T> registry, Identifier contributor, String namespace,
            ResourceConditionContext conditionContext) {
        this.registry = Objects.requireNonNull(registry, "registry");
        this.contributor = Objects.requireNonNull(contributor, "contributor");
        this.namespace = Identifier.of(namespace, "tag").namespace();
        this.prefix = "data/" + this.namespace + "/tags/"
                + registry.key().id().namespace() + '/' + registry.key().id().path();
        this.conditionContext = Objects.requireNonNull(conditionContext, "conditionContext");
    }

    public ModRegistry<T> registry() { return registry; }

    public Identifier contributor() { return contributor; }

    public String namespace() { return namespace; }

    public String resourcePrefix() { return prefix; }

    @Override
    public List<RegistryTagDefinition> prepare(ModResourcePack resources) throws Exception {
        List<ModResource> files = resources.find(prefix, path -> path.endsWith(".json"));
        ArrayList<RegistryTagDefinition> definitions =
                new ArrayList<RegistryTagDefinition>(files.size());
        String prefixWithSlash = prefix + '/';
        for (ModResource file : files) {
            String path = file.relativePath().toString().replace('\\', '/');
            if (!path.startsWith(prefixWithSlash) || !path.endsWith(".json")) {
                throw new IllegalArgumentException("Tag resource is outside its prefix: " + path);
            }
            String relative = path.substring(prefixWithSlash.length(), path.length() - 5);
            Identifier tagId = Identifier.of(namespace, relative);
            RegistryTagDefinition definition = parse(tagId, file.readUtf8(), path,
                    conditionContext);
            if (definition != null) definitions.add(definition);
        }
        Collections.sort(definitions, Comparator.comparing(definition ->
                definition.tagId().toString()));
        return Collections.unmodifiableList(definitions);
    }

    @Override
    public void apply(List<RegistryTagDefinition> prepared) {
        registry.tags().applyContribution(contributor, prepared);
    }

    @Override
    public void close() {
        registry.tags().removeContribution(contributor);
    }

    private static RegistryTagDefinition parse(Identifier tagId, String json, String path,
            ResourceConditionContext conditionContext) {
        JsonElement root;
        try {
            root = JsonParser.parseString(json);
        } catch (RuntimeException failure) {
            throw new IllegalArgumentException("Invalid registry tag JSON " + path, failure);
        }
        if (!root.isJsonObject()) {
            throw new IllegalArgumentException("Registry tag must be a JSON object: " + path);
        }
        JsonObject object = root.getAsJsonObject();
        try {
            if (!ResourceConditions.shouldLoad(object, conditionContext)) return null;
        } catch (RuntimeException failure) {
            throw new IllegalArgumentException("Invalid resource conditions in registry tag "
                    + path, failure);
        }
        boolean replace = object.has("replace") && requireBoolean(object.get("replace"), path);
        JsonElement valuesElement = object.get("values");
        if (valuesElement == null || !valuesElement.isJsonArray()) {
            throw new IllegalArgumentException("Registry tag values must be an array: " + path);
        }
        JsonArray values = valuesElement.getAsJsonArray();
        ArrayList<RegistryTagValue> parsed = new ArrayList<RegistryTagValue>(values.size());
        for (JsonElement element : values) {
            if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isString()) {
                parsed.add(RegistryTagValue.parse(element.getAsString(), true));
                continue;
            }
            if (!element.isJsonObject()) {
                throw new IllegalArgumentException("Invalid registry tag value in " + path);
            }
            JsonObject value = element.getAsJsonObject();
            JsonElement id = value.get("id");
            if (id == null || !id.isJsonPrimitive() || !id.getAsJsonPrimitive().isString()) {
                throw new IllegalArgumentException("Registry tag object value needs a string id: "
                        + path);
            }
            boolean required = !value.has("required")
                    || requireBoolean(value.get("required"), path);
            parsed.add(RegistryTagValue.parse(id.getAsString(), required));
        }
        return RegistryTagDefinition.of(tagId, replace, parsed);
    }

    private static boolean requireBoolean(JsonElement value, String path) {
        if (value == null || !value.isJsonPrimitive()
                || !value.getAsJsonPrimitive().isBoolean()) {
            throw new IllegalArgumentException("Expected boolean in registry tag " + path);
        }
        return value.getAsBoolean();
    }
}
