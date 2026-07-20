package io.github.endx.rustedfabricapi.api.asset.condition;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;

import io.github.endx.rustedfabricapi.api.registry.ModRegistries;
import io.github.endx.rustedfabricapi.api.registry.ModRegistry;
import io.github.endx.rustedfabricapi.api.util.Identifier;
import net.fabricmc.loader.api.FabricLoader;

/** Immutable environment exposed to resource conditions during prepare. */
public final class ResourceConditionContext {
    private final Predicate<String> modLookup;
    private final Function<Identifier, Optional<ModRegistry<?>>> registryLookup;

    private ResourceConditionContext(Predicate<String> modLookup,
            Function<Identifier, Optional<ModRegistry<?>>> registryLookup) {
        this.modLookup = modLookup;
        this.registryLookup = registryLookup;
    }

    /** Live Loader/mod-registry view used by normal Jar resource reloaders. */
    public static ResourceConditionContext runtime() {
        return new ResourceConditionContext(
                modId -> FabricLoader.getInstance().isModLoaded(modId),
                ModRegistries::find);
    }

    /** Explicit immutable context for tools, data validation, and tests. */
    public static Builder builder() { return new Builder(); }

    public boolean isModLoaded(String modId) {
        return modLookup.test(ResourceConditions.validateModId(modId));
    }

    public Optional<ModRegistry<?>> registry(Identifier id) {
        return registryLookup.apply(Objects.requireNonNull(id, "id"));
    }

    public static final class Builder {
        private final Set<String> loadedMods = new LinkedHashSet<String>();
        private final Map<Identifier, ModRegistry<?>> registries =
                new LinkedHashMap<Identifier, ModRegistry<?>>();

        private Builder() {
        }

        public Builder loadedMod(String modId) {
            loadedMods.add(ResourceConditions.validateModId(modId));
            return this;
        }

        public Builder registry(ModRegistry<?> registry) {
            ModRegistry<?> checked = Objects.requireNonNull(registry, "registry");
            ModRegistry<?> previous = registries.get(checked.key().id());
            if (previous != null && previous != checked) {
                throw new IllegalArgumentException("Duplicate registry in condition context: "
                        + checked.key().id());
            }
            registries.put(checked.key().id(), checked);
            return this;
        }

        public ResourceConditionContext build() {
            Set<String> mods = Collections.unmodifiableSet(
                    new LinkedHashSet<String>(loadedMods));
            Map<Identifier, ModRegistry<?>> registrySnapshot = Collections.unmodifiableMap(
                    new LinkedHashMap<Identifier, ModRegistry<?>>(registries));
            return new ResourceConditionContext(mods::contains,
                    id -> Optional.ofNullable(registrySnapshot.get(id)));
        }
    }
}
