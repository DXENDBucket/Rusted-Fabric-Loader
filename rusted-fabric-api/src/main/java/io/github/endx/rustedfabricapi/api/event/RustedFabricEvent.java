package io.github.endx.rustedfabricapi.api.event;

import io.github.endx.rustedfabricapi.api.util.Identifier;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.function.Function;

/**
 * Snapshot-based event with removable subscriptions and Fabric-style ordered phases.
 *
 * <p>Listeners registered through the original one-argument methods belong to
 * {@link #DEFAULT_PHASE} and retain registration order. A phase ordering means every listener in
 * the first phase runs before every listener in the second. Unrelated phases are ordered by their
 * earliest listener registration, with the phase identifier as a stable final tie-breaker.</p>
 */
public final class RustedFabricEvent<T> {
    public static final Identifier DEFAULT_PHASE = Identifier.of("rustedfabric", "default");

    private final Object lock = new Object();
    private final Function<List<T>, T> invokerFactory;
    private final ArrayList<ListenerEntry<T>> listeners = new ArrayList<ListenerEntry<T>>();
    private final LinkedHashMap<Identifier, PhaseNode> phases =
            new LinkedHashMap<Identifier, PhaseNode>();
    private long nextSequence;
    private volatile T invoker;

    private RustedFabricEvent(Function<List<T>, T> invokerFactory) {
        this.invokerFactory = Objects.requireNonNull(invokerFactory, "invokerFactory");
        phases.put(DEFAULT_PHASE, new PhaseNode(DEFAULT_PHASE));
        rebuildInvokerLocked();
    }

    public static <T> RustedFabricEvent<T> create(Function<List<T>, T> invokerFactory) {
        return new RustedFabricEvent<T>(invokerFactory);
    }

    /** Registers a permanent listener in {@link #DEFAULT_PHASE}. */
    public void register(T listener) {
        register(DEFAULT_PHASE, listener);
    }

    /** Registers a permanent listener in a named phase. */
    public void register(Identifier phase, T listener) {
        addListener(phase, listener);
    }

    /** Registers a default-phase listener and returns an idempotent cleanup handle. */
    public Registration subscribe(T listener) {
        return subscribe(DEFAULT_PHASE, listener);
    }

    /** Registers a phased listener and returns an idempotent cleanup handle. */
    public PhasedRegistration subscribe(Identifier phase, T listener) {
        return addListener(phase, listener);
    }

    /**
     * Declares that all listeners in {@code before} run before all listeners in {@code after}.
     * The declaration remains active when either phase temporarily has no listeners.
     *
     * @return {@code true} when a new ordering edge was added, or {@code false} if it existed
     * @throws IllegalArgumentException if the edge is self-referential or creates a cycle
     */
    public boolean addPhaseOrdering(Identifier before, Identifier after) {
        Identifier checkedBefore = Objects.requireNonNull(before, "before");
        Identifier checkedAfter = Objects.requireNonNull(after, "after");
        if (checkedBefore.equals(checkedAfter)) {
            throw new IllegalArgumentException("An event phase cannot run before itself: "
                    + checkedBefore);
        }

        synchronized (lock) {
            boolean hadBefore = phases.containsKey(checkedBefore);
            boolean hadAfter = phases.containsKey(checkedAfter);
            PhaseNode beforeNode = phaseLocked(checkedBefore);
            phaseLocked(checkedAfter);
            if (!beforeNode.after.add(checkedAfter)) return false;
            try {
                orderedPhasesLocked();
                rebuildInvokerLocked();
            } catch (RuntimeException | Error failure) {
                beforeNode.after.remove(checkedAfter);
                if (!hadBefore) removeUnusedPhaseLocked(checkedBefore);
                if (!hadAfter) removeUnusedPhaseLocked(checkedAfter);
                throw failure;
            }
            return true;
        }
    }

    /** Removes one oldest matching listener, regardless of its phase. */
    public boolean unregister(T listener) {
        if (listener == null) return false;
        synchronized (lock) {
            int index = oldestMatchingListenerLocked(null, listener);
            return index >= 0 && removeListenerAtLocked(index);
        }
    }

    /** Removes one oldest matching listener from the requested phase. */
    public boolean unregister(Identifier phase, T listener) {
        if (phase == null || listener == null) return false;
        synchronized (lock) {
            int index = oldestMatchingListenerLocked(phase, listener);
            return index >= 0 && removeListenerAtLocked(index);
        }
    }

    public int listenerCount() {
        synchronized (lock) { return listeners.size(); }
    }

    public int listenerCount(Identifier phase) {
        Identifier checked = Objects.requireNonNull(phase, "phase");
        synchronized (lock) {
            int count = 0;
            for (ListenerEntry<T> entry : listeners) {
                if (entry.phase.equals(checked)) count++;
            }
            return count;
        }
    }

    /** Returns the current topological phase order, including declared empty phases. */
    public List<Identifier> phaseOrder() {
        synchronized (lock) {
            return Collections.unmodifiableList(
                    new ArrayList<Identifier>(orderedPhasesLocked()));
        }
    }

    /** Returns the immutable listener snapshot produced after the latest mutation. */
    public T invoker() {
        return invoker;
    }

    private ListenerRegistration addListener(Identifier phase, T listener) {
        Identifier checkedPhase = Objects.requireNonNull(phase, "phase");
        T checkedListener = Objects.requireNonNull(listener, "listener");
        synchronized (lock) {
            boolean hadPhase = phases.containsKey(checkedPhase);
            phaseLocked(checkedPhase);
            ListenerEntry<T> entry = new ListenerEntry<T>(
                    checkedPhase, checkedListener, nextSequence++);
            listeners.add(entry);
            try {
                rebuildInvokerLocked();
            } catch (RuntimeException | Error failure) {
                listeners.remove(entry);
                if (!hadPhase) removeUnusedPhaseLocked(checkedPhase);
                throw failure;
            }
            return new ListenerRegistration(entry);
        }
    }

    private int oldestMatchingListenerLocked(Identifier phase, T listener) {
        int result = -1;
        long oldest = Long.MAX_VALUE;
        for (int i = 0; i < listeners.size(); i++) {
            ListenerEntry<T> entry = listeners.get(i);
            if ((phase == null || phase.equals(entry.phase))
                    && Objects.equals(entry.listener, listener)
                    && entry.sequence < oldest) {
                result = i;
                oldest = entry.sequence;
            }
        }
        return result;
    }

    private boolean removeListener(ListenerEntry<T> entry) {
        synchronized (lock) {
            int index = listeners.indexOf(entry);
            return index >= 0 && removeListenerAtLocked(index);
        }
    }

    private boolean removeListenerAtLocked(int index) {
        ListenerEntry<T> removed = listeners.remove(index);
        try {
            rebuildInvokerLocked();
        } catch (RuntimeException | Error failure) {
            listeners.add(index, removed);
            throw failure;
        }
        return true;
    }

    private PhaseNode phaseLocked(Identifier id) {
        PhaseNode phase = phases.get(id);
        if (phase == null) {
            phase = new PhaseNode(id);
            phases.put(id, phase);
        }
        return phase;
    }

    private void removeUnusedPhaseLocked(Identifier id) {
        if (DEFAULT_PHASE.equals(id)) return;
        PhaseNode phase = phases.get(id);
        if (phase == null || !phase.after.isEmpty()) return;
        for (ListenerEntry<T> listener : listeners) {
            if (listener.phase.equals(id)) return;
        }
        for (PhaseNode candidate : phases.values()) {
            if (candidate.after.contains(id)) return;
        }
        phases.remove(id);
    }

    private void rebuildInvokerLocked() {
        List<Identifier> orderedPhases = orderedPhasesLocked();
        Map<Identifier, Integer> phaseIndexes = new HashMap<Identifier, Integer>();
        for (int i = 0; i < orderedPhases.size(); i++) {
            phaseIndexes.put(orderedPhases.get(i), Integer.valueOf(i));
        }
        ArrayList<ListenerEntry<T>> orderedListeners =
                new ArrayList<ListenerEntry<T>>(listeners);
        orderedListeners.sort(Comparator
                .comparingInt((ListenerEntry<T> entry) ->
                        phaseIndexes.get(entry.phase).intValue())
                .thenComparingLong(entry -> entry.sequence));
        ArrayList<T> snapshot = new ArrayList<T>(orderedListeners.size());
        for (ListenerEntry<T> entry : orderedListeners) snapshot.add(entry.listener);
        invoker = invokerFactory.apply(Collections.unmodifiableList(snapshot));
    }

    private List<Identifier> orderedPhasesLocked() {
        final Map<Identifier, Long> firstRegistrations =
                new HashMap<Identifier, Long>();
        for (Identifier id : phases.keySet()) firstRegistrations.put(id, Long.MAX_VALUE);
        for (ListenerEntry<T> entry : listeners) {
            long current = firstRegistrations.get(entry.phase).longValue();
            if (entry.sequence < current) firstRegistrations.put(entry.phase, entry.sequence);
        }

        Map<Identifier, Integer> indegrees = new HashMap<Identifier, Integer>();
        for (Identifier id : phases.keySet()) indegrees.put(id, Integer.valueOf(0));
        for (PhaseNode phase : phases.values()) {
            for (Identifier after : phase.after) {
                indegrees.put(after, Integer.valueOf(indegrees.get(after).intValue() + 1));
            }
        }
        Comparator<Identifier> availableOrder = Comparator
                .comparingLong((Identifier id) -> firstRegistrations.get(id).longValue())
                .thenComparing(Comparator.naturalOrder());
        PriorityQueue<Identifier> available = new PriorityQueue<Identifier>(availableOrder);
        for (Map.Entry<Identifier, Integer> entry : indegrees.entrySet()) {
            if (entry.getValue().intValue() == 0) available.add(entry.getKey());
        }

        ArrayList<Identifier> result = new ArrayList<Identifier>(phases.size());
        while (!available.isEmpty()) {
            Identifier id = available.remove();
            result.add(id);
            for (Identifier after : phases.get(id).after) {
                int remaining = indegrees.get(after).intValue() - 1;
                indegrees.put(after, Integer.valueOf(remaining));
                if (remaining == 0) available.add(after);
            }
        }
        if (result.size() != phases.size()) {
            ArrayList<Identifier> cycle = new ArrayList<Identifier>();
            for (Map.Entry<Identifier, Integer> entry : indegrees.entrySet()) {
                if (entry.getValue().intValue() > 0) cycle.add(entry.getKey());
            }
            Collections.sort(cycle);
            throw new IllegalArgumentException("Event phase ordering contains a cycle: " + cycle);
        }
        return result;
    }

    public interface Registration extends AutoCloseable {
        boolean unregister();

        @Override
        void close();
    }

    public interface PhasedRegistration extends Registration {
        Identifier phase();
    }

    private final class ListenerRegistration implements PhasedRegistration {
        private final ListenerEntry<T> entry;
        private boolean active = true;

        ListenerRegistration(ListenerEntry<T> entry) {
            this.entry = entry;
        }

        @Override
        public Identifier phase() { return entry.phase; }

        @Override
        public synchronized boolean unregister() {
            if (!active) return false;
            boolean removed = removeListener(entry);
            active = false;
            return removed;
        }

        @Override
        public void close() { unregister(); }
    }

    private static final class ListenerEntry<T> {
        final Identifier phase;
        final T listener;
        final long sequence;

        ListenerEntry(Identifier phase, T listener, long sequence) {
            this.phase = phase;
            this.listener = listener;
            this.sequence = sequence;
        }
    }

    private static final class PhaseNode {
        final Identifier id;
        final Set<Identifier> after = new LinkedHashSet<Identifier>();

        PhaseNode(Identifier id) { this.id = id; }
    }
}
