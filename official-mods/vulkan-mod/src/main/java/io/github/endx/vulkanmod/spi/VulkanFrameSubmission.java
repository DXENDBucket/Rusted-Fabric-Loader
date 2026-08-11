package io.github.endx.vulkanmod.spi;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Platform-neutral frame graph: ordered offscreen writes followed by one presentation pass.
 *
 * <p>The list order is an execution dependency. A pass may sample targets written by any earlier
 * pass, and the presentation pass may sample every target in the list. Drivers may encode the
 * graph into one command buffer/queue submission when their platform backend supports it.</p>
 */
public final class VulkanFrameSubmission {
    private final List<VulkanRenderTargetPass> renderTargetPasses;
    private final VulkanFrameCommands presentationFrame;

    public VulkanFrameSubmission(List<VulkanRenderTargetPass> renderTargetPasses,
                                 VulkanFrameCommands presentationFrame) {
        if (renderTargetPasses == null) throw new NullPointerException("renderTargetPasses");
        if (presentationFrame == null) throw new NullPointerException("presentationFrame");
        this.renderTargetPasses = Collections.unmodifiableList(
                new ArrayList<VulkanRenderTargetPass>(renderTargetPasses));
        this.presentationFrame = presentationFrame;
    }

    public List<VulkanRenderTargetPass> renderTargetPasses() { return renderTargetPasses; }
    public VulkanFrameCommands presentationFrame() { return presentationFrame; }
}
