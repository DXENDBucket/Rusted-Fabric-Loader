package io.github.endx.rustedfabricapi.api.registry.tag;

import java.util.List;

import io.github.endx.rustedfabricapi.api.event.RustedFabricEvent;
import io.github.endx.rustedfabricapi.api.util.Identifier;

/** Atomic contribution-apply lifecycle for one registry's tags. */
public final class RegistryTagEvents<T> {
    public final RustedFabricEvent<BeforeApply> BEFORE_APPLY =
            RustedFabricEvent.create(listeners -> (contributor, definitions) -> {
                for (BeforeApply listener : listeners) listener.beforeApply(contributor, definitions);
            });
    public final RustedFabricEvent<AfterApply<T>> AFTER_APPLY =
            RustedFabricEvent.create(listeners -> (contributor, tags) -> {
                for (AfterApply<T> listener : listeners) listener.afterApply(contributor, tags);
            });

    RegistryTagEvents() {
    }

    @FunctionalInterface
    public interface BeforeApply {
        void beforeApply(Identifier contributor, List<RegistryTagDefinition> definitions);
    }

    @FunctionalInterface
    public interface AfterApply<T> {
        void afterApply(Identifier contributor, List<RegistryTag<T>> tags);
    }
}
