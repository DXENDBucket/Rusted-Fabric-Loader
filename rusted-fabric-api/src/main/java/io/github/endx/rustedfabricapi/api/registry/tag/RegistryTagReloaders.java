package io.github.endx.rustedfabricapi.api.registry.tag;

import io.github.endx.rustedfabricapi.api.asset.condition.ResourceConditionContext;
import io.github.endx.rustedfabricapi.api.registry.ModRegistry;
import io.github.endx.rustedfabricapi.api.util.Identifier;

/** Factories for data-driven registry tag reloaders. */
public final class RegistryTagReloaders {
    private RegistryTagReloaders() {
    }

    public static <T> RegistryTagJsonReloader<T> json(ModRegistry<T> registry,
            Identifier contributor, String tagNamespace) {
        return new RegistryTagJsonReloader<T>(registry, contributor, tagNamespace);
    }

    public static <T> RegistryTagJsonReloader<T> json(ModRegistry<T> registry,
            String contributor, String tagNamespace) {
        return json(registry, Identifier.parse(contributor), tagNamespace);
    }

    /** Explicit condition context overload for tools, validation, and deterministic tests. */
    public static <T> RegistryTagJsonReloader<T> json(ModRegistry<T> registry,
            Identifier contributor, String tagNamespace,
            ResourceConditionContext conditionContext) {
        return new RegistryTagJsonReloader<T>(registry, contributor, tagNamespace,
                conditionContext);
    }

    public static <T> RegistryTagJsonReloader<T> json(ModRegistry<T> registry,
            String contributor, String tagNamespace,
            ResourceConditionContext conditionContext) {
        return json(registry, Identifier.parse(contributor), tagNamespace, conditionContext);
    }
}
