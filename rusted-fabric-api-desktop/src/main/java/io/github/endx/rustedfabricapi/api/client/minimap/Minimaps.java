package io.github.endx.rustedfabricapi.api.client.minimap;

import android.graphics.Point;
import io.github.endx.rustedfabricapi.api.client.RustedWarfareClient;
import io.github.endx.rustedfabricapi.api.world.WorldPoint;
import rustedwarfare.ui.Minimap;
import rustedwarfare.unit.Unit;

import java.util.Objects;
import java.util.Optional;

/** Coordinate conversion, state queries, and marker helpers for the local minimap. */
public final class Minimaps {
    private Minimaps() {
    }

    public static Minimap manager() {
        Minimap minimap = RustedWarfareClient.requireEngine().minimap;
        if (minimap == null) throw new IllegalStateException("Minimap is not initialized");
        return minimap;
    }

    public static MinimapSnapshot snapshot() {
        Minimap minimap = manager();
        Point origin = minimap.worldMappingReady ? minimap.worldToMinimapPoint(0.0F, 0.0F) : null;
        float screenX = origin != null ? origin.a : Float.NaN;
        float screenY = origin != null ? origin.b : Float.NaN;
        return new MinimapSnapshot(screenX, screenY, minimap.width,
                minimap.height, minimap.mapImageReady, minimap.worldMappingReady,
                minimap.markerPoints.size(), minimap.scanPulsePoints.size());
    }

    /** Converts world units to window coordinates when the minimap mapping is ready. */
    public static Optional<ScreenPoint> worldToScreen(WorldPoint point) {
        Objects.requireNonNull(point, "point");
        Minimap minimap = manager();
        if (!minimap.worldMappingReady) return Optional.empty();
        Point nativePoint = minimap.worldToMinimapPoint(point.x(), point.y());
        if (nativePoint == null || nativePoint.a < 0 || nativePoint.b < 0) return Optional.empty();
        return Optional.of(new ScreenPoint(nativePoint.a, nativePoint.b));
    }

    /** Converts a point inside the minimap to world units. */
    public static Optional<WorldPoint> screenToWorld(float screenX, float screenY) {
        requireFinite(screenX, "screenX");
        requireFinite(screenY, "screenY");
        Minimap minimap = manager();
        if (!minimap.worldMappingReady) return Optional.empty();
        Point point = minimap.screenToMapPoint(screenX, screenY);
        return point == null ? Optional.empty() : Optional.of(new WorldPoint(point.a, point.b));
    }

    public static void addMarker(WorldPoint point, MinimapMarkerKind kind) {
        Objects.requireNonNull(point, "point");
        manager().addDrawMarker(Math.round(point.x()), Math.round(point.y()),
                Objects.requireNonNull(kind, "kind").nativeType());
    }

    public static void addScanPulse(WorldPoint point, float pulseAmount, Unit source) {
        Objects.requireNonNull(point, "point");
        requireFinite(pulseAmount, "pulseAmount");
        if (pulseAmount <= 0.0F) throw new IllegalArgumentException("pulseAmount must be positive");
        manager().addUnitScanPulse(Math.round(point.x()), Math.round(point.y()), pulseAmount, source);
    }

    private static void requireFinite(float value, String name) {
        if (!Float.isFinite(value)) throw new IllegalArgumentException(name + " must be finite");
    }
}
