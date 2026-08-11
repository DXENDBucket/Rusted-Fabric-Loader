package io.github.endx.vulkanmod.spi;

import java.util.ArrayDeque;

/** Thread-confined storage behind pooled native frame builders. */
final class VulkanFrameCommandPool {
    private static final ThreadLocal<VulkanFrameCommandPool> LOCAL =
            ThreadLocal.withInitial(VulkanFrameCommandPool::new);

    private final ArrayDeque<VulkanFrameCommands.CommandArena> arenas =
            new ArrayDeque<VulkanFrameCommands.CommandArena>();
    private final ArrayDeque<VulkanColoredQuad> coloredQuads =
            new ArrayDeque<VulkanColoredQuad>();
    private final ArrayDeque<VulkanTexturedQuad> texturedQuads =
            new ArrayDeque<VulkanTexturedQuad>();
    private final ArrayDeque<VulkanColoredTriangle> coloredTriangles =
            new ArrayDeque<VulkanColoredTriangle>();
    private final ArrayDeque<VulkanTexturedTriangle> texturedTriangles =
            new ArrayDeque<VulkanTexturedTriangle>();

    static VulkanFrameCommandPool current() { return LOCAL.get(); }

    VulkanFrameCommands.CommandArena acquireArena() {
        VulkanFrameCommands.CommandArena arena = arenas.pollFirst();
        return arena == null ? new VulkanFrameCommands.CommandArena() : arena;
    }

    void recycleArena(VulkanFrameCommands.CommandArena arena) {
        arena.size = 0;
        arenas.addFirst(arena);
    }

    VulkanColoredQuad acquireColoredQuad() {
        VulkanColoredQuad command = coloredQuads.pollFirst();
        return command == null ? new VulkanColoredQuad() : command;
    }

    VulkanTexturedQuad acquireTexturedQuad() {
        VulkanTexturedQuad command = texturedQuads.pollFirst();
        return command == null ? new VulkanTexturedQuad() : command;
    }

    VulkanColoredTriangle acquireColoredTriangle() {
        VulkanColoredTriangle command = coloredTriangles.pollFirst();
        return command == null ? new VulkanColoredTriangle() : command;
    }

    VulkanTexturedTriangle acquireTexturedTriangle() {
        VulkanTexturedTriangle command = texturedTriangles.pollFirst();
        return command == null ? new VulkanTexturedTriangle() : command;
    }

    void recycle(VulkanColoredQuad command) { coloredQuads.addFirst(command); }
    void recycle(VulkanTexturedQuad command) { texturedQuads.addFirst(command); }
    void recycle(VulkanColoredTriangle command) { coloredTriangles.addFirst(command); }
    void recycle(VulkanTexturedTriangle command) { texturedTriangles.addFirst(command); }
}
