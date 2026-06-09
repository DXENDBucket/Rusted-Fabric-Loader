package io.github.endx.rustedfabricapi.mixin;

import io.github.endx.rustedfabricapi.api.event.MapSpawnEvents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "com.corrodinggames.rts.game.units.ar", remap = false)
public abstract class StartingUnitSpawnOfficialMixin {
    @Inject(method = "a(Lcom/corrodinggames/rts/game/units/as;FFFFLcom/corrodinggames/rts/game/n;)Z", at = @At("HEAD"), cancellable = true, require = 1)
    private static void rustedfabricapi$beforeStartingUnitSpawn(@Coerce Object unitType, float x, float y, float direction, float height, @Coerce Object team, CallbackInfoReturnable<Boolean> cir) {
        if (MapSpawnEvents.BEFORE_STARTING_UNIT_SPAWN.invoker()
                .beforeStartingUnitSpawn(unitType, x, y, direction, height, team)) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "a(Lcom/corrodinggames/rts/game/units/as;FFFFLcom/corrodinggames/rts/game/n;)Z", at = @At("RETURN"), cancellable = true, require = 1)
    private static void rustedfabricapi$afterStartingUnitSpawn(@Coerce Object unitType, float x, float y, float direction, float height, @Coerce Object team, CallbackInfoReturnable<Boolean> cir) {
        boolean result = Boolean.TRUE.equals(cir.getReturnValue());
        cir.setReturnValue(MapSpawnEvents.AFTER_STARTING_UNIT_SPAWN.invoker()
                .afterStartingUnitSpawn(unitType, x, y, direction, height, team, result));
    }
}
