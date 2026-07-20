package io.github.endx.rustedfabricapi.api.unit.action;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import io.github.endx.rustedfabricapi.api.event.RustedFabricEvent;
import io.github.endx.rustedfabricapi.api.unit.action.event.JavaUnitActionEvents;
import io.github.endx.rustedfabricapi.api.world.WorldPoint;
import io.github.endx.rustedfabricapi.internal.unit.action.JavaUnitActionRuntime;
import rustedwarfare.unit.action.ActionCommandType;
import rustedwarfare.unit.action.ActionDisplayType;

public final class JavaUnitActionContractVerification {
    private JavaUnitActionContractVerification() {
    }

    public static void verify() {
        JavaUnitAction action = JavaUnitAction.builder(
                "contract:java_action", "Contract action", "Runs the contract callback",
                context -> { })
                .displayPriority(12.5F)
                .build();
        require(action.id().toString().equals("contract:java_action")
                        && action.getActionIdString().equals("contract:java_action"),
                "Java action identifier was not preserved");
        require(action.getText().equals("Contract action")
                        && action.getDescription().equals("Runs the contract callback")
                        && action.getCreditCost() == 0
                        && action.cooldownMillis() == 0
                        && action.remainingCooldownMillis(null) == 0
                        && !action.isCoolingDown(null)
                        && action.getDisplayQueueCount(null, false) == 0,
                "Java action presentation contract changed");
        require(action.getActionCommandType() == ActionCommandType.directToAction
                        && action.getDisplayType() == ActionDisplayType.action
                        && !action.isBuildAction() && action.getBuildUnitType() == null
                        && !action.isQueuedAction() && !action.usesActionTargetPoint(),
                "Java action no longer has immediate zero-cost semantics");
        require(!action.isVisible(null) && !action.isAvailable(null) && !action.isLocked(null),
                "Java action null-unit predicates changed");

        AtomicInteger iconLookups = new AtomicInteger();
        JavaUnitAction presentedAction = JavaUnitAction.builder(
                "contract:presented_action", "Fallback text", "Fallback description",
                context -> { })
                .textForUnit(unit -> "Dynamic text")
                .descriptionForUnit(unit -> "Dynamic description")
                .icon(() -> { iconLookups.incrementAndGet(); return null; })
                .build();
        require(presentedAction.getTextForUnit(null).equals("Fallback text")
                        && presentedAction.getDescriptionForUnit(null)
                                .equals("Fallback description"),
                "Java action presentation did not fall back without a unit context");
        require(presentedAction.getIconImage() == null && iconLookups.get() == 1,
                "Java action lazy no-icon presentation contract changed");
        expectNull(() -> JavaUnitAction.builder(
                "contract:null_text_callback", "Text", "Description", context -> { })
                .textForUnit(null));

        JavaUnitAction pointAction = JavaUnitAction.builder(
                "contract:point_action", "Point action", "Targets the map", context -> { })
                .targetPointWhen((unit, point) -> point.x() >= 0.0F && point.y() >= 0.0F)
                .build();
        require(pointAction.targeting() == JavaUnitActionTargeting.WORLD_POINT
                        && pointAction.getActionCommandType() == ActionCommandType.targetGround
                        && pointAction.usesActionTargetPoint(),
                "point Java action did not select the native target-ground command mode");
        require(!pointAction.canTarget(null, new WorldPoint(1.0F, 2.0F))
                        && Boolean.FALSE.equals(JavaUnitActionRuntime.targetedActionAllowed(
                                null, pointAction, 1.0F, 2.0F))
                        && Boolean.FALSE.equals(JavaUnitActionRuntime.targetedActionAllowed(
                                null, pointAction, Float.NaN, 2.0F)),
                "point Java action accepted an invalid native target context");

        JavaUnitAction paidAction = JavaUnitAction.builder(
                "contract:paid_action", "Paid action", "Costs credits", context -> { })
                .creditCost(25)
                .cooldownMillis(3_000)
                .build();
        require(paidAction.creditCost() == 25 && paidAction.getCreditCost() == 25
                        && paidAction.getPrice().getCredits() == 25
                        && paidAction.cooldownMillis() == 3_000,
                "Java action cost or cooldown configuration changed");
        expectIllegal(() -> JavaUnitAction.builder(
                "contract:negative_cost", "Bad", "Negative price", context -> { })
                .creditCost(-1));
        expectIllegal(() -> JavaUnitAction.builder(
                "contract:negative_cooldown", "Bad", "Negative cooldown", context -> { })
                .cooldownMillis(-1));

        require(JavaUnitActions.register(action) == action,
                "first Java action registration did not return the registered instance");
        require(JavaUnitActions.register(action) == action,
                "same-instance Java action registration was not idempotent");
        require(JavaUnitActions.find("contract:java_action").orElse(null) == action,
                "registered Java action could not be found");

        JavaUnitActionBinding binding = JavaUnitActions.attach("Contract_Tank", 2, 4, action);
        require(binding.matches("contract_tank", 2) && binding.matches("CONTRACT_TANK", 4)
                        && !binding.matches("contract_tank", 1)
                        && JavaUnitActions.attach("contract_tank", 2, 4, action) == binding,
                "Java action attachment matching or idempotency changed");
        List<JavaUnitActionBinding> bindings = JavaUnitActions.bindings();
        expectUnsupported(() -> bindings.clear());

        JavaUnitAction later = JavaUnitAction.builder(
                "contract:java_action_z", "Later", "Stable tie-break check", context -> { })
                .displayPriority(12.5F)
                .build();
        require(action.compareTo(later) < 0,
                "equal-priority Java actions lost their stable identifier tie-break");
        expectIllegalState(() -> JavaUnitActions.register(JavaUnitAction.builder(
                "contract:java_action", "Duplicate", "Different instance", context -> { })
                .build()));
        expectIllegal(() -> JavaUnitActions.attach("tank", 3, 2, action));
        expectIllegal(() -> JavaUnitAction.builder(
                "contract:not_finite", "Bad", "Bad priority", context -> { })
                .displayPriority(Float.NaN));

        AtomicInteger calls = new AtomicInteger();
        RustedFabricEvent.Registration first = JavaUnitActionEvents.BEFORE_EXECUTE.subscribe(
                context -> { calls.incrementAndGet(); return false; });
        RustedFabricEvent.Registration second = JavaUnitActionEvents.BEFORE_EXECUTE.subscribe(
                context -> { calls.incrementAndGet(); return true; });
        require(JavaUnitActionEvents.BEFORE_EXECUTE.invoker().beforeExecute(null)
                        && calls.get() == 2,
                "Java action cancellation did not aggregate all listeners");
        first.close();
        second.close();
    }

    private static void expectIllegal(Runnable action) {
        try {
            action.run();
            throw new AssertionError("invalid Java action input was accepted");
        } catch (IllegalArgumentException expected) {
            // Expected.
        }
    }

    private static void expectIllegalState(Runnable action) {
        try {
            action.run();
            throw new AssertionError("duplicate Java action was accepted");
        } catch (IllegalStateException expected) {
            // Expected.
        }
    }

    private static void expectUnsupported(Runnable action) {
        try {
            action.run();
            throw new AssertionError("Java action registry snapshot was mutable");
        } catch (UnsupportedOperationException expected) {
            // Expected.
        }
    }

    private static void expectNull(Runnable action) {
        try {
            action.run();
            throw new AssertionError("null Java action callback was accepted");
        } catch (NullPointerException expected) {
            // Expected.
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
