package io.github.endx.rustedfabricapi.mixin;

import io.github.endx.rustedfabricapi.api.unit.movement.UnitMovementMode;
import io.github.endx.rustedfabricapi.api.unit.movement.UnitMovementOverrides;
import io.github.endx.rustedfabricapi.impl.unit.UnitMovementOverrideAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import rustedwarfare.custom.CustomUnit;
import rustedwarfare.custom.CustomUnitMetadata;
import rustedwarfare.unit.MovementType;

@Mixin(targets = "rustedwarfare.custom.CustomUnit", remap = false)
public abstract class CustomUnitMovementOverrideNamedMixin
        implements UnitMovementOverrideAccess {
    @Unique
    private UnitMovementMode rustedfabricapi$movementMode = UnitMovementMode.NATIVE;

    @Override
    public UnitMovementMode rustedfabricapi$getMovementMode() {
        return rustedfabricapi$movementMode;
    }

    @Override
    public void rustedfabricapi$setMovementMode(UnitMovementMode mode) {
        rustedfabricapi$movementMode = mode;
    }

    @Redirect(
            method = {
                    "getMovementType()Lrustedwarfare/unit/MovementType;",
                    "isFlying()Z"
            },
            at = @At(value = "FIELD",
                    target = "Lrustedwarfare/custom/CustomUnitMetadata;pathingMovementType:Lrustedwarfare/unit/MovementType;"),
            require = 2)
    private MovementType rustedfabricapi$effectivePathingMovementType(
            CustomUnitMetadata metadata) {
        return UnitMovementOverrides.effectiveMovementType(
                (CustomUnit) (Object) this, metadata.pathingMovementType);
    }

    @Redirect(
            method = {
                    "applyUnitMetadataWithStatOverrides(Lrustedwarfare/custom/CustomUnitMetadata;ZZ[Lrustedwarfare/custom/MutableStatAccessor;)V",
                    "update(F)V",
                    "dF()V"
            },
            at = @At(value = "FIELD",
                    target = "Lrustedwarfare/custom/CustomUnitMetadata;movementType:Lrustedwarfare/unit/MovementType;"),
            require = 1)
    private MovementType rustedfabricapi$effectiveVisualMovementType(
            CustomUnitMetadata metadata) {
        return UnitMovementOverrides.effectiveMovementType(
                (CustomUnit) (Object) this, metadata.movementType);
    }

    @Redirect(
            method = {
                    "I()Z",
                    "aS()Z",
                    "applyUnitMetadataWithStatOverrides(Lrustedwarfare/custom/CustomUnitMetadata;ZZ[Lrustedwarfare/custom/MutableStatAccessor;)V",
                    "refreshBaseDrawFlag()V",
                    "completeBuildQueueItem(Lrustedwarfare/unit/build/BuildQueueItem;)V",
                    "queueProducedUnitForBuild(Lrustedwarfare/unit/Unit;)V",
                    "placeBuiltOrProducedUnit(Lrustedwarfare/unit/Unit;)V",
                    "cb()V",
                    "isBuilding()Z"
            },
            at = @At(value = "FIELD",
                    target = "Lrustedwarfare/custom/CustomUnitMetadata;isBuilding:Z"),
            require = 1)
    private boolean rustedfabricapi$effectiveBuilding(CustomUnitMetadata metadata) {
        return UnitMovementOverrides.effectiveBuilding(
                (CustomUnit) (Object) this, metadata.isBuilding);
    }

}
