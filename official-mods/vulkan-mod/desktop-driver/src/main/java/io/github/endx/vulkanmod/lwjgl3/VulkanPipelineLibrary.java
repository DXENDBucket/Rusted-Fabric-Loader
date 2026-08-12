package io.github.endx.vulkanmod.lwjgl3;

import io.github.endx.vulkanmod.spi.VulkanBlendMode;
import io.github.endx.vulkanmod.spi.VulkanCustomFragmentShader;
import io.github.endx.vulkanmod.spi.VulkanCustomShaderProgram;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.VkDevice;
import org.lwjgl.vulkan.VkGraphicsPipelineCreateInfo;
import org.lwjgl.vulkan.VkPipelineColorBlendAttachmentState;
import org.lwjgl.vulkan.VkPipelineColorBlendStateCreateInfo;
import org.lwjgl.vulkan.VkPipelineDynamicStateCreateInfo;
import org.lwjgl.vulkan.VkPipelineInputAssemblyStateCreateInfo;
import org.lwjgl.vulkan.VkPipelineLayoutCreateInfo;
import org.lwjgl.vulkan.VkPipelineMultisampleStateCreateInfo;
import org.lwjgl.vulkan.VkPipelineRasterizationStateCreateInfo;
import org.lwjgl.vulkan.VkPipelineShaderStageCreateInfo;
import org.lwjgl.vulkan.VkPipelineVertexInputStateCreateInfo;
import org.lwjgl.vulkan.VkPipelineViewportStateCreateInfo;
import org.lwjgl.vulkan.VkPushConstantRange;
import org.lwjgl.vulkan.VkShaderModuleCreateInfo;
import org.lwjgl.vulkan.VkVertexInputAttributeDescription;
import org.lwjgl.vulkan.VkVertexInputBindingDescription;

import java.nio.ByteBuffer;
import java.nio.LongBuffer;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.lwjgl.util.shaderc.Shaderc.*;
import static org.lwjgl.vulkan.VK10.*;

/** Owns render-pass-compatible graphics pipelines, layouts, and custom shader registrations. */
final class VulkanPipelineLibrary implements AutoCloseable {
    private static final int COLORED_VERTEX_STRIDE = 6 * Float.BYTES;
    private static final int TEXTURED_VERTEX_STRIDE = 8 * Float.BYTES;
    private static final int CUSTOM_TEXTURED_VERTEX_STRIDE = 16 * Float.BYTES;
    private static final String COLOR_VERTEX_SHADER = "#version 450\n"
            + "layout(location=0) in vec2 inPosition;\n"
            + "layout(location=1) in vec4 inColor;\n"
            + "layout(location=0) out vec4 color;\n"
            + "void main(){ gl_Position=vec4(inPosition,0.0,1.0); color=inColor; }\n";
    private static final String COLOR_FRAGMENT_SHADER = "#version 450\n"
            + "layout(location=0) in vec4 color;\n"
            + "layout(location=0) out vec4 outColor;\n"
            + "void main(){ outColor=color; }\n";
    private static final String TEXTURE_VERTEX_SHADER = "#version 450\n"
            + "layout(location=0) in vec2 inPosition;\n"
            + "layout(location=1) in vec2 inUv;\n"
            + "layout(location=2) in vec4 inColor;\n"
            + "layout(location=0) out vec2 uv;\n"
            + "layout(location=1) out vec4 color;\n"
            + "void main(){ gl_Position=vec4(inPosition,0.0,1.0); uv=inUv; color=inColor; }\n";
    private static final String TEXTURE_FRAGMENT_SHADER = "#version 450\n"
            + "layout(set=0,binding=0) uniform sampler2D image;\n"
            + "layout(set=0,binding=1) uniform sampler2D secondaryImage;\n"
            + "layout(push_constant) uniform ShaderState {\n"
            + "  vec4 teamColor;\n"
            + "  float teamColorAmount;\n"
            + "  int effect;\n"
            + "  vec2 resolution;\n"
            + "  float displacementOffset;\n"
            + "  float uiScaling;\n"
            + "  vec2 screenBaseSize;\n"
            + "} shaderState;\n"
            + "layout(location=0) in vec2 uv;\n"
            + "layout(location=1) in vec4 color;\n"
            + "layout(location=0) out vec4 outColor;\n"
            + "void main(){\n"
            + "  vec4 sampled=texture(image,uv);\n"
            + "  if(shaderState.effect==1){\n"
            + "    float threshold=0.04;\n"
            + "    if(sampled.g>0.0 && abs(sampled.r-sampled.b)<=threshold){\n"
            + "      float lightness=sampled.r;\n"
            + "      float greenness=sampled.g-lightness;\n"
            + "      sampled.rgb=vec3(lightness)+shaderState.teamColor.rgb*greenness;\n"
            + "    }\n"
            + "  }else if(shaderState.effect==2){\n"
            + "    sampled.rgb+=shaderState.teamColor.rgb*shaderState.teamColorAmount;\n"
            + "  }else if(shaderState.effect==3){\n"
            + "    float hueness=max(abs(sampled.r-sampled.g),\n"
            + "      max(abs(sampled.g-sampled.b),abs(sampled.b-sampled.r)));\n"
            + "    if(hueness>(15.0/256.0)){\n"
            + "      float lightness=min(sampled.r,min(sampled.g,sampled.b));\n"
            + "      sampled.rgb=vec3(lightness)+shaderState.teamColor.rgb*hueness;\n"
            + "    }\n"
            + "  }else if(shaderState.effect==4){\n"
            + "    sampled.a=1.0;\n"
            + "  }else if(shaderState.effect==5){\n"
            + "    vec2 usedScreenSize=shaderState.resolution/shaderState.screenBaseSize;\n"
            + "    vec2 screenUv=gl_FragCoord.xy/(shaderState.resolution*shaderState.uiScaling);\n"
            + "    screenUv*=usedScreenSize;\n"
            + "    vec2 screenOffset=shaderState.displacementOffset\n"
            + "      *(sampled.xy-vec2(128.0/255.0))*sampled.a*color.a;\n"
            + "    vec2 displacedUv=clamp(screenUv+screenOffset,vec2(0.0),usedScreenSize);\n"
            + "    outColor=texture(secondaryImage,displacedUv);\n"
            + "    return;\n"
            + "  }else if(shaderState.effect==6){\n"
            + "    sampled.rgb=vec3(1.0,1.0,0.0);\n"
            + "  }\n"
            + "  outColor=sampled*color;\n"
            + "}\n";

    private final VkDevice device;
    private final VulkanDescriptorAllocator descriptors;
    private final long[] colorPipelines = new long[VulkanBlendMode.values().length];
    private final long[] texturePipelines = new long[VulkanBlendMode.values().length];
    private final Map<Long, CustomShader> customShaders =
            new LinkedHashMap<Long, CustomShader>();
    private long renderPass;
    private long colorPipelineLayout;
    private long texturePipelineLayout;
    private long nextCustomShaderHandle = 1L;
    private long pipelineCreates;
    private long pipelineCacheHits;
    private long pipelineDestroys;
    private long shaderModuleCreates;
    private long shaderModuleDestroys;
    private long renderPassChanges;
    private boolean closed;

    VulkanPipelineLibrary(VkDevice device, VulkanDescriptorAllocator descriptors) {
        if (device == null) throw new NullPointerException("device");
        if (descriptors == null) throw new NullPointerException("descriptors");
        this.device = device;
        this.descriptors = descriptors;
    }

    void setRenderPass(long compatibleRenderPass) {
        ensureOpen();
        if (compatibleRenderPass == VK_NULL_HANDLE) {
            throw new IllegalArgumentException("render pass must be valid");
        }
        if (renderPass == compatibleRenderPass) return;
        releaseRenderPass();
        renderPass = compatibleRenderPass;
        renderPassChanges++;
    }

    void releaseRenderPass() {
        destroyPipelines(colorPipelines);
        destroyPipelines(texturePipelines);
        for (CustomShader shader : customShaders.values()) destroyPipelines(shader.pipelines);
        renderPass = VK_NULL_HANDLE;
    }

    long compileFragmentShader(VulkanCustomFragmentShader shader) {
        if (shader == null) throw new NullPointerException("shader");
        validateShader(shader.source(), shaderc_glsl_fragment_shader, shader.name() + ".frag");
        return register(new CustomShader(shader.name(), TEXTURE_VERTEX_SHADER,
                shader.source(), false));
    }

    long compileShaderProgram(VulkanCustomShaderProgram program) {
        if (program == null) throw new NullPointerException("program");
        validateShader(program.vertexSource(), shaderc_glsl_vertex_shader,
                program.name() + ".vert");
        validateShader(program.fragmentSource(), shaderc_glsl_fragment_shader,
                program.name() + ".frag");
        return register(new CustomShader(program.name(), program.vertexSource(),
                program.fragmentSource(), true));
    }

    void destroyShader(long handle) {
        CustomShader shader = customShaders.remove(Long.valueOf(handle));
        if (shader != null) destroyPipelines(shader.pipelines);
    }

    boolean usesExpandedVertexInput(long handle) {
        return requireShader(handle).expandedVertexInput;
    }

    long colorPipeline(MemoryStack stack, VulkanBlendMode blendMode) {
        requireRenderPass();
        int index = blendMode.ordinal();
        if (colorPipelines[index] != VK_NULL_HANDLE) {
            pipelineCacheHits++;
            return colorPipelines[index];
        }
        ensureColorLayout(stack);
        colorPipelines[index] = createPipeline(stack, blendMode,
                COLOR_VERTEX_SHADER, COLOR_FRAGMENT_SHADER, "colored", false,
                colorPipelineLayout);
        return colorPipelines[index];
    }

    long texturePipeline(MemoryStack stack, VulkanBlendMode blendMode) {
        requireRenderPass();
        int index = blendMode.ordinal();
        if (texturePipelines[index] != VK_NULL_HANDLE) {
            pipelineCacheHits++;
            return texturePipelines[index];
        }
        ensureTextureLayout(stack);
        texturePipelines[index] = createPipeline(stack, blendMode,
                TEXTURE_VERTEX_SHADER, TEXTURE_FRAGMENT_SHADER, "textured", false,
                texturePipelineLayout);
        return texturePipelines[index];
    }

    long customPipeline(MemoryStack stack, VulkanBlendMode blendMode, long shaderHandle) {
        requireRenderPass();
        CustomShader shader = requireShader(shaderHandle);
        int index = blendMode.ordinal();
        if (shader.pipelines[index] != VK_NULL_HANDLE) {
            pipelineCacheHits++;
            return shader.pipelines[index];
        }
        ensureTextureLayout(stack);
        shader.pipelines[index] = createPipeline(stack, blendMode,
                shader.vertexSource, shader.fragmentSource, "custom-" + shader.name,
                shader.expandedVertexInput, texturePipelineLayout);
        return shader.pipelines[index];
    }

    long texturePipelineLayout() {
        if (texturePipelineLayout == VK_NULL_HANDLE) {
            throw new IllegalStateException("texture pipeline layout is unavailable");
        }
        return texturePipelineLayout;
    }

    void appendStatistics(Map<String, Long> statistics) {
        statistics.put("pipeline.creates", pipelineCreates);
        statistics.put("pipeline.cacheHits", pipelineCacheHits);
        statistics.put("pipeline.destroys", pipelineDestroys);
        statistics.put("pipeline.live", pipelineCreates - pipelineDestroys);
        statistics.put("pipeline.customShaders", (long) customShaders.size());
        statistics.put("pipeline.renderPassChanges", renderPassChanges);
        statistics.put("pipeline.shaderModuleCreates", shaderModuleCreates);
        statistics.put("pipeline.shaderModuleDestroys", shaderModuleDestroys);
    }

    @Override public void close() {
        if (closed) return;
        releaseRenderPass();
        customShaders.clear();
        if (texturePipelineLayout != VK_NULL_HANDLE) {
            vkDestroyPipelineLayout(device, texturePipelineLayout, null);
            texturePipelineLayout = VK_NULL_HANDLE;
        }
        if (colorPipelineLayout != VK_NULL_HANDLE) {
            vkDestroyPipelineLayout(device, colorPipelineLayout, null);
            colorPipelineLayout = VK_NULL_HANDLE;
        }
        closed = true;
        if (pipelineCreates != pipelineDestroys
                || shaderModuleCreates != shaderModuleDestroys) {
            throw new IllegalStateException("pipeline library leaked native resources");
        }
    }

    private long register(CustomShader shader) {
        ensureOpen();
        long handle = nextCustomShaderHandle++;
        if (handle <= 0L) throw new IllegalStateException("custom shader handles exhausted");
        customShaders.put(Long.valueOf(handle), shader);
        return handle;
    }

    private CustomShader requireShader(long handle) {
        CustomShader shader = customShaders.get(Long.valueOf(handle));
        if (shader == null) throw new IllegalArgumentException("unknown custom shader handle "
                + handle);
        return shader;
    }

    private void validateShader(String source, int kind, String name) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            long module = createShaderModule(stack, source, kind, name);
            destroyShaderModule(module);
        }
    }

    private long createPipeline(MemoryStack stack, VulkanBlendMode blendMode,
                                String vertexSource, String fragmentSource, String label,
                                boolean expandedVertexInput, long layout) {
        long vertexModule = VK_NULL_HANDLE;
        long fragmentModule = VK_NULL_HANDLE;
        try {
            vertexModule = createShaderModule(stack, vertexSource,
                    shaderc_glsl_vertex_shader, label + ".vert");
            fragmentModule = createShaderModule(stack, fragmentSource,
                    shaderc_glsl_fragment_shader, label + ".frag");
            VkPipelineShaderStageCreateInfo.Buffer stages =
                    VkPipelineShaderStageCreateInfo.calloc(2, stack);
            stages.get(0).sType$Default().stage(VK_SHADER_STAGE_VERTEX_BIT)
                    .module(vertexModule).pName(stack.UTF8("main"));
            stages.get(1).sType$Default().stage(VK_SHADER_STAGE_FRAGMENT_BIT)
                    .module(fragmentModule).pName(stack.UTF8("main"));

            boolean colored = layout == colorPipelineLayout;
            VkVertexInputBindingDescription.Buffer binding =
                    VkVertexInputBindingDescription.calloc(1, stack);
            binding.get(0).binding(0).stride(colored ? COLORED_VERTEX_STRIDE
                            : expandedVertexInput ? CUSTOM_TEXTURED_VERTEX_STRIDE
                            : TEXTURED_VERTEX_STRIDE)
                    .inputRate(VK_VERTEX_INPUT_RATE_VERTEX);
            int attributeCount = colored ? 2 : expandedVertexInput ? 6 : 3;
            VkVertexInputAttributeDescription.Buffer attributes =
                    VkVertexInputAttributeDescription.calloc(attributeCount, stack);
            attributes.get(0).location(0).binding(0)
                    .format(VK_FORMAT_R32G32_SFLOAT).offset(0);
            if (colored) {
                attributes.get(1).location(1).binding(0)
                        .format(VK_FORMAT_R32G32B32A32_SFLOAT).offset(2 * Float.BYTES);
            } else {
                attributes.get(1).location(1).binding(0)
                        .format(VK_FORMAT_R32G32_SFLOAT).offset(2 * Float.BYTES);
                attributes.get(2).location(2).binding(0)
                        .format(VK_FORMAT_R32G32B32A32_SFLOAT).offset(4 * Float.BYTES);
                if (expandedVertexInput) {
                    attributes.get(3).location(3).binding(0)
                            .format(VK_FORMAT_R32G32B32_SFLOAT).offset(8 * Float.BYTES);
                    attributes.get(4).location(4).binding(0)
                            .format(VK_FORMAT_R32G32B32_SFLOAT).offset(11 * Float.BYTES);
                    attributes.get(5).location(5).binding(0)
                            .format(VK_FORMAT_R32G32_SFLOAT).offset(14 * Float.BYTES);
                }
            }
            VkPipelineVertexInputStateCreateInfo vertexInput =
                    VkPipelineVertexInputStateCreateInfo.calloc(stack).sType$Default()
                            .pVertexBindingDescriptions(binding)
                            .pVertexAttributeDescriptions(attributes);
            VkPipelineInputAssemblyStateCreateInfo inputAssembly =
                    VkPipelineInputAssemblyStateCreateInfo.calloc(stack).sType$Default()
                            .topology(VK_PRIMITIVE_TOPOLOGY_TRIANGLE_LIST)
                            .primitiveRestartEnable(false);
            VkPipelineViewportStateCreateInfo viewport =
                    VkPipelineViewportStateCreateInfo.calloc(stack).sType$Default()
                            .viewportCount(1).scissorCount(1);
            VkPipelineRasterizationStateCreateInfo rasterizer =
                    VkPipelineRasterizationStateCreateInfo.calloc(stack).sType$Default()
                            .depthClampEnable(false).rasterizerDiscardEnable(false)
                            .polygonMode(VK_POLYGON_MODE_FILL).cullMode(VK_CULL_MODE_NONE)
                            .frontFace(VK_FRONT_FACE_COUNTER_CLOCKWISE)
                            .depthBiasEnable(false).lineWidth(1.0f);
            VkPipelineMultisampleStateCreateInfo multisampling =
                    VkPipelineMultisampleStateCreateInfo.calloc(stack).sType$Default()
                            .rasterizationSamples(VK_SAMPLE_COUNT_1_BIT)
                            .sampleShadingEnable(false);
            VkPipelineColorBlendAttachmentState.Buffer attachment =
                    VkPipelineColorBlendAttachmentState.calloc(1, stack);
            configureBlend(attachment.get(0), blendMode)
                    .colorWriteMask(VK_COLOR_COMPONENT_R_BIT | VK_COLOR_COMPONENT_G_BIT
                            | VK_COLOR_COMPONENT_B_BIT | VK_COLOR_COMPONENT_A_BIT);
            VkPipelineColorBlendStateCreateInfo blending =
                    VkPipelineColorBlendStateCreateInfo.calloc(stack).sType$Default()
                            .logicOpEnable(false).pAttachments(attachment);
            VkPipelineDynamicStateCreateInfo dynamic =
                    VkPipelineDynamicStateCreateInfo.calloc(stack).sType$Default()
                            .pDynamicStates(stack.ints(
                                    VK_DYNAMIC_STATE_VIEWPORT, VK_DYNAMIC_STATE_SCISSOR));
            VkGraphicsPipelineCreateInfo.Buffer createInfo =
                    VkGraphicsPipelineCreateInfo.calloc(1, stack);
            createInfo.get(0).sType$Default().pStages(stages)
                    .pVertexInputState(vertexInput).pInputAssemblyState(inputAssembly)
                    .pViewportState(viewport).pRasterizationState(rasterizer)
                    .pMultisampleState(multisampling).pColorBlendState(blending)
                    .pDynamicState(dynamic).layout(layout).renderPass(renderPass).subpass(0);
            LongBuffer handle = stack.mallocLong(1);
            check(vkCreateGraphicsPipelines(device, VK_NULL_HANDLE,
                    createInfo, null, handle), "vkCreateGraphicsPipelines(" + label + ")");
            pipelineCreates++;
            return handle.get(0);
        } finally {
            if (fragmentModule != VK_NULL_HANDLE) destroyShaderModule(fragmentModule);
            if (vertexModule != VK_NULL_HANDLE) destroyShaderModule(vertexModule);
        }
    }

    private void ensureColorLayout(MemoryStack stack) {
        if (colorPipelineLayout != VK_NULL_HANDLE) return;
        VkPipelineLayoutCreateInfo info =
                VkPipelineLayoutCreateInfo.calloc(stack).sType$Default();
        LongBuffer handle = stack.mallocLong(1);
        check(vkCreatePipelineLayout(device, info, null, handle),
                "vkCreatePipelineLayout(colored)");
        colorPipelineLayout = handle.get(0);
    }

    private void ensureTextureLayout(MemoryStack stack) {
        if (texturePipelineLayout != VK_NULL_HANDLE) return;
        descriptors.ensureInitialized(stack);
        VkPushConstantRange.Buffer pushConstants = VkPushConstantRange.calloc(1, stack);
        pushConstants.get(0).stageFlags(VK_SHADER_STAGE_VERTEX_BIT
                        | VK_SHADER_STAGE_FRAGMENT_BIT)
                .offset(0).size(128);
        VkPipelineLayoutCreateInfo info = VkPipelineLayoutCreateInfo.calloc(stack)
                .sType$Default().pSetLayouts(stack.longs(descriptors.layout()))
                .pPushConstantRanges(pushConstants);
        LongBuffer handle = stack.mallocLong(1);
        check(vkCreatePipelineLayout(device, info, null, handle),
                "vkCreatePipelineLayout(texture)");
        texturePipelineLayout = handle.get(0);
    }

    private long createShaderModule(MemoryStack stack, String source, int kind, String name) {
        long compiler = shaderc_compiler_initialize();
        if (compiler == MemoryUtil.NULL) {
            throw new IllegalStateException("shaderc_compiler_initialize failed");
        }
        long result = MemoryUtil.NULL;
        try {
            result = shaderc_compile_into_spv(
                    compiler, source, kind, name, "main", MemoryUtil.NULL);
            if (result == MemoryUtil.NULL) {
                throw new IllegalStateException("shaderc_compile_into_spv returned null");
            }
            int status = shaderc_result_get_compilation_status(result);
            if (status != shaderc_compilation_status_success) {
                throw new IllegalStateException("Could not compile " + name + ": "
                        + shaderc_result_get_error_message(result));
            }
            ByteBuffer code = shaderc_result_get_bytes(result);
            VkShaderModuleCreateInfo info = VkShaderModuleCreateInfo.calloc(stack)
                    .sType$Default().pCode(code);
            LongBuffer handle = stack.mallocLong(1);
            check(vkCreateShaderModule(device, info, null, handle),
                    "vkCreateShaderModule(" + name + ")");
            shaderModuleCreates++;
            return handle.get(0);
        } finally {
            if (result != MemoryUtil.NULL) shaderc_result_release(result);
            shaderc_compiler_release(compiler);
        }
    }

    private void destroyShaderModule(long module) {
        vkDestroyShaderModule(device, module, null);
        shaderModuleDestroys++;
    }

    private void destroyPipelines(long[] pipelines) {
        for (int index = 0; index < pipelines.length; index++) {
            if (pipelines[index] != VK_NULL_HANDLE) {
                vkDestroyPipeline(device, pipelines[index], null);
                pipelines[index] = VK_NULL_HANDLE;
                pipelineDestroys++;
            }
        }
    }

    private void requireRenderPass() {
        ensureOpen();
        if (renderPass == VK_NULL_HANDLE) {
            throw new IllegalStateException("pipeline library has no compatible render pass");
        }
    }

    private void ensureOpen() {
        if (closed) throw new IllegalStateException("pipeline library is closed");
    }

    private static VkPipelineColorBlendAttachmentState configureBlend(
            VkPipelineColorBlendAttachmentState attachment, VulkanBlendMode blendMode) {
        attachment.blendEnable(true).colorBlendOp(VK_BLEND_OP_ADD).alphaBlendOp(VK_BLEND_OP_ADD);
        switch (blendMode) {
            case ADDITIVE:
                return attachment.srcColorBlendFactor(VK_BLEND_FACTOR_SRC_ALPHA)
                        .dstColorBlendFactor(VK_BLEND_FACTOR_ONE)
                        .srcAlphaBlendFactor(VK_BLEND_FACTOR_SRC_ALPHA)
                        .dstAlphaBlendFactor(VK_BLEND_FACTOR_ONE);
            case COPY:
                return attachment.srcColorBlendFactor(VK_BLEND_FACTOR_ONE)
                        .dstColorBlendFactor(VK_BLEND_FACTOR_ONE)
                        .srcAlphaBlendFactor(VK_BLEND_FACTOR_ONE)
                        .dstAlphaBlendFactor(VK_BLEND_FACTOR_ONE);
            case MODULATE:
                return attachment.srcColorBlendFactor(VK_BLEND_FACTOR_DST_COLOR)
                        .dstColorBlendFactor(VK_BLEND_FACTOR_ONE_MINUS_SRC_ALPHA)
                        .srcAlphaBlendFactor(VK_BLEND_FACTOR_DST_COLOR)
                        .dstAlphaBlendFactor(VK_BLEND_FACTOR_ONE_MINUS_SRC_ALPHA);
            case NORMAL:
            default:
                return attachment.srcColorBlendFactor(VK_BLEND_FACTOR_SRC_ALPHA)
                        .dstColorBlendFactor(VK_BLEND_FACTOR_ONE_MINUS_SRC_ALPHA)
                        .srcAlphaBlendFactor(VK_BLEND_FACTOR_ONE)
                        .dstAlphaBlendFactor(VK_BLEND_FACTOR_ONE_MINUS_SRC_ALPHA);
        }
    }

    private static void check(int result, String operation) {
        if (result != VK_SUCCESS) {
            throw new IllegalStateException(operation + " failed with VkResult " + result);
        }
    }

    private static final class CustomShader {
        private final String name;
        private final String vertexSource;
        private final String fragmentSource;
        private final boolean expandedVertexInput;
        private final long[] pipelines = new long[VulkanBlendMode.values().length];

        private CustomShader(String name, String vertexSource, String fragmentSource,
                             boolean expandedVertexInput) {
            this.name = name;
            this.vertexSource = vertexSource;
            this.fragmentSource = fragmentSource;
            this.expandedVertexInput = expandedVertexInput;
        }
    }
}
