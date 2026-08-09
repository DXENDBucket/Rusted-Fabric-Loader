package io.github.endx.rustedfabricapi.api.fog;

public final class FogEditResult {
    private final int visitedTiles;
    private final int changedTiles;

    public FogEditResult(int visitedTiles, int changedTiles) {
        this.visitedTiles = visitedTiles;
        this.changedTiles = changedTiles;
    }

    public int visitedTiles() { return visitedTiles; }
    public int changedTiles() { return changedTiles; }
    public boolean changed() { return changedTiles != 0; }
}
