package io.github.endx.rustedfabricapi.api.registry.tag;

import java.net.URL;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import io.github.endx.rustedfabricapi.api.asset.ModResourcePack;
import io.github.endx.rustedfabricapi.api.asset.ModResources;
import io.github.endx.rustedfabricapi.api.asset.condition.ResourceConditionContext;
import io.github.endx.rustedfabricapi.api.event.RustedFabricEvent;
import io.github.endx.rustedfabricapi.api.registry.ModRegistries;
import io.github.endx.rustedfabricapi.api.registry.ModRegistry;
import io.github.endx.rustedfabricapi.api.registry.RegistryKey;
import io.github.endx.rustedfabricapi.api.util.Identifier;

/** Multi-contributor merge, replace, reference, rollback, cycle, JSON, and cleanup checks. */
public final class RegistryTagContractVerification {
    private RegistryTagContractVerification() {
    }

    public static void verify() {
        RegistryKey<Widget> registryKey = RegistryKey.of("tag_contract:widgets", Widget.class);
        ModRegistry<Widget> registry = ModRegistries.create(registryKey);
        Widget alpha = new Widget("alpha");
        Widget beta = new Widget("beta");
        Widget gamma = new Widget("gamma");
        registry.register("tag_contract:alpha", alpha);
        registry.register("tag_contract:beta", beta);
        registry.register("tag_contract:gamma", gamma);
        registry.freeze();

        RegistryTagKey<Widget> baseKey = RegistryTagKey.of(registryKey, "contract:base");
        RegistryTagKey<Widget> allKey = RegistryTagKey.of(registryKey, "contract:all");
        AtomicInteger beforeCalls = new AtomicInteger();
        AtomicInteger afterCalls = new AtomicInteger();
        RustedFabricEvent.Registration before = registry.tags().events().BEFORE_APPLY.subscribe(
                (contributor, definitions) -> beforeCalls.incrementAndGet());
        RustedFabricEvent.Registration after = registry.tags().events().AFTER_APPLY.subscribe(
                (contributor, tags) -> afterCalls.incrementAndGet());

        registry.tags().applyContribution(Identifier.parse("contract:base_pack"), Arrays.asList(
                definition("contract:base", false,
                        entry("tag_contract:alpha", true),
                        entry("tag_contract:missing_optional", false)),
                definition("contract:all", false,
                        tag("contract:base", true), entry("tag_contract:beta", true))));
        require(ids(registry, baseKey).equals(Arrays.asList("tag_contract:alpha"))
                        && ids(registry, allKey).equals(Arrays.asList(
                                "tag_contract:alpha", "tag_contract:beta"))
                        && registry.tags().contains(allKey, alpha),
                "base tag contribution did not resolve direct/optional/tag references");

        registry.tags().applyContribution(Identifier.parse("contract:override_pack"), Arrays.asList(
                definition("contract:base", true, entry("tag_contract:gamma", true))));
        require(ids(registry, baseKey).equals(Arrays.asList("tag_contract:gamma"))
                        && ids(registry, allKey).equals(Arrays.asList(
                                "tag_contract:gamma", "tag_contract:beta")),
                "later replace contribution did not rebuild dependent tags");

        List<String> beforeInvalid = ids(registry, allKey);
        expectFailure(() -> registry.tags().applyContribution(
                        Identifier.parse("contract:invalid_pack"), Arrays.asList(
                                definition("contract:broken", false,
                                        entry("tag_contract:not_registered", true)))),
                "required missing registry entry was accepted");
        require(ids(registry, allKey).equals(beforeInvalid)
                        && !registry.tags().contributorIds().contains(
                                Identifier.parse("contract:invalid_pack")),
                "failed contribution partially changed resolved tags");

        expectFailure(() -> registry.tags().applyContribution(
                        Identifier.parse("contract:cycle_pack"), Arrays.asList(
                                definition("contract:cycle_a", false,
                                        tag("contract:cycle_b", true)),
                                definition("contract:cycle_b", false,
                                        tag("contract:cycle_a", true)))),
                "registry tag reference cycle was accepted");

        require(registry.tags().removeContribution(Identifier.parse("contract:override_pack"))
                        && ids(registry, allKey).equals(Arrays.asList(
                                "tag_contract:alpha", "tag_contract:beta")),
                "removing a contribution did not restore remaining definitions");

        ResourceConditionContext conditions = ResourceConditionContext.builder()
                .loadedMod("optional_bridge").registry(registry).build();
        RegistryTagJsonReloader<Widget> json = RegistryTagReloaders.json(registry,
                "contract:json_pack", "contract", conditions);
        try {
            List<RegistryTagDefinition> prepared = json.prepare(testResourcePack());
            require(prepared.size() == 3
                            && "contract:conditional_loaded".equals(
                                    prepared.get(0).tagId().toString())
                            && "contract:json_all".equals(prepared.get(1).tagId().toString())
                            && "contract:json_base".equals(prepared.get(2).tagId().toString()),
                    "JSON tag resources were not condition-filtered/sorted deterministically");
            json.apply(prepared);
        } catch (Exception failure) {
            throw new AssertionError("could not prepare/apply JSON registry tags", failure);
        }
        RegistryTagKey<Widget> jsonAll = RegistryTagKey.of(registryKey, "contract:json_all");
        require(ids(registry, jsonAll).equals(Arrays.asList(
                        "tag_contract:alpha", "tag_contract:beta")),
                "JSON tag loader did not resolve nested and direct values");
        RegistryTagKey<Widget> conditional = RegistryTagKey.of(
                registryKey, "contract:conditional_loaded");
        require(ids(registry, conditional).equals(Arrays.asList("tag_contract:gamma"))
                        && !registry.tags().get(RegistryTagKey.of(
                                registryKey, "contract:conditional_skipped")).isPresent(),
                "JSON tag loader did not apply resource load conditions");
        json.close();
        require(!registry.tags().get(jsonAll).isPresent(),
                "closing JSON tag reloader retained its contribution");

        require(beforeCalls.get() == 7 && afterCalls.get() == 5,
                "tag apply events did not reflect successful/attempted transactions");
        before.close();
        after.close();
    }

    private static ModResourcePack testResourcePack() throws Exception {
        String resource = "data/contract/tags/tag_contract/widgets/json_base.json";
        URL url = RegistryTagContractVerification.class.getClassLoader().getResource(resource);
        if (url == null) throw new AssertionError("missing registry-tag test resource");
        Path root = Path.of(url.toURI());
        for (int i = 0; i < resource.split("/").length; i++) root = root.getParent();
        return ModResources.forDirectory("tag_contract_pack", root);
    }

    private static RegistryTagDefinition definition(String id, boolean replace,
            RegistryTagValue... values) {
        return RegistryTagDefinition.of(Identifier.parse(id), replace, Arrays.asList(values));
    }

    private static RegistryTagValue entry(String id, boolean required) {
        return RegistryTagValue.entry(Identifier.parse(id), required);
    }

    private static RegistryTagValue tag(String id, boolean required) {
        return RegistryTagValue.tag(Identifier.parse(id), required);
    }

    private static List<String> ids(ModRegistry<Widget> registry, RegistryTagKey<Widget> key) {
        RegistryTag<Widget> tag = registry.tags().get(key).orElseThrow(AssertionError::new);
        java.util.ArrayList<String> result = new java.util.ArrayList<String>();
        for (Identifier id : tag.ids()) result.add(id.toString());
        return result;
    }

    private static void expectFailure(Runnable action, String message) {
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

    private static final class Widget {
        final String name;

        Widget(String name) { this.name = name; }
    }
}
