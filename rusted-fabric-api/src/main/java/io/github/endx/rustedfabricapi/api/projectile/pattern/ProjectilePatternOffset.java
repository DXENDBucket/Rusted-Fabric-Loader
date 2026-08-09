package io.github.endx.rustedfabricapi.api.projectile.pattern;

/** One pure-data offset produced while expanding a projectile pattern. */
public final class ProjectilePatternOffset {
    private final int index;
    private final float directionOffset;
    private final float originOffsetX;
    private final float originOffsetY;

    ProjectilePatternOffset(int index, float directionOffset,
                            float originOffsetX, float originOffsetY) {
        this.index = index;
        this.directionOffset = directionOffset;
        this.originOffsetX = originOffsetX;
        this.originOffsetY = originOffsetY;
    }

    public int index() { return index; }
    public float directionOffset() { return directionOffset; }
    public float originOffsetX() { return originOffsetX; }
    public float originOffsetY() { return originOffsetY; }
}
