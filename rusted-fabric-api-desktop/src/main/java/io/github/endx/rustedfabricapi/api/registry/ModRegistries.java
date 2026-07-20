package io.github.endx.rustedfabricapi.api.registry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import io.github.endx.rustedfabricapi.api.util.Identifier;

/** Process-wide root of mod-defined registries. */
public final class ModRegistries {
    private static final Map<Identifier, ModRegistry<?>> REGISTRIES =
            new LinkedHashMap<Identifier, ModRegistry<?>>();

    private ModRegistries() {
    }

    public static <T> ModRegistry<T> create(RegistryKey<T> key) {
        RegistryKey<T> checked = Objects.requireNonNull(key, "key");
        synchronized (REGISTRIES) {
            ModRegistry<?> previous = REGISTRIES.get(checked.id());
            if (previous != null) {
                throw new IllegalArgumentException("Registry ID is already in use: "
                        + checked.id() + " by " + previous.key().valueType().getName());
            }
            ModRegistry<T> registry = new ModRegistry<T>(checked);
            REGISTRIES.put(checked.id(), registry);
            return registry;
        }
    }

    public static <T> ModRegistry<T> create(String id, Class<T> valueType) {
        return create(RegistryKey.of(id, valueType));
    }

    @SuppressWarnings("unchecked")
    public static <T> Optional<ModRegistry<T>> find(RegistryKey<T> key) {
        RegistryKey<T> checked = Objects.requireNonNull(key, "key");
        synchronized (REGISTRIES) {
            ModRegistry<?> registry = REGISTRIES.get(checked.id());
            if (registry == null) return Optional.empty();
            if (!registry.key().valueType().equals(checked.valueType())) {
                throw new IllegalArgumentException("Registry " + checked.id() + " contains "
                        + registry.key().valueType().getName() + ", not "
                        + checked.valueType().getName());
            }
            return Optional.of((ModRegistry<T>) registry);
        }
    }

    public static Optional<ModRegistry<?>> find(Identifier id) {
        synchronized (REGISTRIES) {
            return Optional.ofNullable(REGISTRIES.get(Objects.requireNonNull(id, "id")));
        }
    }

    public static List<ModRegistry<?>> snapshot() {
        synchronized (REGISTRIES) {
            return Collections.unmodifiableList(
                    new ArrayList<ModRegistry<?>>(REGISTRIES.values()));
        }
    }
}
