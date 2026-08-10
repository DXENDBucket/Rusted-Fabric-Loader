package io.github.endx.vulkanmod.spi;

import java.util.Objects;

/** Description of a top-level native window that is owned by the Vulkan driver. */
public final class VulkanWindowRequest {
    private final String title;
    private final int width;
    private final int height;
    private final boolean resizable;

    public VulkanWindowRequest(String title, int width, int height, boolean resizable) {
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("window size must be positive");
        }
        this.title = Objects.requireNonNull(title, "title");
        this.width = width;
        this.height = height;
        this.resizable = resizable;
    }

    public String title() { return title; }
    public int width() { return width; }
    public int height() { return height; }
    public boolean resizable() { return resizable; }
}
