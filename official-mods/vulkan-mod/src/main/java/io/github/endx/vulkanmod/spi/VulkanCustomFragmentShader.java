package io.github.endx.vulkanmod.spi;

/** Vulkan-GLSL fragment program compiled by an isolated platform driver. */
public final class VulkanCustomFragmentShader {
    private final String name;
    private final String source;

    public VulkanCustomFragmentShader(String name, String source) {
        if (source == null || source.trim().isEmpty()) {
            throw new IllegalArgumentException("fragment shader source must not be empty");
        }
        this.name = name == null || name.isEmpty() ? "custom" : name;
        this.source = source;
    }

    public String name() { return name; }
    public String source() { return source; }
}
