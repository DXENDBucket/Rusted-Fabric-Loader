package io.github.endx.strategicai;

/** Persistent high-level intent used to allocate forces before selecting orders. */
enum StrategicPosture {
    FORTIFY,
    EXPAND,
    PRESSURE;

    static StrategicPosture select(int combatUnits, int homeThreats,
            boolean hasUnclaimedResources) {
        if (homeThreats > 0 && (combatUnits < 4 || homeThreats * 2 >= combatUnits)) {
            return FORTIFY;
        }
        if (hasUnclaimedResources && combatUnits < 6) return EXPAND;
        return PRESSURE;
    }
}
