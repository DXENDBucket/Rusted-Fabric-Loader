package io.github.endx.vulkanmod.framestream;

/** Converts renderer resource identities to typed, generation-checked FrameStream handles. */
public interface FrameStreamResourceMapper {
    long texture(long rendererHandle);
    long shaderProgram(long rendererHandle);

    /** Transitional desktop mapping used until ResourceStream owns both handle tables. */
    static FrameStreamResourceMapper generationOneSlots() {
        return GenerationOneSlots.INSTANCE;
    }

    /** ResourceStream already exposes fully typed generation-checked handles. */
    static FrameStreamResourceMapper typedHandles() {
        return TypedHandles.INSTANCE;
    }

    final class TypedHandles implements FrameStreamResourceMapper {
        private static final TypedHandles INSTANCE = new TypedHandles();
        private TypedHandles() { }

        @Override public long texture(long handle) {
            FrameResourceHandle.requireType(handle, FrameResourceHandle.TYPE_TEXTURE);
            return handle;
        }

        @Override public long shaderProgram(long handle) {
            FrameResourceHandle.requireType(handle, FrameResourceHandle.TYPE_SHADER_PROGRAM);
            return handle;
        }
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
