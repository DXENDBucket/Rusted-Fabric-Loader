package io.github.endx.vulkanmod;

public final class NativeFrameClockVerification {
    private NativeFrameClockVerification() { }

    public static void main(String[] args) {
        NativeFrameClock clock = new NativeFrameClock();
        require(clock.nextDeltaMillis(1_000_000_000L) == 16, "first-frame default");

        int advanced = 0;
        for (int frame = 1; frame <= 10; frame++) {
            advanced += clock.nextDeltaMillis(1_000_000_000L + frame * 100_000L);
        }
        require(advanced == 1, "sub-millisecond frames accumulated without acceleration");

        clock.reset(2_000_000_000L);
        require(clock.nextDeltaMillis(2_016_666_666L) == 16, "fractional frame floor");
        require(clock.nextDeltaMillis(2_033_333_333L) == 17, "fractional remainder carry");
        require(clock.nextDeltaMillis(3_000_000_000L) == 250, "long-frame cap");

        clock.clear();
        require(clock.nextDeltaMillis(5_000_000_000L) == 16, "clear resets first frame");
        System.out.println("Native Vulkan frame clock contracts passed");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
