package rustedwarfare.unit;

import rustedwarfare.framework.GameObject;
import rustedwarfare.game.Team;

import java.util.ArrayList;
import java.util.List;

public class Unit extends GameObject {
    public static final List<Unit> allUnits = new ArrayList<Unit>();

    public Team team;
    public boolean dead;
    public boolean registeredWithTeam;
    public float direction;
    public float hp;
    public float maxHp;
    public float shield;
    public float maxShield;
    public float energy;
    public int ammo;
    public float constructionProgress;
    public Unit damager;
    public Unit container;
    public boolean building;
    public boolean flying;
    public boolean underwater;
    public boolean immune;
    public String movement = "land";

    public boolean isBuilding() { return building; }
    public boolean isFlying() { return flying; }
    public boolean isUnderwater() { return underwater; }
    public boolean isDamageImmune() { return immune; }
    public Object getMovementType() { return movement; }
    public Unit getRecentDamager(float seconds) { return damager; }
    public Unit getContainingUnit() { return container; }
    public void setHp(float value) { hp = value; }
    public void setDirection(float value) { direction = value; }
    public void setConstructionProgress(float value) { constructionProgress = value; }
    public void changeTeam(Team value) { team = value; }
}
