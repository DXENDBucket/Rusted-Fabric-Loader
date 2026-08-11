package io.github.endx.vulkanmod.render;

import io.github.endx.vulkanmod.spi.VulkanShaderState;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Pure GLSL-130 to Vulkan-fragment-ABI translation, kept independent from game classes. */
public final class LegacyFragmentShaderTranslator {
    private static final Pattern VERSION = Pattern.compile("(?m)^\\s*#version\\s+\\d+\\s*$");
    private static final Pattern PRECISION = Pattern.compile(
            "(?m)^\\s*precision\\s+\\w+\\s+float\\s*;\\s*$");
    private static final Pattern VARYING = Pattern.compile(
            "(?m)^\\s*varying\\s+(\\w+)\\s+(\\w+)\\s*;\\s*$");
    private static final Pattern UNIFORM = Pattern.compile(
            "(?m)^\\s*uniform\\s+(float|vec2|vec3|vec4|sampler2D)\\s+(\\w+)\\s*;\\s*$");

    private LegacyFragmentShaderTranslator() { }

    public static Result translate(String source) {
        if (source == null || source.trim().isEmpty()) {
            throw new IllegalArgumentException("custom fragment shader source is empty");
        }
        String working = withoutComments(source);
        Matcher varying = VARYING.matcher(working);
        while (varying.find()) {
            String type = varying.group(1);
            String name = varying.group(2);
            boolean supported = ("vec4".equals(type) && "v_color".equals(name))
                    || ("vec2".equals(type) && "v_texCoords".equals(name));
            if (!supported) {
                throw new IllegalArgumentException("unsupported fragment varying "
                        + type + " " + name);
            }
        }

        List<UniformSlot> uniforms = new ArrayList<UniformSlot>();
        String secondarySampler = null;
        Matcher matcher = UNIFORM.matcher(working);
        StringBuffer body = new StringBuffer();
        while (matcher.find()) {
            String type = matcher.group(1);
            String name = matcher.group(2);
            if ("sampler2D".equals(type)) {
                if (!"u_texture".equals(name)) {
                    if (secondarySampler != null && !secondarySampler.equals(name)) {
                        throw new IllegalArgumentException(
                                "only one secondary sampler2D is currently supported");
                    }
                    secondarySampler = name;
                }
            } else {
                if (uniforms.size() >= VulkanShaderState.MAX_CUSTOM_FLOATS / 4) {
                    throw new IllegalArgumentException("custom shader exceeds five numeric uniforms");
                }
                uniforms.add(new UniformSlot(name, components(type), uniforms.size() * 4));
            }
            matcher.appendReplacement(body, "");
        }
        matcher.appendTail(body);

        String translated = VERSION.matcher(body).replaceAll("");
        translated = PRECISION.matcher(translated).replaceAll("");
        translated = VARYING.matcher(translated).replaceAll("");
        translated = translated.replace("texture2D", "texture")
                .replace("gl_FragColor", "rf_outColor");

        StringBuilder prefix = new StringBuilder(1024);
        prefix.append("#version 450\n")
                .append("layout(set=0,binding=0) uniform sampler2D u_texture;\n");
        if (secondarySampler != null) {
            prefix.append("layout(set=0,binding=1) uniform sampler2D ")
                    .append(secondarySampler).append(";\n");
        }
        prefix.append("layout(push_constant) uniform RFShaderState {\n")
                .append("  vec4 teamColor; float teamColorAmount; int effect; vec2 resolution;\n")
                .append("  float displacementOffset; float uiScaling; vec2 screenBaseSize;\n")
                .append("  vec4 customValues[5];\n")
                .append("} rf;\n")
                .append("layout(location=0) in vec2 v_texCoords;\n")
                .append("layout(location=1) in vec4 v_color;\n")
                .append("layout(location=0) out vec4 rf_outColor;\n");
        for (int index = 0; index < uniforms.size(); index++) {
            UniformSlot slot = uniforms.get(index);
            prefix.append("#define ").append(slot.name).append(" rf.customValues[")
                    .append(index).append(']');
            if (slot.components == 1) prefix.append(".x");
            else if (slot.components == 2) prefix.append(".xy");
            else if (slot.components == 3) prefix.append(".xyz");
            prefix.append('\n');
        }
        return new Result(prefix.append(translated).toString(), uniforms, secondarySampler);
    }

    public static boolean usesStockVertexContract(String source) {
        if (source == null) return false;
        String compact = withoutComments(source)
                .replaceAll("\\s+", "");
        boolean desktop = compact.contains("v_color=gl_Color;")
                && compact.contains("v_texCoords=vec2(gl_MultiTexCoord0);")
                && compact.contains("gl_Position=gl_ProjectionMatrix*gl_ModelViewMatrix*gl_Vertex;");
        boolean gdx = compact.contains("v_color=a_color;")
                && compact.contains("v_texCoords=a_texCoord0;")
                && compact.contains("gl_Position=u_projTrans*a_position;");
        return desktop || gdx;
    }

    private static String withoutComments(String source) {
        return source.replaceAll("(?s)/\\*.*?\\*/", "")
                .replaceAll("(?m)//.*$", "");
    }

    private static int components(String type) {
        if ("float".equals(type)) return 1;
        if ("vec2".equals(type)) return 2;
        if ("vec3".equals(type)) return 3;
        return 4;
    }

    public static final class UniformSlot {
        private final String name;
        private final int components;
        private final int offset;

        UniformSlot(String name, int components, int offset) {
            this.name = name;
            this.components = components;
            this.offset = offset;
        }

        public String name() { return name; }
        public int components() { return components; }
        public int offset() { return offset; }
    }

    public static final class Result {
        private final String source;
        private final List<UniformSlot> uniforms;
        private final String secondarySampler;

        Result(String source, List<UniformSlot> uniforms, String secondarySampler) {
            this.source = source;
            this.uniforms = Collections.unmodifiableList(
                    new ArrayList<UniformSlot>(uniforms));
            this.secondarySampler = secondarySampler;
        }

        public String source() { return source; }
        public List<UniformSlot> uniforms() { return uniforms; }
        public String secondarySampler() { return secondarySampler; }
    }
}
