package io.github.endx.rustedfabricapi.api.custom.event;

import io.github.endx.rustedfabricapi.api.custom.CustomUnits;
import io.github.endx.rustedfabricapi.api.event.RustedFabricEvent;
import rustedwarfare.custom.CustomUnitMetadata;

import java.util.List;

/** Typed phases of native custom-unit discovery and registry rebuilding. */
public final class CustomUnitRegistryEvents {
    public static final RustedFabricEvent<LoadPhase> BEFORE_NATIVE_LOAD = loadPhase();
    public static final RustedFabricEvent<LoadPhase> AFTER_PARSE_BEFORE_ENABLE = loadPhase();
    public static final RustedFabricEvent<BeforeRebuild> BEFORE_REGISTRY_REBUILD =
            RustedFabricEvent.create(listeners -> (includeDisabledMods, pendingTypes) -> {
                for (BeforeRebuild listener : listeners) {
                    listener.beforeRebuild(includeDisabledMods, pendingTypes);
                }
            });
    public static final RustedFabricEvent<RegistryPhase> AFTER_OVERRIDE_AND_REPLACE = registryPhase();
    public static final RustedFabricEvent<RegistryPhase> AFTER_ACTION_LINKS_BUILT = registryPhase();

    private CustomUnitRegistryEvents() {
    }

    private static RustedFabricEvent<LoadPhase> loadPhase() {
        return RustedFabricEvent.create(listeners -> pendingTypes -> {
            for (LoadPhase listener : listeners) {
                listener.onLoadPhase(pendingTypes);
            }
        });
    }

    private static RustedFabricEvent<RegistryPhase> registryPhase() {
        return RustedFabricEvent.create(listeners -> activeTypes -> {
            for (RegistryPhase listener : listeners) {
                listener.onRegistryPhase(activeTypes);
            }
        });
    }

    public static List<CustomUnitMetadata> activeTypes() {
        return CustomUnits.activeTypes();
    }

    @FunctionalInterface
    public interface LoadPhase {
        void onLoadPhase(List<CustomUnitMetadata> pendingTypes);
    }

    @FunctionalInterface
    public interface BeforeRebuild {
        void beforeRebuild(boolean includeDisabledMods, List<CustomUnitMetadata> pendingTypes);
    }

    @FunctionalInterface
    public interface RegistryPhase {
        void onRegistryPhase(List<CustomUnitMetadata> activeTypes);
    }
}
