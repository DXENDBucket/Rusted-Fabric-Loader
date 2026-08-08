package io.github.endx.rustedfabricapi.api.registry;

import io.github.endx.rustedfabricapi.api.event.RustedFabricEvent;

/** Per-registry deterministic lifecycle events. */
public final class RegistryEvents<T> {
    public final RustedFabricEvent<EntryAdded<T>> AFTER_ENTRY_ADDED =
            RustedFabricEvent.create(listeners -> (registry, entry) -> {
                for (EntryAdded<T> listener : listeners) listener.afterEntryAdded(registry, entry);
            });
    public final RustedFabricEvent<Freeze<T>> BEFORE_FREEZE = freezeEvent();
    public final RustedFabricEvent<Freeze<T>> AFTER_FREEZE = freezeEvent();

    RegistryEvents() {
    }

    private RustedFabricEvent<Freeze<T>> freezeEvent() {
        return RustedFabricEvent.create(listeners -> registry -> {
            for (Freeze<T> listener : listeners) listener.onFreeze(registry);
        });
    }

    @FunctionalInterface
    public interface EntryAdded<T> {
        void afterEntryAdded(ModRegistry<T> registry, RegistryEntry<T> entry);
    }

    @FunctionalInterface
    public interface Freeze<T> {
        void onFreeze(ModRegistry<T> registry);
    }
}
