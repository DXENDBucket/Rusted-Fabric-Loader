package io.github.endx.rustedfabricapi.api.ini.action;

import java.util.Objects;

/** Immutable location and raw value supplied while a custom action section is parsed. */
public final class IniActionFieldContext {
    private final Object metadata;
    private final Object unitConfig;
    private final String section;
    private final String actionName;
    private final boolean hiddenAction;
    private final String key;
    private final String rawValue;

    public IniActionFieldContext(Object metadata, Object unitConfig, String section,
                                 String actionName, boolean hiddenAction,
                                 String key, String rawValue) {
        this.metadata = Objects.requireNonNull(metadata, "metadata");
        this.unitConfig = Objects.requireNonNull(unitConfig, "unitConfig");
        this.section = Objects.requireNonNull(section, "section");
        this.actionName = Objects.requireNonNull(actionName, "actionName");
        this.hiddenAction = hiddenAction;
        this.key = Objects.requireNonNull(key, "key");
        this.rawValue = Objects.requireNonNull(rawValue, "rawValue");
    }

    public Object metadata() { return metadata; }
    public Object unitConfig() { return unitConfig; }
    public String section() { return section; }
    public String actionName() { return actionName; }
    public boolean hiddenAction() { return hiddenAction; }
    public String key() { return key; }
    public String rawValue() { return rawValue; }
}
