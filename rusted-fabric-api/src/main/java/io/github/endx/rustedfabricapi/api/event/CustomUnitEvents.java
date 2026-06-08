package io.github.endx.rustedfabricapi.api.event;

import java.util.List;

public final class CustomUnitEvents {
    private CustomUnitEvents() {
    }

    public static final RustedFabricEvent<BeforeNativeCustomUnitLoad> BEFORE_NATIVE_CUSTOM_UNIT_LOAD =
            RustedFabricEvent.create(listeners -> () -> {
                for (BeforeNativeCustomUnitLoad listener : listeners) {
                    listener.beforeNativeCustomUnitLoad();
                }
            });

    public static final RustedFabricEvent<AfterNativeCustomUnitParseBeforeEnable> AFTER_NATIVE_CUSTOM_UNIT_PARSE_BEFORE_ENABLE =
            RustedFabricEvent.create(listeners -> () -> {
                for (AfterNativeCustomUnitParseBeforeEnable listener : listeners) {
                    listener.afterNativeCustomUnitParseBeforeEnable();
                }
            });

    public static final RustedFabricEvent<BeforeCustomUnitRegistryRebuild> BEFORE_CUSTOM_UNIT_REGISTRY_REBUILD =
            RustedFabricEvent.create(listeners -> includeDisabledMods -> {
                for (BeforeCustomUnitRegistryRebuild listener : listeners) {
                    listener.beforeCustomUnitRegistryRebuild(includeDisabledMods);
                }
            });

    public static final RustedFabricEvent<AfterCustomUnitOverrideAndReplace> AFTER_CUSTOM_UNIT_OVERRIDE_AND_REPLACE =
            RustedFabricEvent.create(listeners -> () -> {
                for (AfterCustomUnitOverrideAndReplace listener : listeners) {
                    listener.afterCustomUnitOverrideAndReplace();
                }
            });

    public static final RustedFabricEvent<AfterCustomUnitLinkGraphBuilt> AFTER_CUSTOM_UNIT_LINK_GRAPH_BUILT =
            RustedFabricEvent.create(listeners -> () -> {
                for (AfterCustomUnitLinkGraphBuilt listener : listeners) {
                    listener.afterCustomUnitLinkGraphBuilt();
                }
            });

    @FunctionalInterface
    public interface BeforeNativeCustomUnitLoad {
        void beforeNativeCustomUnitLoad();
    }

    @FunctionalInterface
    public interface AfterNativeCustomUnitParseBeforeEnable {
        void afterNativeCustomUnitParseBeforeEnable();
    }

    @FunctionalInterface
    public interface BeforeCustomUnitRegistryRebuild {
        void beforeCustomUnitRegistryRebuild(boolean includeDisabledMods);
    }

    @FunctionalInterface
    public interface AfterCustomUnitOverrideAndReplace {
        void afterCustomUnitOverrideAndReplace();
    }

    @FunctionalInterface
    public interface AfterCustomUnitLinkGraphBuilt {
        void afterCustomUnitLinkGraphBuilt();
    }
}
