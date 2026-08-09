package io.github.endx.rustedfabricapi.impl.ini;

import io.github.endx.rustedfabricapi.api.ini.IniAppliedField;
import io.github.endx.rustedfabricapi.api.ini.IniApplicationPhase;
import io.github.endx.rustedfabricapi.api.ini.IniExtensionException;
import io.github.endx.rustedfabricapi.api.ini.IniExtensions;
import io.github.endx.rustedfabricapi.api.ini.IniFieldContext;
import io.github.endx.rustedfabricapi.api.ini.IniFieldDefinition;
import rustedwarfare.util.UnitConfig;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

/** Internal bridge between the public registry and the mapped game parser. */
public final class IniExtensionRuntime {
    private static final Map<Object, State> STATES =
            Collections.synchronizedMap(new WeakHashMap<Object, State>());

    private IniExtensionRuntime() { }

    public static void index(Object rawConfig) {
        if (!IniExtensions.hasDefinitions()) {
            STATES.remove(rawConfig);
            return;
        }

        UnitConfig config = asConfig(rawConfig);
        State previous = STATES.get(rawConfig);
        Set<Stamp> alreadyApplied = previous != null
                ? previous.applied : Collections.<Stamp>emptySet();
        List<ActiveField<?>> active = new ArrayList<ActiveField<?>>();
        LinkedHashMap sections = config.getSectionMap();
        if (sections != null) {
            for (Object sectionEntryObject : sections.entrySet()) {
                Map.Entry sectionEntry = (Map.Entry) sectionEntryObject;
                String section = String.valueOf(sectionEntry.getKey());
                if (!(sectionEntry.getValue() instanceof Map)) continue;
                for (Object keyEntryObject : ((Map) sectionEntry.getValue()).entrySet()) {
                    Map.Entry keyEntry = (Map.Entry) keyEntryObject;
                    String key = String.valueOf(keyEntry.getKey());
                    if (keyEntry.getValue() == null) continue;
                    String rawValue = String.valueOf(keyEntry.getValue());
                    ActiveField<?> match = findActive(rawConfig, section, key, rawValue);
                    if (match != null) active.add(match);
                }
            }
        }
        Set<Stamp> retained = new HashSet<Stamp>();
        for (ActiveField<?> field : active) {
            if (alreadyApplied.contains(field.stamp)) retained.add(field.stamp);
        }
        STATES.put(rawConfig, new State(active, retained));
    }

    public static String nativeFallback(Object rawConfig, String section, String key) {
        if (!IniExtensions.hasDefinitions()) return null;
        State state = stateFor(rawConfig);
        for (ActiveField<?> field : state.active) {
            if (field.context.section().equalsIgnoreCase(section)
                    && field.context.key().equalsIgnoreCase(key)
                    && field.definition.hasNativeFallback()) {
                return fallback(field);
            }
        }
        return null;
    }

    public static void markActiveFieldsRead(Object rawConfig) {
        if (!IniExtensions.hasDefinitions()) return;
        index(rawConfig);
        UnitConfig config = asConfig(rawConfig);
        State state = STATES.get(rawConfig);
        for (ActiveField<?> field : state.active) {
            config.markKeyReadUnknownType(field.context.section(), field.context.key());
        }
    }

    public static void applyAfterStaticVariables(Object metadata, Object rawConfig) {
        apply(IniApplicationPhase.AFTER_STATIC_VARIABLES, metadata, rawConfig);
    }

    public static void applyBeforeStaticVariables(Object metadata, Object rawConfig) {
        apply(IniApplicationPhase.BEFORE_STATIC_VARIABLES, metadata, rawConfig);
    }

    private static void apply(IniApplicationPhase phase, Object metadata, Object rawConfig) {
        if (!IniExtensions.hasDefinitions()) return;
        index(rawConfig);
        State state = STATES.get(rawConfig);
        for (ActiveField<?> field : state.active) {
            if (field.definition.applicationPhase() != phase) continue;
            if (state.applied.contains(field.stamp)) continue;
            apply(field, metadata);
            state.applied.add(field.stamp);
        }
    }

    private static State stateFor(Object rawConfig) {
        State state = STATES.get(rawConfig);
        if (state == null) {
            index(rawConfig);
            state = STATES.get(rawConfig);
        }
        return state;
    }

    private static ActiveField<?> findActive(Object config, String section, String key, String rawValue) {
        ActiveField<?> result = null;
        IniFieldContext context = new IniFieldContext(config, section, key, rawValue);
        for (IniFieldDefinition<?> definition : IniExtensions.definitions()) {
            if (!definition.matches(section, key)) continue;
            boolean activates;
            try {
                activates = definition.activates(context);
            } catch (RuntimeException failure) {
                throw failure(definition, context, "activation", failure);
            }
            if (!activates) continue;
            if (result != null) {
                throw new IniExtensionException(location(context) + " activates both "
                        + result.definition.qualifiedId() + " and " + definition.qualifiedId());
            }
            result = decode(definition, context);
        }
        return result;
    }

    private static <T> ActiveField<T> decode(IniFieldDefinition<T> definition,
                                              IniFieldContext context) {
        try {
            T value = definition.decode(context);
            definition.validate(context, value);
            return new ActiveField<T>(definition, context, value);
        } catch (IniExtensionException failure) {
            throw failure;
        } catch (Exception failure) {
            throw failure(definition, context, "decode/validation", failure);
        }
    }

    private static <T> String fallbackTyped(ActiveField<T> field) {
        try {
            return field.definition.nativeFallback(field.context, field.value);
        } catch (RuntimeException failure) {
            throw failure(field.definition, field.context, "native fallback", failure);
        }
    }

    @SuppressWarnings("unchecked")
    private static String fallback(ActiveField<?> field) {
        return fallbackTyped((ActiveField<Object>) field);
    }

    private static <T> void applyTyped(ActiveField<T> field, Object metadata) {
        try {
            field.definition.apply(new IniAppliedField<T>(
                    field.definition, metadata, field.context, field.value));
        } catch (IniExtensionException failure) {
            throw failure;
        } catch (Exception failure) {
            throw failure(field.definition, field.context, "application", failure);
        }
    }

    @SuppressWarnings("unchecked")
    private static void apply(ActiveField<?> field, Object metadata) {
        applyTyped((ActiveField<Object>) field, metadata);
    }

    private static IniExtensionException failure(IniFieldDefinition<?> definition,
                                                  IniFieldContext context,
                                                  String stage, Throwable cause) {
        return new IniExtensionException(location(context) + " failed " + stage
                + " for " + definition.qualifiedId() + ": " + cause.getMessage(), cause);
    }

    private static String location(IniFieldContext context) {
        return "[" + context.section() + "] " + context.key() + "=" + context.rawValue();
    }

    private static UnitConfig asConfig(Object value) {
        if (!(value instanceof UnitConfig)) {
            throw new IllegalArgumentException("Expected UnitConfig, got "
                    + (value != null ? value.getClass().getName() : "null"));
        }
        return (UnitConfig) value;
    }

    private static final class ActiveField<T> {
        private final IniFieldDefinition<T> definition;
        private final IniFieldContext context;
        private final T value;
        private final Stamp stamp;

        private ActiveField(IniFieldDefinition<T> definition, IniFieldContext context, T value) {
            this.definition = definition;
            this.context = context;
            this.value = value;
            this.stamp = new Stamp(definition.qualifiedId(), context.section(),
                    context.key(), context.rawValue());
        }
    }

    private static final class State {
        private final List<ActiveField<?>> active;
        private final Set<Stamp> applied;
        private State(List<ActiveField<?>> active, Set<Stamp> applied) {
            this.active = active;
            this.applied = applied;
        }
    }

    private static final class Stamp {
        private final String definition;
        private final String section;
        private final String key;
        private final String raw;

        private Stamp(String definition, String section, String key, String raw) {
            this.definition = definition;
            this.section = section;
            this.key = key;
            this.raw = raw;
        }

        @Override public boolean equals(Object other) {
            if (!(other instanceof Stamp)) return false;
            Stamp that = (Stamp) other;
            return definition.equals(that.definition) && section.equals(that.section)
                    && key.equals(that.key) && raw.equals(that.raw);
        }

        @Override public int hashCode() {
            int result = definition.hashCode();
            result = 31 * result + section.hashCode();
            result = 31 * result + key.hashCode();
            return 31 * result + raw.hashCode();
        }
    }
}
