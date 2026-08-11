package io.github.endx.vulkanmod.lwjgl3;

import io.github.endx.vulkanmod.framestream.FrameStreamEncoder;
import io.github.endx.vulkanmod.framestream.FrameStreamResourceMapper;
import io.github.endx.vulkanmod.render.LegacyShaderProgramTranslator;
import io.github.endx.vulkanmod.spi.VulkanBlendMode;
import io.github.endx.vulkanmod.spi.VulkanCustomShaderProgram;
import io.github.endx.vulkanmod.spi.VulkanDrawState;
import io.github.endx.vulkanmod.spi.VulkanFrameCommands;
import io.github.endx.vulkanmod.spi.VulkanFrameSubmission;
import io.github.endx.vulkanmod.spi.VulkanRenderTargetPass;
import io.github.endx.vulkanmod.spi.VulkanShaderState;
import io.github.endx.vulkanmod.spi.VulkanTextureData;
import io.github.endx.vulkanmod.spi.VulkanTextureFilter;
import io.github.endx.vulkanmod.spi.VulkanTexturedQuad;
import io.github.endx.vulkanmod.spi.VulkanTransform2D;
import io.github.endx.vulkanmod.spi.VulkanWindowRequest;

import java.util.Collections;

/** Builds and executes a real graphics pipeline containing a translated custom vertex stage. */
public final class StandaloneCustomVertexPipelineVerification {
    private StandaloneCustomVertexPipelineVerification() { }

    public static void main(String[] arguments) {
        String vertex = "#version 130\n"
                + "varying vec4 v_color; varying vec2 v_texCoords; varying float v_gain;\n"
                + "uniform sampler2D u_texture; uniform vec2 vertexOffset; uniform float gain;\n"
                + "void main(){ vec4 p=gl_Vertex; p.xy+=vertexOffset;"
                + "gl_Position=gl_ProjectionMatrix*gl_ModelViewMatrix*p;"
                + "v_color=gl_Color; v_texCoords=vec2(gl_MultiTexCoord0);"
                + "v_gain=gain*texture2D(u_texture,vec2(0.5)).r; }";
        String fragment = "#version 130\n"
                + "varying vec4 v_color; varying vec2 v_texCoords; varying float v_gain;\n"
                + "uniform sampler2D u_texture; uniform float gain;\n"
                + "void main(){ gl_FragColor=texture2D(u_texture,v_texCoords)"
                + "*v_color*v_gain+vec4(gain*0.0); }";
        LegacyShaderProgramTranslator.Result translated =
                LegacyShaderProgramTranslator.translate(vertex, fragment);
        try (Lwjgl3VulkanDriver driver = new Lwjgl3VulkanDriver()) {
            driver.createNativeWindowSurface(new VulkanWindowRequest(
                    "RustedVK custom vertex verification", 64, 64, false));
            long program = driver.compileShaderProgram(new VulkanCustomShaderProgram(
                    "custom-vertex-verification", translated.vertexSource(),
                    translated.fragmentSource()));
            long texture = driver.uploadTexture(new VulkanTextureData(1, 1,
                    new byte[] {(byte) 255, (byte) 255, (byte) 255, (byte) 255}));
            long target = driver.createRenderTarget(8, 8);
            try {
                // vertexOffset occupies slot 0; gain is shared by both stages in slot 1.
                // Moving one local unit before a 2x ModelView scale must move two screen pixels.
                VulkanShaderState shader = VulkanShaderState.custom(program, 0L,
                        new float[] {1.0f, 0.0f, 0.0f, 0.0f,
                                1.0f, 0.0f, 0.0f, 0.0f});
                VulkanTransform2D transform = VulkanTransform2D.scale(2.0f, 2.0f)
                        .then(VulkanTransform2D.translation(2.0f, 2.0f));
                VulkanDrawState state = new VulkanDrawState(transform,
                        null, VulkanBlendMode.NORMAL, VulkanTextureFilter.NEAREST, shader);
                VulkanFrameCommands targetFrame = VulkanFrameCommands.builder(8, 8)
                        .clear(0.0f, 0.0f, 0.0f, 1.0f)
                        .texturedQuad(new VulkanTexturedQuad(texture,
                                0.0f, 0.0f, 2.0f, 2.0f,
                                0.0f, 0.0f, 1.0f, 1.0f,
                                1.0f, 0.0f, 0.0f, 1.0f))
                        .texturedQuad(new VulkanTexturedQuad(texture,
                                0.0f, 0.0f, 2.0f, 2.0f,
                                0.0f, 0.0f, 1.0f, 1.0f,
                                1.0f, 1.0f, 1.0f, 1.0f, state))
                        .build();
                VulkanFrameCommands presentation = VulkanFrameCommands.builder(64, 64)
                        .clear(0.0f, 0.0f, 0.0f, 1.0f).build();
                VulkanFrameSubmission submission = new VulkanFrameSubmission(
                        Collections.singletonList(new VulkanRenderTargetPass(target, targetFrame)),
                        presentation);
                FrameStreamEncoder encoder = new FrameStreamEncoder(
                        FrameStreamResourceMapper.generationOneSlots(),
                        driver::customShaderUsesExpandedVertexInput);
                if (driver.presentFrameStream(encoder.encode(1L, 0L, submission)) == null) {
                    throw new AssertionError("custom FrameStream frame was unavailable");
                }
                byte[] rgba = driver.readTexture(target).copyRgba();
                for (int y = 0; y < 8; y++) {
                    for (int x = 0; x < 8; x++) {
                        int index = (y * 8 + x) * 4;
                        boolean inside = x >= 4 && x < 8 && y >= 2 && y < 6;
                        boolean stock = x < 2 && y < 2;
                        int expectedRed = inside || stock ? 255 : 0;
                        int expectedGreenBlue = inside ? 255 : 0;
                        if (Math.abs((rgba[index] & 255) - expectedRed) > 1
                                || Math.abs((rgba[index + 1] & 255)
                                        - expectedGreenBlue) > 1
                                || Math.abs((rgba[index + 2] & 255)
                                        - expectedGreenBlue) > 1
                                || (rgba[index + 3] & 255) < 250) {
                            throw new AssertionError("custom local/model/projection output mismatch at "
                                    + x + "," + y);
                        }
                    }
                }
            } finally {
                driver.destroyTexture(target);
                driver.destroyTexture(texture);
                driver.destroyShaderProgram(program);
            }
        }
        System.out.println("Native Vulkan custom vertex pipeline passed");
    }
}
