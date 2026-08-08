package io.github.endx.rustedfabricapi.api.path;

import rustedwarfare.unit.MovementType;

import java.util.OptionalInt;

/** Immutable native movement-cost view for one tile. */
public final class TilePathingSnapshot {
    private final MovementType movementType;
    private final int tileX;
    private final int tileY;
    private final boolean blocked;
    private final boolean blockedIgnoringBuildings;
    private final int totalCost;
    private final int clearance;
    private final int connectedRegion;
    private final int connectedRegionSize;

    TilePathingSnapshot(MovementType movementType, int tileX, int tileY, boolean blocked,
            boolean blockedIgnoringBuildings, int totalCost, int clearance,
            int connectedRegion, int connectedRegionSize) {
        this.movementType = movementType;
        this.tileX = tileX;
        this.tileY = tileY;
        this.blocked = blocked;
        this.blockedIgnoringBuildings = blockedIgnoringBuildings;
        this.totalCost = totalCost;
        this.clearance = clearance;
        this.connectedRegion = connectedRegion;
        this.connectedRegionSize = connectedRegionSize;
    }

    public MovementType movementType() { return movementType; }
    public int tileX() { return tileX; }
    public int tileY() { return tileY; }
    public boolean blocked() { return blocked; }
    public boolean blockedIgnoringBuildings() { return blockedIgnoringBuildings; }
    /** Exact native cost, or {@code -1} when blocked/outside the map. */
    public int totalCost() { return totalCost; }
    public OptionalInt clearance() {
        return clearance >= 0 ? OptionalInt.of(clearance) : OptionalInt.empty();
    }
    public OptionalInt connectedRegion() {
        return connectedRegion > 0 ? OptionalInt.of(connectedRegion) : OptionalInt.empty();
    }
    public OptionalInt connectedRegionSize() {
        return connectedRegionSize >= 0 ? OptionalInt.of(connectedRegionSize) : OptionalInt.empty();
    }
}
