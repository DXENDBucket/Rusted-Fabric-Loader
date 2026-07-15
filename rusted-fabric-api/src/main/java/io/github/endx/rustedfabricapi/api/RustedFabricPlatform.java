package io.github.endx.rustedfabricapi.api;

import java.util.Locale;

public enum RustedFabricPlatform {
    WINDOWS,
    ANDROID,
    UNKNOWN;

    static RustedFabricPlatform parse(Object value, boolean legacyAndroidFlag) {
        if (value != null) {
            try {
                return valueOf(value.toString().trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ignored) {
                return UNKNOWN;
            }
        }
        return legacyAndroidFlag ? ANDROID : WINDOWS;
    }
}
