package io.github.endx.rustedfabricapi.api.custom;

import io.github.endx.rustedfabricapi.api.data.PersistentData;
import io.github.endx.rustedfabricapi.api.data.PersistentDataKey;
import io.github.endx.rustedfabricapi.api.unit.attribute.CustomUnitStats;
import io.github.endx.rustedfabricapi.api.unit.attribute.UnitStat;
import io.github.endx.rustedfabricapi.api.unit.attribute.UnitStatModifier;
import io.github.endx.rustedfabricapi.api.unit.attribute.UnitVitals;
import io.github.endx.rustedfabricapi.api.unit.attribute.UnitVitalsSnapshot;
import io.github.endx.rustedfabricapi.api.unit.tag.UnitTags;
import io.github.endx.rustedfabricapi.api.util.Identifier;
import io.github.endx.rustedfabricapi.mixin.accessor.CustomUnitBuildQueueAccessor;
import rustedwarfare.custom.CustomUnit;
import rustedwarfare.custom.resource.ResourceType;
import rustedwarfare.unit.Unit;
import rustedwarfare.unit.action.UnitAction;
import rustedwarfare.unit.build.FactoryQueueManager;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;

/**
 * Namespace-stable handle for a native custom-unit instance.
 *
 * <p>The public signatures deliberately avoid mapped game classes, allowing an exploded Java mod
 * to use memory and stat APIs without compiling or remapping against the game Jar.</p>
 */
public final class CustomUnitHandle {
    private final CustomUnit unit;

    private CustomUnitHandle(CustomUnit unit) {
        this.unit = Objects.requireNonNull(unit, "unit");
    }

    public static CustomUnitHandle of(Object unit) {
        if (!(unit instanceof CustomUnit)) {
            throw new IllegalArgumentException("object is not a native custom unit");
        }
        return new CustomUnitHandle((CustomUnit) unit);
    }

    /** Returns an empty result for native units that are not INI-backed custom units. */
    public static Optional<CustomUnitHandle> tryOf(Object unit) {
        return unit instanceof CustomUnit
                ? Optional.of(new CustomUnitHandle((CustomUnit) unit))
                : Optional.empty();
    }

    /** Stable identity object suitable for identity comparison and weak-map keys. */
    public Object identity() { return unit; }

    public boolean alive() { return !unit.dead && !unit.removed; }
    public long id() { return unit.id; }
    public float x() { return ((Unit) unit).x; }
    public float y() { return ((Unit) unit).y; }
    public float height() { return ((Unit) unit).height; }

    /** Current normalized native energy, where {@code 0} is empty and {@code 1} is full. */
    public float energy() { return UnitVitals.snapshot(unit).energy(); }

    /** Writes normalized native energy, clamped to the range shown by the game's energy bar. */
    public void setEnergy(float energy) {
        if (!Float.isFinite(energy)) throw new IllegalArgumentException("energy must be finite");
        UnitVitals.setEnergy(unit, Math.max(0.0F, Math.min(1.0F, energy)));
    }

    /** Returns the current INI unit type name, including after an in-place conversion. */
    public String internalTypeName() { return unit.unitMetadata.getInternalName(); }

    /** Stable identity of the owning native team, or an empty result before ownership is assigned. */
    public Optional<Object> teamIdentity() { return Optional.ofNullable(unit.team); }

    /** Returns the stable native team slot, or empty before ownership is assigned. */
    public OptionalInt teamId() {
        return unit.team != null ? OptionalInt.of(unit.team.teamId) : OptionalInt.empty();
    }

    /** Rebuilds the owning team's cached unit economy after a runtime production mode changes. */
    public void refreshTeamEconomyStats() {
        if (unit.team != null) unit.team.refreshCachedTeamStats(true);
    }

    /** Returns the current native build/action queue progress, or zero for an empty queue. */
    public float currentBuildQueueProgress() {
        FactoryQueueManager queue = buildQueue();
        return queue.getCurrentQueueItem() != null ? queue.buildProgress : 0.0F;
    }

    /** Returns whether the current native queue action carries the requested INI action tag. */
    public boolean currentBuildQueueActionHasTag(String actionTag) {
        UnitAction action = buildQueue().getCurrentAction();
        return action != null && UnitTags.contains(action.getTags(), UnitTags.tag(actionTag));
    }

    /** Synchronizes a tagged current queue item's visible progress. */
    public boolean setCurrentBuildQueueProgress(String actionTag, double progress) {
        requireFinite(progress, "progress");
        if (!currentBuildQueueActionHasTag(actionTag)) return false;
        buildQueue().buildProgress = (float) clamp(progress, 0.0D, 1.0D);
        return true;
    }

    /** Marks a tagged current queue item complete on its next native queue update. */
    public boolean completeCurrentBuildQueueAction(String actionTag) {
        if (!currentBuildQueueActionHasTag(actionTag)) return false;
        unit.forceBuildQueueProgressComplete();
        return true;
    }

    /** Cancels and normally refunds a tagged current queue item through the native action path. */
    public boolean cancelCurrentBuildQueueAction(String actionTag) {
        UnitAction action = buildQueue().getCurrentAction();
        if (action == null || !UnitTags.contains(action.getTags(), UnitTags.tag(actionTag))) {
            return false;
        }
        unit.queueActionNoTarget(action, true);
        return true;
    }

    /** Reads a built-in or unit-local resource by the name used in INI expressions. */
    public double resourceAmount(String name) {
        return resourceType(name).getAmount(unit);
    }

    /** Writes a built-in or unit-local resource through its normal storage path. */
    public void setResourceAmount(String name, double amount) {
        requireFinite(amount, "amount");
        resourceType(name).setAmount(unit, amount);
    }

    /** Adds a finite delta to a built-in or unit-local resource. */
    public void addResourceAmount(String name, double delta) {
        requireFinite(delta, "delta");
        resourceType(name).addAmount(unit, delta);
    }

    public boolean memoryDefined(String name) { return CustomUnitMemory.isDefined(unit, name); }
    public boolean memoryContains(String name) { return CustomUnitMemory.contains(unit, name); }
    public double memoryNumber(String name, double fallback) {
        return CustomUnitMemory.number(unit, name, fallback);
    }
    public boolean memoryBoolean(String name, boolean fallback) {
        return CustomUnitMemory.bool(unit, name, fallback);
    }
    public String memoryString(String name, String fallback) {
        return CustomUnitMemory.string(unit, name, fallback);
    }

    /** Writes a finite number to native unit memory for INI expressions and Decals. */
    public void setMemoryNumber(String name, double value) {
        CustomUnitMemory.setNumber(unit, name, value);
    }

    public void setMemoryBoolean(String name, boolean value) {
        CustomUnitMemory.setBoolean(unit, name, value);
    }

    public void setMemoryString(String name, String value) {
        CustomUnitMemory.setString(unit, name, value);
    }

    public double stat(UnitStat stat) { return CustomUnitStats.get(unit, stat); }

    public void addStatModifier(UnitStat stat, UnitStatModifier modifier) {
        CustomUnitStats.addModifier(unit, stat, modifier);
    }

    public boolean removeStatModifier(UnitStat stat, Identifier modifierId) {
        return CustomUnitStats.removeModifier(unit, stat, modifierId);
    }

    public List<UnitStatModifier> statModifiers(UnitStat stat) {
        return CustomUnitStats.modifiers(unit, stat);
    }

    public <T> void setPersistent(PersistentDataKey<T> key, T value) {
        PersistentData.set(unit, key, value);
    }

    public <T> Optional<T> persistent(PersistentDataKey<T> key) {
        return PersistentData.get(unit, key);
    }

    public boolean removePersistent(PersistentDataKey<?> key) {
        return PersistentData.remove(unit, key);
    }

    /** Restores current health, shield and energy to the range allowed by modified maxima. */
    public void clampVitalsToModifiedMaximums() {
        UnitVitalsSnapshot vitals = UnitVitals.snapshot(unit);
        UnitVitals.setHealth(unit, finiteFloat(clamp(vitals.health(), 0.0D,
                stat(UnitStat.MAX_HEALTH))));
        UnitVitals.setShield(unit, finiteFloat(clamp(vitals.shield(), 0.0D,
                stat(UnitStat.MAX_SHIELD))));
        UnitVitals.setEnergy(unit, finiteFloat(clamp(vitals.energy(), 0.0D,
                stat(UnitStat.MAX_ENERGY))));
    }

    private static double clamp(double value, double minimum, double maximum) {
        return Math.max(minimum, Math.min(value, Math.max(minimum, maximum)));
    }

    private static float finiteFloat(double value) {
        return (float) Math.max(-Float.MAX_VALUE, Math.min(Float.MAX_VALUE, value));
    }

    private ResourceType resourceType(String name) {
        String checked = Objects.requireNonNull(name, "name").trim();
        if (checked.isEmpty()) throw new IllegalArgumentException("name must not be blank");
        ResourceType type = unit.unitMetadata.getResourceTypeByNameOrBuiltin(checked);
        if (type == null) {
            throw new IllegalArgumentException("Unknown resource '" + checked
                    + "' for custom unit " + internalTypeName());
        }
        return type;
    }

    private FactoryQueueManager buildQueue() {
        return ((CustomUnitBuildQueueAccessor) unit).rustedfabricapi$getBuildQueue();
    }

    private static void requireFinite(double value, String name) {
        if (!Double.isFinite(value)) throw new IllegalArgumentException(name + " must be finite");
    }
}
