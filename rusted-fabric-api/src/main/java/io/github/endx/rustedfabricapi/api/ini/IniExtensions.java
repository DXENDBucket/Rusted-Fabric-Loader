package io.github.endx.rustedfabricapi.api.ini;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

/** Process-wide registry for explicitly activated INI extensions. */
public final class IniExtensions {
    private static final CopyOnWriteArrayList<IniFieldDefinition<?>> DEFINITIONS =
            new CopyOnWriteArrayList<IniFieldDefinition<?>>();

    private IniExtensions() { }

    public static Registration register(IniFieldDefinition<?> definition) {
        Objects.requireNonNull(definition, "definition");
        for (IniFieldDefinition<?> existing : DEFINITIONS) {
            if (existing.qualifiedId().equals(definition.qualifiedId())) {
                throw new IllegalArgumentException("Duplicate INI extension ID " + definition.qualifiedId());
            }
        }
        DEFINITIONS.add(definition);
        return new Registration(definition);
    }

    public static List<IniFieldDefinition<?>> definitions() {
        return Collections.unmodifiableList(new ArrayList<IniFieldDefinition<?>>(DEFINITIONS));
    }

    public static boolean hasDefinitions() {
        return !DEFINITIONS.isEmpty();
    }

    public static final class Registration implements AutoCloseable {
        private final IniFieldDefinition<?> definition;
        private final AtomicBoolean closed = new AtomicBoolean();

        private Registration(IniFieldDefinition<?> definition) { this.definition = definition; }
        public IniFieldDefinition<?> definition() { return definition; }
        public boolean unregister() {
            return closed.compareAndSet(false, true) && DEFINITIONS.remove(definition);
        }
        @Override public void close() { unregister(); }
    }
}
