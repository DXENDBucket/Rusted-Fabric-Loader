package io.github.endx.vulkanmod;

import io.github.endx.vulkanmod.framestream.FrameResourceHandle;
import io.github.endx.vulkanmod.resourcestream.ResourceStreamFormat;
import io.github.endx.vulkanmod.resourcestream.ResourceStreamReader;
import io.github.endx.vulkanmod.spi.VulkanTextureData;
import io.github.endx.vulkanmod.spi.VulkanCustomShaderProgram;

import java.util.ArrayList;
import java.util.List;

/** Verifies the shared client emits contiguous streams and never resurrects stale handles. */
public final class ResourceStreamClientVerification {
    private ResourceStreamClientVerification() { }

    public static void main(String[] arguments) {
        List<ResourceStreamReader> streams = new ArrayList<ResourceStreamReader>();
        ResourceStreamClient client = new ResourceStreamClient(bytes -> {
            ResourceStreamReader stream = ResourceStreamReader.read(bytes);
            streams.add(stream);
            return stream.lastSequence();
        });
        VulkanTextureData pixel = new VulkanTextureData(1, 1,
                new byte[] {1, 2, 3, 4});
        long first = client.uploadTexture(pixel);
        require(FrameResourceHandle.type(first) == FrameResourceHandle.TYPE_TEXTURE
                        && FrameResourceHandle.generation(first) == 1,
                "client did not allocate a typed generation-one texture");
        require(streams.get(0).firstSequence() == 1L
                        && streams.get(0).lastSequence() == 2L
                        && client.requiredForNextFrame() == 2L,
                "create/upload resource sequence changed");
        client.updateTexture(first, pixel);
        require(streams.get(1).record(0).type()
                        == ResourceStreamFormat.TEXTURE_REGION_UPDATE
                        && client.requiredForNextFrame() == 3L,
                "texture update stream changed");
        client.destroyTexture(first);
        require(client.requiredForNextFrame() == 4L,
                "texture destroy sequence changed");
        long replacement = client.createRenderTarget(2, 3);
        require(FrameResourceHandle.slot(replacement) == FrameResourceHandle.slot(first)
                        && FrameResourceHandle.generation(replacement) == 2,
                "destroyed client slot did not advance generation");
        require(client.isRenderTarget(replacement) && client.requiredForNextFrame() == 5L,
                "render-target stream changed");
        long shader = client.compileShaderProgram(new VulkanCustomShaderProgram("test",
                "void main(){gl_Position=vec4(0.0);}", "void main(){}"));
        require(FrameResourceHandle.type(shader) == FrameResourceHandle.TYPE_SHADER_PROGRAM
                        && client.shaderUsesExpandedVertexInput(shader)
                        && client.requiredForNextFrame() == 6L,
                "shader-program stream changed");
        client.destroyShader(shader);
        require(client.requiredForNextFrame() == 7L,
                "shader destroy stream changed");

        ResourceStreamClient broken = new ResourceStreamClient(bytes -> 0L);
        expectFailure(() -> broken.uploadTexture(pixel), "wrong applied sequence was accepted");
        expectFailure(() -> broken.createRenderTarget(1, 1),
                "faulted ordered client accepted later work");
        System.out.println("RustedVK shared ResourceStream client contracts passed");
    }

    private static void expectFailure(Runnable operation, String message) {
        try {
            operation.run();
            throw new AssertionError(message);
        } catch (IllegalStateException expected) {
            // Expected.
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
