package io.github.endx.rustedfabricapi.api.datagen;

import java.util.Objects;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.github.endx.rustedfabricapi.api.asset.condition.BuiltinResourceConditions;
import io.github.endx.rustedfabricapi.api.asset.condition.ResourceConditions;
import io.github.endx.rustedfabricapi.api.registry.RegistryKey;
import io.github.endx.rustedfabricapi.api.registry.tag.RegistryTagKey;
import io.github.endx.rustedfabricapi.api.util.Identifier;

/** Type-safe JSON factories for built-in resource conditions. */
public final class ResourceConditionJson {
    private ResourceConditionJson() {
    }

    public static JsonObject always(boolean value) {
        return type(value ? BuiltinResourceConditions.TRUE : BuiltinResourceConditions.FALSE);
    }

    public static JsonObject allModsLoaded(String... modIds) {
        return mods(BuiltinResourceConditions.ALL_MODS_LOADED, modIds);
    }

    public static JsonObject anyModLoaded(String... modIds) {
        return mods(BuiltinResourceConditions.ANY_MOD_LOADED, modIds);
    }

    public static JsonObject not(JsonObject condition) {
        JsonObject result = type(BuiltinResourceConditions.NOT);
        result.add("value", copy(condition));
        return result;
    }

    public static JsonObject all(JsonObject... conditions) {
        return nested(BuiltinResourceConditions.ALL, conditions);
    }

    public static JsonObject any(JsonObject... conditions) {
        return nested(BuiltinResourceConditions.ANY, conditions);
    }

    public static JsonObject registryContains(RegistryKey<?> registry, Identifier value) {
        return registryContains(Objects.requireNonNull(registry, "registry").id(), value);
    }

    public static JsonObject registryContains(Identifier registry, Identifier value) {
        JsonObject result = type(BuiltinResourceConditions.REGISTRY_CONTAINS);
        result.addProperty("registry", Objects.requireNonNull(registry, "registry").toString());
        result.addProperty("value", Objects.requireNonNull(value, "value").toString());
        return result;
    }

    public static JsonObject tagContains(RegistryTagKey<?> tag, Identifier value) {
        RegistryTagKey<?> checked = Objects.requireNonNull(tag, "tag");
        return tagContains(checked.registry().id(), checked.id(), value);
    }

    public static JsonObject tagContains(Identifier registry, Identifier tag, Identifier value) {
        JsonObject result = type(BuiltinResourceConditions.TAG_CONTAINS);
        result.addProperty("registry", Objects.requireNonNull(registry, "registry").toString());
        result.addProperty("tag", Objects.requireNonNull(tag, "tag").toString());
        result.addProperty("value", Objects.requireNonNull(value, "value").toString());
        return result;
    }

    public static JsonObject custom(Identifier conditionType) {
        return type(Objects.requireNonNull(conditionType, "conditionType"));
    }

    /** Adds a copied top-level condition array and returns the same resource object. */
    public static JsonObject attach(JsonObject resource, JsonObject... conditions) {
        JsonObject checked = Objects.requireNonNull(resource, "resource");
        if (checked.has(ResourceConditions.CONDITIONS_MEMBER)) {
            throw new IllegalArgumentException("Resource already has "
                    + ResourceConditions.CONDITIONS_MEMBER);
        }
        JsonArray values = new JsonArray();
        if (conditions != null) {
            for (JsonObject condition : conditions) values.add(copy(condition));
        }
        checked.add(ResourceConditions.CONDITIONS_MEMBER, values);
        return checked;
    }

    private static JsonObject mods(Identifier type, String... modIds) {
        JsonObject result = type(type);
        JsonArray values = new JsonArray();
        if (modIds != null) {
            for (String modId : modIds) values.add(DataOutput.validateModId(modId));
        }
        result.add("values", values);
        return result;
    }

    private static JsonObject nested(Identifier type, JsonObject... conditions) {
        JsonObject result = type(type);
        JsonArray values = new JsonArray();
        if (conditions != null) {
            for (JsonObject condition : conditions) values.add(copy(condition));
        }
        result.add("values", values);
        return result;
    }

    private static JsonObject type(Identifier id) {
        JsonObject result = new JsonObject();
        result.addProperty(ResourceConditions.TYPE_MEMBER, id.toString());
        return result;
    }

    private static JsonObject copy(JsonObject object) {
        return Objects.requireNonNull(object, "condition").deepCopy();
    }
}
