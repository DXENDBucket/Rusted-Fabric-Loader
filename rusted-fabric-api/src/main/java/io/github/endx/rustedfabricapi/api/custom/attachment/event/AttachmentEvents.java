package io.github.endx.rustedfabricapi.api.custom.attachment.event;

import io.github.endx.rustedfabricapi.api.event.RustedFabricEvent;
import rustedwarfare.custom.CustomUnit;
import rustedwarfare.custom.attachment.AttachmentSlot;
import rustedwarfare.unit.OrderableUnit;

/** Strongly typed custom-unit attachment mutation events. */
public final class AttachmentEvents {
    public static final RustedFabricEvent<BeforeAttach> BEFORE_ATTACH =
            RustedFabricEvent.create(listeners -> (parent, child, slot) -> {
                boolean cancelled = false;
                for (BeforeAttach listener : listeners) {
                    cancelled |= listener.beforeAttach(parent, child, slot);
                }
                return cancelled;
            });
    public static final RustedFabricEvent<AfterAttach> AFTER_ATTACH =
            RustedFabricEvent.create(listeners -> (parent, child, slot, attached) -> {
                for (AfterAttach listener : listeners) {
                    listener.afterAttach(parent, child, slot, attached);
                }
            });
    public static final RustedFabricEvent<BeforeDetach> BEFORE_DETACH =
            RustedFabricEvent.create(listeners -> (parent, child, slot) -> {
                boolean cancelled = false;
                for (BeforeDetach listener : listeners) {
                    cancelled |= listener.beforeDetach(parent, child, slot);
                }
                return cancelled;
            });
    public static final RustedFabricEvent<AfterDetach> AFTER_DETACH =
            RustedFabricEvent.create(listeners -> (parent, child, detached) -> {
                for (AfterDetach listener : listeners) {
                    listener.afterDetach(parent, child, detached);
                }
            });

    private AttachmentEvents() {
    }

    @FunctionalInterface
    public interface BeforeAttach {
        boolean beforeAttach(CustomUnit parent, OrderableUnit child, AttachmentSlot slot);
    }

    @FunctionalInterface
    public interface AfterAttach {
        void afterAttach(CustomUnit parent, OrderableUnit child,
                         AttachmentSlot slot, boolean attached);
    }

    @FunctionalInterface
    public interface BeforeDetach {
        boolean beforeDetach(CustomUnit parent, OrderableUnit child, AttachmentSlot slot);
    }

    @FunctionalInterface
    public interface AfterDetach {
        void afterDetach(CustomUnit parent, OrderableUnit child, boolean detached);
    }
}
