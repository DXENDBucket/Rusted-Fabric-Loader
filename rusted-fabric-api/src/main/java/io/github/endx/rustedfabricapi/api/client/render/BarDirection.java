package io.github.endx.rustedfabricapi.api.client.render;

/** Direction in which a normalized bar value occupies its drawing rectangle. */
public enum BarDirection {
    LEFT_TO_RIGHT,
    RIGHT_TO_LEFT,
    TOP_TO_BOTTOM,
    BOTTOM_TO_TOP;

    public boolean isHorizontal() {
        return this == LEFT_TO_RIGHT || this == RIGHT_TO_LEFT;
    }
}
