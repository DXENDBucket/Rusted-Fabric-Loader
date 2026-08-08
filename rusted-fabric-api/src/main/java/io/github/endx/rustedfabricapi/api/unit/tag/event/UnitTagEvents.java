package io.github.endx.rustedfabricapi.api.unit.tag.event;

import io.github.endx.rustedfabricapi.api.event.RustedFabricEvent;
import rustedwarfare.custom.CustomTagList;
import rustedwarfare.custom.CustomUnit;

/** Strongly typed runtime-tag replacement events. */
public final class UnitTagEvents {
    public static final RustedFabricEvent<BeforeSet> BEFORE_SET =
            RustedFabricEvent.create(listeners -> (unit, current, replacement, skipTeamIndexRefresh) -> {
                boolean cancelled = false;
                for (BeforeSet listener : listeners) {
                    cancelled |= listener.beforeSet(
                            unit, current, replacement, skipTeamIndexRefresh);
                }
                return cancelled;
            });
    public static final RustedFabricEvent<AfterSet> AFTER_SET =
            RustedFabricEvent.create(listeners -> (unit, tags, skipTeamIndexRefresh) -> {
                for (AfterSet listener : listeners) {
                    listener.afterSet(unit, tags, skipTeamIndexRefresh);
                }
            });

    private UnitTagEvents() {
    }

    @FunctionalInterface
    public interface BeforeSet {
        boolean beforeSet(CustomUnit unit, CustomTagList currentTags,
                          CustomTagList replacementTags, boolean skipTeamIndexRefresh);
    }

    @FunctionalInterface
    public interface AfterSet {
        void afterSet(CustomUnit unit, CustomTagList tags, boolean skipTeamIndexRefresh);
    }
}
