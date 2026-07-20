package io.github.endx.rustedfabricapi.api.resource;

import io.github.endx.rustedfabricapi.api.unit.Teams;
import rustedwarfare.custom.resource.ResourceAmount;
import rustedwarfare.custom.resource.ResourceType;
import rustedwarfare.game.Team;
import rustedwarfare.unit.Unit;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Typed resource discovery, querying, and transaction helpers. */
public final class Resources {
    private Resources() {
    }

    public static List<ResourceType> builtIn() {
        return typedSnapshot(ResourceType.builtInResources);
    }

    public static List<ResourceType> activeDisplayResources() {
        return typedSnapshot(ResourceType.getActiveDisplayResources());
    }

    public static Optional<ResourceType> find(String internalName) {
        Objects.requireNonNull(internalName, "internalName");
        return Optional.ofNullable(ResourceType.getAnyResourceTypeByName(internalName));
    }

    public static double get(Unit unit, ResourceType type) {
        Objects.requireNonNull(unit, "unit");
        return Objects.requireNonNull(type, "type").getAmount(unit);
    }

    /** Mutates a unit/team resource through the resource type's normal storage path. */
    public static void set(Unit unit, ResourceType type, double amount) {
        requireFinite(amount, "amount");
        Objects.requireNonNull(unit, "unit");
        Objects.requireNonNull(type, "type").setAmount(unit, amount);
    }

    public static void add(Unit unit, ResourceType type, double delta) {
        requireFinite(delta, "delta");
        Objects.requireNonNull(unit, "unit");
        Objects.requireNonNull(type, "type").addAmount(unit, delta);
    }

    public static boolean canAfford(Unit unit, ResourceAmount cost) {
        Objects.requireNonNull(unit, "unit");
        return Objects.requireNonNull(cost, "cost").hasEnoughResources(unit);
    }

    /** Returns false without changing state when the complete cost cannot be paid. */
    public static boolean tryPay(Unit unit, ResourceAmount cost) {
        Objects.requireNonNull(unit, "unit");
        Objects.requireNonNull(cost, "cost");
        if (!cost.hasEnoughResources(unit)) return false;
        cost.subtractFromUnit(unit);
        return true;
    }

    public static void refund(Unit unit, ResourceAmount amount) {
        Objects.requireNonNull(unit, "unit");
        Objects.requireNonNull(amount, "amount").addToUnit(unit);
    }

    public static double getTeamCredits(Team team) {
        return Teams.credits(team);
    }

    public static void setTeamCredits(Team team, double credits) {
        Teams.setCredits(team, credits);
    }

    public static double getTeamGlobal(Team team, ResourceType type) {
        Objects.requireNonNull(type, "type");
        return Objects.requireNonNull(team, "team").getGlobalResourceAmount(type);
    }

    private static List<ResourceType> typedSnapshot(List<?> source) {
        if (source == null || source.isEmpty()) return Collections.emptyList();
        List<ResourceType> result = new ArrayList<ResourceType>(source.size());
        for (Object value : source) {
            if (value instanceof ResourceType) result.add((ResourceType) value);
        }
        return Collections.unmodifiableList(result);
    }

    private static void requireFinite(double value, String name) {
        if (!Double.isFinite(value)) throw new IllegalArgumentException(name + " must be finite");
    }
}
