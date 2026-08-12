package io.github.endx.vulkanmod.resourcestream;

import java.nio.ByteBuffer;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/** Ownership, back-pressure, registration, and bounded-growth contracts for upload arenas. */
public final class ResourceUploadArenaVerification {
    private ResourceUploadArenaVerification() { }

    public static void main(String[] arguments) throws Exception {
        Map<Long, ByteBuffer> registered = new LinkedHashMap<Long, ByteBuffer>();
        ResourceUploadArenaPool pool = new ResourceUploadArenaPool(
                new ResourceUploadArenaPool.Registry() {
                    @Override public synchronized void register(long id, ByteBuffer memory) {
                        require(memory.isDirect(), "registered upload arena is not direct");
                        require(registered.put(id, memory) == null,
                                "duplicate upload arena registration");
                    }
                    @Override public synchronized void unregister(long id) {
                        require(registered.remove(id) != null,
                                "unknown upload arena unregistration");
                    }
                }, 2, 1024);
        require(registered.size() == 2, "initial upload arenas were not registered");
        ResourceUploadArenaPool.Lease first = pool.acquire(16);
        ResourceUploadArenaPool.Lease second = pool.acquire(16);
        first.buffer().putInt(0, 0x12345678);
        require(registered.get(first.arenaId()).getInt(0) == 0x12345678,
                "registry does not observe arena writes");

        AtomicReference<ResourceUploadArenaPool.Lease> third =
                new AtomicReference<ResourceUploadArenaPool.Lease>();
        AtomicReference<Throwable> failure = new AtomicReference<Throwable>();
        Thread waiter = new Thread(() -> {
            try { third.set(pool.acquire(16)); }
            catch (Throwable problem) { failure.set(problem); }
        }, "Resource arena back-pressure verification");
        waiter.start();
        Thread.sleep(40L);
        require(waiter.isAlive() && third.get() == null,
                "resource arena pool did not apply back-pressure");
        first.close();
        waiter.join(2_000L);
        require(!waiter.isAlive() && failure.get() == null && third.get() != null,
                "blocked resource arena writer did not resume");
        second.close();
        third.get().close();

        ResourceUploadArenaPool.Lease grown = pool.acquire(1500);
        require(pool.arenaCapacity() == 2048 && registered.size() == 2,
                "resource arena set did not grow geometrically and atomically");
        grown.close();
        pool.close();
        require(registered.isEmpty(), "resource arenas remained registered after close");
        System.out.println("RustedVK resource upload arena contracts passed");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
