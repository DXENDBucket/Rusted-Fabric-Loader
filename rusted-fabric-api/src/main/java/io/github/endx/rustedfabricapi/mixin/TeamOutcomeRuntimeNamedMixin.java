package io.github.endx.rustedfabricapi.mixin;

import io.github.endx.rustedfabricapi.api.unit.TeamOutcome;
import io.github.endx.rustedfabricapi.api.unit.event.TeamStateEvents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import rustedwarfare.game.Team;
import rustedwarfare.network.NetworkEngine;

/** Observes the game's native multiplayer outcome-announcement methods. */
@Mixin(targets = "rustedwarfare.network.NetworkEngine", remap = false)
public abstract class TeamOutcomeRuntimeNamedMixin {
    @Unique
    private boolean rustedfabricapi$wasVictorious;

    @Inject(method = "announcePlayerVictory(Lrustedwarfare/game/Team;)V",
            at = @At("HEAD"), require = 1)
    private void rustedfabricapi$beforeVictory(Team team, CallbackInfo ci) {
        rustedfabricapi$wasVictorious = team.victorious;
    }

    @Inject(method = "announcePlayerVictory(Lrustedwarfare/game/Team;)V",
            at = @At("RETURN"), require = 1)
    private void rustedfabricapi$afterVictory(Team team, CallbackInfo ci) {
        if (!rustedfabricapi$wasVictorious && team.victorious) {
            TeamStateEvents.OUTCOME_ANNOUNCED.invoker().onOutcome(
                    (NetworkEngine) (Object) this, team, TeamOutcome.VICTORY);
        }
    }

    @Inject(method = "announcePlayerDefeated(Lrustedwarfare/game/Team;)V",
            at = @At("RETURN"), require = 1)
    private void rustedfabricapi$afterDefeat(Team team, CallbackInfo ci) {
        TeamStateEvents.OUTCOME_ANNOUNCED.invoker().onOutcome(
                (NetworkEngine) (Object) this, team, TeamOutcome.DEFEATED);
    }

    @Inject(method = "announcePlayerWipedOut(Lrustedwarfare/game/Team;)V",
            at = @At("RETURN"), require = 1)
    private void rustedfabricapi$afterWipedOut(Team team, CallbackInfo ci) {
        TeamStateEvents.OUTCOME_ANNOUNCED.invoker().onOutcome(
                (NetworkEngine) (Object) this, team, TeamOutcome.WIPED_OUT);
    }
}
