package io.github.endx.iniessentials;

import rustedwarfare.custom.CustomUnit;
import rustedwarfare.unit.Unit;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

public final class NegativeHpPolicy {
    private static final Map<Object, Boolean> ENABLED_METADATA =
            Collections.synchronizedMap(new WeakHashMap<Object, Boolean>());

    private NegativeHpPolicy() { }

    public static void configure(Object metadata, boolean enabled) {
        if (metadata == null) return;
        if (enabled) {
            ENABLED_METADATA.put(metadata, Boolean.TRUE);
        } else {
            ENABLED_METADATA.remove(metadata);
        }
    }

    public static boolean allows(Unit unit) {
        return unit instanceof CustomUnit
                && ENABLED_METADATA.containsKey(((CustomUnit) unit).unitMetadata);
    }
}
