package io.github.endx.rustedfabricapi.api.registry.tag;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import io.github.endx.rustedfabricapi.api.registry.RegistryEntry;
import io.github.endx.rustedfabricapi.api.util.Identifier;

/** Immutable, insertion-ordered resolved registry tag. */
public final class RegistryTag<T> implements Iterable<T> {
    private final RegistryTagKey<T> key;
    private final List<RegistryEntry<T>> entries;
    private final Map<T, Boolean> values;

    RegistryTag(RegistryTagKey<T> key, List<RegistryEntry<T>> entries) {
        this.key = key;
        this.entries = Collections.unmodifiableList(
                new ArrayList<RegistryEntry<T>>(entries));
        IdentityHashMap<T, Boolean> indexed = new IdentityHashMap<T, Boolean>();
        for (RegistryEntry<T> entry : entries) indexed.put(entry.value(), Boolean.TRUE);
        this.values = indexed;
    }

    public RegistryTagKey<T> key() { return key; }

    public List<RegistryEntry<T>> entries() { return entries; }

    public List<Identifier> ids() {
        ArrayList<Identifier> result = new ArrayList<Identifier>(entries.size());
        for (RegistryEntry<T> entry : entries) result.add(entry.id());
        return Collections.unmodifiableList(result);
    }

    public List<T> values() {
        ArrayList<T> result = new ArrayList<T>(entries.size());
        for (RegistryEntry<T> entry : entries) result.add(entry.value());
        return Collections.unmodifiableList(result);
    }

    public boolean contains(T value) { return value != null && values.containsKey(value); }

    public boolean contains(RegistryEntry<T> entry) {
        return entry != null && entries.contains(entry);
    }

    public int size() { return entries.size(); }

    public boolean isEmpty() { return entries.isEmpty(); }

    @Override public Iterator<T> iterator() { return values().iterator(); }

    @Override public String toString() { return "RegistryTag{" + key.id() + ", size=" + size() + '}'; }
}
