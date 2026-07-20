package io.github.endx.rustedfabricapi.mixin;

import io.github.endx.rustedfabricapi.api.event.MapSpawnEvents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "rustedwarfare.unit.BuiltinUnitType", remap = false)
public abstract class StartingUnitSpawnNamedMixin {
    @Inject(method = "spawnStartingUnit(Lrustedwarfare/unit/UnitType;FFFFLrustedwarfare/game/Team;)Z", at = @At("HEAD"), cancellable = true, require = 1)
    private static void rustedfabricapi$beforeStartingUnitSpawn(@Coerce Object unitType, float x, float y, float direction, float height, @Coerce Object team, CallbackInfoReturnable<Boolean> cir) {
        boolean cancelled = MapSpawnEvents.BEFORE_STARTING_UNIT_SPAWN.invoker()
                .beforeStartingUnitSpawn(unitType, x, y, direction, height, team);
        cancelled |= io.github.endx.rustedfabricapi.api.unit.type.event.UnitTypeEvents
                .BEFORE_STARTING_SPAWN.invoker().beforeSpawn(
                        (rustedwarfare.unit.UnitType) unitType, x, y, direction, height,
                        (rustedwarfare.game.Team) team);
        if (cancelled) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "spawnStartingUnit(Lrustedwarfare/unit/UnitType;FFFFLrustedwarfare/game/Team;)Z", at = @At("RETURN"), cancellable = true, require = 1)
    private static void rustedfabricapi$afterStartingUnitSpawn(@Coerce Object unitType, float x, float y, float direction, float height, @Coerce Object team, CallbackInfoReturnable<Boolean> cir) {
        boolean result = Boolean.TRUE.equals(cir.getReturnValue());
        result = MapSpawnEvents.AFTER_STARTING_UNIT_SPAWN.invoker()
                .afterStartingUnitSpawn(unitType, x, y, direction, height, team, result);
        result = io.github.endx.rustedfabricapi.api.unit.type.event.UnitTypeEvents
                .AFTER_STARTING_SPAWN.invoker().afterSpawn(
                        (rustedwarfare.unit.UnitType) unitType, x, y, direction, height,
                        (rustedwarfare.game.Team) team, result);
        cir.setReturnValue(result);
    }
}
