package io.github.endx.vulkanmod.render;

import io.github.endx.vulkanmod.VulkanRuntime;
import io.github.endx.vulkanmod.spi.VulkanCustomShaderProgram;
import io.github.endx.vulkanmod.spi.VulkanShaderState;
import rustedwarfare.client.render.GameImage;
import rustedwarfare.render.ShaderParameter;
import rustedwarfare.render.ShaderProgram;

import java.util.List;

/** Owns the Vulkan handle and per-draw uniform snapshots for one legacy fragment program. */
final class NativeCustomShaderBinding {
    private final long handle;
    private final String vertexSource;
    private final String fragmentSource;
    private final List<LegacyShaderProgramTranslator.UniformSlot> uniforms;
    private final String secondarySampler;

    private NativeCustomShaderBinding(long handle, ShaderProgram shader,
                                      LegacyShaderProgramTranslator.Result translation) {
        this.handle = handle;
        this.vertexSource = shader.vertexSource;
        this.fragmentSource = shader.fragmentSource;
        this.uniforms = translation.uniforms();
        this.secondarySampler = translation.secondarySampler();
    }

    static NativeCustomShaderBinding compile(ShaderProgram shader) {
        LegacyShaderProgramTranslator.Result translation =
                LegacyShaderProgramTranslator.translate(
                        shader.vertexSource, shader.fragmentSource);
        long handle = VulkanRuntime.compileNativeShaderProgram(
                new VulkanCustomShaderProgram(shader.name,
                        translation.vertexSource(), translation.fragmentSource()));
        return new NativeCustomShaderBinding(handle, shader, translation);
    }

    boolean matches(ShaderProgram shader) {
        return same(vertexSource, shader.vertexSource) && same(fragmentSource, shader.fragmentSource);
    }

    VulkanShaderState snapshot(ShaderProgram shader) {
        float[] values = new float[VulkanShaderState.MAX_CUSTOM_FLOATS];
        for (LegacyShaderProgramTranslator.UniformSlot slot : uniforms) {
            ShaderParameter parameter = parameter(shader, slot.name());
            if (parameter == null || parameter.floatValues == null) continue;
            int count = Math.min(slot.components(), parameter.floatValues.length);
            for (int index = 0; index < count; index++) {
                float value = parameter.floatValues[index];
                values[slot.offset() + index] = Float.isFinite(value) ? value : 0.0f;
            }
        }
        long secondary = 0L;
        if (secondarySampler != null) {
            ShaderParameter parameter = parameter(shader, secondarySampler);
            if (parameter != null && parameter.texture != null) {
                GameImage image = parameter.texture.getRealImage();
                if (image == null) image = parameter.texture;
                if (image instanceof VulkanGameImage) {
                    ((VulkanGameImage) image).submitPendingNativeDraws();
                }
                secondary = VulkanRuntime.textureForGameImage(image);
            }
        }
        return VulkanShaderState.custom(handle, secondary, values);
    }

    void destroy() {
        VulkanRuntime.destroyNativeShaderProgram(handle);
    }

    private static ShaderParameter parameter(ShaderProgram shader, String name) {
        for (ShaderParameter parameter : shader.parameters) {
            if (parameter != null && name.equals(parameter.name)) return parameter;
        }
        return null;
    }

    private static boolean same(String left, String right) {
        return left == null ? right == null : left.equals(right);
    }
}
