package io.github.endx.vulkanmod.framestream;

/** Converts renderer resource identities to typed, generation-checked FrameStream handles. */
public interface FrameStreamResourceMapper {
    long texture(long rendererHandle);
    long shaderProgram(long rendererHandle);

    /** Transitional desktop mapping used until ResourceStream owns both handle tables. */
    static FrameStreamResourceMapper generationOneSlots() {
        return GenerationOneSlots.INSTANCE;
    }

    final class GenerationOneSlots implements FrameStreamResourceMapper {
        private static final GenerationOneSlots INSTANCE = new GenerationOneSlots();

        private GenerationOneSlots() { }

        @Override public long texture(long rendererHandle) {
            return map(rendererHandle, FrameResourceHandle.TYPE_TEXTURE);
        }

        @Override public long shaderProgram(long rendererHandle) {
            return map(rendererHandle, FrameResourceHandle.TYPE_SHADER_PROGRAM);
        }

        private static long map(long rendererHandle, int type) {
            if (rendererHandle <= 0L || rendererHandle > 0xffffffffL) {
                throw new IllegalArgumentException("renderer handle does not fit a FrameStream slot: "
                        + rendererHandle);
            }
            return FrameResourceHandle.encode(type, 1, rendererHandle);
        }
    }
}
