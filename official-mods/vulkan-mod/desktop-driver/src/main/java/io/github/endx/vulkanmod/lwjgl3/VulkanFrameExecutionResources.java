package io.github.endx.vulkanmod.lwjgl3;

import io.github.endx.vulkanmod.lwjgl3.VulkanMemoryAllocator.BufferAllocation;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkCommandBuffer;
import org.lwjgl.vulkan.VkCommandBufferAllocateInfo;
import org.lwjgl.vulkan.VkCommandPoolCreateInfo;
import org.lwjgl.vulkan.VkDevice;
import org.lwjgl.vulkan.VkFenceCreateInfo;
import org.lwjgl.vulkan.VkSemaphoreCreateInfo;

import java.nio.LongBuffer;
import java.util.Map;

import static org.lwjgl.vulkan.VK10.*;

/** Owns swapchain-sized presentation slots and device-lifetime offscreen execution slots. */
final class VulkanFrameExecutionResources implements AutoCloseable {
    private static final int READBACK_COMMAND_BUFFERS = 1;

    private final VkDevice device;
    private final VulkanMemoryAllocator memory;
    private final int queueFamily;
    private final int offscreenSlotCount;

    private long mainCommandPool;
    private VkCommandBuffer[] mainCommands = new VkCommandBuffer[0];
    private long[] imageAvailableSemaphores = new long[0];
    private long[] mainFences = new long[0];
    private BufferAllocation[] mainVertexAllocations = new BufferAllocation[0];
    private int[] mainVertexCapacities = new int[0];
    private BufferAllocation[] mainUploadAllocations = new BufferAllocation[0];
    private int[] mainUploadCapacities = new int[0];
    private int mainCursor;

    private long offscreenCommandPool;
    private VkCommandBuffer[] offscreenCommands = new VkCommandBuffer[0];
    private VkCommandBuffer readbackCommand;
    private long[] offscreenFences = new long[0];
    private long[] offscreenOwnerFences = new long[0];
    private BufferAllocation[] offscreenVertexAllocations = new BufferAllocation[0];
    private int[] offscreenVertexCapacities = new int[0];
    private BufferAllocation[] offscreenUploadAllocations = new BufferAllocation[0];
    private int[] offscreenUploadCapacities = new int[0];
    private final BufferAllocation[] readbackUploadAllocations = new BufferAllocation[1];
    private final int[] readbackUploadCapacities = new int[1];
    private int offscreenCursor;

    private long mainGenerations;
    private long mainPoolCreates;
    private long mainPoolDestroys;
    private long offscreenPoolCreates;
    private long offscreenPoolDestroys;
    private long mainFenceCreates;
    private long mainFenceDestroys;
    private long mainSemaphoreCreates;
    private long mainSemaphoreDestroys;
    private long offscreenFenceCreates;
    private long offscreenFenceDestroys;
    private boolean initialized;
    private boolean closed;

    VulkanFrameExecutionResources(VkDevice device, VulkanMemoryAllocator memory,
                                  int queueFamily, int offscreenSlotCount) {
        if (device == null) throw new NullPointerException("device");
        if (memory == null) throw new NullPointerException("memory");
        if (queueFamily < 0) throw new IllegalArgumentException("queueFamily");
        if (offscreenSlotCount <= 0) throw new IllegalArgumentException("offscreenSlotCount");
        this.device = device;
        this.memory = memory;
        this.queueFamily = queueFamily;
        this.offscreenSlotCount = offscreenSlotCount;
    }

    void initialize(MemoryStack stack, int mainSlotCount) {
        ensureOpen();
        if (initialized) throw new IllegalStateException("execution resources already initialized");
        createOffscreenResources(stack);
        createMainGeneration(stack, mainSlotCount);
        initialized = true;
    }

    void replaceMainGeneration(MemoryStack stack, int mainSlotCount) {
        ensureInitialized();
        destroyMainGeneration();
        resetOffscreenOwners();
        createMainGeneration(stack, mainSlotCount);
    }

    int mainSlotCount() { ensureInitialized(); return mainCommands.length; }
    int offscreenSlotCount() { ensureInitialized(); return offscreenCommands.length; }

    int mainCursor() { ensureInitialized(); return mainCursor; }
    void advanceMainCursor() { mainCursor = (mainCursor + 1) % mainCommands.length; }
    int offscreenCursor() { ensureInitialized(); return offscreenCursor; }
    void advanceOffscreenCursor() {
        offscreenCursor = (offscreenCursor + 1) % offscreenCommands.length;
    }
    void advanceOffscreenCursor(int count) {
        offscreenCursor = (offscreenCursor + count) % offscreenCommands.length;
    }

    VkCommandBuffer mainCommand(int slot) { return mainCommands[requireMainSlot(slot)]; }
    long mainFence(int slot) { return mainFences[requireMainSlot(slot)]; }
    long imageAvailableSemaphore(int slot) {
        return imageAvailableSemaphores[requireMainSlot(slot)];
    }
    BufferAllocation[] mainVertexAllocations() { return mainVertexAllocations; }
    int[] mainVertexCapacities() { return mainVertexCapacities; }
    BufferAllocation[] mainUploadAllocations() { return mainUploadAllocations; }
    int[] mainUploadCapacities() { return mainUploadCapacities; }

    VkCommandBuffer offscreenCommand(int slot) {
        return offscreenCommands[requireOffscreenSlot(slot)];
    }
    VkCommandBuffer readbackCommand() { ensureInitialized(); return readbackCommand; }
    long offscreenFence(int slot) { return offscreenFences[requireOffscreenSlot(slot)]; }
    long offscreenOwnerFence(int slot) {
        return offscreenOwnerFences[requireOffscreenSlot(slot)];
    }
    void setOffscreenOwnerFence(int slot, long fence) {
        if (fence == VK_NULL_HANDLE) throw new IllegalArgumentException("owner fence");
        offscreenOwnerFences[requireOffscreenSlot(slot)] = fence;
    }
    BufferAllocation[] offscreenVertexAllocations() { return offscreenVertexAllocations; }
    int[] offscreenVertexCapacities() { return offscreenVertexCapacities; }
    BufferAllocation[] offscreenUploadAllocations() { return offscreenUploadAllocations; }
    int[] offscreenUploadCapacities() { return offscreenUploadCapacities; }
    BufferAllocation[] readbackUploadAllocations() { return readbackUploadAllocations; }
    int[] readbackUploadCapacities() { return readbackUploadCapacities; }

    boolean allFencesSignaled() {
        ensureInitialized();
        for (long fence : mainFences) {
            int status = vkGetFenceStatus(device, fence);
            if (status == VK_NOT_READY) return false;
            check(status, "vkGetFenceStatus(main)");
        }
        for (long fence : offscreenFences) {
            int status = vkGetFenceStatus(device, fence);
            if (status == VK_NOT_READY) return false;
            check(status, "vkGetFenceStatus(offscreen)");
        }
        return true;
    }

    int waitForAllFences(MemoryStack stack, long timeout) {
        ensureInitialized();
        LongBuffer fences = stack.mallocLong(mainFences.length + offscreenFences.length);
        fences.put(mainFences).put(offscreenFences).flip();
        return vkWaitForFences(device, fences, true, timeout);
    }

    int totalUploadSlotCount() {
        ensureInitialized();
        return mainUploadAllocations.length + offscreenUploadAllocations.length + 1;
    }

    void appendStatistics(Map<String, Long> statistics) {
        statistics.put("execution.mainGenerations", mainGenerations);
        statistics.put("execution.mainSlots", initialized ? (long) mainCommands.length : 0L);
        statistics.put("execution.offscreenGenerations", offscreenPoolCreates);
        statistics.put("execution.offscreenSlots",
                initialized ? (long) offscreenCommands.length : 0L);
        statistics.put("execution.mainPoolCreates", mainPoolCreates);
        statistics.put("execution.mainPoolDestroys", mainPoolDestroys);
        statistics.put("execution.offscreenPoolCreates", offscreenPoolCreates);
        statistics.put("execution.offscreenPoolDestroys", offscreenPoolDestroys);
        statistics.put("execution.mainFenceCreates", mainFenceCreates);
        statistics.put("execution.mainFenceDestroys", mainFenceDestroys);
        statistics.put("execution.mainSemaphoreCreates", mainSemaphoreCreates);
        statistics.put("execution.mainSemaphoreDestroys", mainSemaphoreDestroys);
        statistics.put("execution.offscreenFenceCreates", offscreenFenceCreates);
        statistics.put("execution.offscreenFenceDestroys", offscreenFenceDestroys);
        statistics.put("execution.mainVertexBuffers",
                (long) countAllocations(mainVertexAllocations));
        statistics.put("execution.mainUploadBuffers",
                (long) countAllocations(mainUploadAllocations));
        statistics.put("execution.offscreenVertexBuffers",
                (long) countAllocations(offscreenVertexAllocations));
        statistics.put("execution.offscreenUploadBuffers",
                (long) countAllocations(offscreenUploadAllocations));
        statistics.put("execution.readbackUploadBuffers",
                (long) countAllocations(readbackUploadAllocations));
    }

    @Override public void close() {
        if (closed) return;
        destroyMainGeneration();
        destroyOffscreenResources();
        initialized = false;
        closed = true;
        if (mainPoolCreates != mainPoolDestroys
                || offscreenPoolCreates != offscreenPoolDestroys
                || mainFenceCreates != mainFenceDestroys
                || mainSemaphoreCreates != mainSemaphoreDestroys
                || offscreenFenceCreates != offscreenFenceDestroys) {
            throw new IllegalStateException("frame execution resources leaked native handles");
        }
    }

    private void createMainGeneration(MemoryStack stack, int slotCount) {
        if (slotCount <= 0) throw new IllegalArgumentException("main slot count");
        mainGenerations++;
        LongBuffer handle = stack.mallocLong(1);
        mainCommandPool = createCommandPool(stack, "main", handle);
        mainPoolCreates++;
        mainCommands = allocateCommands(stack, mainCommandPool, slotCount, "main");
        imageAvailableSemaphores = new long[slotCount];
        mainFences = new long[slotCount];
        mainVertexAllocations = new BufferAllocation[slotCount];
        mainVertexCapacities = new int[slotCount];
        mainUploadAllocations = new BufferAllocation[slotCount];
        mainUploadCapacities = new int[slotCount];
        VkSemaphoreCreateInfo semaphoreInfo = VkSemaphoreCreateInfo.calloc(stack).sType$Default();
        VkFenceCreateInfo fenceInfo = VkFenceCreateInfo.calloc(stack)
                .sType$Default().flags(VK_FENCE_CREATE_SIGNALED_BIT);
        for (int index = 0; index < slotCount; index++) {
            check(vkCreateSemaphore(device, semaphoreInfo, null, handle),
                    "vkCreateSemaphore(imageAvailable[" + index + "])");
            imageAvailableSemaphores[index] = handle.get(0);
            mainSemaphoreCreates++;
            check(vkCreateFence(device, fenceInfo, null, handle),
                    "vkCreateFence(main[" + index + "])");
            mainFences[index] = handle.get(0);
            mainFenceCreates++;
        }
        mainCursor = 0;
    }

    private void createOffscreenResources(MemoryStack stack) {
        LongBuffer handle = stack.mallocLong(1);
        offscreenCommandPool = createCommandPool(stack, "offscreen", handle);
        offscreenPoolCreates++;
        VkCommandBuffer[] commands = allocateCommands(stack, offscreenCommandPool,
                offscreenSlotCount + READBACK_COMMAND_BUFFERS, "offscreen");
        offscreenCommands = new VkCommandBuffer[offscreenSlotCount];
        System.arraycopy(commands, 0, offscreenCommands, 0, offscreenSlotCount);
        readbackCommand = commands[offscreenSlotCount];
        offscreenFences = new long[offscreenSlotCount];
        offscreenOwnerFences = new long[offscreenSlotCount];
        offscreenVertexAllocations = new BufferAllocation[offscreenSlotCount];
        offscreenVertexCapacities = new int[offscreenSlotCount];
        offscreenUploadAllocations = new BufferAllocation[offscreenSlotCount];
        offscreenUploadCapacities = new int[offscreenSlotCount];
        VkFenceCreateInfo fenceInfo = VkFenceCreateInfo.calloc(stack)
                .sType$Default().flags(VK_FENCE_CREATE_SIGNALED_BIT);
        for (int index = 0; index < offscreenSlotCount; index++) {
            check(vkCreateFence(device, fenceInfo, null, handle),
                    "vkCreateFence(offscreen[" + index + "])");
            offscreenFences[index] = handle.get(0);
            offscreenOwnerFences[index] = offscreenFences[index];
            offscreenFenceCreates++;
        }
        offscreenCursor = 0;
    }

    private long createCommandPool(MemoryStack stack, String label, LongBuffer handle) {
        VkCommandPoolCreateInfo info = VkCommandPoolCreateInfo.calloc(stack)
                .sType$Default().flags(VK_COMMAND_POOL_CREATE_RESET_COMMAND_BUFFER_BIT)
                .queueFamilyIndex(queueFamily);
        check(vkCreateCommandPool(device, info, null, handle),
                "vkCreateCommandPool(" + label + ")");
        return handle.get(0);
    }

    private VkCommandBuffer[] allocateCommands(MemoryStack stack, long pool,
                                                int count, String label) {
        VkCommandBufferAllocateInfo info = VkCommandBufferAllocateInfo.calloc(stack)
                .sType$Default().commandPool(pool)
                .level(VK_COMMAND_BUFFER_LEVEL_PRIMARY).commandBufferCount(count);
        PointerBuffer pointers = stack.mallocPointer(count);
        check(vkAllocateCommandBuffers(device, info, pointers),
                "vkAllocateCommandBuffers(" + label + ")");
        VkCommandBuffer[] commands = new VkCommandBuffer[count];
        for (int index = 0; index < count; index++) {
            commands[index] = new VkCommandBuffer(pointers.get(index), device);
        }
        return commands;
    }

    private void destroyMainGeneration() {
        destroyAllocations(mainVertexAllocations);
        mainVertexAllocations = new BufferAllocation[0];
        mainVertexCapacities = new int[0];
        destroyAllocations(mainUploadAllocations);
        mainUploadAllocations = new BufferAllocation[0];
        mainUploadCapacities = new int[0];
        for (long fence : mainFences) {
            if (fence != VK_NULL_HANDLE) {
                vkDestroyFence(device, fence, null);
                mainFenceDestroys++;
            }
        }
        mainFences = new long[0];
        for (long semaphore : imageAvailableSemaphores) {
            if (semaphore != VK_NULL_HANDLE) {
                vkDestroySemaphore(device, semaphore, null);
                mainSemaphoreDestroys++;
            }
        }
        imageAvailableSemaphores = new long[0];
        if (mainCommandPool != VK_NULL_HANDLE) {
            vkDestroyCommandPool(device, mainCommandPool, null);
            mainCommandPool = VK_NULL_HANDLE;
            mainPoolDestroys++;
        }
        mainCommands = new VkCommandBuffer[0];
        mainCursor = 0;
    }

    private void destroyOffscreenResources() {
        destroyAllocations(offscreenVertexAllocations);
        offscreenVertexAllocations = new BufferAllocation[0];
        offscreenVertexCapacities = new int[0];
        destroyAllocations(offscreenUploadAllocations);
        offscreenUploadAllocations = new BufferAllocation[0];
        offscreenUploadCapacities = new int[0];
        destroyAllocations(readbackUploadAllocations);
        readbackUploadCapacities[0] = 0;
        for (long fence : offscreenFences) {
            if (fence != VK_NULL_HANDLE) {
                vkDestroyFence(device, fence, null);
                offscreenFenceDestroys++;
            }
        }
        offscreenFences = new long[0];
        offscreenOwnerFences = new long[0];
        if (offscreenCommandPool != VK_NULL_HANDLE) {
            vkDestroyCommandPool(device, offscreenCommandPool, null);
            offscreenCommandPool = VK_NULL_HANDLE;
            offscreenPoolDestroys++;
        }
        offscreenCommands = new VkCommandBuffer[0];
        readbackCommand = null;
        offscreenCursor = 0;
    }

    private void resetOffscreenOwners() {
        for (int index = 0; index < offscreenOwnerFences.length; index++) {
            offscreenOwnerFences[index] = offscreenFences[index];
        }
    }

    private void destroyAllocations(BufferAllocation[] allocations) {
        for (int index = 0; index < allocations.length; index++) {
            if (allocations[index] != null) {
                memory.destroyBuffer(allocations[index]);
                allocations[index] = null;
            }
        }
    }

    private static int countAllocations(BufferAllocation[] allocations) {
        int count = 0;
        for (BufferAllocation allocation : allocations) {
            if (allocation != null) count++;
        }
        return count;
    }

    private int requireMainSlot(int slot) {
        ensureInitialized();
        if (slot < 0 || slot >= mainCommands.length) throw new IndexOutOfBoundsException(slot);
        return slot;
    }

    private int requireOffscreenSlot(int slot) {
        ensureInitialized();
        if (slot < 0 || slot >= offscreenCommands.length) {
            throw new IndexOutOfBoundsException(slot);
        }
        return slot;
    }

    private void ensureInitialized() {
        ensureOpen();
        if (!initialized) throw new IllegalStateException("execution resources not initialized");
    }

    private void ensureOpen() {
        if (closed) throw new IllegalStateException("execution resources are closed");
    }

    private static void check(int result, String operation) {
        if (result != VK_SUCCESS) {
            throw new IllegalStateException(operation + " failed with VkResult " + result);
        }
    }
}
