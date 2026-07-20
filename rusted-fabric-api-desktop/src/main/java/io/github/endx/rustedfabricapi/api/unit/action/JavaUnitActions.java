package io.github.endx.rustedfabricapi.api.unit.action;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import io.github.endx.rustedfabricapi.api.util.Identifier;
import rustedwarfare.unit.UnitType;

/** Process-wide registry and unit-type attachments for Java unit actions. */
public final class JavaUnitActions {
    private static final ConcurrentHashMap<Identifier, JavaUnitAction> ACTIONS =
            new ConcurrentHashMap<Identifier, JavaUnitAction>();
    private static final CopyOnWriteArrayList<JavaUnitActionBinding> BINDINGS =
            new CopyOnWriteArrayList<JavaUnitActionBinding>();

    private JavaUnitActions() {
    }

    public static JavaUnitAction register(JavaUnitAction action) {
        Objects.requireNonNull(action, "action");
        JavaUnitAction existing = ACTIONS.putIfAbsent(action.id(), action);
        if (existing != null && existing != action) {
            throw new IllegalStateException("Java unit action already registered: " + action.id());
        }
        return existing != null ? existing : action;
    }

    public static Optional<JavaUnitAction> find(String id) {
        return Optional.ofNullable(ACTIONS.get(Identifier.parse(id)));
    }

    public static List<JavaUnitAction> registered() {
        ArrayList<JavaUnitAction> result = new ArrayList<JavaUnitAction>(ACTIONS.values());
        result.sort((first, second) -> first.id().compareTo(second.id()));
        return Collections.unmodifiableList(result);
    }

    public static JavaUnitActionBinding attach(String unitTypeName, JavaUnitAction action) {
        return attach(unitTypeName, 0, Integer.MAX_VALUE, action);
    }

    public static JavaUnitActionBinding attach(String unitTypeName, int minimumTechLevel,
            int maximumTechLevel, JavaUnitAction action) {
        JavaUnitAction registered = register(action);
        JavaUnitActionBinding candidate = new JavaUnitActionBinding(unitTypeName,
                minimumTechLevel, maximumTechLevel, registered);
        for (JavaUnitActionBinding binding : BINDINGS) {
            if (binding.action() == registered
                    && binding.minimumTechLevel() == minimumTechLevel
                    && binding.maximumTechLevel() == maximumTechLevel
                    && binding.unitTypeName().equalsIgnoreCase(candidate.unitTypeName())) {
                return binding;
            }
        }
        BINDINGS.add(candidate);
        return candidate;
    }

    public static List<JavaUnitActionBinding> bindings() {
        return Collections.unmodifiableList(new ArrayList<JavaUnitActionBinding>(BINDINGS));
    }

    public static List<JavaUnitAction> forType(UnitType type, int techLevel) {
        Objects.requireNonNull(type, "type");
        if (techLevel < 0) throw new IllegalArgumentException("techLevel must be non-negative");
        String typeName = type.getInternalName();
        ArrayList<JavaUnitAction> result = new ArrayList<JavaUnitAction>();
        for (JavaUnitActionBinding binding : BINDINGS) {
            if (binding.matches(typeName, techLevel) && !result.contains(binding.action())) {
                result.add(binding.action());
            }
        }
        return Collections.unmodifiableList(result);
    }
}
