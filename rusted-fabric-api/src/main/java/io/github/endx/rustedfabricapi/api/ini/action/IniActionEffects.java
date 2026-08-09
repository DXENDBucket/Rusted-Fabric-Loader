package io.github.endx.rustedfabricapi.api.ini.action;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

/** Process-wide registry for Java-backed custom-unit action fields. */
public final class IniActionEffects {
    private static final CopyOnWriteArrayList<IniActionEffectDefinition<?>> DEFINITIONS =
            new CopyOnWriteArrayList<IniActionEffectDefinition<?>>();

    private IniActionEffects() { }

    public static Registration register(IniActionEffectDefinition<?> definition) {
        Objects.requireNonNull(definition, "definition");
        for (IniActionEffectDefinition<?> existing : DEFINITIONS) {
            if (existing.qualifiedId().equals(definition.qualifiedId())) {
                throw new IllegalArgumentException(
                        "Duplicate INI action effect ID " + definition.qualifiedId());
            }
            if (existing.key().equals(definition.key())
                    && existing.scope().overlaps(definition.scope())) {
                throw new IllegalArgumentException("INI action field " + definition.key()
                        + " overlaps " + existing.qualifiedId());
            }
        }
        DEFINITIONS.add(definition);
        return new Registration(definition);
    }

    public static List<IniActionEffectDefinition<?>> definitions() {
        ArrayList<IniActionEffectDefinition<?>> result =
                new ArrayList<IniActionEffectDefinition<?>>(DEFINITIONS);
        // TimSort is stable, preserving registration order for equal priorities.
        Collections.sort(result, Comparator.comparingInt(
                IniActionEffectDefinition<?>::priority).reversed());
        return Collections.unmodifiableList(result);
    }

    public static boolean hasDefinitions() { return !DEFINITIONS.isEmpty(); }

    public static final class Registration implements AutoCloseable {
        private final IniActionEffectDefinition<?> definition;
        private final AtomicBoolean closed = new AtomicBoolean();

        private Registration(IniActionEffectDefinition<?> definition) {
            this.definition = definition;
        }

        public IniActionEffectDefinition<?> definition() { return definition; }
        public boolean unregister() {
            return closed.compareAndSet(false, true) && DEFINITIONS.remove(definition);
        }
        @Override public void close() { unregister(); }
    }
}
