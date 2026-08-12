package io.github.endx.vulkanmod;

import io.github.endx.vulkanmod.framestream.FrameResourceHandle;
import io.github.endx.vulkanmod.resourcestream.ResourceStreamFormat;
import io.github.endx.vulkanmod.resourcestream.ResourceStreamReader;
import io.github.endx.vulkanmod.resourcestream.ResourceUploadArenaPool;
import io.github.endx.vulkanmod.spi.VulkanTextureData;
import io.github.endx.vulkanmod.spi.VulkanCustomShaderProgram;
import io.github.endx.vulkanmod.spi.VulkanResourceStreamResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import java.nio.ByteBuffer;

/** Verifies the shared client emits contiguous streams and never resurrects stale handles. */
public final class ResourceStreamClientVerification {
    private ResourceStreamClientVerification() { }

    public static void main(String[] arguments) {
        List<ResourceStreamReader> streams = new ArrayList<ResourceStreamReader>();
        Map<Long, ByteBuffer> arenas = new LinkedHashMap<Long, ByteBuffer>();
        ResourceStreamClient client = new ResourceStreamClient(bytes -> {
            ResourceStreamReader stream = ResourceStreamReader.read(bytes);
            streams.add(stream);
            if (stream.completionId() != 0L) {
                return VulkanResourceStreamResult.textureReadback(stream.lastSequence(),
                        stream.completionId(), new VulkanTextureData(2, 3, new byte[24]));
            }
            return VulkanResourceStreamResult.applied(stream.lastSequence());
        }, new ResourceUploadArenaPool.Registry() {
            @Override public void register(long arenaId, ByteBuffer memory) {
                require(arenas.put(arenaId, memory) == null, "duplicate registered arena");
            }
            @Override public void unregister(long arenaId) {
                require(arenas.remove(arenaId) != null, "unknown unregistered arena");
            }
        }, 2, 1024, 0);
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
        require((streams.get(0).record(1).flags()
                        & ResourceStreamFormat.RECORD_HAS_EXTERNAL_PAYLOAD) != 0,
                "large-payload path did not emit an external texture record");
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
        client.updateTextureRegion(replacement, 1, 1, pixel);
        require(client.requiredForNextFrame() == 6L
                        && streams.get(streams.size() - 1).record(0).type()
                        == ResourceStreamFormat.TEXTURE_REGION_UPDATE,
                "partial texture update stream changed");
        VulkanTextureData readback = client.readTexture(replacement);
        require(readback.width() == 2 && readback.height() == 3
                        && client.requiredForNextFrame() == 7L,
                "ResourceStream readback completion changed");
        long shader = client.compileShaderProgram(new VulkanCustomShaderProgram("test",
                "void main(){gl_Position=vec4(0.0);}", "void main(){}"));
        require(FrameResourceHandle.type(shader) == FrameResourceHandle.TYPE_SHADER_PROGRAM
                        && client.shaderUsesExpandedVertexInput(shader)
                        && client.requiredForNextFrame() == 8L,
                "shader-program stream changed");
        client.destroyShader(shader);
        require(client.requiredForNextFrame() == 9L,
                "shader destroy stream changed");
        client.close();
        require(arenas.isEmpty(), "ResourceStream client left upload arenas registered");

        ResourceStreamClient broken = new ResourceStreamClient(
                bytes -> VulkanResourceStreamResult.applied(0L));
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
