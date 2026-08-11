package io.github.endx.vulkanmod.render;

import io.github.endx.vulkanmod.spi.VulkanShaderState;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Translates the game's linked GLSL-130 vertex/fragment pair to one Vulkan shader ABI. */
public final class LegacyShaderProgramTranslator {
    private static final Pattern VERSION = Pattern.compile("(?m)^\\s*#version\\s+\\d+\\s*$");
    private static final Pattern PRECISION = Pattern.compile(
            "(?m)^\\s*precision\\s+\\w+\\s+float\\s*;\\s*$");
    private static final Pattern VARYING = Pattern.compile(
            "\\bvarying\\s+(float|vec2|vec3|vec4)\\s+(\\w+)\\s*;");
    private static final Pattern ATTRIBUTE = Pattern.compile(
            "\\battribute\\s+(float|vec2|vec3|vec4)\\s+(\\w+)\\s*;");
    private static final Pattern UNIFORM = Pattern.compile(
            "\\buniform\\s+(float|vec2|vec3|vec4|mat4|sampler2D)\\s+(\\w+)\\s*;");

    private LegacyShaderProgramTranslator() { }

    public static Result translate(String vertexSource, String fragmentSource) {
        requireSource("vertex", vertexSource);
        requireSource("fragment", fragmentSource);
        String vertex = withoutComments(vertexSource);
        String fragment = withoutComments(fragmentSource);

        LinkedHashMap<String, String> vertexVaryings = varyings(vertex);
        LinkedHashMap<String, String> fragmentVaryings = varyings(fragment);
        for (Map.Entry<String, String> entry : fragmentVaryings.entrySet()) {
            String vertexType = vertexVaryings.get(entry.getKey());
            if (vertexType == null) {
                throw new IllegalArgumentException("fragment varying " + entry.getKey()
                        + " is not written by the vertex shader");
            }
            if (!vertexType.equals(entry.getValue())) {
                throw new IllegalArgumentException("varying type mismatch for "
                        + entry.getKey());
            }
        }
        LinkedHashMap<String, String> orderedVaryings = new LinkedHashMap<String, String>();
        addIfPresent(orderedVaryings, vertexVaryings, "v_texCoords");
        addIfPresent(orderedVaryings, vertexVaryings, "v_color");
        for (Map.Entry<String, String> entry : vertexVaryings.entrySet()) {
            orderedVaryings.putIfAbsent(entry.getKey(), entry.getValue());
        }

        LinkedHashMap<String, String> numericTypes = new LinkedHashMap<String, String>();
        collectVertexUniforms(vertex, numericTypes);
        String secondarySampler = collectFragmentUniforms(fragment, numericTypes);
        if (numericTypes.size() > VulkanShaderState.MAX_CUSTOM_FLOATS / 4) {
            throw new IllegalArgumentException("custom program exceeds five numeric uniforms");
        }
        List<UniformSlot> uniforms = new ArrayList<UniformSlot>();
        int offset = 0;
        for (Map.Entry<String, String> entry : numericTypes.entrySet()) {
            uniforms.add(new UniformSlot(entry.getKey(), components(entry.getValue()), offset));
            offset += 4;
        }

        String vertexBody = stripDeclarations(vertex, true);
        vertexBody = replaceWord(vertexBody, "gl_Vertex", "rf_vertex");
        vertexBody = replaceWord(vertexBody, "gl_Color", "rf_color");
        vertexBody = replaceWord(vertexBody, "gl_MultiTexCoord0", "rf_texCoord0");
        vertexBody = replaceWord(vertexBody, "gl_ModelViewMatrix", "mat4(1.0)");
        vertexBody = replaceWord(vertexBody, "gl_ProjectionMatrix", "mat4(1.0)");
        vertexBody = replaceWord(vertexBody, "a_position", "rf_vertex");
        vertexBody = replaceWord(vertexBody, "a_color", "rf_color");
        vertexBody = replaceWord(vertexBody, "a_texCoord0", "inUv");
        vertexBody = replaceWord(vertexBody, "u_projTrans", "mat4(1.0)");

        String fragmentBody = stripDeclarations(fragment, false)
                .replace("texture2D", "texture")
                .replace("gl_FragColor", "rf_outColor");

        StringBuilder vertexPrefix = new StringBuilder(1024);
        vertexPrefix.append("#version 450\n")
                .append("layout(location=0) in vec2 inPosition;\n")
                .append("layout(location=1) in vec2 inUv;\n")
                .append("layout(location=2) in vec4 inColor;\n")
                .append("#define rf_vertex vec4(inPosition,0.0,1.0)\n")
                .append("#define rf_color inColor\n")
                .append("#define rf_texCoord0 vec4(inUv,0.0,1.0)\n")
                .append(pushConstantBlock());
        appendVaryings(vertexPrefix, orderedVaryings, "out");
        appendUniformAliases(vertexPrefix, uniforms);

        StringBuilder fragmentPrefix = new StringBuilder(1024);
        fragmentPrefix.append("#version 450\n")
                .append("layout(set=0,binding=0) uniform sampler2D u_texture;\n");
        if (secondarySampler != null) {
            fragmentPrefix.append("layout(set=0,binding=1) uniform sampler2D ")
                    .append(secondarySampler).append(";\n");
        }
        fragmentPrefix.append(pushConstantBlock());
        appendVaryings(fragmentPrefix, orderedVaryings, "in");
        fragmentPrefix.append("layout(location=0) out vec4 rf_outColor;\n");
        appendUniformAliases(fragmentPrefix, uniforms);

        return new Result(vertexPrefix.append(vertexBody).toString(),
                fragmentPrefix.append(fragmentBody).toString(), uniforms,
                secondarySampler);
    }

    private static void collectVertexUniforms(String source, Map<String, String> numeric) {
        Matcher matcher = UNIFORM.matcher(source);
        while (matcher.find()) {
            String type = matcher.group(1);
            String name = matcher.group(2);
            if ("mat4".equals(type) && "u_projTrans".equals(name)) continue;
            if ("sampler2D".equals(type) || "mat4".equals(type)) {
                throw new IllegalArgumentException("unsupported vertex uniform " + type
                        + " " + name);
            }
            putUniform(numeric, name, type);
        }
    }

    private static String collectFragmentUniforms(String source, Map<String, String> numeric) {
        String secondary = null;
        Matcher matcher = UNIFORM.matcher(source);
        while (matcher.find()) {
            String type = matcher.group(1);
            String name = matcher.group(2);
            if ("sampler2D".equals(type)) {
                if (!"u_texture".equals(name)) {
                    if (secondary != null && !secondary.equals(name)) {
                        throw new IllegalArgumentException(
                                "only one secondary sampler2D is currently supported");
                    }
                    secondary = name;
                }
            } else if ("mat4".equals(type)) {
                throw new IllegalArgumentException("unsupported fragment uniform mat4 " + name);
            } else {
                putUniform(numeric, name, type);
            }
        }
        return secondary;
    }

    private static void putUniform(Map<String, String> uniforms, String name, String type) {
        String old = uniforms.putIfAbsent(name, type);
        if (old != null && !old.equals(type)) {
            throw new IllegalArgumentException("uniform type mismatch for " + name);
        }
    }

    private static LinkedHashMap<String, String> varyings(String source) {
        LinkedHashMap<String, String> result = new LinkedHashMap<String, String>();
        Matcher matcher = VARYING.matcher(source);
        while (matcher.find()) {
            String old = result.putIfAbsent(matcher.group(2), matcher.group(1));
            if (old != null && !old.equals(matcher.group(1))) {
                throw new IllegalArgumentException("varying declared with multiple types: "
                        + matcher.group(2));
            }
        }
        return result;
    }

    private static void addIfPresent(Map<String, String> destination,
                                     Map<String, String> source, String name) {
        if (source.containsKey(name)) destination.put(name, source.get(name));
    }

    private static String stripDeclarations(String source, boolean vertex) {
        String result = VERSION.matcher(source).replaceAll("");
        result = PRECISION.matcher(result).replaceAll("");
        result = VARYING.matcher(result).replaceAll("");
        result = UNIFORM.matcher(result).replaceAll("");
        if (vertex) result = ATTRIBUTE.matcher(result).replaceAll("");
        return result;
    }

    private static String pushConstantBlock() {
        return "layout(push_constant) uniform RFShaderState {\n"
                + "  vec4 teamColor; float teamColorAmount; int effect; vec2 resolution;\n"
                + "  float displacementOffset; float uiScaling; vec2 screenBaseSize;\n"
                + "  vec4 customValues[5];\n"
                + "} rf;\n";
    }

    private static void appendVaryings(StringBuilder target,
                                       Map<String, String> varyings, String qualifier) {
        int location = 0;
        for (Map.Entry<String, String> entry : varyings.entrySet()) {
            target.append("layout(location=").append(location++).append(") ")
                    .append(qualifier).append(' ').append(entry.getValue()).append(' ')
                    .append(entry.getKey()).append(";\n");
        }
    }

    private static void appendUniformAliases(StringBuilder target, List<UniformSlot> uniforms) {
        for (int index = 0; index < uniforms.size(); index++) {
            UniformSlot slot = uniforms.get(index);
            target.append("#define ").append(slot.name).append(" rf.customValues[")
                    .append(index).append(']');
            if (slot.components == 1) target.append(".x");
            else if (slot.components == 2) target.append(".xy");
            else if (slot.components == 3) target.append(".xyz");
            target.append('\n');
        }
    }

    private static String replaceWord(String source, String from, String to) {
        return source.replaceAll("\\b" + Pattern.quote(from) + "\\b",
                Matcher.quoteReplacement(to));
    }

    private static String withoutComments(String source) {
        return source.replaceAll("(?s)/\\*.*?\\*/", "")
                .replaceAll("(?m)//.*$", "");
    }

    private static void requireSource(String stage, String source) {
        if (source == null || source.trim().isEmpty()) {
            throw new IllegalArgumentException("custom " + stage + " shader source is empty");
        }
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

        private UniformSlot(String name, int components, int offset) {
            this.name = name;
            this.components = components;
            this.offset = offset;
        }

        public String name() { return name; }
        public int components() { return components; }
        public int offset() { return offset; }
    }

    public static final class Result {
        private final String vertexSource;
        private final String fragmentSource;
        private final List<UniformSlot> uniforms;
        private final String secondarySampler;

        private Result(String vertexSource, String fragmentSource,
                       List<UniformSlot> uniforms, String secondarySampler) {
            this.vertexSource = vertexSource;
            this.fragmentSource = fragmentSource;
            this.uniforms = Collections.unmodifiableList(
                    new ArrayList<UniformSlot>(uniforms));
            this.secondarySampler = secondarySampler;
        }

        public String vertexSource() { return vertexSource; }
        public String fragmentSource() { return fragmentSource; }
        public List<UniformSlot> uniforms() { return uniforms; }
        public String secondarySampler() { return secondarySampler; }
    }
}
