package io.github.endx.rustedfabricapi.api.custom.event;

import io.github.endx.rustedfabricapi.impl.custom.QueuedEventActionRuntime;
import rustedwarfare.custom.CustomTagList;
import rustedwarfare.custom.CustomUnit;
import rustedwarfare.custom.event.CustomUnitEventType;
import rustedwarfare.unit.Unit;

import java.util.Objects;
import java.util.Optional;

/**
 * Mutable context while one queued {@code autoTriggerOnEvent} action is executing.
 *
 * <p>Cancellation stops later configured actions belonging to the same queued event. Native game
 * work that happened before the event was queued is deliberately not represented as cancellable.</p>
 */
public final class QueuedEventActionContext {
    private final CustomUnit actor;
    private final CustomUnitEventType eventType;
    private final Unit sourceUnit;
    private final CustomTagList tags;
    private final CustomUnitEventData data;
    private final Control control;

    public QueuedEventActionContext(CustomUnit actor, CustomUnitEventType eventType,
                                    Unit sourceUnit, CustomTagList tags,
                                    CustomUnitEventData data,
                                    Control control) {
        this.actor = Objects.requireNonNull(actor, "actor");
        this.eventType = Objects.requireNonNull(eventType, "eventType");
        this.sourceUnit = sourceUnit;
        this.tags = tags;
        this.data = Objects.requireNonNull(data, "data");
        this.control = Objects.requireNonNull(control, "control");
    }

    public static Optional<QueuedEventActionContext> current() {
        return QueuedEventActionRuntime.current();
    }

    public CustomUnit actor() { return actor; }
    public CustomUnitEventType eventType() { return eventType; }
    public Optional<Unit> sourceUnit() { return Optional.ofNullable(sourceUnit); }
    public Optional<CustomTagList> tags() { return Optional.ofNullable(tags); }
    public CustomUnitEventData data() { return data; }

    public void cancelRemainingActions() { control.cancelRemainingActions(); }
    public boolean remainingActionsCancelled() { return control.remainingActionsCancelled(); }

    /** Shared cancellation state supplied by the Loader runtime. */
    public interface Control {
        void cancelRemainingActions();
        boolean remainingActionsCancelled();
    }
}
