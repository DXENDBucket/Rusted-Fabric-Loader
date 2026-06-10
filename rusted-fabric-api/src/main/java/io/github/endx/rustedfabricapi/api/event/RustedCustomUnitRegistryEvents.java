package io.github.endx.rustedfabricapi.api.event;

import java.util.List;
import java.util.Map;

public final class RustedCustomUnitRegistryEvents {
    private RustedCustomUnitRegistryEvents() {
    }

    public static final RustedFabricEvent<AfterMetadataParsed> AFTER_METADATA_PARSED =
            RustedFabricEvent.create(listeners -> (context, metadata) -> {
                Object result = metadata;
                for (AfterMetadataParsed listener : listeners) {
                    result = listener.afterMetadataParsed(context, result);
                }
                return result;
            });

    public static final RustedFabricEvent<BeforePendingRegister> BEFORE_PENDING_REGISTER =
            RustedFabricEvent.create(listeners -> (metadata, pendingSnapshot) -> {
                Object result = metadata;
                for (BeforePendingRegister listener : listeners) {
                    result = listener.beforePendingRegister(result, pendingSnapshot);
                }
                return result;
            });

    public static final RustedFabricEvent<AfterPendingRegister> AFTER_PENDING_REGISTER =
            RustedFabricEvent.create(listeners -> (metadata, pendingSize) -> {
                for (AfterPendingRegister listener : listeners) {
                    listener.afterPendingRegister(metadata, pendingSize);
                }
            });

    public static final RustedFabricEvent<BeforeCommit> BEFORE_COMMIT =
            RustedFabricEvent.create(listeners -> (pendingSnapshot, includeDisabledMods) -> {
                boolean cancelled = false;
                for (BeforeCommit listener : listeners) {
                    cancelled |= listener.beforeCommit(pendingSnapshot, includeDisabledMods);
                }
                return cancelled;
            });

    public static final RustedFabricEvent<AfterCommit> AFTER_COMMIT =
            RustedFabricEvent.create(listeners -> (activeSnapshot, result, replacementMap) -> {
                for (AfterCommit listener : listeners) {
                    listener.afterCommit(activeSnapshot, result, replacementMap);
                }
            });

    public static final RustedFabricEvent<AfterRebuildLinks> AFTER_REBUILD_LINKS =
            RustedFabricEvent.create(listeners -> (activeSnapshot, replacementMap) -> {
                for (AfterRebuildLinks listener : listeners) {
                    listener.afterRebuildLinks(activeSnapshot, replacementMap);
                }
            });

    public static final RustedFabricEvent<AfterValidateLinks> AFTER_VALIDATE_LINKS =
            RustedFabricEvent.create(listeners -> (strict, currentResult) -> {
                boolean result = currentResult;
                for (AfterValidateLinks listener : listeners) {
                    result = listener.afterValidateLinks(strict, result);
                }
                return result;
            });

    @FunctionalInterface
    public interface AfterMetadataParsed {
        Object afterMetadataParsed(RustedIniEvents.ParseStreamContext context, Object metadata);
    }

    @FunctionalInterface
    public interface BeforePendingRegister {
        Object beforePendingRegister(Object metadata, List<Object> pendingSnapshot);
    }

    @FunctionalInterface
    public interface AfterPendingRegister {
        void afterPendingRegister(Object metadata, int pendingSize);
    }

    @FunctionalInterface
    public interface BeforeCommit {
        boolean beforeCommit(List<Object> pendingSnapshot, boolean includeDisabledMods);
    }

    @FunctionalInterface
    public interface AfterCommit {
        void afterCommit(List<Object> activeSnapshot, String result, Map<Object, Object> replacementMap);
    }

    @FunctionalInterface
    public interface AfterRebuildLinks {
        void afterRebuildLinks(List<Object> activeSnapshot, Map<Object, Object> replacementMap);
    }

    @FunctionalInterface
    public interface AfterValidateLinks {
        boolean afterValidateLinks(boolean strict, boolean currentResult);
    }
}
