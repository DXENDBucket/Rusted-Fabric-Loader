package io.github.endx.rustedfabricapi.api.asset.condition;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import io.github.endx.rustedfabricapi.api.registry.ModRegistries;
import io.github.endx.rustedfabricapi.api.registry.ModRegistry;
import io.github.endx.rustedfabricapi.api.registry.tag.RegistryTagDefinition;
import io.github.endx.rustedfabricapi.api.registry.tag.RegistryTagValue;
import io.github.endx.rustedfabricapi.api.util.Identifier;

/** Built-in/custom decoding, diagnostics, registry lookup, and short-circuit checks. */
public final class ResourceConditionContractVerification {
    private ResourceConditionContractVerification() {
    }

    public static void verify() {
        ModRegistry<String> registry = ModRegistries.create(
                "condition_contract:values", String.class);
        registry.register("condition_contract:alpha", "alpha");
        registry.register("condition_contract:beta", "beta");
        registry.freeze();
        registry.tags().applyContribution(Identifier.parse("condition_contract:tags"),
                Arrays.asList(RegistryTagDefinition.of(
                        Identifier.parse("condition_contract:active"), false,
                        Arrays.asList(RegistryTagValue.entry(
                                Identifier.parse("condition_contract:alpha"), true)))));

        ResourceConditionContext context = ResourceConditionContext.builder()
                .loadedMod("alpha_mod")
                .loadedMod("shared_api")
                .registry(registry)
                .build();

        ResourceConditionEvaluation absent = ResourceConditions.evaluate(json("{}"), context);
        require(absent.shouldLoad() && absent.evaluatedCount() == 0
                        && !absent.failedIndex().isPresent(),
                "resource without conditions was not accepted as an empty condition list");

        JsonObject accepted = json("{\"rustedfabric:load_conditions\":["
                + "{\"condition\":\"rustedfabric:all_mods_loaded\","
                + "\"values\":[\"alpha_mod\",\"shared_api\"]},"
                + "{\"condition\":\"rustedfabric:any_mod_loaded\","
                + "\"values\":[\"missing_mod\",\"alpha_mod\"]},"
                + "{\"condition\":\"rustedfabric:registry_contains\","
                + "\"registry\":\"condition_contract:values\","
                + "\"value\":\"condition_contract:beta\"},"
                + "{\"condition\":\"rustedfabric:tag_contains\","
                + "\"registry\":\"condition_contract:values\","
                + "\"tag\":\"condition_contract:active\","
                + "\"value\":\"condition_contract:alpha\"}]}" );
        ResourceConditionEvaluation acceptedResult = ResourceConditions.evaluate(accepted, context);
        require(acceptedResult.shouldLoad() && acceptedResult.evaluatedCount() == 4,
                "built-in mod/registry/tag conditions did not all pass");

        JsonObject nested = json("{\"rustedfabric:load_conditions\":[{"
                + "\"condition\":\"rustedfabric:all\",\"values\":["
                + "{\"condition\":\"rustedfabric:true\"},"
                + "{\"condition\":\"rustedfabric:not\",\"value\":{"
                + "\"condition\":\"rustedfabric:false\"}},"
                + "{\"condition\":\"rustedfabric:any\",\"values\":["
                + "{\"condition\":\"rustedfabric:false\"},"
                + "{\"condition\":\"rustedfabric:true\"}]}]}]}" );
        require(ResourceConditions.shouldLoad(nested, context),
                "nested all/any/not conditions had incorrect boolean semantics");

        AtomicInteger customTests = new AtomicInteger();
        ResourceConditionType custom = ResourceConditions.register(
                "condition_contract:enabled", object -> {
                    if (!object.has("enabled") || !object.get("enabled").isJsonPrimitive()
                            || !object.getAsJsonPrimitive("enabled").isBoolean()) {
                        throw new JsonParseException("enabled must be a boolean");
                    }
                    boolean enabled = object.get("enabled").getAsBoolean();
                    return environment -> {
                        customTests.incrementAndGet();
                        return enabled;
                    };
                });
        require(custom.id().equals(Identifier.parse("condition_contract:enabled"))
                        && ResourceConditions.find(custom.id()).orElse(null) == custom,
                "custom condition registration did not preserve its stable identity");

        JsonObject rejected = json("{\"rustedfabric:load_conditions\":["
                + "{\"condition\":\"condition_contract:enabled\",\"enabled\":false},"
                + "{\"condition\":\"condition_contract:enabled\",\"enabled\":true}]}" );
        ResourceConditionEvaluation rejectedResult = ResourceConditions.evaluate(rejected, context);
        require(!rejectedResult.shouldLoad() && rejectedResult.evaluatedCount() == 1
                        && rejectedResult.failedIndex().orElse(-1) == 0
                        && rejectedResult.failedCondition().orElse(null).equals(custom.id())
                        && customTests.get() == 1,
                "condition list did not report/short-circuit at its first rejection");

        expectJsonFailure(() -> ResourceConditions.shouldLoad(json(
                        "{\"rustedfabric:load_conditions\":[{"
                                + "\"condition\":\"missing:type\"}]}"), context),
                "unknown condition type was silently accepted");
        expectJsonFailure(() -> ResourceConditions.shouldLoad(json(
                        "{\"rustedfabric:load_conditions\":{}}"), context),
                "non-array condition member was silently accepted");
        expectJsonFailure(() -> ResourceConditions.shouldLoad(json(
                        "{\"rustedfabric:load_conditions\":[{"
                                + "\"condition\":\"condition_contract:enabled\"}]}"), context),
                "custom decoder failure was not surfaced as invalid resource JSON");
        expectIllegal(() -> ResourceConditions.register(custom.id(), object -> environment -> true),
                "duplicate condition type registration was accepted");
    }

    private static JsonObject json(String value) {
        return JsonParser.parseString(value).getAsJsonObject();
    }

    private static void expectJsonFailure(Runnable action, String message) {
        try {
            action.run();
            throw new AssertionError(message);
        } catch (JsonParseException expected) {
            // Expected.
        }
    }

    private static void expectIllegal(Runnable action, String message) {
        try {
            action.run();
            throw new AssertionError(message);
        } catch (IllegalArgumentException expected) {
            // Expected.
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
