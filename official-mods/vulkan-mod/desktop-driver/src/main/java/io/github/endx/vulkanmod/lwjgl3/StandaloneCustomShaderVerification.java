package io.github.endx.vulkanmod.lwjgl3;

import io.github.endx.vulkanmod.render.LegacyFragmentShaderTranslator;
import io.github.endx.vulkanmod.render.LegacyShaderProgramTranslator;
import org.lwjgl.system.MemoryUtil;

import static org.lwjgl.util.shaderc.Shaderc.*;

/** Compiles a translated legacy shader through the isolated native shaderc runtime. */
public final class StandaloneCustomShaderVerification {
    private StandaloneCustomShaderVerification() { }

    public static void main(String[] arguments) {
        String legacy = "#version 130\n"
                + "precision mediump float;\n"
                + "varying vec4 v_color;\n"
                + "varying vec2 v_texCoords;\n"
                + "uniform sampler2D u_texture;\n"
                + "uniform sampler2D noiseMap;\n"
                + "uniform vec2 offset;\n"
                + "uniform float strength;\n"
                + "void main(){ vec4 base=texture2D(u_texture,v_texCoords+offset);"
                + "vec4 noise=texture2D(noiseMap,v_texCoords);"
                + "gl_FragColor=mix(base,noise,strength)*v_color; }\n";
        String source = LegacyFragmentShaderTranslator.translate(legacy).source();
        String legacyVertex = "#version 130\n"
                + "varying vec4 v_color;\n"
                + "varying vec2 v_texCoords;\n"
                + "varying float v_wave;\n"
                + "uniform float time;\n"
                + "uniform vec2 sway;\n"
                + "void main(){ vec4 moved=gl_Vertex;"
                + "moved.xy+=sway*sin(time);"
                + "gl_Position=gl_ProjectionMatrix*gl_ModelViewMatrix*moved;"
                + "v_color=gl_Color; v_texCoords=vec2(gl_MultiTexCoord0);"
                + "v_wave=sin(time); }\n";
        String pairedFragment = "#version 130\n"
                + "varying vec4 v_color;\n"
                + "varying vec2 v_texCoords;\n"
                + "varying float v_wave;\n"
                + "uniform sampler2D u_texture;\n"
                + "uniform float time;\n"
                + "void main(){ gl_FragColor=texture2D(u_texture,v_texCoords)"
                + "*v_color*(0.75+0.25*v_wave+time*0.0); }\n";
        LegacyShaderProgramTranslator.Result program =
                LegacyShaderProgramTranslator.translate(legacyVertex, pairedFragment);
        LegacyShaderProgramTranslator.Result gdxProgram =
                LegacyShaderProgramTranslator.translate(
                        "attribute vec4 a_position; attribute vec4 a_color;"
                                + "attribute vec2 a_texCoord0; uniform mat4 u_projTrans;"
                                + "varying vec4 v_color; varying vec2 v_texCoords;"
                                + "void main(){ v_color=a_color; v_texCoords=a_texCoord0;"
                                + "gl_Position=u_projTrans*a_position; }",
                        "varying vec4 v_color; varying vec2 v_texCoords;"
                                + "uniform sampler2D u_texture;"
                                + "void main(){ gl_FragColor=texture2D(u_texture,v_texCoords)"
                                + "*v_color; }");
        if (program.uniforms().size() != 2
                || !"time".equals(program.uniforms().get(0).name())
                || !"sway".equals(program.uniforms().get(1).name())) {
            throw new AssertionError("vertex/fragment uniforms did not share one ABI");
        }
        long compiler = shaderc_compiler_initialize();
        if (compiler == MemoryUtil.NULL) {
            throw new IllegalStateException("shaderc_compiler_initialize failed");
        }
        long result = MemoryUtil.NULL;
        try {
            result = shaderc_compile_into_spv(compiler, source,
                    shaderc_glsl_fragment_shader, "legacy-custom.frag", "main",
                    MemoryUtil.NULL);
            if (result == MemoryUtil.NULL) {
                throw new IllegalStateException("shaderc_compile_into_spv returned null");
            }
            int status = shaderc_result_get_compilation_status(result);
            if (status != shaderc_compilation_status_success) {
                throw new IllegalStateException(shaderc_result_get_error_message(result));
            }
            if (shaderc_result_get_bytes(result).remaining() == 0) {
                throw new AssertionError("translated custom shader produced empty SPIR-V");
            }
            shaderc_result_release(result);
            result = MemoryUtil.NULL;
            result = compile(compiler, program.vertexSource(),
                    shaderc_glsl_vertex_shader, "legacy-custom.vert");
            shaderc_result_release(result);
            result = MemoryUtil.NULL;
            result = compile(compiler, program.fragmentSource(),
                    shaderc_glsl_fragment_shader, "legacy-custom-paired.frag");
            shaderc_result_release(result);
            result = MemoryUtil.NULL;
            result = compile(compiler, gdxProgram.vertexSource(),
                    shaderc_glsl_vertex_shader, "legacy-gdx-custom.vert");
            System.out.println("Native custom vertex/fragment translation compiled successfully");
        } finally {
            if (result != MemoryUtil.NULL) shaderc_result_release(result);
            shaderc_compiler_release(compiler);
        }
    }

    private static long compile(long compiler, String source, int kind, String name) {
        long result = shaderc_compile_into_spv(compiler, source, kind, name, "main",
                MemoryUtil.NULL);
        if (result == MemoryUtil.NULL) {
            throw new IllegalStateException("shaderc_compile_into_spv returned null for " + name);
        }
        int status = shaderc_result_get_compilation_status(result);
        if (status != shaderc_compilation_status_success) {
            String message = shaderc_result_get_error_message(result);
            shaderc_result_release(result);
            throw new IllegalStateException(name + ": " + message);
        }
        if (shaderc_result_get_bytes(result).remaining() == 0) {
            shaderc_result_release(result);
            throw new AssertionError(name + " produced empty SPIR-V");
        }
        return result;
    }
}
