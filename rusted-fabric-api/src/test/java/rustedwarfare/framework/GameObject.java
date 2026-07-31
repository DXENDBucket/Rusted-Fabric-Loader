package rustedwarfare.framework;

import rustedwarfare.unit.Unit;

public class GameObject {
    public long id;
    public boolean removed;
    public int drawLayer;
    public float x;
    public float y;
    public float height;

    public void removeFromGame() {
        removed = true;
    }

    public static Unit getUnitById(long id, boolean includeRemoved) {
        for (Unit unit : Unit.allUnits) {
            if (unit.id == id && (includeRemoved || !unit.removed)) return unit;
        }
        return null;
    }
}
