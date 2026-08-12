package io.github.endx.vulkanmod.lwjgl3;

import io.github.endx.vulkanmod.spi.VulkanClipRect;
import io.github.endx.vulkanmod.spi.VulkanShaderState;
import org.lwjgl.vulkan.VkCommandBuffer;
import org.lwjgl.vulkan.VkRect2D;

import java.nio.LongBuffer;
import java.util.Map;

import static org.lwjgl.vulkan.VK10.*;

/** Pass-local Vulkan state cache shared by presentation and offscreen command recording. */
final class VulkanCommandStateCache {
    private long pipeline;
    private long vertexBuffer;
    private long vertexOffset;
    private long indexBuffer;
    private long indexOffset;
    private int indexType;
    private long textureDescriptor;
    private VulkanShaderState shaderState;
    private boolean scissorSet;
    private int scissorX;
    private int scissorY;
    private int scissorWidth;
    private int scissorHeight;

    private long descriptorBindCalls;
    private long descriptorBindSkips;
    private long pipelineBindCalls;
    private long pipelineBindSkips;
    private long vertexBindCalls;
    private long vertexBindSkips;
    private long indexBindCalls;
    private long indexBindSkips;
    private long scissorCalls;
    private long scissorSkips;
    private long shaderPushCalls;
    private long shaderPushSkips;

    void reset() {
        pipeline = VK_NULL_HANDLE;
        vertexBuffer = VK_NULL_HANDLE;
        vertexOffset = Long.MIN_VALUE;
        indexBuffer = VK_NULL_HANDLE;
        indexOffset = Long.MIN_VALUE;
        indexType = -1;
        textureDescriptor = VK_NULL_HANDLE;
        shaderState = null;
        scissorSet = false;
    }

    void bindPipeline(VkCommandBuffer command, long nextPipeline) {
        if (pipeline == nextPipeline) {
            pipelineBindSkips++;
            return;
        }
        vkCmdBindPipeline(command, VK_PIPELINE_BIND_POINT_GRAPHICS, nextPipeline);
        pipeline = nextPipeline;
        pipelineBindCalls++;
    }

    void bindVertexBuffer(VkCommandBuffer command, long buffer, long offset,
                          LongBuffer bufferPointer, LongBuffer offsetPointer) {
        if (vertexBuffer == buffer && vertexOffset == offset) {
            vertexBindSkips++;
            return;
        }
        bufferPointer.put(0, buffer);
        offsetPointer.put(0, offset);
        vkCmdBindVertexBuffers(command, 0, bufferPointer, offsetPointer);
        vertexBuffer = buffer;
        vertexOffset = offset;
        vertexBindCalls++;
    }

    void bindDescriptor(VkCommandBuffer command, long pipelineLayout,
                        long descriptorSet, LongBuffer descriptorPointer) {
        if (textureDescriptor == descriptorSet) {
            descriptorBindSkips++;
            return;
        }
        descriptorPointer.put(0, descriptorSet);
        vkCmdBindDescriptorSets(command, VK_PIPELINE_BIND_POINT_GRAPHICS,
                pipelineLayout, 0, descriptorPointer, null);
        textureDescriptor = descriptorSet;
        descriptorBindCalls++;
    }

    void bindIndexBuffer(VkCommandBuffer command, long buffer, long offset, int type) {
        if (indexBuffer == buffer && indexOffset == offset && indexType == type) {
            indexBindSkips++;
            return;
        }
        vkCmdBindIndexBuffer(command, buffer, offset, type);
        indexBuffer = buffer;
        indexOffset = offset;
        indexType = type;
        indexBindCalls++;
    }

    boolean setScissor(VkCommandBuffer command, VulkanClipRect clip,
                       int targetWidth, int targetHeight, VkRect2D.Buffer output) {
        int left = clip == null ? 0 : Math.max(0, (int) Math.floor(clip.x()));
        int top = clip == null ? 0 : Math.max(0, (int) Math.floor(clip.y()));
        int right = clip == null ? targetWidth
                : Math.min(targetWidth, (int) Math.ceil(clip.x() + clip.width()));
        int bottom = clip == null ? targetHeight
                : Math.min(targetHeight, (int) Math.ceil(clip.y() + clip.height()));
        if (right <= left || bottom <= top) return false;
        int width = right - left;
        int height = bottom - top;
        if (scissorSet && scissorX == left && scissorY == top
                && scissorWidth == width && scissorHeight == height) {
            scissorSkips++;
            return true;
        }
        output.get(0).offset().x(left).y(top);
        output.get(0).extent().width(width).height(height);
        vkCmdSetScissor(command, 0, output);
        scissorSet = true;
        scissorX = left;
        scissorY = top;
        scissorWidth = width;
        scissorHeight = height;
        scissorCalls++;
        return true;
    }

    boolean shouldPushShaderState(VulkanShaderState nextState) {
        if (nextState.equals(shaderState)) {
            shaderPushSkips++;
            return false;
        }
        shaderState = nextState;
        shaderPushCalls++;
        return true;
    }

    void appendStatistics(Map<String, Long> statistics) {
        statistics.put("descriptor.bindCalls", descriptorBindCalls);
        statistics.put("descriptor.bindSkips", descriptorBindSkips);
        statistics.put("command.pipelineBindCalls", pipelineBindCalls);
        statistics.put("command.pipelineBindSkips", pipelineBindSkips);
        statistics.put("command.vertexBindCalls", vertexBindCalls);
        statistics.put("command.vertexBindSkips", vertexBindSkips);
        statistics.put("command.indexBindCalls", indexBindCalls);
        statistics.put("command.indexBindSkips", indexBindSkips);
        statistics.put("command.scissorSetCalls", scissorCalls);
        statistics.put("command.scissorSetSkips", scissorSkips);
        statistics.put("command.shaderPushCalls", shaderPushCalls);
        statistics.put("command.shaderPushSkips", shaderPushSkips);
    }
}
