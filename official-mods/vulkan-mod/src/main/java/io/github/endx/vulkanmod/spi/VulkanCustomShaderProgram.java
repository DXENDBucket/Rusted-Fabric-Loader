package io.github.endx.vulkanmod.spi;

/** Complete Vulkan-GLSL program compiled by an isolated platform driver. */
public final class VulkanCustomShaderProgram {
    private final String name;
    private final String vertexSource;
    private final String fragmentSource;

    public VulkanCustomShaderProgram(String name, String vertexSource,
                                     String fragmentSource) {
        if (vertexSource == null || vertexSource.trim().isEmpty()) {
            throw new IllegalArgumentException("vertex shader source must not be empty");
        }
        if (fragmentSource == null || fragmentSource.trim().isEmpty()) {
            throw new IllegalArgumentException("fragment shader source must not be empty");
        }
        this.name = name == null || name.isEmpty() ? "custom" : name;
        this.vertexSource = vertexSource;
        this.fragmentSource = fragmentSource;
    }

    public String name() { return name; }
    public String vertexSource() { return vertexSource; }
    public String fragmentSource() { return fragmentSource; }
}
