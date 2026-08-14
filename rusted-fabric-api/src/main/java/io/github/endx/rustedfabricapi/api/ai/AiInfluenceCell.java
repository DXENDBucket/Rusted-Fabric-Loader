package io.github.endx.rustedfabricapi.api.ai;

import java.util.Optional;

/** Dynamic friendly/enemy presence over one static terrain cell. */
public final class AiInfluenceCell {
    private final AiTerrainCell terrain;
    private final int ownUnitCount;
    private final int alliedUnitCount;
    private final int enemyUnitCount;
    private final float ownInfluence;
    private final float alliedInfluence;
    private final float enemyInfluence;
    private final AiCellControl control;
    private final boolean frontline;
    private final float frontlineScore;
    private final AiMovementDomain frontlineDomain;

    AiInfluenceCell(AiTerrainCell terrain, int ownUnitCount, int alliedUnitCount,
            int enemyUnitCount, float ownInfluence, float alliedInfluence,
            float enemyInfluence, AiCellControl control, boolean frontline,
            float frontlineScore, AiMovementDomain frontlineDomain) {
        this.terrain = terrain;
        this.ownUnitCount = ownUnitCount;
        this.alliedUnitCount = alliedUnitCount;
        this.enemyUnitCount = enemyUnitCount;
        this.ownInfluence = ownInfluence;
        this.alliedInfluence = alliedInfluence;
        this.enemyInfluence = enemyInfluence;
        this.control = control;
        this.frontline = frontline;
        this.frontlineScore = frontlineScore;
        this.frontlineDomain = frontlineDomain;
    }

    public AiTerrainCell terrain() { return terrain; }
    public int ownUnitCount() { return ownUnitCount; }
    public int alliedUnitCount() { return alliedUnitCount; }
    public int enemyUnitCount() { return enemyUnitCount; }
    public float ownInfluence() { return ownInfluence; }
    public float alliedInfluence() { return alliedInfluence; }
    public float friendlyInfluence() { return ownInfluence + alliedInfluence; }
    public float enemyInfluence() { return enemyInfluence; }
    public AiCellControl control() { return control; }
    public boolean frontline() { return frontline; }
    public float frontlineScore() { return frontlineScore; }
    /** Dominant movement domain that caused this front classification. */
    public Optional<AiMovementDomain> frontlineDomain() {
        return Optional.ofNullable(frontlineDomain);
    }
}
