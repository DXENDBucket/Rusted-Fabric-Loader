package io.github.endx.iniessentials;

import io.github.endx.rustedfabricapi.api.ini.action.IniActionExecutionContext;
import io.github.endx.rustedfabricapi.api.logic.UnitReferenceExpression;
import io.github.endx.rustedfabricapi.api.world.WorldPoint;
import rustedwarfare.framework.GameObject;
import rustedwarfare.unit.Unit;

import java.util.Locale;
import java.util.Optional;

/** A position resolved from legacy action context tokens or a native UnitReference expression. */
final class ActionPositionReference {
    private final Kind kind;
    private final UnitReferenceExpression expression;

    private ActionPositionReference(Kind kind, UnitReferenceExpression expression) {
        this.kind = kind;
        this.expression = expression;
    }

    static ActionPositionReference compile(Object metadata, String raw) {
        String checked = raw != null ? raw.trim() : "";
        if (checked.isEmpty()) throw new IllegalArgumentException("position reference must not be empty");
        String normalized = checked.replace("_", "").toLowerCase(Locale.ROOT);
        if ("self".equals(normalized)) return new ActionPositionReference(Kind.SELF, null);
        if ("target".equals(normalized)) return new ActionPositionReference(Kind.TARGET, null);
        if ("actiontarget".equals(normalized)) {
            return new ActionPositionReference(Kind.ACTION_TARGET, null);
        }
        return new ActionPositionReference(Kind.UNIT_REFERENCE,
                UnitReferenceExpression.compile(metadata, checked));
    }

    Optional<WorldPoint> resolve(IniActionExecutionContext context) {
        switch (kind) {
            case SELF:
                return Optional.of(context.actorPosition());
            case TARGET:
                return context.targetUnit().map(ActionPositionReference::position);
            case ACTION_TARGET:
                return context.actionTargetPosition();
            case UNIT_REFERENCE:
                return expression.evaluate(context.actor()).map(ActionPositionReference::position);
            default:
                throw new AssertionError(kind);
        }
    }

    WorldPoint require(IniActionExecutionContext context, String field) {
        return resolve(context).orElseThrow(() -> new IllegalArgumentException(
                field + " unit reference resolved to null"));
    }

    private static WorldPoint position(Unit unit) {
        GameObject object = unit;
        return new WorldPoint(object.x, object.y);
    }

    private enum Kind { SELF, TARGET, ACTION_TARGET, UNIT_REFERENCE }
}
