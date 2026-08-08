package io.github.endx.rustedfabricapi.api.text;

import io.github.endx.rustedfabricapi.api.event.RustedFabricEvent;

/** Native language-bundle reload boundaries. */
public final class LanguageEvents {
    public static final RustedFabricEvent<Reload> BEFORE_RELOAD = reload();
    public static final RustedFabricEvent<Reload> AFTER_RELOAD = reload();

    private LanguageEvents() {
    }

    private static RustedFabricEvent<Reload> reload() {
        return RustedFabricEvent.create(listeners -> language -> {
            for (Reload listener : listeners) listener.onReload(language);
        });
    }

    @FunctionalInterface
    public interface Reload {
        void onReload(String language);
    }
}
