package io.github.endx.rustedfabricapi.api.logic;

import io.github.endx.rustedfabricapi.api.util.RustedReflection;
import io.github.endx.rustedfabricapi.impl.logic.RegisteredLogicUnitProperty;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Registry for parameterless {@code self.*} properties parsed by native LogicBoolean. */
public final class LogicUnitProperties {
    private static final String[] LOGIC_BOOLEAN_CLASSES = {
            "rustedwarfare.custom.logic.LogicBoolean",
            "com.corrodinggames.rts.game.units.custom.logicBooleans.LogicBoolean"
    };
    private static final Object LOCK = new Object();
    private static final Map<String, LogicUnitPropertyDefinition> DEFINITIONS =
            new LinkedHashMap<String, LogicUnitPropertyDefinition>();

    private LogicUnitProperties() { }

    public static void register(LogicUnitPropertyDefinition definition) {
        LogicUnitPropertyDefinition checked = Objects.requireNonNull(definition, "definition");
        synchronized (LOCK) {
            if (DEFINITIONS.containsKey(checked.canonicalName())) {
                throw new IllegalArgumentException("duplicate LogicBoolean unit property: "
                        + checked.name());
            }
            RustedReflection.invokeStatic(LOGIC_BOOLEAN_CLASSES, new String[]{"addBooleanType"},
                    RegisteredLogicUnitProperty.create(checked), new String[]{checked.name()});
            DEFINITIONS.put(checked.canonicalName(), checked);
        }
    }

    public static boolean isRegistered(String name) {
        if (name == null) return false;
        synchronized (LOCK) {
            return DEFINITIONS.containsKey(name.trim().toLowerCase(java.util.Locale.ROOT));
        }
    }

    public static List<LogicUnitPropertyDefinition> definitions() {
        synchronized (LOCK) {
            return Collections.unmodifiableList(
                    new ArrayList<LogicUnitPropertyDefinition>(DEFINITIONS.values()));
        }
    }
}
