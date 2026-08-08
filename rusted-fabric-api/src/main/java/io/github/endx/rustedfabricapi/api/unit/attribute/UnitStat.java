package io.github.endx.rustedfabricapi.api.unit.attribute;

import rustedwarfare.custom.MutableUnitStats;
import rustedwarfare.custom.MutableStatAccessor;

/** The complete mapped 1.15 custom-unit mutable-stat catalog. */
public enum UnitStat {
    MASS(0, "mass", false, false),
    MAX_ENERGY(1, "maxenergy", false, false),
    ENERGY(2, "energy", true, false),
    MAX_HEALTH(3, "maxhp", false, true),
    HEALTH(4, "hp", true, false),
    MAX_SHIELD(5, "maxshield", false, true),
    SHIELD(6, "shield", true, false),
    SHIELD_REGENERATION(7, "shieldregen", false, false),
    ARMOUR(8, "armour", false, false),
    MAX_ATTACK_RANGE(9, "maxattackrange", false, false),
    SHOOT_DELAY_MULTIPLIER(10, "shootdelaymultiplier", false, false),
    SHOOT_DAMAGE_MULTIPLIER(11, "shootdamagemultiplier", false, false),
    MOVE_SPEED(12, "movespeed", false, false),
    MAX_TURN_SPEED(13, "maxturnspeed", false, false),
    FOG_OF_WAR_SIGHT_RANGE(14, "fogofwarsightrange", false, true),
    NANO_RANGE(15, "nanorange", false, true),
    SELF_REGENERATION_RATE(16, "selfregenrate", false, false),
    TARGET_HEIGHT(17, "targetheight", false, false),
    NANO_FACTORY_SPEED(18, "nanofactoryspeed", false, false);

    private final int nativeId;
    private final String nativeName;
    private final boolean runtimeValue;
    private final boolean integral;

    UnitStat(int nativeId, String nativeName, boolean runtimeValue, boolean integral) {
        this.nativeId = nativeId;
        this.nativeName = nativeName;
        this.runtimeValue = runtimeValue;
        this.integral = integral;
    }

    public int nativeId() { return nativeId; }
    public String nativeName() { return nativeName; }
    public boolean runtimeValue() { return runtimeValue; }
    public boolean supportsModifiers() { return !runtimeValue; }
    public boolean integral() { return integral; }

    double normalize(double value) {
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException(name() + " must be finite");
        }
        return integral ? (int) value : value;
    }

    MutableStatAccessor accessor() {
        MutableStatAccessor accessor = MutableUnitStats.getMutableStatAccessorById(nativeId);
        if (accessor == null) {
            throw new IllegalStateException("Native mutable stat is unavailable: " + nativeName);
        }
        return accessor;
    }

    static UnitStat fromAccessor(MutableStatAccessor accessor) {
        if (accessor == null) return null;
        for (UnitStat stat : values()) {
            if (stat.accessor() == accessor) return stat;
        }
        return null;
    }
}
