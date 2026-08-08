package io.github.endx.rustedfabricapi.api.unit.action;

import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import io.github.endx.rustedfabricapi.api.client.render.ClientImage;
import io.github.endx.rustedfabricapi.api.unit.status.StatusEffects;
import io.github.endx.rustedfabricapi.api.util.Identifier;
import io.github.endx.rustedfabricapi.api.world.WorldPoint;
import rustedwarfare.client.render.GameImage;
import rustedwarfare.unit.Unit;
import rustedwarfare.unit.UnitType;
import rustedwarfare.unit.action.ActionCommandType;
import rustedwarfare.unit.action.ActionDisplayType;
import rustedwarfare.unit.action.UnitAction;

/** Immediate or map-targeted action implemented by a deterministic Java callback. */
public final class JavaUnitAction extends UnitAction {
    private final Identifier id;
    private final String text;
    private final String description;
    private final Function<Unit, String> textForUnit;
    private final Function<Unit, String> descriptionForUnit;
    private final Predicate<Unit> visibleWhen;
    private final Predicate<Unit> availableWhen;
    private final Predicate<Unit> lockedWhen;
    private final Function<Unit, String> lockMessage;
    private final JavaUnitActionTargeting targeting;
    private final JavaUnitActionTargetPredicate targetPredicate;
    private final int creditCost;
    private final int cooldownMillis;
    private final Supplier<? extends ClientImage> iconSupplier;
    private final JavaUnitActionHandler handler;

    private JavaUnitAction(Builder builder) {
        super(builder.id.toString());
        this.id = builder.id;
        this.text = requireText(builder.text, "text", 256);
        this.description = requireText(builder.description, "description", 4096);
        this.textForUnit = builder.textForUnit;
        this.descriptionForUnit = builder.descriptionForUnit;
        this.visibleWhen = builder.visibleWhen;
        this.availableWhen = builder.availableWhen;
        this.lockedWhen = builder.lockedWhen;
        this.lockMessage = builder.lockMessage;
        this.targeting = builder.targeting;
        this.targetPredicate = builder.targetPredicate;
        this.creditCost = builder.creditCost;
        this.cooldownMillis = builder.cooldownMillis;
        this.iconSupplier = builder.iconSupplier;
        this.handler = builder.handler;
        this.displayPriority = builder.displayPriority;
    }

    public static Builder builder(String id, String text, String description,
            JavaUnitActionHandler handler) {
        return new Builder(Identifier.parse(id), text, description, handler);
    }

    public Identifier id() { return id; }
    public JavaUnitActionTargeting targeting() { return targeting; }
    public int creditCost() { return creditCost; }
    /** Native deterministic cooldown duration measured by the simulation millisecond clock. */
    public int cooldownMillis() { return cooldownMillis; }
    public int remainingCooldownMillis(Unit unit) {
        return unit == null ? 0 : StatusEffects.remainingActionBlockTime(unit, getActionId());
    }
    public boolean isCoolingDown(Unit unit) { return remainingCooldownMillis(unit) > 0; }
    public boolean canTarget(Unit unit, WorldPoint target) {
        return unit != null && target != null && targetPredicate.canTarget(unit, target);
    }
    public void execute(JavaUnitActionContext context) {
        handler.execute(Objects.requireNonNull(context, "context"));
    }

    @Override public String getText() { return text; }
    @Override public String getDescription() { return description; }
    @Override public String getTextForUnit(Unit unit) {
        return unit != null && textForUnit != null
                ? requireText(textForUnit.apply(unit), "textForUnit", 256) : text;
    }
    @Override public String getDescriptionForUnit(Unit unit) {
        return unit != null && descriptionForUnit != null
                ? requireText(descriptionForUnit.apply(unit), "descriptionForUnit", 4096)
                : description;
    }
    @Override public int getCreditCost() { return creditCost; }
    @Override public int getDisplayQueueCount(Unit unit, boolean includePending) { return 0; }
    @Override public UnitType getBuildUnitType() { return null; }
    @Override public boolean isBuildAction() { return false; }
    @Override public ActionCommandType getActionCommandType() {
        return targeting == JavaUnitActionTargeting.WORLD_POINT
                ? ActionCommandType.targetGround : ActionCommandType.directToAction;
    }
    @Override public ActionDisplayType getDisplayType() { return ActionDisplayType.action; }
    @Override public boolean isQueuedAction() { return false; }
    @Override public boolean isAlwaysSinglePress(Unit unit) { return true; }
    @Override public boolean usesActionTargetPoint() {
        return targeting == JavaUnitActionTargeting.WORLD_POINT;
    }
    @Override public GameImage getIconImage() {
        if (iconSupplier == null) return super.getIconImage();
        ClientImage icon = iconSupplier.get();
        if (icon == null || icon.isClosed()) return null;
        try {
            return icon.nativeImage();
        } catch (IllegalStateException closedDuringLookup) {
            return null;
        }
    }
    @Override public boolean isVisible(Unit unit) {
        return unit != null && visibleWhen.test(unit);
    }
    @Override public boolean isAvailable(Unit unit) {
        return unit != null && availableWhen.test(unit);
    }
    @Override public boolean isLocked(Unit unit) {
        return unit != null && lockedWhen.test(unit);
    }
    @Override public String getLockMessage(Unit unit) {
        String message = lockMessage.apply(unit);
        return message != null ? message : "";
    }

    @Override public int compareTo(Object other) {
        int nativeOrder = compareToAction((UnitAction) other);
        if (nativeOrder != 0 || !(other instanceof JavaUnitAction)) return nativeOrder;
        return id.compareTo(((JavaUnitAction) other).id);
    }

    private static String requireText(String value, String name, int maximum) {
        Objects.requireNonNull(value, name);
        String checked = value.trim();
        if (checked.isEmpty()) throw new IllegalArgumentException(name + " must not be blank");
        if (checked.length() > maximum) {
            throw new IllegalArgumentException(name + " exceeds " + maximum + " characters");
        }
        return checked;
    }

    public static final class Builder {
        private final Identifier id;
        private final String text;
        private final String description;
        private final JavaUnitActionHandler handler;
        private Function<Unit, String> textForUnit;
        private Function<Unit, String> descriptionForUnit;
        private Predicate<Unit> visibleWhen = unit -> true;
        private Predicate<Unit> availableWhen = unit -> true;
        private Predicate<Unit> lockedWhen = unit -> false;
        private Function<Unit, String> lockMessage = unit -> "";
        private JavaUnitActionTargeting targeting = JavaUnitActionTargeting.IMMEDIATE;
        private JavaUnitActionTargetPredicate targetPredicate = (unit, target) -> true;
        private int creditCost;
        private int cooldownMillis;
        private Supplier<? extends ClientImage> iconSupplier;
        private float displayPriority;

        private Builder(Identifier id, String text, String description,
                JavaUnitActionHandler handler) {
            this.id = Objects.requireNonNull(id, "id");
            this.text = text;
            this.description = description;
            this.handler = Objects.requireNonNull(handler, "handler");
        }

        public Builder visibleWhen(Predicate<Unit> condition) {
            this.visibleWhen = Objects.requireNonNull(condition, "condition");
            return this;
        }
        public Builder availableWhen(Predicate<Unit> condition) {
            this.availableWhen = Objects.requireNonNull(condition, "condition");
            return this;
        }
        /** Computes client presentation text for the concrete unit shown in the action panel. */
        public Builder textForUnit(Function<Unit, String> text) {
            this.textForUnit = Objects.requireNonNull(text, "text");
            return this;
        }
        /** Computes client tooltip text for the concrete unit shown in the action panel. */
        public Builder descriptionForUnit(Function<Unit, String> description) {
            this.descriptionForUnit = Objects.requireNonNull(description, "description");
            return this;
        }
        public Builder lockedWhen(Predicate<Unit> condition, Function<Unit, String> message) {
            this.lockedWhen = Objects.requireNonNull(condition, "condition");
            this.lockMessage = Objects.requireNonNull(message, "message");
            return this;
        }
        /** Uses the native map cursor and accepts every finite world point. */
        public Builder targetPoint() {
            return targetPointWhen((unit, target) -> true);
        }
        /** Uses the native map cursor and validates the point for every selected unit. */
        public Builder targetPointWhen(JavaUnitActionTargetPredicate condition) {
            this.targeting = JavaUnitActionTargeting.WORLD_POINT;
            this.targetPredicate = Objects.requireNonNull(condition, "condition");
            return this;
        }
        /** Charges this many team credits once for every unit whose handler executes. */
        public Builder creditCost(int credits) {
            if (credits < 0) throw new IllegalArgumentException("creditCost must be non-negative");
            this.creditCost = credits;
            return this;
        }
        /**
         * Blocks this action after a successful start for the given simulation milliseconds.
         * The native status is saved with the unit and participates in synchronized gameplay.
         */
        public Builder cooldownMillis(int durationMillis) {
            if (durationMillis < 0) {
                throw new IllegalArgumentException("cooldownMillis must be non-negative");
            }
            this.cooldownMillis = durationMillis;
            return this;
        }
        /** Uses one already loaded Jar/engine image for the process lifetime. */
        public Builder icon(ClientImage icon) {
            ClientImage checked = Objects.requireNonNull(icon, "icon");
            return icon(() -> checked);
        }
        /** Resolves a client-only image lazily; returning null displays the native no-icon state. */
        public Builder icon(Supplier<? extends ClientImage> icon) {
            this.iconSupplier = Objects.requireNonNull(icon, "icon");
            return this;
        }
        public Builder displayPriority(float priority) {
            if (!Float.isFinite(priority)) {
                throw new IllegalArgumentException("displayPriority must be finite");
            }
            this.displayPriority = priority;
            return this;
        }
        public JavaUnitAction build() { return new JavaUnitAction(this); }
    }
}
