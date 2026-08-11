package io.github.endx.vulkanmod.resourcestream;

import io.github.endx.vulkanmod.framestream.FrameResourceHandle;

import java.util.ArrayDeque;
import java.util.ArrayList;

/**
 * Shared logical handle allocator with generation and sequence-aware lifetime checks.
 * Physical backend destruction remains deferred until {@link #releaseRetired(long)}.
 */
public final class ResourceHandleTable<T> {
    private final int type;
    private final ArrayList<Entry<T>> slots = new ArrayList<Entry<T>>();
    private final ArrayDeque<Integer> reusable = new ArrayDeque<Integer>();
    private int allocated;

    public ResourceHandleTable(int type) {
        if (type <= 0 || type > FrameResourceHandle.MAX_TYPE) {
            throw new IllegalArgumentException("resource handle type is out of range");
        }
        this.type = type;
        slots.add(null); // Slot zero stays invalid and matches the transitional desktop table.
    }

    public synchronized long reserve(T metadata, long createSequence) {
        if (metadata == null) throw new NullPointerException("metadata");
        if (createSequence < 0L) throw new IllegalArgumentException("negative create sequence");
        int slot;
        Entry<T> entry;
        if (!reusable.isEmpty()) {
            slot = reusable.removeFirst();
            entry = slots.get(slot);
            if (entry.occupied || entry.generation >= FrameResourceHandle.MAX_GENERATION) {
                throw new IllegalStateException("corrupt reusable resource slot");
            }
            entry.generation++;
        } else {
            if (slots.size() == Integer.MAX_VALUE) {
                throw new IllegalStateException("resource handle slots exhausted");
            }
            slot = slots.size();
            entry = new Entry<T>();
            entry.generation = 1;
            slots.add(entry);
        }
        entry.metadata = metadata;
        entry.createSequence = createSequence;
        entry.destroySequence = Long.MAX_VALUE;
        entry.occupied = true;
        allocated++;
        return FrameResourceHandle.encode(type, entry.generation, slot);
    }

    /** Returns metadata only when this exact generation was alive at the frame dependency. */
    public synchronized T requireVisible(long handle, long requiredResourceSequence) {
        if (requiredResourceSequence < 0L) {
            throw new IllegalArgumentException("negative required resource sequence");
        }
        Entry<T> entry = requireEntry(handle);
        if (requiredResourceSequence < entry.createSequence) {
            throw new IllegalArgumentException("resource handle is newer than the frame");
        }
        if (requiredResourceSequence >= entry.destroySequence) {
            throw new IllegalArgumentException("resource handle was destroyed before the frame");
        }
        return entry.metadata;
    }

    /** Logically destroys a handle while retaining its physical metadata for older GPU work. */
    public synchronized T retire(long handle, long destroySequence) {
        Entry<T> entry = requireEntry(handle);
        if (entry.destroySequence != Long.MAX_VALUE) {
            throw new IllegalStateException("resource handle is already retired");
        }
        if (destroySequence <= entry.createSequence) {
            throw new IllegalArgumentException("destroy sequence does not follow create sequence");
        }
        entry.destroySequence = destroySequence;
        return entry.metadata;
    }

    /**
     * Releases a retired slot after the backend proves no queued/GPU frame can reference it.
     * The next reservation increments its generation; exhausted generations are never reused.
     */
    public synchronized void releaseRetired(long handle) {
        Entry<T> entry = requireEntry(handle);
        if (entry.destroySequence == Long.MAX_VALUE) {
            throw new IllegalStateException("live resource handle cannot be released");
        }
        int slot = checkedSlot(handle);
        entry.metadata = null;
        entry.createSequence = 0L;
        entry.destroySequence = 0L;
        entry.occupied = false;
        allocated--;
        if (entry.generation < FrameResourceHandle.MAX_GENERATION) reusable.addLast(slot);
    }

    public synchronized int allocatedCount() { return allocated; }
    public int type() { return type; }

    private Entry<T> requireEntry(long handle) {
        if (FrameResourceHandle.type(handle) != type
                || FrameResourceHandle.generation(handle) == 0) {
            throw new IllegalArgumentException("wrong or invalid resource handle type");
        }
        int slot = checkedSlot(handle);
        Entry<T> entry = slots.get(slot);
        if (entry == null || !entry.occupied
                || entry.generation != FrameResourceHandle.generation(handle)) {
            throw new IllegalArgumentException("stale or unknown resource handle");
        }
        return entry;
    }

    private int checkedSlot(long handle) {
        long slot = FrameResourceHandle.slot(handle);
        if (slot <= 0L || slot >= slots.size()) {
            throw new IllegalArgumentException("resource handle slot is out of range");
        }
        return (int) slot;
    }

    private static final class Entry<T> {
        private int generation;
        private long createSequence;
        private long destroySequence;
        private boolean occupied;
        private T metadata;
    }
}
