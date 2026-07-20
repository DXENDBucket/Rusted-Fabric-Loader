package io.github.endx.rustedfabricapi.api.registry.tag;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import io.github.endx.rustedfabricapi.api.registry.ModRegistry;
import io.github.endx.rustedfabricapi.api.registry.RegistryEntry;
import io.github.endx.rustedfabricapi.api.util.Identifier;

/** Transactional, multi-contributor resolved tags for one {@link ModRegistry}. */
public final class RegistryTags<T> {
    private final ModRegistry<T> registry;
    private final RegistryTagEvents<T> events = new RegistryTagEvents<T>();
    private LinkedHashMap<Identifier, List<RegistryTagDefinition>> contributions =
            new LinkedHashMap<Identifier, List<RegistryTagDefinition>>();
    private LinkedHashMap<Identifier, RegistryTag<T>> resolved =
            new LinkedHashMap<Identifier, RegistryTag<T>>();

    public RegistryTags(ModRegistry<T> registry) {
        this.registry = Objects.requireNonNull(registry, "registry");
    }

    public ModRegistry<T> registry() { return registry; }

    public RegistryTagEvents<T> events() { return events; }

    public synchronized Optional<RegistryTag<T>> get(RegistryTagKey<T> key) {
        requireRegistry(key);
        return Optional.ofNullable(resolved.get(key.id()));
    }

    public synchronized Optional<RegistryTag<T>> get(Identifier id) {
        return Optional.ofNullable(resolved.get(Objects.requireNonNull(id, "id")));
    }

    public synchronized boolean contains(RegistryTagKey<T> key, T value) {
        return get(key).map(tag -> tag.contains(value)).orElse(false);
    }

    public synchronized List<RegistryTag<T>> snapshot() {
        return Collections.unmodifiableList(new ArrayList<RegistryTag<T>>(resolved.values()));
    }

    public synchronized List<Identifier> contributorIds() {
        return Collections.unmodifiableList(new ArrayList<Identifier>(contributions.keySet()));
    }

    /** Replaces one contributor's previous definitions and atomically recomputes every tag. */
    public synchronized void applyContribution(Identifier contributor,
            List<RegistryTagDefinition> definitions) {
        Identifier checkedContributor = Objects.requireNonNull(contributor, "contributor");
        List<RegistryTagDefinition> checkedDefinitions = immutableDefinitions(definitions);
        events.BEFORE_APPLY.invoker().beforeApply(checkedContributor, checkedDefinitions);
        LinkedHashMap<Identifier, List<RegistryTagDefinition>> prospective =
                new LinkedHashMap<Identifier, List<RegistryTagDefinition>>(contributions);
        prospective.put(checkedContributor, checkedDefinitions);
        LinkedHashMap<Identifier, RegistryTag<T>> next = resolve(prospective);
        contributions = prospective;
        resolved = next;
        events.AFTER_APPLY.invoker().afterApply(checkedContributor, snapshot());
    }

    public synchronized boolean removeContribution(Identifier contributor) {
        Identifier checked = Objects.requireNonNull(contributor, "contributor");
        if (!contributions.containsKey(checked)) return false;
        events.BEFORE_APPLY.invoker().beforeApply(checked, Collections.emptyList());
        LinkedHashMap<Identifier, List<RegistryTagDefinition>> prospective =
                new LinkedHashMap<Identifier, List<RegistryTagDefinition>>(contributions);
        prospective.remove(checked);
        LinkedHashMap<Identifier, RegistryTag<T>> next = resolve(prospective);
        contributions = prospective;
        resolved = next;
        events.AFTER_APPLY.invoker().afterApply(checked, snapshot());
        return true;
    }

    private LinkedHashMap<Identifier, RegistryTag<T>> resolve(
            LinkedHashMap<Identifier, List<RegistryTagDefinition>> sources) {
        LinkedHashMap<Identifier, List<RegistryTagValue>> merged =
                new LinkedHashMap<Identifier, List<RegistryTagValue>>();
        for (List<RegistryTagDefinition> definitions : sources.values()) {
            for (RegistryTagDefinition definition : definitions) {
                List<RegistryTagValue> values = merged.computeIfAbsent(definition.tagId(),
                        ignored -> new ArrayList<RegistryTagValue>());
                if (definition.replace()) values.clear();
                values.addAll(definition.values());
            }
        }
        LinkedHashMap<Identifier, RegistryTag<T>> result =
                new LinkedHashMap<Identifier, RegistryTag<T>>();
        Map<Identifier, Visit> visits = new HashMap<Identifier, Visit>();
        for (Identifier id : merged.keySet()) resolveOne(id, merged, result, visits);
        return result;
    }

    private RegistryTag<T> resolveOne(Identifier id,
            LinkedHashMap<Identifier, List<RegistryTagValue>> merged,
            LinkedHashMap<Identifier, RegistryTag<T>> result, Map<Identifier, Visit> visits) {
        RegistryTag<T> existing = result.get(id);
        if (existing != null) return existing;
        if (visits.get(id) == Visit.VISITING) {
            throw new IllegalArgumentException("Registry tag reference cycle at #" + id);
        }
        visits.put(id, Visit.VISITING);
        LinkedHashSet<RegistryEntry<T>> entries = new LinkedHashSet<RegistryEntry<T>>();
        List<RegistryTagValue> values = merged.get(id);
        if (values == null) throw new IllegalArgumentException("Unknown registry tag #" + id);
        for (RegistryTagValue value : values) {
            if (value.tagReference()) {
                if (!merged.containsKey(value.id())) {
                    if (value.required()) {
                        throw new IllegalArgumentException("Missing required registry tag #"
                                + value.id() + " referenced by #" + id);
                    }
                    continue;
                }
                entries.addAll(resolveOne(value.id(), merged, result, visits).entries());
            } else {
                Optional<RegistryEntry<T>> entry = registry.entry(value.id());
                if (!entry.isPresent()) {
                    if (value.required()) {
                        throw new IllegalArgumentException("Missing required registry entry "
                                + value.id() + " referenced by #" + id);
                    }
                    continue;
                }
                entries.add(entry.get());
            }
        }
        RegistryTag<T> tag = new RegistryTag<T>(RegistryTagKey.of(registry.key(), id),
                new ArrayList<RegistryEntry<T>>(entries));
        visits.put(id, Visit.RESOLVED);
        result.put(id, tag);
        return tag;
    }

    private void requireRegistry(RegistryTagKey<T> key) {
        if (!registry.key().equals(Objects.requireNonNull(key, "key").registry())) {
            throw new IllegalArgumentException("Tag key belongs to another registry: " + key);
        }
    }

    private static List<RegistryTagDefinition> immutableDefinitions(
            List<RegistryTagDefinition> definitions) {
        ArrayList<RegistryTagDefinition> copy = new ArrayList<RegistryTagDefinition>(
                Objects.requireNonNull(definitions, "definitions"));
        for (RegistryTagDefinition definition : copy) {
            Objects.requireNonNull(definition, "definition");
        }
        return Collections.unmodifiableList(copy);
    }

    private enum Visit { VISITING, RESOLVED }
}
