package io.github.endx.rustedfabricapi.api.client.minimap;

import io.github.endx.rustedfabricapi.api.event.RustedFabricEvent;
import rustedwarfare.ui.Minimap;
import rustedwarfare.unit.Unit;

/** Typed interception points for transient minimap markers and scan pulses. */
public final class MinimapEvents {
    public static final RustedFabricEvent<BeforeMarker> BEFORE_MARKER =
            RustedFabricEvent.create(listeners -> (minimap, x, y, kind) -> {
                boolean cancelled = false;
                for (BeforeMarker listener : listeners) cancelled |= listener.beforeMarker(minimap, x, y, kind);
                return cancelled;
            });
    public static final RustedFabricEvent<AfterMarker> AFTER_MARKER =
            RustedFabricEvent.create(listeners -> (minimap, x, y, kind) -> {
                for (AfterMarker listener : listeners) listener.afterMarker(minimap, x, y, kind);
            });
    public static final RustedFabricEvent<BeforeScanPulse> BEFORE_SCAN_PULSE =
            RustedFabricEvent.create(listeners -> (minimap, x, y, amount, source) -> {
                boolean cancelled = false;
                for (BeforeScanPulse listener : listeners) {
                    cancelled |= listener.beforeScanPulse(minimap, x, y, amount, source);
                }
                return cancelled;
            });
    public static final RustedFabricEvent<AfterScanPulse> AFTER_SCAN_PULSE =
            RustedFabricEvent.create(listeners -> (minimap, x, y, amount, source) -> {
                for (AfterScanPulse listener : listeners) {
                    listener.afterScanPulse(minimap, x, y, amount, source);
                }
            });

    private MinimapEvents() {
    }

    @FunctionalInterface
    public interface BeforeMarker {
        boolean beforeMarker(Minimap minimap, int worldX, int worldY, MinimapMarkerKind kind);
    }

    @FunctionalInterface
    public interface AfterMarker {
        void afterMarker(Minimap minimap, int worldX, int worldY, MinimapMarkerKind kind);
    }

    @FunctionalInterface
    public interface BeforeScanPulse {
        boolean beforeScanPulse(Minimap minimap, int worldX, int worldY, float amount, Unit source);
    }

    @FunctionalInterface
    public interface AfterScanPulse {
        void afterScanPulse(Minimap minimap, int worldX, int worldY, float amount, Unit source);
    }
}
