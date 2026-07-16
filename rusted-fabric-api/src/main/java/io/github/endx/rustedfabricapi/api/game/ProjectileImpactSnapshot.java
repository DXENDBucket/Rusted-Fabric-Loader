package io.github.endx.rustedfabricapi.api.game;

import io.github.endx.rustedfabricapi.api.util.RustedReflection;

/** Immutable, namespace-neutral projectile state captured at an impact boundary. */
public final class ProjectileImpactSnapshot {
    public enum Kind {
        UNIT_TARGET,
        GROUND_TARGET,
        FIXED_POSITION,
        UNSPECIFIED
    }

    private final Object projectile;
    private final Object targetUnit;
    private final Kind kind;
    private final float impactX;
    private final float impactY;
    private final float impactHeight;
    private final float targetX;
    private final float targetY;
    private final boolean targetGround;
    private final boolean collideWithUnits;
    private final boolean collideWithTerrain;
    private final float contactCollisionRadius;

    private ProjectileImpactSnapshot(Object projectile) {
        if (projectile == null) throw new IllegalArgumentException("projectile must not be null");
        this.projectile = projectile;
        this.targetUnit = field(projectile, "targetUnit", "l");
        this.impactX = number(projectile, "impactX", "aV");
        this.impactY = number(projectile, "impactY", "aW");
        this.impactHeight = number(projectile, "impactHeight", "aX");
        this.targetX = number(projectile, "targetX", "n");
        this.targetY = number(projectile, "targetY", "o");
        this.targetGround = bool(projectile, "targetGround", "m");
        this.collideWithUnits = bool(projectile, "collideWithUnits", "as");
        this.collideWithTerrain = bool(projectile, "collideWithTerrain", "at");
        this.contactCollisionRadius = number(projectile, "contactCollisionRadius", "aA");
        boolean fixedPosition = bool(projectile, "hasFixedTargetPosition", "aC");
        this.kind = targetUnit != null ? Kind.UNIT_TARGET
                : targetGround ? Kind.GROUND_TARGET
                : fixedPosition ? Kind.FIXED_POSITION : Kind.UNSPECIFIED;
    }

    public static ProjectileImpactSnapshot capture(Object projectile) {
        return new ProjectileImpactSnapshot(projectile);
    }

    public Object projectile() { return projectile; }
    public Object targetUnit() { return targetUnit; }
    public Kind kind() { return kind; }
    public float impactX() { return impactX; }
    public float impactY() { return impactY; }
    public float impactHeight() { return impactHeight; }
    public float targetX() { return targetX; }
    public float targetY() { return targetY; }
    public boolean targetGround() { return targetGround; }
    public boolean collideWithUnits() { return collideWithUnits; }
    public boolean collideWithTerrain() { return collideWithTerrain; }
    public float contactCollisionRadius() { return contactCollisionRadius; }

    private static Object field(Object owner, String named, String official) {
        return RustedReflection.getFieldValue(owner, new String[]{named, official});
    }

    private static float number(Object owner, String named, String official) {
        Object value = field(owner, named, official);
        return value instanceof Number ? ((Number) value).floatValue() : 0.0F;
    }

    private static boolean bool(Object owner, String named, String official) {
        return Boolean.TRUE.equals(field(owner, named, official));
    }
}
