package io.github.endx.vulkanmod;

import io.github.endx.vulkanmod.spi.VulkanProbeResult;
import io.github.endx.vulkanmod.spi.VulkanBlendMode;
import io.github.endx.vulkanmod.spi.VulkanColoredQuad;
import io.github.endx.vulkanmod.spi.VulkanColoredTriangle;
import io.github.endx.vulkanmod.spi.VulkanFrameCommands;
import io.github.endx.vulkanmod.spi.VulkanTextureData;
import io.github.endx.vulkanmod.spi.VulkanTextureFilter;
import io.github.endx.vulkanmod.spi.VulkanTexturedQuad;
import io.github.endx.vulkanmod.spi.VulkanTexturedTriangle;
import io.github.endx.vulkanmod.spi.VulkanClipRect;
import io.github.endx.vulkanmod.spi.VulkanDrawState;
import io.github.endx.vulkanmod.spi.VulkanTransform2D;
import io.github.endx.vulkanmod.spi.VulkanShaderState;

import java.nio.file.Files;
import java.nio.file.Path;

/** Executable contract check for the embedded child-first driver boundary. */
public final class VulkanDriverIsolationVerification {
    private VulkanDriverIsolationVerification() { }

    public static void main(String[] arguments) throws Exception {
        byte[] pixel = {(byte) 255, 0, 0, (byte) 255};
        VulkanTextureData texture = new VulkanTextureData(1, 1, pixel);
        pixel[0] = 0;
        if (texture.copyRgba()[0] != (byte) 255) {
            throw new AssertionError("texture upload data is externally mutable");
        }
        VulkanTransform2D transform = VulkanTransform2D.translation(10.0f, 20.0f)
                .then(VulkanTransform2D.scale(2.0f, 3.0f));
        if (transform.transformX(5.0f, 7.0f) != 30.0f
                || transform.transformY(5.0f, 7.0f) != 81.0f) {
            throw new AssertionError("affine transform composition order is invalid");
        }
        VulkanDrawState drawState = new VulkanDrawState(transform,
                new VulkanClipRect(0.0f, 0.0f, 640.0f, 360.0f),
                VulkanBlendMode.ADDITIVE, VulkanTextureFilter.NEAREST,
                new VulkanShaderState(VulkanShaderState.HUE_ADD_TEAM_COLOR,
                        0.25f, 0.5f, 0.75f, 1.0f, 0.2f));
        VulkanShaderState displacement = new VulkanShaderState(
                VulkanShaderState.POST_DISPLACEMENT,
                1.0f, 1.0f, 1.0f, 1.0f, 0.15f,
                11L, 1024.0f, 512.0f, 1920.0f, 1080.0f, 0.12f, 1.0f);
        if (displacement.secondaryTextureHandle() != 11L
                || displacement.screenBaseWidth() != 1024.0f
                || displacement.screenBaseHeight() != 512.0f
                || displacement.resolutionWidth() != 1920.0f
                || displacement.resolutionHeight() != 1080.0f
                || displacement.displacementOffset() != 0.12f
                || displacement.uiScaling() != 1.0f
                || displacement.equals(VulkanShaderState.DEFAULT)) {
            throw new AssertionError("secondary-texture shader state is incomplete");
        }
        try {
            new VulkanShaderState(VulkanShaderState.POST_DISPLACEMENT,
                    1.0f, 1.0f, 1.0f, 1.0f, 0.15f);
            throw new AssertionError("displacement state accepted no secondary texture");
        } catch (IllegalArgumentException expected) {
            // A driver must never receive a displacement draw without its screen-base image.
        }
        VulkanFrameCommands commands = VulkanFrameCommands.builder(1280, 720)
                .clear(0.1f, 0.2f, 0.3f, 1.0f)
                .coloredQuad(new VulkanColoredQuad(
                        10.0f, 20.0f, 100.0f, 50.0f, 1.0f, 0.5f, 0.0f, 0.75f))
                .texturedQuad(new VulkanTexturedQuad(7L,
                        30.0f, 40.0f, 64.0f, 64.0f,
                        0.0f, 0.0f, 1.0f, 1.0f,
                        1.0f, 1.0f, 1.0f, 1.0f, drawState))
                .coloredTriangle(new VulkanColoredTriangle(
                        new float[] {0.0f, 0.0f, 4.0f, 0.0f, 0.0f, 4.0f},
                        new float[] {1.0f, 0.0f, 0.0f, 1.0f,
                                0.0f, 1.0f, 0.0f, 1.0f,
                                0.0f, 0.0f, 1.0f, 1.0f}, drawState))
                .texturedTriangle(new VulkanTexturedTriangle(7L,
                        new float[] {4.0f, 4.0f, 8.0f, 4.0f, 4.0f, 8.0f},
                        new float[] {0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f},
                        new float[] {1.0f, 1.0f, 1.0f, 1.0f,
                                1.0f, 1.0f, 1.0f, 1.0f,
                                1.0f, 1.0f, 1.0f, 1.0f}, drawState))
                .build();
        if (commands.coloredQuads().size() != 1
                || commands.coloredQuads().get(0).alpha() != 0.75f
                || commands.texturedQuads().size() != 1
                || commands.texturedQuads().get(0).textureHandle() != 7L
                || commands.texturedQuads().get(0).state().clip() == null
                || commands.texturedQuads().get(0).state().blendMode()
                        != VulkanBlendMode.ADDITIVE
                || commands.texturedQuads().get(0).state().textureFilter()
                        != VulkanTextureFilter.NEAREST
                || commands.texturedQuads().get(0).state().shaderState().effect()
                        != VulkanShaderState.HUE_ADD_TEAM_COLOR
                || commands.coloredTriangles().size() != 1
                || commands.texturedTriangles().size() != 1
                || commands.commands().size() != 4
                || commands.commands().get(0) != commands.coloredQuads().get(0)
                || commands.commands().get(1) != commands.texturedQuads().get(0)
                || commands.commands().get(2) != commands.coloredTriangles().get(0)
                || commands.commands().get(3) != commands.texturedTriangles().get(0)) {
            throw new AssertionError("binding-neutral frame command contract is invalid");
        }
        try {
            commands.coloredQuads().clear();
            throw new AssertionError("frame commands expose a mutable list");
        } catch (UnsupportedOperationException expected) {
            // Frames are immutable once submitted across the driver boundary.
        }
        try {
            commands.texturedQuads().clear();
            throw new AssertionError("frame texture commands expose a mutable list");
        } catch (UnsupportedOperationException expected) {
            // Textured commands follow the same immutable frame contract.
        }
        try {
            commands.commands().clear();
            throw new AssertionError("ordered frame commands expose a mutable list");
        } catch (UnsupportedOperationException expected) {
            // Rendering order is immutable across the driver boundary too.
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
