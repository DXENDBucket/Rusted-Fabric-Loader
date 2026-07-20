package io.github.endx.rustedfabricapi.mixin;

import io.github.endx.rustedfabricapi.api.unit.event.UnitTeamEvents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import rustedwarfare.game.Team;
import rustedwarfare.unit.Unit;

@Mixin(targets = "rustedwarfare.unit.Unit", remap = false)
public abstract class UnitTeamChangeNamedMixin {
    @Inject(method = "changeTeam(Lrustedwarfare/game/Team;)V", at = @At("HEAD"), require = 1)
    private void rustedfabricapi$beforeTeamChange(Team newTeam, CallbackInfo ci) {
        Unit unit = (Unit) (Object) this;
        UnitTeamEvents.BEFORE_CHANGE.invoker().beforeChange(unit, unit.team, newTeam);
    }

    @Inject(method = "changeTeam(Lrustedwarfare/game/Team;)V", at = @At("RETURN"), require = 1)
    private void rustedfabricapi$afterTeamChange(Team newTeam, CallbackInfo ci) {
        UnitTeamEvents.AFTER_CHANGE.invoker().afterChange((Unit) (Object) this, newTeam);
    }
}
