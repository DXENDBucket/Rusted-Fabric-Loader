package io.github.endx.vulkanmod.resourcestream;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/** Bounded registered direct-memory arenas for large reliable resource payloads. */
public final class ResourceUploadArenaPool implements AutoCloseable {
    public interface Registry {
        void register(long arenaId, ByteBuffer memory);
        void unregister(long arenaId);
    }

    public static final int DEFAULT_ARENA_COUNT = 3;
    public static final int DEFAULT_ARENA_BYTES = 16 * 1024 * 1024;
    public static final int MAX_ARENA_BYTES = 256 * 1024 * 1024;

    private final Registry registry;
    private final int arenaCount;
    private Arena[] arenas;
    private int arenaBytes;
    private long nextArenaId = 1L;
    private boolean closed;

    public ResourceUploadArenaPool(Registry registry) {
        this(registry, DEFAULT_ARENA_COUNT, DEFAULT_ARENA_BYTES);
    }

    public ResourceUploadArenaPool(Registry registry, int arenaCount, int arenaBytes) {
        if (registry == null) throw new NullPointerException("registry");
        if (arenaCount <= 0 || arenaCount > 16) {
            throw new IllegalArgumentException("resource arena count is out of range");
        }
        validateCapacity(arenaBytes);
        this.registry = registry;
        this.arenaCount = arenaCount;
        this.arenaBytes = arenaBytes;
        this.arenas = allocateSet(arenaBytes);
    }

    public synchronized Lease acquire(int requiredBytes) throws InterruptedException {
        requireOpen();
        if (requiredBytes <= 0 || requiredBytes > MAX_ARENA_BYTES) {
            throw new IllegalArgumentException("resource payload is outside arena limits");
        }
        ensureCapacity(requiredBytes);
        for (;;) {
            for (Arena arena : arenas) {
                if (!arena.owned) {
                    arena.owned = true;
                    arena.generation++;
                    ByteBuffer writable = arena.memory.duplicate().order(ByteOrder.LITTLE_ENDIAN);
                    writable.clear().limit(requiredBytes);
                    return new Lease(this, arena, arena.generation, writable);
                }
            }
            wait();
            requireOpen();
        }
    }

    public synchronized int arenaCapacity() { return arenaBytes; }
    public int arenaCount() { return arenaCount; }

    private void ensureCapacity(int requiredBytes) {
        if (requiredBytes <= arenaBytes) return;
        for (Arena arena : arenas) {
            if (arena.owned) throw new IllegalStateException(
                    "cannot grow resource arenas while a lease is active");
        }
        int grown = arenaBytes;
        while (grown < requiredBytes && grown < MAX_ARENA_BYTES) {
            grown = (int) Math.min((long) grown * 2L, MAX_ARENA_BYTES);
        }
        if (grown < requiredBytes) throw new IllegalArgumentException(
                "resource payload exceeds maximum arena capacity");
        Arena[] replacement = allocateSet(grown);
        Arena[] previous = arenas;
        arenas = replacement;
        arenaBytes = grown;
        for (Arena arena : previous) registry.unregister(arena.id);
    }

    private Arena[] allocateSet(int capacity) {
        Arena[] result = new Arena[arenaCount];
        int registered = 0;
        try {
            for (int index = 0; index < result.length; index++) {
                if (nextArenaId == Long.MAX_VALUE) {
                    throw new IllegalStateException("resource arena IDs exhausted");
                }
                long id = nextArenaId++;
                ByteBuffer memory = ByteBuffer.allocateDirect(capacity)
                        .order(ByteOrder.LITTLE_ENDIAN);
                registry.register(id, memory.asReadOnlyBuffer().order(ByteOrder.LITTLE_ENDIAN));
                result[index] = new Arena(id, memory);
                registered++;
            }
            return result;
        } catch (RuntimeException | Error failure) {
            for (int index = 0; index < registered; index++) {
                try { registry.unregister(result[index].id); }
                catch (RuntimeException ignored) { failure.addSuppressed(ignored); }
            }
            throw failure;
        }
    }

    private synchronized void release(Arena arena, long generation) {
        if (!arena.owned || arena.generation != generation) {
            throw new IllegalStateException("stale resource arena lease");
        }
        arena.owned = false;
        notifyAll();
    }

    private void requireOpen() {
        if (closed) throw new IllegalStateException("resource arena pool is closed");
    }

    private static void validateCapacity(int capacity) {
        if (capacity < 1024 || capacity > MAX_ARENA_BYTES) {
            throw new IllegalArgumentException("resource arena capacity is out of range");
        }
    }

    @Override public synchronized void close() {
        if (closed) return;
        for (Arena arena : arenas) {
            if (arena.owned) throw new IllegalStateException(
                    "cannot close resource arenas while a lease is active");
        }
        closed = true;
        for (Arena arena : arenas) registry.unregister(arena.id);
        notifyAll();
    }

    private static final class Arena {
        private final long id;
        private final ByteBuffer memory;
        private long generation;
        private boolean owned;
        private Arena(long id, ByteBuffer memory) { this.id = id; this.memory = memory; }
    }

    public static final class Lease implements AutoCloseable {
        private final ResourceUploadArenaPool pool;
        private final Arena arena;
        private final long generation;
        private final ByteBuffer buffer;
        private boolean closed;

        private Lease(ResourceUploadArenaPool pool, Arena arena, long generation,
                      ByteBuffer buffer) {
            this.pool = pool; this.arena = arena; this.generation = generation;
            this.buffer = buffer;
        }

        public long arenaId() { requireOpen(); return arena.id; }
        public ByteBuffer buffer() { requireOpen(); return buffer; }

        private void requireOpen() {
            if (closed) throw new IllegalStateException("resource arena lease is closed");
        }

        @Override public void close() {
            if (closed) return;
            closed = true;
            pool.release(arena, generation);
        }
    }
}
