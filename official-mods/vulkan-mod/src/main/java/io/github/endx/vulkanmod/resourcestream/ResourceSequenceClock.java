package io.github.endx.vulkanmod.resourcestream;

/** Allocates contiguous reliable record sequences and tracks decoder progress without gaps. */
public final class ResourceSequenceClock {
    private long submittedThrough;
    private long appliedThrough;

    public synchronized Reservation reserve(int recordCount) {
        if (recordCount <= 0 || recordCount > ResourceStreamFormat.MAX_RECORDS) {
            throw new IllegalArgumentException("resource reservation count is out of range");
        }
        long first;
        long last;
        try {
            first = Math.addExact(submittedThrough, 1L);
            last = Math.addExact(submittedThrough, (long) recordCount);
        } catch (ArithmeticException overflow) {
            throw new IllegalStateException("resource sequence exhausted", overflow);
        }
        submittedThrough = last;
        return new Reservation(first, last, recordCount);
    }

    public synchronized void markApplied(Reservation reservation) {
        if (reservation == null) throw new NullPointerException("reservation");
        if (reservation.first != appliedThrough + 1L
                || reservation.last > submittedThrough) {
            throw new IllegalStateException("resource completion is out of order");
        }
        appliedThrough = reservation.last;
    }

    /** Dependency placed in every frame encoded after all currently submitted resource work. */
    public synchronized long requiredForNextFrame() { return submittedThrough; }
    public synchronized long appliedThrough() { return appliedThrough; }

    public static final class Reservation {
        public final long first, last;
        public final int count;
        private Reservation(long first, long last, int count) {
            this.first = first; this.last = last; this.count = count;
        }
    }
}
