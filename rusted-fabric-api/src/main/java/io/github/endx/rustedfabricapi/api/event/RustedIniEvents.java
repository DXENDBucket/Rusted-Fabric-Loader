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

    public static final RustedFabricEvent<BeforeKeyRead> BEFORE_KEY_READ =
            RustedFabricEvent.create(listeners -> context -> {
                for (BeforeKeyRead listener : listeners) {
                    listener.beforeKeyRead(context);
                }
            });

    public static final RustedFabricEvent<AfterKeyRead> AFTER_KEY_READ =
            RustedFabricEvent.create(listeners -> context -> {
                for (AfterKeyRead listener : listeners) {
                    listener.afterKeyRead(context);
                }
            });

    public static final RustedFabricEvent<BeforeUnusedKeyCheck> BEFORE_UNUSED_KEY_CHECK =
            RustedFabricEvent.create(listeners -> unitConfig -> {
                for (BeforeUnusedKeyCheck listener : listeners) {
                    listener.beforeUnusedKeyCheck(unitConfig);
                }
            });

    public static final RustedFabricEvent<AfterUnusedKeyCheck> AFTER_UNUSED_KEY_CHECK =
            RustedFabricEvent.create(listeners -> unitConfig -> {
                for (AfterUnusedKeyCheck listener : listeners) {
                    listener.afterUnusedKeyCheck(unitConfig);
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

    @FunctionalInterface
    public interface BeforeKeyRead {
        void beforeKeyRead(KeyReadContext context);
    }

    @FunctionalInterface
    public interface AfterKeyRead {
        void afterKeyRead(KeyReadContext context);
    }

    @FunctionalInterface
    public interface BeforeUnusedKeyCheck {
        void beforeUnusedKeyCheck(Object unitConfig);
    }

    @FunctionalInterface
    public interface AfterUnusedKeyCheck {
        void afterUnusedKeyCheck(Object unitConfig);
    }

    public static final class ParseStreamContext {
        private String unitId;
        private InputStream inputStream;
        private long sourceTimestamp;
        private Object modInfo;
        private Object namedInputStream;
        private String resourceRoot;
        private String templateRoot;
        private Object assetProvider;
        private boolean cancelled;
        private Object metadataOverride;

        public ParseStreamContext(String unitId, InputStream inputStream, long sourceTimestamp,
                                  Object modInfo, Object namedInputStream, String resourceRoot, String templateRoot) {
            this(unitId, inputStream, sourceTimestamp, modInfo, namedInputStream, resourceRoot, templateRoot, null);
        }

        public ParseStreamContext(String unitId, InputStream inputStream, long sourceTimestamp,
                                  Object modInfo, Object namedInputStream, String resourceRoot, String templateRoot,
                                  Object assetProvider) {
            this.unitId = unitId;
            this.inputStream = inputStream;
            this.sourceTimestamp = sourceTimestamp;
            this.modInfo = modInfo;
            this.namedInputStream = namedInputStream;
            this.resourceRoot = resourceRoot;
            this.templateRoot = templateRoot;
            this.assetProvider = assetProvider;
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

        public Object assetProvider() {
            return assetProvider;
        }

        public void assetProvider(Object assetProvider) {
            this.assetProvider = assetProvider;
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

    public static final class KeyReadContext {
        private final Object unitConfig;
        private final String section;
        private final String key;
        private final String valueType;
        private final String sourceMethod;
        private final boolean required;
        private Object rawValue;
        private boolean rawValueOverrideSet;

        public KeyReadContext(Object unitConfig, String section, String key, String valueType,
                              String sourceMethod, boolean required, Object rawValue) {
            this.unitConfig = unitConfig;
            this.section = section;
            this.key = key;
            this.valueType = valueType;
            this.sourceMethod = sourceMethod;
            this.required = required;
            this.rawValue = rawValue;
        }

        public Object unitConfig() {
            return unitConfig;
        }

        public String section() {
            return section;
        }

        public String key() {
            return key;
        }

        public String valueType() {
            return valueType;
        }

        public String sourceMethod() {
            return sourceMethod;
        }

        public boolean required() {
            return required;
        }

        public Object rawValue() {
            return rawValue;
        }

        public boolean rawValueOverrideSet() {
            return rawValueOverrideSet;
        }

        public void rawValue(Object rawValue) {
            this.rawValue = rawValue;
            this.rawValueOverrideSet = true;
        }
    }
}
