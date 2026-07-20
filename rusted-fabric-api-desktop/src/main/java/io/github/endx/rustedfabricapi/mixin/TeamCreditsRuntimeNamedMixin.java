package io.github.endx.rustedfabricapi.mixin;

import io.github.endx.rustedfabricapi.api.unit.TeamCreditChangeSource;
import io.github.endx.rustedfabricapi.api.unit.event.TeamStateEvents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import rustedwarfare.game.Team;

/** Observes the native scaled, recorded-income credit addition boundary. */
@Mixin(targets = "rustedwarfare.game.Team", remap = false)
public abstract class TeamCreditsRuntimeNamedMixin {
    @Unique
    private double rustedfabricapi$creditsBeforeRecordedIncome;

    @Inject(method = "addCreditsAndRecordIncome(F)V", at = @At("HEAD"), require = 1)
    private void rustedfabricapi$beforeRecordedIncome(float amount, CallbackInfo ci) {
        rustedfabricapi$creditsBeforeRecordedIncome = ((Team) (Object) this).credits;
    }

    @Inject(method = "addCreditsAndRecordIncome(F)V", at = @At("RETURN"), require = 1)
    private void rustedfabricapi$afterRecordedIncome(float amount, CallbackInfo ci) {
        Team team = (Team) (Object) this;
        double current = team.credits;
        if (Double.compare(rustedfabricapi$creditsBeforeRecordedIncome, current) != 0) {
            TeamStateEvents.AFTER_CREDITS_CHANGED.invoker().afterChange(team,
                    rustedfabricapi$creditsBeforeRecordedIncome, current,
                    TeamCreditChangeSource.NATIVE_RECORDED_INCOME);
        }
    }
}
