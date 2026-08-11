package io.github.endx.vulkanmod.lwjgl3;

import io.github.endx.vulkanmod.render.LegacyShaderProgramTranslator;
import io.github.endx.vulkanmod.spi.VulkanBlendMode;
import io.github.endx.vulkanmod.spi.VulkanCustomShaderProgram;
import io.github.endx.vulkanmod.spi.VulkanDrawState;
import io.github.endx.vulkanmod.spi.VulkanFrameCommands;
import io.github.endx.vulkanmod.spi.VulkanShaderState;
import io.github.endx.vulkanmod.spi.VulkanTextureData;
import io.github.endx.vulkanmod.spi.VulkanTextureFilter;
import io.github.endx.vulkanmod.spi.VulkanTexturedQuad;
import io.github.endx.vulkanmod.spi.VulkanTransform2D;
import io.github.endx.vulkanmod.spi.VulkanWindowRequest;

/** Builds and executes a real graphics pipeline containing a translated custom vertex stage. */
public final class StandaloneCustomVertexPipelineVerification {
    private StandaloneCustomVertexPipelineVerification() { }

    public static void main(String[] arguments) {
        String vertex = "#version 130\n"
                + "varying vec4 v_color; varying vec2 v_texCoords; varying float v_gain;\n"
                + "uniform vec2 vertexOffset; uniform float gain;\n"
                + "void main(){ vec4 p=gl_Vertex; p.xy+=vertexOffset;"
                + "gl_Position=gl_ProjectionMatrix*gl_ModelViewMatrix*p;"
                + "v_color=gl_Color; v_texCoords=vec2(gl_MultiTexCoord0); v_gain=gain; }";
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
                VulkanShaderState shader = VulkanShaderState.custom(program, 0L,
                        new float[] {0.0f, 0.0f, 0.0f, 0.0f,
                                1.0f, 0.0f, 0.0f, 0.0f});
                VulkanDrawState state = new VulkanDrawState(VulkanTransform2D.IDENTITY,
                        null, VulkanBlendMode.NORMAL, VulkanTextureFilter.NEAREST, shader);
                driver.renderToTexture(target, VulkanFrameCommands.builder(8, 8)
                        .clear(0.0f, 0.0f, 0.0f, 1.0f)
                        .texturedQuad(new VulkanTexturedQuad(texture,
                                0.0f, 0.0f, 8.0f, 8.0f,
                                0.0f, 0.0f, 1.0f, 1.0f,
                                1.0f, 1.0f, 1.0f, 1.0f, state))
                        .build());
                byte[] rgba = driver.readTexture(target).copyRgba();
                for (int index = 0; index < rgba.length; index += 4) {
                    if ((rgba[index] & 255) < 250 || (rgba[index + 1] & 255) < 250
                            || (rgba[index + 2] & 255) < 250
                            || (rgba[index + 3] & 255) < 250) {
                        throw new AssertionError("custom vertex pipeline output was not white at "
                                + (index / 4));
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
