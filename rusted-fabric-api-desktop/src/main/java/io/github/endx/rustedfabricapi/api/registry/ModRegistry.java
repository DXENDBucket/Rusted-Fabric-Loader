package io.github.endx.rustedfabricapi.api.registry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import io.github.endx.rustedfabricapi.api.util.Identifier;
import io.github.endx.rustedfabricapi.api.registry.tag.RegistryTags;

/**
 * Insertion-ordered registry for mod extension objects.
 * IDs and raw IDs never change; freeze the registry once all cooperating mods have registered.
 */
public final class ModRegistry<T> implements Iterable<T> {
    private final RegistryKey<T> key;
    private final LinkedHashMap<Identifier, RegistryEntry<T>> byId =
            new LinkedHashMap<Identifier, RegistryEntry<T>>();
    private final IdentityHashMap<T, RegistryEntry<T>> byValue =
            new IdentityHashMap<T, RegistryEntry<T>>();
    private final ArrayList<RegistryEntry<T>> byRawId = new ArrayList<RegistryEntry<T>>();
    private final RegistryEvents<T> events = new RegistryEvents<T>();
    private final RegistryTags<T> tags;
    private boolean frozen;

    ModRegistry(RegistryKey<T> key) {
        this.key = key;
        this.tags = new RegistryTags<T>(this);
    }

    public RegistryKey<T> key() { return key; }

    public RegistryEvents<T> events() { return events; }

    public RegistryTags<T> tags() { return tags; }

    public synchronized RegistryEntry<T> register(Identifier id, T value) {
        if (frozen) throw new IllegalStateException("Registry is frozen: " + key.id());
        Identifier checkedId = Objects.requireNonNull(id, "id");
        T checkedValue = Objects.requireNonNull(value, "value");
        if (!key.valueType().isInstance(checkedValue)) {
            throw new IllegalArgumentException("Value for " + key.id() + " must be a "
                    + key.valueType().getName());
        }
        if (byId.containsKey(checkedId)) {
            throw new IllegalArgumentException("Duplicate registry ID " + checkedId
                    + " in " + key.id());
        }
        if (byValue.containsKey(checkedValue)) {
            throw new IllegalArgumentException("The same value instance is already registered as "
                    + byValue.get(checkedValue).id());
        }
        RegistryEntry<T> entry = new RegistryEntry<T>(key, checkedId, byRawId.size(), checkedValue);
        byId.put(checkedId, entry);
        byValue.put(checkedValue, entry);
        byRawId.add(entry);
        events.AFTER_ENTRY_ADDED.invoker().afterEntryAdded(this, entry);
        return entry;
    }

    public RegistryEntry<T> register(String id, T value) {
        return register(Identifier.parse(id), value);
    }

    public synchronized Optional<T> get(Identifier id) {
        RegistryEntry<T> entry = byId.get(Objects.requireNonNull(id, "id"));
        return entry != null ? Optional.of(entry.value()) : Optional.empty();
    }

    public Optional<T> get(String id) { return get(Identifier.parse(id)); }

    public synchronized T getOrThrow(Identifier id) {
        RegistryEntry<T> entry = byId.get(Objects.requireNonNull(id, "id"));
        if (entry == null) throw new IllegalArgumentException("Unknown registry ID " + id
                + " in " + key.id());
        return entry.value();
    }

    public synchronized Optional<RegistryEntry<T>> entry(Identifier id) {
        return Optional.ofNullable(byId.get(Objects.requireNonNull(id, "id")));
    }

    public synchronized Optional<RegistryEntry<T>> entry(T value) {
        return Optional.ofNullable(byValue.get(Objects.requireNonNull(value, "value")));
    }

    public synchronized Optional<RegistryEntry<T>> entry(int rawId) {
        return rawId >= 0 && rawId < byRawId.size()
                ? Optional.of(byRawId.get(rawId)) : Optional.empty();
    }

    public synchronized boolean containsId(Identifier id) {
        return byId.containsKey(Objects.requireNonNull(id, "id"));
    }

    public synchronized boolean containsValue(T value) {
        return byValue.containsKey(Objects.requireNonNull(value, "value"));
    }

    public synchronized List<RegistryEntry<T>> entries() {
        return Collections.unmodifiableList(new ArrayList<RegistryEntry<T>>(byRawId));
    }

    public synchronized List<Identifier> ids() {
        return Collections.unmodifiableList(new ArrayList<Identifier>(byId.keySet()));
    }

    public synchronized List<T> values() {
        ArrayList<T> result = new ArrayList<T>(byRawId.size());
        for (RegistryEntry<T> entry : byRawId) result.add(entry.value());
        return Collections.unmodifiableList(result);
    }

    public synchronized int size() { return byRawId.size(); }

    public synchronized boolean isEmpty() { return byRawId.isEmpty(); }

    public synchronized boolean isFrozen() { return frozen; }

    public synchronized RegistrySnapshot snapshot() {
        return RegistrySnapshot.of(key.id(), key.valueType().getName(),
                new ArrayList<Identifier>(byId.keySet()), frozen);
    }

    public synchronized boolean freeze() {
        if (frozen) return false;
        events.BEFORE_FREEZE.invoker().onFreeze(this);
        frozen = true;
        events.AFTER_FREEZE.invoker().onFreeze(this);
        return true;
    }

    public Stream<T> stream() { return values().stream(); }

    @Override public Iterator<T> iterator() { return values().iterator(); }

    @Override public String toString() { return "ModRegistry{" + key.id() + ", size=" + size() + '}'; }
}
