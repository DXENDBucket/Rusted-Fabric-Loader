package io.github.endx.vulkanmod.spi;

/** Blend equations used by Rusted Warfare's Slick paint compatibility path. */
public enum VulkanBlendMode {
    /** sourceAlpha * source + (1 - sourceAlpha) * destination */
    NORMAL,
    /** sourceAlpha * source + destination */
    ADDITIVE,
    /** source + destination; matches the game's ShaderBlendMode.copy GL state. */
    COPY,
    /** destinationColor * source + (1 - sourceAlpha) * destination */
    MODULATE
}
