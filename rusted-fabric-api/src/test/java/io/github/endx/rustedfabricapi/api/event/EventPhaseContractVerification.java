package io.github.endx.rustedfabricapi.api.event;

import io.github.endx.rustedfabricapi.api.util.Identifier;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public final class EventPhaseContractVerification {
    private static final Identifier EARLY = Identifier.of("contract", "early");
    private static final Identifier COMPAT = Identifier.of("contract", "compat");
    private static final Identifier LATE = Identifier.of("contract", "late");

    private EventPhaseContractVerification() {
    }

    public static void main(String[] args) {
        verify();
        System.out.println("Event phase contracts passed");
    }

    public static void verify() {
        verifyPhaseOrderingAndRemoval();
        verifyCycleRejectionIsTransactional();
        verifyInvokerSnapshotDuringMutation();
        verifyFailedInvokerBuildRollsBackRegistration();
    }

    private static void verifyPhaseOrderingAndRemoval() {
        List<String> calls = new ArrayList<String>();
        RustedFabricEvent<Probe> event = event();
        RustedFabricEvent.Registration defaultFirst =
                event.subscribe(value -> calls.add("default-first:" + value));
        RustedFabricEvent.PhasedRegistration late =
                event.subscribe(LATE, value -> calls.add("late:" + value));
        RustedFabricEvent.PhasedRegistration early =
                event.subscribe(EARLY, value -> calls.add("early:" + value));
        RustedFabricEvent.Registration defaultSecond =
                event.subscribe(value -> calls.add("default-second:" + value));

        require(event.addPhaseOrdering(EARLY, RustedFabricEvent.DEFAULT_PHASE),
                "new early/default phase edge was not added");
        require(event.addPhaseOrdering(RustedFabricEvent.DEFAULT_PHASE, LATE),
                "new default/late phase edge was not added");
        require(!event.addPhaseOrdering(EARLY, RustedFabricEvent.DEFAULT_PHASE),
                "duplicate phase edge was reported as new");
        event.invoker().accept("one");
        require(calls.equals(Arrays.asList("early:one", "default-first:one",
                        "default-second:one", "late:one")),
                "phase constraints or within-phase registration order were not respected");
        require(event.phaseOrder().equals(Arrays.asList(
                        EARLY, RustedFabricEvent.DEFAULT_PHASE, LATE)),
                "reported topological phase order disagrees with invocation order");
        require(early.phase().equals(EARLY) && late.phase().equals(LATE),
                "phased subscription did not retain its phase identity");
        require(event.listenerCount() == 4
                        && event.listenerCount(RustedFabricEvent.DEFAULT_PHASE) == 2
                        && event.listenerCount(EARLY) == 1,
                "per-phase listener diagnostics were incorrect");

        calls.clear();
        require(early.unregister() && !early.unregister(),
                "phased subscription cleanup was not idempotent");
        event.invoker().accept("two");
        require(calls.equals(Arrays.asList("default-first:two",
                        "default-second:two", "late:two")),
                "removed phased listener remained in the invoker snapshot");
        defaultFirst.close();
        defaultSecond.close();
        late.close();
    }

    private static void verifyCycleRejectionIsTransactional() {
        List<String> calls = new ArrayList<String>();
        RustedFabricEvent<Probe> event = event();
        event.register(EARLY, value -> calls.add("early"));
        event.register(COMPAT, value -> calls.add("compat"));
        event.register(LATE, value -> calls.add("late"));
        event.addPhaseOrdering(EARLY, COMPAT);
        event.addPhaseOrdering(COMPAT, LATE);
        List<Identifier> before = event.phaseOrder();

        boolean cycleRejected = false;
        try {
            event.addPhaseOrdering(LATE, EARLY);
        } catch (IllegalArgumentException expected) {
            cycleRejected = true;
        }
        require(cycleRejected && event.phaseOrder().equals(before),
                "cyclic phase edge was accepted or partially committed");
        event.invoker().accept("ignored");
        require(calls.equals(Arrays.asList("early", "compat", "late")),
                "cycle rejection changed the existing invoker order");

        boolean selfRejected = false;
        try {
            event.addPhaseOrdering(EARLY, EARLY);
        } catch (IllegalArgumentException expected) {
            selfRejected = true;
        }
        require(selfRejected, "self-referential phase ordering was accepted");
    }

    private static void verifyInvokerSnapshotDuringMutation() {
        List<String> calls = new ArrayList<String>();
        RustedFabricEvent<Probe> event = event();
        AtomicBoolean added = new AtomicBoolean();
        event.register(value -> {
            calls.add("first:" + value);
            if (added.compareAndSet(false, true)) {
                event.register(probe -> calls.add("added:" + probe));
            }
        });
        event.invoker().accept("one");
        require(calls.equals(Arrays.asList("first:one")),
                "listener added during dispatch leaked into the active snapshot");
        event.invoker().accept("two");
        require(calls.equals(Arrays.asList("first:one", "first:two", "added:two")),
                "listener mutation was not visible to the next invocation");
    }

    private static void verifyFailedInvokerBuildRollsBackRegistration() {
        AtomicBoolean reject = new AtomicBoolean();
        RustedFabricEvent<Probe> event = RustedFabricEvent.create(listeners -> {
            if (reject.get()) throw new IllegalStateException("synthetic factory failure");
            return value -> {
                for (Probe listener : listeners) listener.accept(value);
            };
        });
        Probe originalInvoker = event.invoker();
        reject.set(true);
        boolean failed = false;
        try {
            event.register(EARLY, value -> { });
        } catch (IllegalStateException expected) {
            failed = true;
        }
        require(failed && event.listenerCount() == 0 && event.invoker() == originalInvoker
                        && event.phaseOrder().equals(
                        Arrays.asList(RustedFabricEvent.DEFAULT_PHASE)),
                "failed invoker rebuild left a partial listener or phase mutation");
    }

    private static RustedFabricEvent<Probe> event() {
        return RustedFabricEvent.create(listeners -> value -> {
            for (Probe listener : listeners) listener.accept(value);
        });
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private interface Probe {
        void accept(String value);
    }
}
