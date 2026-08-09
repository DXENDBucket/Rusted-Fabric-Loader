package io.github.endx.rustedfabricapi.api.ini.action;

import io.github.endx.rustedfabricapi.api.client.RustedWarfareClient;
import io.github.endx.rustedfabricapi.api.world.WorldPoint;
import rustedwarfare.custom.CustomUnit;
import rustedwarfare.framework.GameObject;
import rustedwarfare.game.Team;
import rustedwarfare.unit.Unit;
import rustedwarfare.unit.action.UnitAction;

import java.util.Objects;
import java.util.Optional;

/** Immutable actor and target context for one Java-backed INI action effect. */
public final class IniActionExecutionContext {
    private final CustomUnit actor;
    private final UnitAction action;
    private final WorldPoint targetPoint;
    private final Unit targetUnit;
    private final int recursionDepth;

    public IniActionExecutionContext(CustomUnit actor, UnitAction action,
                                     WorldPoint targetPoint, Unit targetUnit,
                                     int recursionDepth) {
        this.actor = Objects.requireNonNull(actor, "actor");
        this.action = Objects.requireNonNull(action, "action");
        this.targetPoint = targetPoint;
        this.targetUnit = targetUnit;
        if (recursionDepth < 0) {
            throw new IllegalArgumentException("recursionDepth must be non-negative");
        }
        this.recursionDepth = recursionDepth;
    }

    public CustomUnit actor() { return actor; }
    public UnitAction action() { return action; }
    public Optional<WorldPoint> targetPoint() { return Optional.ofNullable(targetPoint); }
    public Optional<Unit> targetUnit() { return Optional.ofNullable(targetUnit); }
    public int recursionDepth() { return recursionDepth; }

    public WorldPoint actorPosition() {
        GameObject object = actor;
        return new WorldPoint(object.x, object.y);
    }

    /** Uses the explicit action point first, then the unit target position. */
    public Optional<WorldPoint> actionTargetPosition() {
        if (targetPoint != null) return Optional.of(targetPoint);
        if (targetUnit != null) {
            GameObject object = targetUnit;
            return Optional.of(new WorldPoint(object.x, object.y));
        }
        return Optional.empty();
    }

    /** True only on the client locally controlling the actor's team. */
    public boolean isActorOwnedByLocalPlayer() {
        Team playerTeam = RustedWarfareClient.getPlayerTeam();
        return playerTeam != null && actor.team == playerTeam;
    }
}
