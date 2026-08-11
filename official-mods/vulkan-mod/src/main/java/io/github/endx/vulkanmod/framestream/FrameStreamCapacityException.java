package io.github.endx.vulkanmod.framestream;

/** A valid encoded frame does not fit the currently registered fixed arena set. */
public final class FrameStreamCapacityException extends IllegalArgumentException {
    private final int requiredBytes;
    private final int availableBytes;

    public FrameStreamCapacityException(int requiredBytes, int availableBytes) {
        super("FrameStream needs " + requiredBytes + " bytes but the arena has "
                + availableBytes);
        this.requiredBytes = requiredBytes;
        this.availableBytes = availableBytes;
    }

    public int requiredBytes() { return requiredBytes; }
    public int availableBytes() { return availableBytes; }
}
