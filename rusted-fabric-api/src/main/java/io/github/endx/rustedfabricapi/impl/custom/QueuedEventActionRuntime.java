package io.github.endx.rustedfabricapi.impl.custom;

import android.graphics.PointF;
import io.github.endx.rustedfabricapi.api.custom.event.CustomUnitEventData;
import io.github.endx.rustedfabricapi.api.custom.event.QueuedEventActionContext;
import io.github.endx.rustedfabricapi.mixin.accessor.LogicBooleanAccessor;
import io.github.endx.rustedfabricapi.mixin.accessor.CustomUnitEventActionAccessor;
import rustedwarfare.custom.CustomUnit;
import rustedwarfare.custom.logic.LogicEventContext;
import rustedwarfare.custom.logic.VariableScope;
import rustedwarfare.unit.Unit;
import rustedwarfare.unit.action.UnitAction;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.WeakHashMap;

/** Internal bridge around native queued-event action execution. */
public final class QueuedEventActionRuntime {
    private static final ThreadLocal<QueuedEventActionContext> CURRENT = new ThreadLocal<>();
    private static final Map<VariableScope, Control> CONTROLS = Collections.synchronizedMap(
            new WeakHashMap<VariableScope, Control>());

    private QueuedEventActionRuntime() { }

    public static Optional<QueuedEventActionContext> current() {
        return Optional.ofNullable(CURRENT.get());
    }

    public static void cancel(VariableScope scope) {
        control(scope).cancelRemainingActions();
    }

    public static boolean execute(CustomUnit actor, UnitAction action, PointF targetPoint,
                                  Unit targetUnit, int recursionDepth, int extraDepth) {
        LogicEventContext nativeContext = LogicBooleanAccessor.rustedfabricapi$getActiveEvent();
        if (nativeContext == null || nativeContext.eventAction == null
                || nativeContext.eventData == null) {
            return actor.executeActionWithContext(
                    action, targetPoint, targetUnit, recursionDepth, extraDepth);
        }
        Control control = control(nativeContext.eventData);
        if (control.remainingActionsCancelled()) return false;

        QueuedEventActionContext previous = CURRENT.get();
        QueuedEventActionContext current = new QueuedEventActionContext(
                actor, ((CustomUnitEventActionAccessor) nativeContext.eventAction)
                        .rustedfabricapi$getEventType(),
                nativeContext.eventSourceUnit, nativeContext.eventTags,
                CustomUnitEventData.wrap(nativeContext.eventData), control);
        CURRENT.set(current);
        try {
            return actor.executeActionWithContext(
                    action, targetPoint, targetUnit, recursionDepth, extraDepth);
        } finally {
            if (previous != null) CURRENT.set(previous);
            else CURRENT.remove();
        }
    }

    private static Control control(VariableScope scope) {
        synchronized (CONTROLS) {
            return CONTROLS.computeIfAbsent(scope, ignored -> new Control());
        }
    }

    public static final class Control implements QueuedEventActionContext.Control {
        private boolean cancelRemainingActions;

        @Override
        public void cancelRemainingActions() { cancelRemainingActions = true; }
        @Override
        public boolean remainingActionsCancelled() { return cancelRemainingActions; }
    }
}
