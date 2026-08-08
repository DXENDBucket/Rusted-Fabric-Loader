package io.github.endx.rustedfabricapi.api.unit.event;

import io.github.endx.rustedfabricapi.api.event.RustedFabricEvent;
import io.github.endx.rustedfabricapi.api.unit.TeamCreditChangeSource;
import io.github.endx.rustedfabricapi.api.unit.TeamOutcome;
import rustedwarfare.game.Team;
import rustedwarfare.network.NetworkEngine;

/** Economy mutation hooks and native team-outcome announcement observation. */
public final class TeamStateEvents {
    /** Chains requested values for assignments made through {@code Teams.setCredits}. */
    public static final RustedFabricEvent<ModifySetCredits> MODIFY_SET_CREDITS =
            RustedFabricEvent.create(listeners -> (team, current, requested) -> {
                double value = requested;
                for (ModifySetCredits listener : listeners) {
                    value = listener.modify(team, current, value);
                }
                return value;
            });

    /** Cancels an API-mediated credit assignment after modifiers have run. */
    public static final RustedFabricEvent<BeforeSetCredits> BEFORE_SET_CREDITS =
            RustedFabricEvent.create(listeners -> (team, current, requested) -> {
                boolean cancelled = false;
                for (BeforeSetCredits listener : listeners) {
                    cancelled |= listener.beforeSet(team, current, requested);
                }
                return cancelled;
            });

    /** Observes API assignments and the native recorded-income addition path. */
    public static final RustedFabricEvent<AfterCreditsChanged> AFTER_CREDITS_CHANGED =
            RustedFabricEvent.create(listeners -> (team, previous, current, source) -> {
                for (AfterCreditsChanged listener : listeners) {
                    listener.afterChange(team, previous, current, source);
                }
            });

    /** Fires after the native multiplayer engine announces an outcome. */
    public static final RustedFabricEvent<OutcomeAnnounced> OUTCOME_ANNOUNCED =
            RustedFabricEvent.create(listeners -> (network, team, outcome) -> {
                for (OutcomeAnnounced listener : listeners) {
                    listener.onOutcome(network, team, outcome);
                }
            });

    private TeamStateEvents() {
    }

    @FunctionalInterface
    public interface ModifySetCredits {
        double modify(Team team, double currentCredits, double requestedCredits);
    }

    @FunctionalInterface
    public interface BeforeSetCredits {
        boolean beforeSet(Team team, double currentCredits, double requestedCredits);
    }

    @FunctionalInterface
    public interface AfterCreditsChanged {
        void afterChange(Team team, double previousCredits, double currentCredits,
                TeamCreditChangeSource source);
    }

    @FunctionalInterface
    public interface OutcomeAnnounced {
        void onOutcome(NetworkEngine network, Team team, TeamOutcome outcome);
    }
}
