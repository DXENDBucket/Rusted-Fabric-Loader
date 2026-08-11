package io.github.endx.vulkanmod.lwjgl3;

import io.github.endx.vulkanmod.render.LegacyFragmentShaderTranslator;
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
            System.out.println("Native custom fragment translation compiled successfully");
        } finally {
            if (result != MemoryUtil.NULL) shaderc_result_release(result);
            shaderc_compiler_release(compiler);
        }
    }
}
