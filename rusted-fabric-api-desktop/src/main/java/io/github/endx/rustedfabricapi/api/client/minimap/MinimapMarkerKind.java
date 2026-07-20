package io.github.endx.rustedfabricapi.api.client.minimap;

import rustedwarfare.ui.MinimapMarkerType;

/** Stable marker kinds rendered by the native minimap. */
public enum MinimapMarkerKind {
    BASE(MinimapMarkerType.BASE),
    UNIT(MinimapMarkerType.UNIT),
    NUKE(MinimapMarkerType.NUKE),
    MESSAGE(MinimapMarkerType.MESSAGE);

    private final MinimapMarkerType nativeType;

    MinimapMarkerKind(MinimapMarkerType nativeType) {
        this.nativeType = nativeType;
    }

    MinimapMarkerType nativeType() { return nativeType; }

    public static MinimapMarkerKind fromNative(MinimapMarkerType type) {
        for (MinimapMarkerKind kind : values()) if (kind.nativeType == type) return kind;
        throw new IllegalArgumentException("Unknown native minimap marker type: " + type);
    }
}
