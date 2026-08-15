package io.github.endx.rustedfabricapi.api.unit.type;

import rustedwarfare.custom.CustomUnitMetadata;
import rustedwarfare.game.Team;
import rustedwarfare.unit.BuiltinUnitType;
import rustedwarfare.unit.Unit;
import rustedwarfare.unit.UnitType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Discovery and safe spawn helpers for built-in and custom unit types. */
public final class UnitTypes {
    private UnitTypes() {
    }

    public static List<BuiltinUnitType> builtIn() {
        BuiltinUnitType[] values = BuiltinUnitType.values();
        List<BuiltinUnitType> result = new ArrayList<BuiltinUnitType>(values.length);
        Collections.addAll(result, values);
        return Collections.unmodifiableList(result);
    }

    /** Returns the currently active custom-unit registry as an immutable snapshot. */
    public static List<CustomUnitMetadata> custom() {
        List<?> active = CustomUnitMetadata.activeCustomUnitTypes;
        if (active == null || active.isEmpty()) return Collections.emptyList();
        List<CustomUnitMetadata> result = new ArrayList<CustomUnitMetadata>(active.size());
        for (Object value : active) {
            if (value instanceof CustomUnitMetadata) result.add((CustomUnitMetadata) value);
        }
        return Collections.unmodifiableList(result);
    }

    public static List<UnitType> all() {
        List<UnitType> result = new ArrayList<UnitType>();
        result.addAll(builtIn());
        result.addAll(custom());
        return Collections.unmodifiableList(result);
    }

    /** Resolves an internal built-in name, custom name, or custom alias. */
    public static Optional<UnitType> find(String internalName) {
        Objects.requireNonNull(internalName, "internalName");
        return Optional.ofNullable(BuiltinUnitType.findByNameWithCustom(internalName, true));
    }

    public static UnitType require(String internalName) {
        return find(internalName).orElseThrow(() ->
                new IllegalArgumentException("Unknown unit type: " + internalName));
    }

    /** Returns the active replacement selected by custom-unit override rules. */
    public static UnitType resolveReplacement(UnitType type) {
        Objects.requireNonNull(type, "type");
        UnitType replacement = CustomUnitMetadata.getReplacement(type);
        return replacement != null ? replacement : type;
    }

    /**
     * Creates a capability/tooltip prototype that is not inserted into the active game world.
     *
     * <p>The native {@link UnitType#createUnit()} method passes the inverse registration flag and
     * creates a live world object. It must not be used for inspection-only prototypes.</p>
     */
    public static Unit createUnregisteredPrototype(UnitType type) {
        Objects.requireNonNull(type, "type");
        int activeBefore = Unit.allUnits.size();
        Unit prototype;
        if (type instanceof BuiltinUnitType) {
            prototype = ((BuiltinUnitType) type).a(true);
        } else if (type instanceof CustomUnitMetadata) {
            prototype = ((CustomUnitMetadata) type).createUnitWithFlag(true);
        } else {
            throw new IllegalArgumentException("Unsupported UnitType implementation: "
                    + type.getClass().getName());
        }
        if (prototype == null) {
            throw new IllegalStateException("Unit type returned a null prototype: "
                    + type.getInternalName());
        }
        if (Unit.allUnits.size() != activeBefore) {
            prototype.removeFromGame();
            throw new IllegalStateException("Unit type registered an inspection prototype: "
                    + type.getInternalName());
        }
        return prototype;
    }

    /** Checks whether a starting unit can be placed at this location; it does not create a unit. */
    public static boolean canSpawnStarting(UnitType type, float x, float y, float direction,
                                           float height, Team team) {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(team, "team");
        requireFinite(x, "x");
        requireFinite(y, "y");
        requireFinite(direction, "direction");
        requireFinite(height, "height");
        return BuiltinUnitType.spawnStartingUnit(type, x, y, direction, height, team);
    }

    /** @deprecated Misleading legacy name; use {@link #canSpawnStarting} or {@code UnitSpawns}. */
    @Deprecated
    public static boolean spawnStarting(UnitType type, float x, float y, float direction,
                                        float height, Team team) {
        return canSpawnStarting(type, x, y, direction, height, team);
    }

    private static void requireFinite(float value, String name) {
        if (!Float.isFinite(value)) throw new IllegalArgumentException(name + " must be finite");
    }
}
