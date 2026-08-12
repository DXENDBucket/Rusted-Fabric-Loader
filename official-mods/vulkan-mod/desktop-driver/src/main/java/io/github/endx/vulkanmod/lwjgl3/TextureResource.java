package io.github.endx.vulkanmod.lwjgl3;

import io.github.endx.vulkanmod.spi.VulkanTextureFilter;

import java.util.ArrayList;
import java.util.List;

/** Raw Vulkan image ownership plus the session metadata attached to one public texture handle. */
final class TextureResource {
    long image;
    long memory;
    long view;
    int width;
    int height;
    boolean initialized;
    boolean renderTarget;
    long framebuffer;
    boolean destroyed;
    final long[] descriptorSets = new long[VulkanTextureFilter.values().length];
    final List<Long> dependentDescriptorSets = new ArrayList<Long>();
}
