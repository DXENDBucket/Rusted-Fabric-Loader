package io.github.endx.rustedfabricapi.api.custom;

import rustedwarfare.custom.CustomUnit;
import rustedwarfare.custom.CustomUnitLoader;
import rustedwarfare.custom.CustomUnitMetadata;
import rustedwarfare.unit.Unit;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Typed access to loaded custom-unit definitions and instances. */
public final class CustomUnits {
    private CustomUnits() {
    }

    /** Returns an immutable snapshot of currently active custom-unit definitions. */
    public static List<CustomUnitMetadata> activeTypes() {
        return snapshot(CustomUnitMetadata.activeCustomUnitTypes);
    }

    /** Returns definitions parsed during the current load but not necessarily enabled yet. */
    public static List<CustomUnitMetadata> pendingTypes() {
        return snapshot(CustomUnitMetadata.pendingCustomUnitTypes);
    }

    public static Optional<CustomUnitMetadata> findType(String nameOrAlias) {
        Objects.requireNonNull(nameOrAlias, "nameOrAlias");
        return Optional.ofNullable(CustomUnitMetadata.findByNameOrAlias(nameOrAlias));
    }

    public static boolean isCustomUnit(Unit unit) {
        return unit instanceof CustomUnit;
    }

    public static Optional<CustomUnit> asCustomUnit(Unit unit) {
        return unit instanceof CustomUnit
                ? Optional.of((CustomUnit) unit)
                : Optional.empty();
    }

    public static Optional<CustomUnitMetadata> metadata(Unit unit) {
        return unit instanceof CustomUnit
                ? Optional.ofNullable(((CustomUnit) unit).getUnitMetadata())
                : Optional.empty();
    }

    /** Creates a unit instance without adding it to the world. Call on the update thread. */
    public static Unit create(CustomUnitMetadata metadata) {
        return Objects.requireNonNull(metadata, "metadata").createUnit();
    }

    /** Creates a unit using the game's alternate creation flag. Call on the update thread. */
    public static Unit create(CustomUnitMetadata metadata, boolean createFlag) {
        return Objects.requireNonNull(metadata, "metadata").createUnitWithFlag(createFlag);
    }

    /** Reloads one definition through the game's normal custom-unit loader. */
    public static CustomUnitMetadata reload(CustomUnitMetadata metadata) {
        return CustomUnitLoader.reloadSingleCustomUnitMetadata(
                Objects.requireNonNull(metadata, "metadata"));
    }

    private static List<CustomUnitMetadata> snapshot(List<?> source) {
        if (source == null || source.isEmpty()) {
            return Collections.emptyList();
        }
        ArrayList<CustomUnitMetadata> result = new ArrayList<CustomUnitMetadata>(source.size());
        for (Object value : source) {
            if (value instanceof CustomUnitMetadata) {
                result.add((CustomUnitMetadata) value);
            }
        }
        return Collections.unmodifiableList(result);
    }
}
