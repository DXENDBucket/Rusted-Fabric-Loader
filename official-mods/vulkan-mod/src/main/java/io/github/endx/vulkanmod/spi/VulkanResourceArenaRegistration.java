package io.github.endx.vulkanmod.spi;

/** Stable platform registration for one externally referenced resource-upload arena. */
public final class VulkanResourceArenaRegistration {
    private final long arenaId;
    private final int capacity;
    private final long nativeAddress;

    public VulkanResourceArenaRegistration(long arenaId, int capacity, long nativeAddress) {
        if (arenaId <= 0L) throw new IllegalArgumentException("arena ID must be positive");
        if (capacity <= 0) throw new IllegalArgumentException("arena capacity must be positive");
        if (nativeAddress < 0L) throw new IllegalArgumentException("native address is negative");
        this.arenaId = arenaId;
        this.capacity = capacity;
        this.nativeAddress = nativeAddress;
    }

    public long arenaId() { return arenaId; }
    public int capacity() { return capacity; }

    /**
     * Stable base address while this registration remains active, or zero when the platform
     * intentionally keeps its address private. Java callers must never dereference this value.
     */
    public long nativeAddress() { return nativeAddress; }
    public boolean hasNativeAddress() { return nativeAddress != 0L; }

    public long nativeAddressAt(long offset, int bytes) {
        if (!hasNativeAddress()) throw new IllegalStateException("native arena address unavailable");
        long end;
        try { end = Math.addExact(offset, (long) bytes); }
        catch (ArithmeticException overflow) {
            throw new IllegalArgumentException("arena range overflows", overflow);
        }
        if (offset < 0L || bytes < 0 || end > capacity) {
            throw new IllegalArgumentException("arena range is outside registration");
        }
        try { return Math.addExact(nativeAddress, offset); }
        catch (ArithmeticException overflow) {
            throw new IllegalStateException("native arena address overflows", overflow);
        }
    }
}
