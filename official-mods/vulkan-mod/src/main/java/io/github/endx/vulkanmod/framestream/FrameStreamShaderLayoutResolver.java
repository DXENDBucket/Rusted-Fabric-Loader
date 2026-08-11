package io.github.endx.vulkanmod.framestream;

/** Supplies resource metadata that is reliable but intentionally absent from per-draw state. */
public interface FrameStreamShaderLayoutResolver {
    boolean usesExpandedVertexInput(long rendererShaderHandle);

    FrameStreamShaderLayoutResolver NO_CUSTOM_SHADERS = new FrameStreamShaderLayoutResolver() {
        @Override public boolean usesExpandedVertexInput(long rendererShaderHandle) {
            throw new IllegalArgumentException("custom shader metadata is unavailable for handle "
                    + rendererShaderHandle);
        }
    };
}
