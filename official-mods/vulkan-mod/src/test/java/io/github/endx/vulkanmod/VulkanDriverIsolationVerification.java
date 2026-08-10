package io.github.endx.vulkanmod;

import io.github.endx.vulkanmod.spi.VulkanProbeResult;
import io.github.endx.vulkanmod.spi.VulkanColoredQuad;
import io.github.endx.vulkanmod.spi.VulkanFrameCommands;

import java.nio.file.Files;
import java.nio.file.Path;

/** Executable contract check for the embedded child-first driver boundary. */
public final class VulkanDriverIsolationVerification {
    private VulkanDriverIsolationVerification() { }

    public static void main(String[] arguments) throws Exception {
        VulkanFrameCommands commands = VulkanFrameCommands.builder(1280, 720)
                .clear(0.1f, 0.2f, 0.3f, 1.0f)
                .coloredQuad(new VulkanColoredQuad(
                        10.0f, 20.0f, 100.0f, 50.0f, 1.0f, 0.5f, 0.0f, 0.75f))
                .build();
        if (commands.coloredQuads().size() != 1
                || commands.coloredQuads().get(0).alpha() != 0.75f) {
            throw new AssertionError("binding-neutral frame command contract is invalid");
        }
        try {
            commands.coloredQuads().clear();
            throw new AssertionError("frame commands expose a mutable list");
        } catch (UnsupportedOperationException expected) {
            // Frames are immutable once submitted across the driver boundary.
        }
        try {
            Class.forName("org.lwjgl.vulkan.VK", false,
                    VulkanDriverIsolationVerification.class.getClassLoader());
            throw new AssertionError("LWJGL 3 leaked onto Vulkan Mod's parent class path");
        } catch (ClassNotFoundException expected) {
            // The binding must only exist inside the driver loader.
        }
        try {
            Class.forName("org.lwjgl.util.shaderc.Shaderc", false,
                    VulkanDriverIsolationVerification.class.getClassLoader());
            throw new AssertionError("Shaderc leaked onto Vulkan Mod's parent class path");
        } catch (ClassNotFoundException expected) {
            // Runtime shader compilation is also isolated with the platform driver.
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
