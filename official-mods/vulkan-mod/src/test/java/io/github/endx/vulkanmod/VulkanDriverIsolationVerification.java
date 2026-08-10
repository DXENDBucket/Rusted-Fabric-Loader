package io.github.endx.vulkanmod;

import io.github.endx.vulkanmod.spi.VulkanProbeResult;

import java.nio.file.Files;
import java.nio.file.Path;

/** Executable contract check for the embedded child-first driver boundary. */
public final class VulkanDriverIsolationVerification {
    private VulkanDriverIsolationVerification() { }

    public static void main(String[] arguments) throws Exception {
        try {
            Class.forName("org.lwjgl.vulkan.VK", false,
                    VulkanDriverIsolationVerification.class.getClassLoader());
            throw new AssertionError("LWJGL 3 leaked onto Vulkan Mod's parent class path");
        } catch (ClassNotFoundException expected) {
            // The binding must only exist inside the driver loader.
        }
        Path cache = Files.createTempDirectory("rusted-fabric-vulkan-driver-test");
        String libraryPathBefore = System.getProperty("org.lwjgl.librarypath");
        try (VulkanDriverLoader.LoadedDriver loaded = VulkanDriverLoader.loadDesktop(cache)) {
            VulkanProbeResult result = loaded.probe();
            if (loaded.name().trim().isEmpty()) {
                throw new AssertionError("isolated Vulkan driver has no name");
            }
            if (result == null) {
                throw new AssertionError("isolated Vulkan probe returned null");
            }
            System.out.println("Isolated Vulkan driver contract passed: available="
                    + result.available() + ", devices=" + result.devices().size());
        }
        String libraryPathAfter = System.getProperty("org.lwjgl.librarypath");
        if (libraryPathBefore == null ? libraryPathAfter != null
                : !libraryPathBefore.equals(libraryPathAfter)) {
            throw new AssertionError("LWJGL 3 polluted the parent org.lwjgl.librarypath");
        }
    }
}
