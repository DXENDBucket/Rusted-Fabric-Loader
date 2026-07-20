package io.github.endx.rustedfabricapi.api.game;

import io.github.endx.rustedfabricapi.api.util.RustedReflection;

import java.util.Optional;

/**
 * A live, namespace-neutral view of a Rusted Warfare unit.
 *
 * <p>The view keeps the underlying game object and reads its current values on every call. It is
 * therefore cheap to create, but it must only be used from a game callback or the game thread.</p>
 */
public final class UnitView {
    private final Object unit;

    UnitView(Object unit) {
        this.unit = unit;
    }

    /** Returns the underlying mapped game object for an API not covered by this view yet. */
    public Object raw() {
        return unit;
    }

    public long id() {
        return number(new String[]{"id", "eh"}).longValue();
    }

    public float x() {
        return number(new String[]{"x", "eo"}).floatValue();
    }

    public float y() {
        return number(new String[]{"y", "ep"}).floatValue();
    }

    public float height() {
        return number(new String[]{"height", "eq"}).floatValue();
    }

    public float direction() {
        return number(new String[]{"direction", "cg"}).floatValue();
    }

    public float health() {
        return number(new String[]{"hp", "cu"}).floatValue();
    }

    public float maxHealth() {
        return number(new String[]{"maxHp", "cv"}).floatValue();
    }

    public float healthFraction() {
        float maximum = maxHealth();
        return maximum > 0.0F ? health() / maximum : 0.0F;
    }

    public float shield() {
        return number(new String[]{"shield", "cx"}).floatValue();
    }

    public float maxShield() {
        return number(new String[]{"maxShield", "cA"}).floatValue();
    }

    public float energy() {
        return number(new String[]{"energy", "cB"}).floatValue();
    }

    public int ammo() {
        return number(new String[]{"ammo", "cE"}).intValue();
    }

    public boolean dead() {
        return RustedReflection.getBooleanField(unit, new String[]{"dead", "bV"});
    }

    public boolean removed() {
        return RustedReflection.getBooleanField(unit, new String[]{"removed", "ej"});
    }

    public boolean alive() {
        return !dead() && !removed();
    }

    public boolean registeredWithTeam() {
        return RustedReflection.getBooleanField(unit, new String[]{"registeredWithTeam", "bY"});
    }

    public Optional<TeamView> team() {
        Object value = RustedReflection.getFieldValue(unit, new String[]{"team", "bX"});
        return value == null ? Optional.empty() : Optional.of(Teams.view(value));
    }

    public boolean building() {
        return Boolean.TRUE.equals(RustedReflection.invokeInstance(unit,
                new String[]{"isBuilding", "bI"}));
    }

    public boolean flying() {
        return Boolean.TRUE.equals(RustedReflection.invokeInstance(unit,
                new String[]{"isFlying", "i"}));
    }

    public boolean underwater() {
        return Boolean.TRUE.equals(RustedReflection.invokeInstance(unit,
                new String[]{"isUnderwater", "Q"}));
    }

    public boolean damageImmune() {
        return Boolean.TRUE.equals(RustedReflection.invokeInstance(unit,
                new String[]{"isDamageImmune", "J"}));
    }

    /** Returns the mapped movement enum name, such as {@code land}, {@code air}, or {@code water}. */
    public String movementType() {
        Object value = RustedReflection.invokeInstance(unit,
                new String[]{"getMovementType", "h"});
        if (value instanceof Enum<?>) return ((Enum<?>) value).name();
        return value == null ? "unknown" : value.toString();
    }

    /** A mapping-independent diagnostic name for the concrete unit implementation. */
    public String implementationName() {
        return unit.getClass().getName();
    }

    public Optional<UnitView> recentDamager(float seconds) {
        Object value = RustedReflection.invokeInstance(unit,
                new String[]{"getRecentDamager", "q"}, Float.valueOf(seconds));
        return value == null ? Optional.empty() : Optional.of(Units.view(value));
    }

    public Optional<UnitView> containingUnit() {
        Object value = RustedReflection.invokeInstance(unit,
                new String[]{"getContainingUnit", "dr"});
        return value == null ? Optional.empty() : Optional.of(Units.view(value));
    }

    /** Uses the game's health setter so its normal invariants remain in effect. */
    public UnitView setHealth(float health) {
        RustedReflection.invokeInstance(unit, new String[]{"setHp", "o"}, Float.valueOf(health));
        return this;
    }

    public UnitView setDirection(float direction) {
        RustedReflection.invokeInstance(unit, new String[]{"setDirection", "h"},
                Float.valueOf(direction));
        return this;
    }

    public UnitView setConstructionProgress(float progress) {
        if (Float.isNaN(progress) || progress < 0.0F || progress > 1.0F) {
            throw new IllegalArgumentException("progress must be between 0 and 1");
        }
        RustedReflection.invokeInstance(unit, new String[]{"setConstructionProgress", "r"},
                Float.valueOf(progress));
        return this;
    }

    public UnitView changeTeam(TeamView team) {
        if (team == null) throw new IllegalArgumentException("team must not be null");
        RustedReflection.invokeInstance(unit, new String[]{"changeTeam", "e"}, team.raw());
        return this;
    }

    public void removeFromGame() {
        RustedReflection.invokeInstance(unit, new String[]{"removeFromGame", "a"});
    }

    private Number number(String[] names) {
        Object value = RustedReflection.getFieldValue(unit, names);
        return value instanceof Number ? (Number) value : Integer.valueOf(0);
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof UnitView && ((UnitView) other).unit == unit;
    }

    @Override
    public int hashCode() {
        return System.identityHashCode(unit);
    }

    @Override
    public String toString() {
        return "UnitView{id=" + id() + ", type=" + implementationName() + "}";
    }
}
