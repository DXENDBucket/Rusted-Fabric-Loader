package io.github.endx.vulkanmod.spi;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/** Result of a platform driver probe without leaking binding-specific classes. */
public final class VulkanProbeResult {
    private final boolean available;
    private final int instanceVersion;
    private final List<VulkanDeviceInfo> devices;
    private final String diagnostic;

    private VulkanProbeResult(boolean available, int instanceVersion,
                              List<VulkanDeviceInfo> devices, String diagnostic) {
        this.available = available;
        this.instanceVersion = instanceVersion;
        this.devices = Collections.unmodifiableList(new ArrayList<VulkanDeviceInfo>(devices));
        this.diagnostic = Objects.requireNonNull(diagnostic, "diagnostic");
    }

    public static VulkanProbeResult available(int instanceVersion,
                                               List<VulkanDeviceInfo> devices) {
        return new VulkanProbeResult(true, instanceVersion, devices, "");
    }

    public static VulkanProbeResult unavailable(String diagnostic) {
        String checked = diagnostic != null && !diagnostic.trim().isEmpty()
                ? diagnostic.trim() : "Vulkan is unavailable";
        return new VulkanProbeResult(false, 0, Collections.emptyList(), checked);
    }

    public boolean available() { return available; }
    public int instanceVersion() { return instanceVersion; }
    public List<VulkanDeviceInfo> devices() { return devices; }
    public String diagnostic() { return diagnostic; }

    public static String formatVersion(int version) {
        return ((version >>> 22) & 0x3ff) + "."
                + ((version >>> 12) & 0x3ff) + "." + (version & 0xfff);
    }
}
