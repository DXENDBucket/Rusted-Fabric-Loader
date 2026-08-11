package io.github.endx.vulkanmod.render;

import io.github.endx.vulkanmod.spi.VulkanShaderState;
import io.github.endx.vulkanmod.spi.VulkanBuiltInShaders;

/** Locks native coverage to every fragment shader shipped by the stock desktop game. */
public final class NativeShaderCoverageVerification {
    private NativeShaderCoverageVerification() { }

    public static void main(String[] args) {
        require("plain", VulkanShaderState.PLAIN);
        require("pureGreenTeamColor", VulkanShaderState.PURE_GREEN_TEAM_COLOR);
        require("hueAddTeamColor", VulkanShaderState.HUE_ADD_TEAM_COLOR);
        require("hueShiftTeamColor", VulkanShaderState.HUE_SHIFT_TEAM_COLOR);
        require("post_base", VulkanShaderState.POST_BASE);
        require("post_displacement", VulkanShaderState.POST_DISPLACEMENT);
        require("error", VulkanShaderState.ERROR);
        if (VulkanBuiltInShaders.effectForName("third_party_shader") != -1) {
            throw new AssertionError("unknown shader was classified as a stock shader");
        }
        String legacy = "#version 130\n"
                + "varying vec4 v_color;\n"
                + "varying vec2 v_texCoords;\n"
                + "uniform sampler2D u_texture;\n"
                + "uniform sampler2D noiseMap;\n"
                + "uniform vec2 offset; // dynamic offset\n"
                + "void main(){ gl_FragColor=texture2D(u_texture,v_texCoords+offset)"
                + "*texture2D(noiseMap,v_texCoords)*v_color; }\n";
        LegacyFragmentShaderTranslator.Result translated =
                LegacyFragmentShaderTranslator.translate(legacy);
        if (!translated.source().startsWith("#version 450\n")
                || !translated.source().contains("layout(set=0,binding=1) uniform sampler2D noiseMap;")
                || !translated.source().contains("#define offset rf.customValues[0].xy")
                || !translated.source().contains("rf_outColor=texture(u_texture")
                || translated.uniforms().size() != 1
                || !"noiseMap".equals(translated.secondarySampler())) {
            throw new AssertionError("legacy fragment translation contract is incomplete");
        }
        String stockVertex = "varying vec4 v_color; varying vec2 v_texCoords;"
                + "void main(){ gl_Position=gl_ProjectionMatrix*gl_ModelViewMatrix*gl_Vertex;"
                + "v_color=gl_Color;v_texCoords=vec2(gl_MultiTexCoord0);}";
        if (!LegacyFragmentShaderTranslator.usesStockVertexContract(stockVertex)) {
            throw new AssertionError("stock desktop vertex contract was rejected");
        }
        try {
            LegacyFragmentShaderTranslator.translate(legacy
                    .replace("void main()", "uniform sampler2D thirdMap;\nvoid main()"));
            throw new AssertionError("multiple secondary samplers were accepted");
        } catch (IllegalArgumentException expected) {
            // The current descriptor ABI deliberately exposes one secondary image.
        }
        System.out.println("Native stock shader coverage contracts passed");
    }

    private static void require(String name, int expected) {
        int actual = VulkanBuiltInShaders.effectForName(name);
        if (actual != expected) {
            throw new AssertionError(name + " mapped to " + actual + ", expected " + expected);
        }
    }
}
