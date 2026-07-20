package io.github.endx.rustedfabricapi.api.lobby;

import io.github.endx.rustedfabricapi.api.event.RustedFabricEvent;
import rustedwarfare.game.Team;
import rustedwarfare.network.NetworkEngine;
import rustedwarfare.network.TeamLayout;

/** Cancellable, typed boundaries around native lobby player administration. */
public final class LobbyPlayerEvents {
    public static final RustedFabricEvent<TeamOperation> BEFORE_KICK_REQUEST = teamOperation();
    public static final RustedFabricEvent<AfterTeamOperation> AFTER_KICK_REQUEST = afterTeamOperation();
    public static final RustedFabricEvent<TeamOperation> BEFORE_KICK = teamOperation();
    public static final RustedFabricEvent<AfterTeamOperation> AFTER_KICK = afterTeamOperation();
    public static final RustedFabricEvent<NetworkOperation> BEFORE_ADD_AI = networkOperation();
    public static final RustedFabricEvent<AfterNetworkOperation> AFTER_ADD_AI = afterNetworkOperation();
    public static final RustedFabricEvent<MoveOperation> BEFORE_MOVE_REQUEST = moveOperation();
    public static final RustedFabricEvent<AfterMoveOperation> AFTER_MOVE_REQUEST = afterMoveOperation();
    public static final RustedFabricEvent<AllyOperation> BEFORE_ALLY_TEAM_REQUEST = allyOperation();
    public static final RustedFabricEvent<AfterAllyOperation> AFTER_ALLY_TEAM_REQUEST = afterAllyOperation();
    public static final RustedFabricEvent<LayoutOperation> BEFORE_LAYOUT = layoutOperation();
    public static final RustedFabricEvent<AfterLayoutOperation> AFTER_LAYOUT = afterLayoutOperation();
    public static final RustedFabricEvent<PauseOperation> BEFORE_PAUSE_CHANGE = pauseOperation();
    public static final RustedFabricEvent<AfterPauseOperation> AFTER_PAUSE_CHANGE = afterPauseOperation();

    private LobbyPlayerEvents() {
    }

    private static RustedFabricEvent<TeamOperation> teamOperation() {
        return RustedFabricEvent.create(listeners -> (network, team) -> {
            boolean cancelled = false;
            for (TeamOperation listener : listeners) cancelled |= listener.before(network, team);
            return cancelled;
        });
    }

    private static RustedFabricEvent<AfterTeamOperation> afterTeamOperation() {
        return RustedFabricEvent.create(listeners -> (network, team) -> {
            for (AfterTeamOperation listener : listeners) listener.after(network, team);
        });
    }

    private static RustedFabricEvent<NetworkOperation> networkOperation() {
        return RustedFabricEvent.create(listeners -> network -> {
            boolean cancelled = false;
            for (NetworkOperation listener : listeners) cancelled |= listener.before(network);
            return cancelled;
        });
    }

    private static RustedFabricEvent<AfterNetworkOperation> afterNetworkOperation() {
        return RustedFabricEvent.create(listeners -> network -> {
            for (AfterNetworkOperation listener : listeners) listener.after(network);
        });
    }

    private static RustedFabricEvent<MoveOperation> moveOperation() {
        return RustedFabricEvent.create(listeners -> (network, team, slot, allyOverride) -> {
            boolean cancelled = false;
            for (MoveOperation listener : listeners) {
                cancelled |= listener.before(network, team, slot, allyOverride);
            }
            return cancelled;
        });
    }

    private static RustedFabricEvent<AfterMoveOperation> afterMoveOperation() {
        return RustedFabricEvent.create(listeners -> (network, team, slot, allyOverride) -> {
            for (AfterMoveOperation listener : listeners) {
                listener.after(network, team, slot, allyOverride);
            }
        });
    }

    private static RustedFabricEvent<AllyOperation> allyOperation() {
        return RustedFabricEvent.create(listeners -> (network, team, allyTeam) -> {
            boolean cancelled = false;
            for (AllyOperation listener : listeners) {
                cancelled |= listener.before(network, team, allyTeam);
            }
            return cancelled;
        });
    }

    private static RustedFabricEvent<AfterAllyOperation> afterAllyOperation() {
        return RustedFabricEvent.create(listeners -> (network, team, allyTeam) -> {
            for (AfterAllyOperation listener : listeners) listener.after(network, team, allyTeam);
        });
    }

    private static RustedFabricEvent<LayoutOperation> layoutOperation() {
        return RustedFabricEvent.create(listeners -> (network, layout) -> {
            boolean cancelled = false;
            for (LayoutOperation listener : listeners) cancelled |= listener.before(network, layout);
            return cancelled;
        });
    }

    private static RustedFabricEvent<AfterLayoutOperation> afterLayoutOperation() {
        return RustedFabricEvent.create(listeners -> (network, layout) -> {
            for (AfterLayoutOperation listener : listeners) listener.after(network, layout);
        });
    }

    private static RustedFabricEvent<PauseOperation> pauseOperation() {
        return RustedFabricEvent.create(listeners -> (network, paused) -> {
            boolean cancelled = false;
            for (PauseOperation listener : listeners) cancelled |= listener.before(network, paused);
            return cancelled;
        });
    }

    private static RustedFabricEvent<AfterPauseOperation> afterPauseOperation() {
        return RustedFabricEvent.create(listeners -> (network, paused) -> {
            for (AfterPauseOperation listener : listeners) listener.after(network, paused);
        });
    }

    @FunctionalInterface public interface TeamOperation {
        boolean before(NetworkEngine network, Team team);
    }
    @FunctionalInterface public interface AfterTeamOperation {
        void after(NetworkEngine network, Team team);
    }
    @FunctionalInterface public interface NetworkOperation {
        boolean before(NetworkEngine network);
    }
    @FunctionalInterface public interface AfterNetworkOperation {
        void after(NetworkEngine network);
    }
    @FunctionalInterface public interface MoveOperation {
        boolean before(NetworkEngine network, Team team, int zeroBasedSlot, Integer allyTeamOverride);
    }
    @FunctionalInterface public interface AfterMoveOperation {
        void after(NetworkEngine network, Team team, int zeroBasedSlot, Integer allyTeamOverride);
    }
    @FunctionalInterface public interface AllyOperation {
        boolean before(NetworkEngine network, Team team, int zeroBasedAllyTeam);
    }
    @FunctionalInterface public interface AfterAllyOperation {
        void after(NetworkEngine network, Team team, int zeroBasedAllyTeam);
    }
    @FunctionalInterface public interface LayoutOperation {
        boolean before(NetworkEngine network, TeamLayout layout);
    }
    @FunctionalInterface public interface AfterLayoutOperation {
        void after(NetworkEngine network, TeamLayout layout);
    }
    @FunctionalInterface public interface PauseOperation {
        boolean before(NetworkEngine network, boolean paused);
    }
    @FunctionalInterface public interface AfterPauseOperation {
        void after(NetworkEngine network, boolean paused);
    }
}
