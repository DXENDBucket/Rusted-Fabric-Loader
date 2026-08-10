package io.github.endx.rustedfabricapi.api.unit.movement;

import io.github.endx.rustedfabricapi.api.client.RustedWarfareClient;
import io.github.endx.rustedfabricapi.api.data.PersistentData;
import io.github.endx.rustedfabricapi.api.data.PersistentDataCodec;
import io.github.endx.rustedfabricapi.api.data.PersistentDataKey;
import io.github.endx.rustedfabricapi.api.data.event.PersistentDataEvents;
import io.github.endx.rustedfabricapi.api.unit.Units;
import io.github.endx.rustedfabricapi.impl.unit.UnitMovementOverrideAccess;
import rustedwarfare.core.GameEngine;
import rustedwarfare.custom.CustomUnit;
import rustedwarfare.unit.MovementType;

import java.util.Objects;
import java.util.Optional;

/** Sets an instance-local movement mode without mutating shared CustomUnitMetadata. */
public final class UnitMovementOverrides {
    private static final PersistentDataKey<UnitMovementMode> PERSISTENT_KEY =
            PersistentData.register("rusted_fabric_api", "unit_movement_mode", 1,
                    new PersistentDataCodec<UnitMovementMode>() {
                        @Override public void encode(
                                io.github.endx.rustedfabricapi.api.networking.PacketBuffer buffer,
                                UnitMovementMode value) {
                            buffer.writeString(value.name());
                        }

                        @Override public UnitMovementMode decode(
                                io.github.endx.rustedfabricapi.api.networking.PacketBuffer buffer,
                                int storedVersion) {
                            return UnitMovementMode.parse(buffer.readString());
                        }
                    });

    static {
        PersistentDataEvents.AFTER_READ.register((formatVersion, entries) -> {
            for (rustedwarfare.unit.Unit unit : Units.snapshot()) {
                if (!(unit instanceof CustomUnit)) continue;
                Optional<UnitMovementMode> restored = PersistentData.get(unit, PERSISTENT_KEY);
                restored.ifPresent(mode -> applyRuntime((CustomUnit) unit, mode));
            }
        });
    }

    private UnitMovementOverrides() { }

    /** Ensures the persistent key and restore listener are registered during API startup. */
    public static void bootstrap() { }

    public static UnitMovementMode get(CustomUnit unit) {
        return access(unit).rustedfabricapi$getMovementMode();
    }

    public static Optional<UnitMovementMode> getOverride(CustomUnit unit) {
        UnitMovementMode mode = get(unit);
        return mode.overridesNative() ? Optional.of(mode) : Optional.empty();
    }

    public static void set(CustomUnit unit, UnitMovementMode mode) {
        Objects.requireNonNull(unit, "unit");
        Objects.requireNonNull(mode, "mode");
        applyRuntime(unit, mode);
        if (mode.overridesNative()) PersistentData.set(unit, PERSISTENT_KEY, mode);
        else PersistentData.remove(unit, PERSISTENT_KEY);
    }

    public static void clear(CustomUnit unit) { set(unit, UnitMovementMode.NATIVE); }

    public static MovementType effectiveMovementType(CustomUnit unit, MovementType nativeType) {
        UnitMovementMode mode = get(unit);
        return mode.overridesNative() ? mode.movementType() : nativeType;
    }

    public static boolean effectiveBuilding(CustomUnit unit, boolean nativeValue) {
        return get(unit).building(nativeValue);
    }

    private static void applyRuntime(CustomUnit unit, UnitMovementMode mode) {
        UnitMovementMode previous = get(unit);
        if (previous == mode) return;
        boolean buildingChanged = previous.building(unit.unitMetadata.isBuilding)
                != mode.building(unit.unitMetadata.isBuilding);
        access(unit).rustedfabricapi$setMovementMode(mode);
        unit.clearActivePathAndCollisions();
        unit.refreshBaseDrawFlag();
        unit.dF();
        if (buildingChanged) rebuildBuildingCosts(unit);
    }

    private static void rebuildBuildingCosts(CustomUnit unit) {
        GameEngine engine = RustedWarfareClient.getEngine();
        if (engine != null && engine.pathfindingEngine != null) {
            engine.pathfindingEngine.rebuildCostsAroundUnit(unit);
        }
    }

    private static UnitMovementOverrideAccess access(CustomUnit unit) {
        Objects.requireNonNull(unit, "unit");
        if (!(unit instanceof UnitMovementOverrideAccess)) {
            throw new IllegalStateException("Rusted Fabric movement mixin is not active");
        }
        return (UnitMovementOverrideAccess) unit;
    }
}
