package io.github.endx.vulkanmod.framestream;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayDeque;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Fixed, registered-once direct-memory arenas for the FrameStream producer/decoder boundary.
 *
 * <p>Version 1 deliberately blocks the producer when all arenas are owned. A decoder returns an
 * arena as soon as it has validated and copied/encoded the stream; GPU completion is not part of
 * this ownership protocol.</p>
 */
public final class FrameStreamArenaPool {
    public static final int DEFAULT_ARENA_COUNT = 3;

    private static final int FREE = 0;
    private static final int WRITING = 1;
    private static final int SUBMITTED = 2;
    private static final int DECODING = 3;

    private final ReentrantLock lock = new ReentrantLock();
    private final Condition freeAvailable = lock.newCondition();
    private final Condition submissionAvailable = lock.newCondition();
    private final Arena[] arenas;
    private final ArrayDeque<Arena> submitted;
    private long lastPublishedFrameId = -1L;

    public FrameStreamArenaPool(int arenaBytes) {
        this(DEFAULT_ARENA_COUNT, arenaBytes);
    }

    FrameStreamArenaPool(int arenaCount, int arenaBytes) {
        if (arenaCount <= 0 || arenaCount > 64) {
            throw new IllegalArgumentException("arena count must be in [1,64]");
        }
        if (arenaBytes < FrameStreamFormat.FIXED_HEADER_BYTES
                || arenaBytes > FrameStreamFormat.MAX_STREAM_BYTES) {
            throw new IllegalArgumentException("arena capacity is outside FrameStream limits");
        }
        arenas = new Arena[arenaCount];
        submitted = new ArrayDeque<Arena>(arenaCount);
        for (int index = 0; index < arenaCount; index++) {
            arenas[index] = new Arena(index, ByteBuffer.allocateDirect(arenaBytes)
                    .order(ByteOrder.LITTLE_ENDIAN));
        }
    }

    /** Blocks with bounded memory until an arena is free for the Java encoder. */
    public WriteLease acquireWriter() throws InterruptedException {
        lock.lockInterruptibly();
        try {
            for (;;) {
                for (Arena arena : arenas) {
                    if (arena.state != FREE) continue;
                    arena.state = WRITING;
                    arena.generation++;
                    arena.usedBytes = 0;
                    arena.frameId = -1L;
                    ByteBuffer writable = arena.memory.duplicate().order(ByteOrder.LITTLE_ENDIAN);
                    writable.clear();
                    return new WriteLease(this, arena, arena.generation, writable);
                }
                freeAvailable.await();
            }
        } finally {
            lock.unlock();
        }
    }

    /** Blocks until the next submitted frame is ready for native decoding. */
    public DecodeLease acquireDecoder() throws InterruptedException {
        lock.lockInterruptibly();
        try {
            while (submitted.isEmpty()) submissionAvailable.await();
            Arena arena = submitted.removeFirst();
            requireState(arena, SUBMITTED, arena.generation);
            arena.state = DECODING;
            ByteBuffer view = readOnlyFrame(arena);
            return new DecodeLease(this, arena, arena.generation, view,
                    arena.usedBytes, arena.frameId);
        } finally {
            lock.unlock();
        }
    }

    /** Stable direct buffers for one-time JNI registration. Do not mutate them directly. */
    public ByteBuffer registeredArena(int index) {
        if (index < 0 || index >= arenas.length) throw new IndexOutOfBoundsException(index);
        ByteBuffer view = arenas[index].memory.asReadOnlyBuffer().order(ByteOrder.LITTLE_ENDIAN);
        view.clear();
        return view;
    }

    public int arenaCount() { return arenas.length; }
    public int arenaCapacity() { return arenas[0].memory.capacity(); }

    private void publish(WriteLease lease) {
        int usedBytes = lease.writable.position();
        if (usedBytes <= 0) throw new IllegalStateException("cannot publish an empty arena");
        if (usedBytes > lease.arena.memory.capacity()) {
            throw new IllegalStateException("published length exceeds arena capacity");
        }
        ByteBuffer candidate = lease.arena.memory.asReadOnlyBuffer()
                .order(ByteOrder.LITTLE_ENDIAN);
        candidate.position(0).limit(usedBytes);
        FrameStreamReader reader = FrameStreamReader.read(candidate);

        lock.lock();
        try {
            requireState(lease.arena, WRITING, lease.generation);
            if (reader.frameId() <= lastPublishedFrameId) {
                throw new IllegalStateException("frame IDs must increase: " + reader.frameId()
                        + " <= " + lastPublishedFrameId);
            }
            lease.arena.usedBytes = usedBytes;
            lease.arena.frameId = reader.frameId();
            lease.arena.state = SUBMITTED;
            lastPublishedFrameId = reader.frameId();
            submitted.addLast(lease.arena);
            lease.closed = true;
            submissionAvailable.signal();
        } finally {
            lock.unlock();
        }
    }

    private void abort(WriteLease lease) {
        lock.lock();
        try {
            if (lease.closed) return;
            requireState(lease.arena, WRITING, lease.generation);
            lease.closed = true;
            releaseArena(lease.arena);
        } finally {
            lock.unlock();
        }
    }

    private void finishDecode(DecodeLease lease) {
        lock.lock();
        try {
            if (lease.closed) return;
            requireState(lease.arena, DECODING, lease.generation);
            lease.closed = true;
            releaseArena(lease.arena);
        } finally {
            lock.unlock();
        }
    }

    private void releaseArena(Arena arena) {
        arena.state = FREE;
        arena.usedBytes = 0;
        arena.frameId = -1L;
        freeAvailable.signal();
    }

    private static ByteBuffer readOnlyFrame(Arena arena) {
        ByteBuffer view = arena.memory.asReadOnlyBuffer().order(ByteOrder.LITTLE_ENDIAN);
        view.position(0).limit(arena.usedBytes);
        return view;
    }

    private static void requireState(Arena arena, int expected, long generation) {
        if (arena.generation != generation || arena.state != expected) {
            throw new IllegalStateException("stale or invalid FrameStream arena lease");
        }
    }

    public static final class WriteLease implements AutoCloseable {
        private final FrameStreamArenaPool pool;
        private final Arena arena;
        private final long generation;
        private final ByteBuffer writable;
        private boolean closed;

        private WriteLease(FrameStreamArenaPool pool, Arena arena, long generation,
                           ByteBuffer writable) {
            this.pool = pool;
            this.arena = arena;
            this.generation = generation;
            this.writable = writable;
        }

        public int arenaIndex() { return arena.index; }

        public ByteBuffer buffer() {
            ensureOpen();
            return writable;
        }

        /** Validates the encoded bytes and transfers this arena to the decoder queue. */
        public void publish() {
            ensureOpen();
            pool.publish(this);
        }

        /** Aborts an unpublished write and immediately returns the arena. */
        @Override public void close() {
            pool.abort(this);
        }

        private void ensureOpen() {
            if (closed) throw new IllegalStateException("FrameStream write lease is closed");
        }
    }

    public static final class DecodeLease implements AutoCloseable {
        private final FrameStreamArenaPool pool;
        private final Arena arena;
        private final long generation;
        private final ByteBuffer frame;
        private final int usedBytes;
        private final long frameId;
        private boolean closed;

        private DecodeLease(FrameStreamArenaPool pool, Arena arena, long generation,
                            ByteBuffer frame, int usedBytes, long frameId) {
            this.pool = pool;
            this.arena = arena;
            this.generation = generation;
            this.frame = frame;
            this.usedBytes = usedBytes;
            this.frameId = frameId;
        }

        public int arenaIndex() { return arena.index; }
        public int usedBytes() { return usedBytes; }
        public long frameId() { return frameId; }

        public ByteBuffer buffer() {
            if (closed) throw new IllegalStateException("FrameStream decode lease is closed");
            ByteBuffer view = frame.duplicate().order(ByteOrder.LITTLE_ENDIAN);
            view.position(0).limit(usedBytes);
            return view;
        }

        /** Call after native has decoded/copied the bytes, independently of GPU completion. */
        @Override public void close() {
            pool.finishDecode(this);
        }
    }

    private static final class Arena {
        private final int index;
        private final ByteBuffer memory;
        private int state;
        private long generation;
        private int usedBytes;
        private long frameId = -1L;

        private Arena(int index, ByteBuffer memory) {
            this.index = index;
            this.memory = memory;
        }
    }
}
