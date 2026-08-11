package io.github.endx.vulkanmod.framestream;

/** A structurally invalid or unsupported FrameStream envelope. */
public final class FrameStreamFormatException extends IllegalArgumentException {
    public FrameStreamFormatException(String message) {
        super(message);
    }
}
