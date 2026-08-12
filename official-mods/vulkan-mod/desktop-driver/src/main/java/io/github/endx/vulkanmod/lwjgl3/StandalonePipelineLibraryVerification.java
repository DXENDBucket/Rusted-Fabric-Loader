package io.github.endx.vulkanmod.lwjgl3;

import io.github.endx.vulkanmod.spi.VulkanBlendMode;
import io.github.endx.vulkanmod.spi.VulkanCustomFragmentShader;
import io.github.endx.vulkanmod.spi.VulkanDrawState;
import io.github.endx.vulkanmod.spi.VulkanFrameCommands;
import io.github.endx.vulkanmod.spi.VulkanShaderState;
import io.github.endx.vulkanmod.spi.VulkanTextureData;
import io.github.endx.vulkanmod.spi.VulkanTextureFilter;
import io.github.endx.vulkanmod.spi.VulkanTransform2D;
import io.github.endx.vulkanmod.spi.VulkanWindowRequest;
import org.lwjgl.system.windows.User32;

import java.lang.reflect.Field;
import java.util.Map;

/** Exercises render-pass pipeline ownership, cache reuse, and custom shader retirement. */
public final class StandalonePipelineLibraryVerification {
    private static final String WINDOW_TITLE = "RustedVK pipeline library verification";

    private StandalonePipelineLibraryVerification() { }

    public static void main(String[] arguments) {
        try (Lwjgl3VulkanDriver driver = new Lwjgl3VulkanDriver()) {
            driver.createNativeWindowSurface(new VulkanWindowRequest(
                    WINDOW_TITLE, 32, 32, false));
            require(stat(driver, "pipeline.renderPassChanges") == 1L,
                    "initial render pass was not attached to the pipeline library");
            require(stat(driver, "pipeline.creates") == 0L,
                    "pipeline creation was not lazy");

            present(driver, coloredFrame(VulkanBlendMode.NORMAL));
            require(stat(driver, "pipeline.creates") == 1L,
                    "first colored pipeline was not created");
            present(driver, coloredFrame(VulkanBlendMode.NORMAL));
            require(stat(driver, "pipeline.creates") == 1L
                            && stat(driver, "pipeline.cacheHits") >= 1L,
                    "colored pipeline cache was not reused");

            present(driver, coloredFrame(VulkanBlendMode.ADDITIVE));
            require(stat(driver, "pipeline.creates") == 2L,
                    "blend-specific colored pipeline was not isolated");

            long texture = driver.uploadTexture(new VulkanTextureData(1, 1,
                    new byte[] {(byte) 255, (byte) 255, (byte) 255, (byte) 255}));
            long shader = driver.compileFragmentShader(new VulkanCustomFragmentShader(
                    "pipeline-library-verification",
                    "#version 450\n"
                            + "layout(set=0,binding=0) uniform sampler2D image;\n"
                            + "layout(location=0) in vec2 uv;\n"
                            + "layout(location=1) in vec4 color;\n"
                            + "layout(location=0) out vec4 outColor;\n"
                            + "void main(){ outColor=texture(image,uv)*color; }\n"));
            try {
                present(driver, texturedFrame(texture, VulkanShaderState.DEFAULT));
                present(driver, texturedFrame(texture, VulkanShaderState.DEFAULT));
                require(stat(driver, "pipeline.creates") == 3L
                                && stat(driver, "pipeline.cacheHits") >= 2L,
                        "stock textured pipeline cache was not reused");

                present(driver, texturedFrame(texture,
                        VulkanShaderState.custom(shader, 0L, new float[0])));
                present(driver, texturedFrame(texture,
                        VulkanShaderState.custom(shader, 0L, new float[0])));
                require(stat(driver, "pipeline.creates") == 4L
                                && stat(driver, "pipeline.customShaders") == 1L,
                        "custom shader pipeline was not owned by the library");

                long window = nativeWindowHandle(driver);
                require(window != 0L && User32.SetWindowPos(null, window, 0L,
                                0, 0, 96, 72,
                                User32.SWP_NOMOVE | User32.SWP_NOZORDER
                                        | User32.SWP_NOACTIVATE),
                        "could not resize the verification window");
                driver.maintainSurfaceWindow();
                present(driver, texturedFrame(texture,
                        VulkanShaderState.custom(shader, 0L, new float[0])));
                require(stat(driver, "pipeline.renderPassChanges") == 2L
                                && stat(driver, "pipeline.destroys") == 4L
                                && stat(driver, "pipeline.creates") == 5L
                                && stat(driver, "pipeline.customShaders") == 1L,
                        "swapchain recreation did not retain the shader and rebuild its pipeline: "
                                + driver.performanceStatistics());
            } finally {
                driver.destroyFragmentShader(shader);
                driver.destroyTexture(texture);
            }
            require(stat(driver, "pipeline.customShaders") == 0L,
                    "destroyed custom shader remained registered");
            require(stat(driver, "pipeline.live") == 0L,
                    "custom pipeline was not retired immediately");
            require(stat(driver, "pipeline.shaderModuleCreates")
                            == stat(driver, "pipeline.shaderModuleDestroys"),
                    "temporary shader modules leaked");
        }
        System.out.println("Native Vulkan pipeline library lifecycle passed");
    }

    private static VulkanFrameCommands coloredFrame(VulkanBlendMode blendMode) {
        VulkanDrawState state = new VulkanDrawState(VulkanTransform2D.IDENTITY,
                null, blendMode, VulkanTextureFilter.LINEAR, VulkanShaderState.DEFAULT);
        return VulkanFrameCommands.builder(32, 32).clear(0, 0, 0, 1)
                .coloredQuad(0, 0, 16, 16, 1, 0, 0, 1, state)
                .build();
    }

    private static VulkanFrameCommands texturedFrame(long texture,
                                                       VulkanShaderState shader) {
        return texturedFrame(texture, shader, 32, 32);
    }

    private static VulkanFrameCommands texturedFrame(long texture,
                                                       VulkanShaderState shader,
                                                       int width, int height) {
        VulkanDrawState state = new VulkanDrawState(VulkanTransform2D.IDENTITY,
                null, VulkanBlendMode.NORMAL, VulkanTextureFilter.NEAREST, shader);
        return VulkanFrameCommands.builder(width, height).clear(0, 0, 0, 1)
                .texturedQuad(texture, 0, 0, 16, 16, 0, 0, 1, 1,
                        1, 1, 1, 1, state)
                .build();
    }

    private static void present(Lwjgl3VulkanDriver driver, VulkanFrameCommands frame) {
        if (driver.presentFrame(frame) == null) {
            throw new AssertionError("verification frame was not presented");
        }
    }

    private static long stat(Lwjgl3VulkanDriver driver, String name) {
        Map<String, Long> statistics = driver.performanceStatistics();
        Long value = statistics.get(name);
        if (value == null) throw new AssertionError("missing statistic: " + name);
        return value.longValue();
    }

    private static long nativeWindowHandle(Lwjgl3VulkanDriver driver) {
        try {
            Field sessionField = Lwjgl3VulkanDriver.class.getDeclaredField("surfaceSession");
            sessionField.setAccessible(true);
            Object session = sessionField.get(driver);
            Field windowField = session.getClass().getDeclaredField("nativeWindow");
            windowField.setAccessible(true);
            Object window = windowField.get(session);
            Field handleField = window.getClass().getDeclaredField("handle");
            handleField.setAccessible(true);
            return handleField.getLong(window);
        } catch (ReflectiveOperationException failure) {
            throw new AssertionError("could not access verification window", failure);
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
