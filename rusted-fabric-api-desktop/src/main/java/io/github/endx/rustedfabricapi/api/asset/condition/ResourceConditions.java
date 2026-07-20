package io.github.endx.rustedfabricapi.api.asset.condition;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import io.github.endx.rustedfabricapi.api.registry.ModRegistry;
import io.github.endx.rustedfabricapi.api.util.Identifier;

/** Extensible Fabric-style condition registry and JSON evaluator. */
public final class ResourceConditions {
    public static final String CONDITIONS_MEMBER = "rustedfabric:load_conditions";
    public static final String TYPE_MEMBER = "condition";

    private static final Map<Identifier, ResourceConditionType> TYPES =
            new LinkedHashMap<Identifier, ResourceConditionType>();

    static {
        registerBuiltin(BuiltinResourceConditions.TRUE, object -> context -> true);
        registerBuiltin(BuiltinResourceConditions.FALSE, object -> context -> false);
        registerBuiltin(BuiltinResourceConditions.ALL_MODS_LOADED,
                object -> modsCondition(object, true));
        registerBuiltin(BuiltinResourceConditions.ANY_MOD_LOADED,
                object -> modsCondition(object, false));
        registerBuiltin(BuiltinResourceConditions.NOT, object -> {
            ResourceCondition nested = decode(requireObject(object, "value"));
            return context -> !nested.test(context);
        });
        registerBuiltin(BuiltinResourceConditions.ALL,
                object -> nestedCondition(object, true));
        registerBuiltin(BuiltinResourceConditions.ANY,
                object -> nestedCondition(object, false));
        registerBuiltin(BuiltinResourceConditions.REGISTRY_CONTAINS, object -> {
            Identifier registryId = requireIdentifier(object, "registry");
            Identifier valueId = requireIdentifier(object, "value");
            return context -> context.registry(registryId)
                    .map(registry -> registry.containsId(valueId)).orElse(false);
        });
        registerBuiltin(BuiltinResourceConditions.TAG_CONTAINS, object -> {
            Identifier registryId = requireIdentifier(object, "registry");
            Identifier tagId = requireIdentifier(object, "tag");
            Identifier valueId = requireIdentifier(object, "value");
            return context -> context.registry(registryId)
                    .map(registry -> tagContains(registry, tagId, valueId)).orElse(false);
        });
    }

    private ResourceConditions() {
    }

    public static ResourceConditionType register(Identifier id,
            ResourceConditionDecoder decoder) {
        Identifier checkedId = Objects.requireNonNull(id, "id");
        ResourceConditionType type = new ResourceConditionType(checkedId,
                Objects.requireNonNull(decoder, "decoder"));
        synchronized (TYPES) {
            if (TYPES.containsKey(checkedId)) {
                throw new IllegalArgumentException("Duplicate resource condition type: "
                        + checkedId);
            }
            TYPES.put(checkedId, type);
        }
        return type;
    }

    public static ResourceConditionType register(String id,
            ResourceConditionDecoder decoder) {
        return register(Identifier.parse(id), decoder);
    }

    public static Optional<ResourceConditionType> find(Identifier id) {
        synchronized (TYPES) {
            return Optional.ofNullable(TYPES.get(Objects.requireNonNull(id, "id")));
        }
    }

    public static List<ResourceConditionType> registeredTypes() {
        synchronized (TYPES) {
            return Collections.unmodifiableList(
                    new ArrayList<ResourceConditionType>(TYPES.values()));
        }
    }

    /** Decodes one object containing a required {@code condition} type member. */
    public static ResourceCondition decode(JsonObject object) {
        JsonObject checked = Objects.requireNonNull(object, "object");
        Identifier id = requireIdentifier(checked, TYPE_MEMBER);
        ResourceConditionType type = find(id).orElseThrow(() ->
                new JsonParseException("Unknown resource condition type: " + id));
        try {
            return type.decode(checked);
        } catch (JsonParseException failure) {
            throw failure;
        } catch (RuntimeException failure) {
            throw new JsonParseException("Could not decode resource condition " + id, failure);
        }
    }

    /** Evaluates a resource object's top-level condition list in order. */
    public static ResourceConditionEvaluation evaluate(JsonObject resource,
            ResourceConditionContext context) {
        JsonObject checkedResource = Objects.requireNonNull(resource, "resource");
        ResourceConditionContext checkedContext = Objects.requireNonNull(context, "context");
        JsonElement member = checkedResource.get(CONDITIONS_MEMBER);
        if (member == null) return new ResourceConditionEvaluation(true, 0, -1, null);
        if (!member.isJsonArray()) {
            throw new JsonParseException(CONDITIONS_MEMBER + " must be an array");
        }
        JsonArray conditions = member.getAsJsonArray();
        int evaluated = 0;
        for (int i = 0; i < conditions.size(); i++) {
            JsonElement element = conditions.get(i);
            if (!element.isJsonObject()) {
                throw new JsonParseException("Resource condition at index " + i
                        + " must be an object");
            }
            JsonObject conditionObject = element.getAsJsonObject();
            Identifier typeId = requireIdentifier(conditionObject, TYPE_MEMBER);
            ResourceCondition condition = decode(conditionObject);
            evaluated++;
            if (!condition.test(checkedContext)) {
                return new ResourceConditionEvaluation(false, evaluated, i, typeId);
            }
        }
        return new ResourceConditionEvaluation(true, evaluated, -1, null);
    }

    public static ResourceConditionEvaluation evaluate(JsonObject resource) {
        return evaluate(resource, ResourceConditionContext.runtime());
    }

    public static boolean shouldLoad(JsonObject resource, ResourceConditionContext context) {
        return evaluate(resource, context).shouldLoad();
    }

    public static boolean shouldLoad(JsonObject resource) {
        return evaluate(resource).shouldLoad();
    }

    static String validateModId(String modId) {
        if (modId == null) throw new NullPointerException("modId");
        String value = modId.trim().toLowerCase(Locale.ROOT);
        if (value.length() < 2 || value.length() > 64) {
            throw new IllegalArgumentException("modId length must be between 2 and 64 characters");
        }
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (!(c >= 'a' && c <= 'z') && !(c >= '0' && c <= '9')
                    && c != '_' && c != '-') {
                throw new IllegalArgumentException("Invalid modId character: " + c);
            }
        }
        return value;
    }

    private static ResourceCondition modsCondition(JsonObject object, boolean requireAll) {
        JsonArray values = requireArray(object, "values");
        ArrayList<String> modIds = new ArrayList<String>(values.size());
        for (int i = 0; i < values.size(); i++) {
            modIds.add(validateModId(requireString(values.get(i), "values[" + i + "]")));
        }
        List<String> immutable = Collections.unmodifiableList(modIds);
        if (requireAll) {
            return context -> {
                for (String modId : immutable) if (!context.isModLoaded(modId)) return false;
                return true;
            };
        }
        return context -> {
            for (String modId : immutable) if (context.isModLoaded(modId)) return true;
            return false;
        };
    }

    private static ResourceCondition nestedCondition(JsonObject object, boolean requireAll) {
        JsonArray values = requireArray(object, "values");
        ArrayList<ResourceCondition> nested = new ArrayList<ResourceCondition>(values.size());
        for (int i = 0; i < values.size(); i++) {
            JsonElement element = values.get(i);
            if (!element.isJsonObject()) {
                throw new JsonParseException("values[" + i + "] must be a condition object");
            }
            nested.add(decode(element.getAsJsonObject()));
        }
        List<ResourceCondition> immutable = Collections.unmodifiableList(nested);
        if (requireAll) {
            return context -> {
                for (ResourceCondition condition : immutable) {
                    if (!condition.test(context)) return false;
                }
                return true;
            };
        }
        return context -> {
            for (ResourceCondition condition : immutable) {
                if (condition.test(context)) return true;
            }
            return false;
        };
    }

    private static boolean tagContains(ModRegistry<?> registry, Identifier tagId,
            Identifier valueId) {
        return registry.tags().get(tagId)
                .map(tag -> tag.ids().contains(valueId)).orElse(false);
    }

    private static void registerBuiltin(Identifier id, ResourceConditionDecoder decoder) {
        ResourceConditionType type = new ResourceConditionType(id, decoder);
        TYPES.put(id, type);
    }

    private static JsonObject requireObject(JsonObject object, String member) {
        JsonElement value = object.get(member);
        if (value == null || !value.isJsonObject()) {
            throw new JsonParseException(member + " must be an object");
        }
        return value.getAsJsonObject();
    }

    private static JsonArray requireArray(JsonObject object, String member) {
        JsonElement value = object.get(member);
        if (value == null || !value.isJsonArray()) {
            throw new JsonParseException(member + " must be an array");
        }
        return value.getAsJsonArray();
    }

    private static Identifier requireIdentifier(JsonObject object, String member) {
        String value = requireString(object.get(member), member);
        try {
            return Identifier.parse(value);
        } catch (IllegalArgumentException failure) {
            throw new JsonParseException("Invalid identifier in " + member + ": " + value,
                    failure);
        }
    }

    private static String requireString(JsonElement element, String member) {
        if (element == null || !element.isJsonPrimitive()
                || !element.getAsJsonPrimitive().isString()) {
            throw new JsonParseException(member + " must be a string");
        }
        return element.getAsString();
    }
}
