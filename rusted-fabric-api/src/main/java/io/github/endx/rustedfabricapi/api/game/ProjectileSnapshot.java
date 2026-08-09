package io.github.endx.rustedfabricapi.api.game;

import io.github.endx.rustedfabricapi.api.util.RustedReflection;

/** Immutable, namespace-neutral snapshot of commonly used mapped Projectile state. */
public final class ProjectileSnapshot {
    private final Object projectile;
    private final long id;
    private final Object sourceUnit;
    private final Object targetUnit;
    private final float x;
    private final float y;
    private final float height;
    private final float targetX;
    private final float targetY;
    private final float remainingLife;
    private final float age;
    private final float speed;
    private final float direction;
    private final float directDamage;
    private final float areaDamage;
    private final float areaRadius;
    private final boolean instant;
    private final boolean continuousDamage;
    private final boolean ballistic;
    private final boolean impactTriggered;
    private final boolean removalRequested;

    private ProjectileSnapshot(Object projectile) {
        if (projectile == null) throw new IllegalArgumentException("projectile must not be null");
        this.projectile = projectile;
        this.id = number(projectile, "id", "eh").longValue();
        this.sourceUnit = field(projectile, "sourceUnit", "j");
        this.targetUnit = field(projectile, "targetUnit", "l");
        this.x = number(projectile, "x", "eo").floatValue();
        this.y = number(projectile, "y", "ep").floatValue();
        this.height = number(projectile, "height", "eq").floatValue();
        this.targetX = number(projectile, "targetX", "n").floatValue();
        this.targetY = number(projectile, "targetY", "o").floatValue();
        this.remainingLife = number(projectile, "remainingLife", "h").floatValue();
        this.age = number(projectile, "ageTimer", "J").floatValue();
        this.speed = number(projectile, "speed", "t").floatValue();
        this.direction = number(projectile, "direction", "az").floatValue();
        this.directDamage = number(projectile, "directDamage", "U").floatValue();
        this.areaDamage = number(projectile, "areaDamage", "Y").floatValue();
        this.areaRadius = number(projectile, "areaRadius", "Z").floatValue();
        this.instant = bool(projectile, "instant", "A");
        this.continuousDamage = bool(projectile, "continuousDamage", "E");
        this.ballistic = bool(projectile, "ballistic", "aH");
        this.impactTriggered = bool(projectile, "impactTriggered", "bn");
        this.removalRequested = bool(projectile, "removalRequested", "aS");
    }

    public static ProjectileSnapshot capture(Object projectile) {
        return new ProjectileSnapshot(projectile);
    }

    public Object projectile() { return projectile; }
    public long id() { return id; }
    public Object sourceUnit() { return sourceUnit; }
    public Object targetUnit() { return targetUnit; }
    public float x() { return x; }
    public float y() { return y; }
    public float height() { return height; }
    public float targetX() { return targetX; }
    public float targetY() { return targetY; }
    public float remainingLife() { return remainingLife; }
    public float age() { return age; }
    public float speed() { return speed; }
    public float direction() { return direction; }
    public float directDamage() { return directDamage; }
    public float areaDamage() { return areaDamage; }
    public float areaRadius() { return areaRadius; }
    public boolean instant() { return instant; }
    public boolean continuousDamage() { return continuousDamage; }
    public boolean ballistic() { return ballistic; }
    public boolean impactTriggered() { return impactTriggered; }
    public boolean removalRequested() { return removalRequested; }

    private static Object field(Object owner, String named, String official) {
        return RustedReflection.getFieldValue(owner, new String[]{named, official});
    }

    private static Number number(Object owner, String named, String official) {
        Object value = field(owner, named, official);
        return value instanceof Number ? (Number) value : Integer.valueOf(0);
    }

    private static boolean bool(Object owner, String named, String official) {
        return Boolean.TRUE.equals(field(owner, named, official));
    }

}
