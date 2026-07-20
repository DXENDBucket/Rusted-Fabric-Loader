package io.github.endx.rustedfabricapi.api.unit.type;

import rustedwarfare.custom.CustomUnitMetadata;
import rustedwarfare.game.Team;
import rustedwarfare.unit.BuiltinUnitType;
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
