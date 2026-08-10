package io.github.endx.vulkanmod.spi;

import java.util.Objects;

/** Immutable physical-device summary that can cross an isolated driver class loader. */
public final class VulkanDeviceInfo {
    private final String name;
    private final int vendorId;
    private final int deviceId;
    private final int deviceType;
    private final int apiVersion;
    private final int driverVersion;

    public VulkanDeviceInfo(String name, int vendorId, int deviceId, int deviceType,
                            int apiVersion, int driverVersion) {
        this.name = Objects.requireNonNull(name, "name");
        this.vendorId = vendorId;
        this.deviceId = deviceId;
        this.deviceType = deviceType;
        this.apiVersion = apiVersion;
        this.driverVersion = driverVersion;
    }

    public String name() { return name; }
    public int vendorId() { return vendorId; }
    public int deviceId() { return deviceId; }
    public int deviceType() { return deviceType; }
    public int apiVersion() { return apiVersion; }
    public int driverVersion() { return driverVersion; }
}
