package io.github.endx.rustedfabricapi.api.ini;

import java.util.Objects;

/** The original text and location considered by an INI extension. */
public final class IniFieldContext {
    private final Object unitConfig;
    private final String section;
    private final String key;
    private final String rawValue;

    public IniFieldContext(Object unitConfig, String section, String key, String rawValue) {
        this.unitConfig = Objects.requireNonNull(unitConfig, "unitConfig");
        this.section = Objects.requireNonNull(section, "section");
        this.key = Objects.requireNonNull(key, "key");
        this.rawValue = Objects.requireNonNull(rawValue, "rawValue");
    }

    public Object unitConfig() { return unitConfig; }
    public String section() { return section; }
    public String key() { return key; }
    public String rawValue() { return rawValue; }
}
