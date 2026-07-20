package io.github.endx.rustedfabricapi.api.unit.combat;

import io.github.endx.rustedfabricapi.api.util.RustedReflection;
import rustedwarfare.math.Point3F;
import rustedwarfare.unit.OrderableUnit;
import rustedwarfare.unit.Unit;
import rustedwarfare.unit.combat.TurretRuntimeState;

import java.util.Objects;

/** Immutable snapshot of a mapped turret's configuration, position and runtime state. */
public final class TurretSnapshot {
    private final OrderableUnit owner;
    private final int index;
    private final Unit target;
    private final boolean canFire;
    private final boolean aimedAtTarget;
    private final float fireDelay;
    private final float warmupTime;
    private final boolean warmupNoReset;
    private final float warmupDelayTransfer;
    private final float projectileDirectDamage;
    private final float barrelForwardOffset;
    private final float muzzleX;
    private final float muzzleY;
    private final float muzzleHeight;
    private final float worldX;
    private final float worldY;
    private final float recoilOffset;
    private final float recoilOutTime;
    private final float recoilReturnTime;
    private final float shotSpreadX;
    private final float shotSpreadY;
    private final float aimOffsetSpread;
    private final float aimAngle;
    private final float previousAimAngle;
    private final float aimAngularVelocity;
    private final float aimLockTimer;
    private final float reloadTimer;
    private final float warmupTimer;
    private final boolean aimReady;
    private final boolean alternateFireSide;

    private TurretSnapshot(OrderableUnit owner, int index) {
        this.owner = Objects.requireNonNull(owner, "owner");
        int turretCount = Math.max(0, owner.getTurretCount());
        if (index < 0 || index >= turretCount) {
            throw new IndexOutOfBoundsException(
                    "turretIndex=" + index + ", count=" + turretCount);
        }
        this.index = index;

        TurretRuntimeState state = owner.turretStates != null
                && index >= 0 && index < owner.turretStates.length
                ? owner.turretStates[index] : null;
        this.target = state != null ? state.target : null;
        this.canFire = owner.canTurretFire(index);
        this.aimedAtTarget = owner.isTurretAimedAtTarget(index);
        this.fireDelay = owner.getTurretFireDelay(index);
        this.warmupTime = owner.getTurretWarmupTime(index);
        this.warmupNoReset = owner.isTurretWarmupNoReset(index);
        this.warmupDelayTransfer = owner.getTurretWarmupShootDelayTransfer(index);
        this.projectileDirectDamage = owner.getTurretProjectileDirectDamage(index);
        this.barrelForwardOffset = owner.getTurretBarrelForwardOffset(index);

        Point3F muzzle = owner.getTurretMuzzlePoint3D(index);
        this.muzzleX = muzzle != null ? muzzle.x : Float.NaN;
        this.muzzleY = muzzle != null ? muzzle.y : Float.NaN;
        this.muzzleHeight = muzzle != null ? muzzle.height : Float.NaN;
        Object world = owner.getTurretWorldPoint(index);
        this.worldX = coordinate(world, "x", "a");
        this.worldY = coordinate(world, "y", "b");
        this.recoilOffset = owner.getTurretRecoilOffset(index);
        this.recoilOutTime = owner.getTurretRecoilOutTime(index);
        this.recoilReturnTime = owner.getTurretRecoilReturnTime(index);
        Object spread = owner.getTurretShotSpreadOffset(index);
        this.shotSpreadX = coordinate(spread, "x", "a");
        this.shotSpreadY = coordinate(spread, "y", "b");
        this.aimOffsetSpread = owner.getTurretAimOffsetSpread(index);

        this.aimAngle = state != null ? state.aimAngle : Float.NaN;
        this.previousAimAngle = state != null ? state.previousAimAngle : Float.NaN;
        this.aimAngularVelocity = state != null ? state.aimAngularVelocity : Float.NaN;
        this.aimLockTimer = state != null ? state.aimLockTimer : Float.NaN;
        this.reloadTimer = state != null ? state.reloadTimer : Float.NaN;
        this.warmupTimer = state != null ? state.warmupTimer : Float.NaN;
        this.aimReady = state != null && state.aimReady;
        this.alternateFireSide = state != null && state.alternateFireSide;
    }

    public static TurretSnapshot capture(OrderableUnit owner, int index) {
        return new TurretSnapshot(owner, index);
    }

    public OrderableUnit owner() { return owner; }
    public int index() { return index; }
    public Unit target() { return target; }
    public boolean canFire() { return canFire; }
    public boolean aimedAtTarget() { return aimedAtTarget; }
    public float fireDelay() { return fireDelay; }
    public float warmupTime() { return warmupTime; }
    public boolean warmupNoReset() { return warmupNoReset; }
    public float warmupDelayTransfer() { return warmupDelayTransfer; }
    public float projectileDirectDamage() { return projectileDirectDamage; }
    public float barrelForwardOffset() { return barrelForwardOffset; }
    public float muzzleX() { return muzzleX; }
    public float muzzleY() { return muzzleY; }
    public float muzzleHeight() { return muzzleHeight; }
    public float worldX() { return worldX; }
    public float worldY() { return worldY; }
    public float recoilOffset() { return recoilOffset; }
    public float recoilOutTime() { return recoilOutTime; }
    public float recoilReturnTime() { return recoilReturnTime; }
    public float shotSpreadX() { return shotSpreadX; }
    public float shotSpreadY() { return shotSpreadY; }
    public float aimOffsetSpread() { return aimOffsetSpread; }
    public float aimAngle() { return aimAngle; }
    public float previousAimAngle() { return previousAimAngle; }
    public float aimAngularVelocity() { return aimAngularVelocity; }
    public float aimLockTimer() { return aimLockTimer; }
    public float reloadTimer() { return reloadTimer; }
    public float warmupTimer() { return warmupTimer; }
    public boolean aimReady() { return aimReady; }
    public boolean alternateFireSide() { return alternateFireSide; }

    private static float coordinate(Object point, String namedField, String officialField) {
        return point == null ? Float.NaN
                : RustedReflection.getFloatField(point, new String[]{namedField, officialField});
    }
}
