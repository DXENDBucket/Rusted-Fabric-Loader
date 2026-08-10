package io.github.endx.vulkanmod;

import java.util.Locale;

/** Startup policy while the renderer is under development. */
public enum VulkanMode {
    OFF,
    PROBE,
    FRAME_TEST,
    REQUIRED;

    static VulkanMode configured() {
        String raw = System.getProperty("rusted.fabric.vulkan.mode", "probe")
                .trim().toLowerCase(Locale.ROOT);
        switch (raw) {
            case "off": return OFF;
            case "probe":
            case "auto": return PROBE;
            case "frame-test":
            case "frame_test": return FRAME_TEST;
            case "required": return REQUIRED;
            default: throw new IllegalArgumentException(
                    "Unknown rusted.fabric.vulkan.mode: " + raw);
        }
    }
}
