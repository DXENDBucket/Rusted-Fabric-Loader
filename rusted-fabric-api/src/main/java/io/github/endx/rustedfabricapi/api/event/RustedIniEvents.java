package io.github.endx.rustedfabricapi.api.event;

import java.io.InputStream;

public final class RustedIniEvents {
    private RustedIniEvents() {
    }

    public static final RustedFabricEvent<BeforeParseStream> BEFORE_PARSE_STREAM =
            RustedFabricEvent.create(listeners -> context -> {
                for (BeforeParseStream listener : listeners) {
                    listener.beforeParseStream(context);
                }
            });

    public static final RustedFabricEvent<AfterParseUnitConfig> AFTER_PARSE_UNIT_CONFIG =
            RustedFabricEvent.create(listeners -> (unitConfig, inputStream) -> {
                for (AfterParseUnitConfig listener : listeners) {
                    listener.afterParseUnitConfig(unitConfig, inputStream);
                }
            });

    public static final RustedFabricEvent<BeforeCopyFrom> BEFORE_COPY_FROM =
            RustedFabricEvent.create(listeners -> (metadata, targetConfig, sourceConfig, copyFromPath, recursionDepth) -> {
                boolean cancelled = false;
                for (BeforeCopyFrom listener : listeners) {
                    cancelled |= listener.beforeCopyFrom(metadata, targetConfig, sourceConfig, copyFromPath, recursionDepth);
                }
                return cancelled;
            });

    public static final RustedFabricEvent<AfterCopyFrom> AFTER_COPY_FROM =
            RustedFabricEvent.create(listeners -> (metadata, targetConfig, sourceConfig, copyFromPath, recursionDepth) -> {
                for (AfterCopyFrom listener : listeners) {
                    listener.afterCopyFrom(metadata, targetConfig, sourceConfig, copyFromPath, recursionDepth);
                }
            });

    public static final RustedFabricEvent<BeforeStaticVariables> BEFORE_STATIC_VARIABLES =
            RustedFabricEvent.create(listeners -> (metadata, unitConfig) -> {
                boolean cancelled = false;
                for (BeforeStaticVariables listener : listeners) {
                    cancelled |= listener.beforeStaticVariables(metadata, unitConfig);
                }
                return cancelled;
            });

    public static final RustedFabricEvent<AfterStaticVariables> AFTER_STATIC_VARIABLES =
            RustedFabricEvent.create(listeners -> (metadata, unitConfig) -> {
                for (AfterStaticVariables listener : listeners) {
                    listener.afterStaticVariables(metadata, unitConfig);
                }
            });

    @FunctionalInterface
    public interface BeforeParseStream {
        void beforeParseStream(ParseStreamContext context);
    }

    @FunctionalInterface
    public interface AfterParseUnitConfig {
        void afterParseUnitConfig(Object unitConfig, InputStream inputStream);
    }

    @FunctionalInterface
    public interface BeforeCopyFrom {
        boolean beforeCopyFrom(Object metadata, Object targetConfig, Object sourceConfig, String copyFromPath, int recursionDepth);
    }

    @FunctionalInterface
    public interface AfterCopyFrom {
        void afterCopyFrom(Object metadata, Object targetConfig, Object sourceConfig, String copyFromPath, int recursionDepth);
    }

    @FunctionalInterface
    public interface BeforeStaticVariables {
        boolean beforeStaticVariables(Object metadata, Object unitConfig);
    }

    @FunctionalInterface
    public interface AfterStaticVariables {
        void afterStaticVariables(Object metadata, Object unitConfig);
    }

    public static final class ParseStreamContext {
        private String unitId;
        private InputStream inputStream;
        private long sourceTimestamp;
        private Object modInfo;
        private Object namedInputStream;
        private String resourceRoot;
        private String templateRoot;
        private boolean cancelled;
        private Object metadataOverride;

        public ParseStreamContext(String unitId, InputStream inputStream, long sourceTimestamp,
                                  Object modInfo, Object namedInputStream, String resourceRoot, String templateRoot) {
            this.unitId = unitId;
            this.inputStream = inputStream;
            this.sourceTimestamp = sourceTimestamp;
            this.modInfo = modInfo;
            this.namedInputStream = namedInputStream;
            this.resourceRoot = resourceRoot;
            this.templateRoot = templateRoot;
        }

        public String unitId() {
            return unitId;
        }

        public void unitId(String unitId) {
            this.unitId = unitId;
        }

        public InputStream inputStream() {
            return inputStream;
        }

        public void inputStream(InputStream inputStream) {
            this.inputStream = inputStream;
        }

        public long sourceTimestamp() {
            return sourceTimestamp;
        }

        public void sourceTimestamp(long sourceTimestamp) {
            this.sourceTimestamp = sourceTimestamp;
        }

        public Object modInfo() {
            return modInfo;
        }

        public void modInfo(Object modInfo) {
            this.modInfo = modInfo;
        }

        public Object namedInputStream() {
            return namedInputStream;
        }

        public void namedInputStream(Object namedInputStream) {
            this.namedInputStream = namedInputStream;
        }

        public String resourceRoot() {
            return resourceRoot;
        }

        public void resourceRoot(String resourceRoot) {
            this.resourceRoot = resourceRoot;
        }

        public String templateRoot() {
            return templateRoot;
        }

        public void templateRoot(String templateRoot) {
            this.templateRoot = templateRoot;
        }

        public boolean cancelled() {
            return cancelled;
        }

        public Object metadataOverride() {
            return metadataOverride;
        }

        public void cancelWith(Object metadataOverride) {
            this.cancelled = true;
            this.metadataOverride = metadataOverride;
        }
    }
}
