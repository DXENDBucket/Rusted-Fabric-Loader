#include "rustedfabric_vulkan_backend.h"

#include <android/native_window.h>
#include <android/log.h>
#include <vulkan/vulkan.h>

#include <algorithm>
#include <array>
#include <cstring>
#include <limits>
#include <mutex>
#include <set>
#include <string>
#include <vector>

#include "rustedfabric_renderbridge.h"
#include "rustedfabric_framestream.h"
#include "framestream_colored.vert.h"
#include "framestream_colored.frag.h"
#include "framestream_textured.vert.h"
#include "framestream_textured.frag.h"

namespace rustedfabric::vulkan {
namespace {

constexpr int kBackendAbiMajor = 1;
constexpr int kBackendAbiMinor = 0;
constexpr int kFrameStreamMajor = 1;
constexpr int kFrameStreamMinor = 0;
constexpr int kResourceStreamMajor = 1;
constexpr int kResourceStreamMinor = 0;
constexpr size_t kMaximumDevices = 256;
constexpr uint64_t kAcquireTimeoutNanos = 100'000'000ULL;
constexpr uint64_t kFenceTimeoutNanos = 1'000'000'000ULL;
constexpr uint32_t kFramesInFlight = 2;

struct FrameSlot {
    VkCommandBuffer command_buffer = VK_NULL_HANDLE;
    VkSemaphore image_available = VK_NULL_HANDLE;
    VkSemaphore render_finished = VK_NULL_HANDLE;
    VkFence fence = VK_NULL_HANDLE;
    VkBuffer geometry = VK_NULL_HANDLE;
    VkDeviceMemory geometry_memory = VK_NULL_HANDLE;
    size_t geometry_capacity = 0;
};

struct TextureResource {
    VkImage image = VK_NULL_HANDLE;
    VkDeviceMemory memory = VK_NULL_HANDLE;
    VkImageView view = VK_NULL_HANDLE;
    VkSampler sampler = VK_NULL_HANDLE;
    VkDescriptorSet descriptor_set = VK_NULL_HANDLE;
    VkFramebuffer framebuffer = VK_NULL_HANDLE;
    uint32_t width = 0;
    uint32_t height = 0;
    bool render_target = false;
};

struct Session {
    ANativeWindow* window = nullptr;
    uint64_t window_generation = 0;
    VkInstance instance = VK_NULL_HANDLE;
    VkSurfaceKHR surface = VK_NULL_HANDLE;
    VkPhysicalDevice physical_device = VK_NULL_HANDLE;
    VkDevice device = VK_NULL_HANDLE;
    VkQueue graphics_queue = VK_NULL_HANDLE;
    VkQueue present_queue = VK_NULL_HANDLE;
    uint32_t graphics_family = 0;
    uint32_t present_family = 0;
    std::string device_name;
    VkSwapchainKHR swapchain = VK_NULL_HANDLE;
    VkFormat format = VK_FORMAT_UNDEFINED;
    VkColorSpaceKHR color_space = VK_COLOR_SPACE_SRGB_NONLINEAR_KHR;
    VkPresentModeKHR present_mode = VK_PRESENT_MODE_FIFO_KHR;
    VkExtent2D extent{};
    std::vector<VkImage> images;
    std::vector<VkImageView> image_views;
    VkRenderPass render_pass = VK_NULL_HANDLE;
    VkRenderPass offscreen_render_pass = VK_NULL_HANDLE;
    VkRenderPass offscreen_load_render_pass = VK_NULL_HANDLE;
    VkDescriptorSetLayout texture_set_layout = VK_NULL_HANDLE;
    VkDescriptorPool descriptor_pool = VK_NULL_HANDLE;
    VkPipelineLayout pipeline_layout = VK_NULL_HANDLE;
    VkPipeline colored_pipeline = VK_NULL_HANDLE;
    VkPipeline textured_pipeline = VK_NULL_HANDLE;
    std::vector<VkFramebuffer> framebuffers;
    VkCommandPool command_pool = VK_NULL_HANDLE;
    std::array<FrameSlot, kFramesInFlight> frames{};
    // Object-submission handles are one-based slots. FrameStream wraps these as
    // type=texture,generation=1 without exposing native pointers to Java.
    std::vector<TextureResource> textures;
    uint32_t next_frame = 0;
};

std::mutex state_mutex;
bool initialized = false;
uint32_t loader_instance_version = VK_API_VERSION_1_0;
std::string diagnostic = "Android Vulkan backend has not been initialized";
std::vector<DeviceInfo> probe_devices;
Session session;

void set_diagnostic(const std::string& value) {
    diagnostic = value;
}

std::string result_message(const char* operation, VkResult result) {
    return std::string(operation) + " failed with VkResult "
            + std::to_string(static_cast<int>(result));
}

bool has_instance_extension(const std::vector<VkExtensionProperties>& extensions,
                            const char* required) {
    return std::any_of(extensions.begin(), extensions.end(),
                       [required](const VkExtensionProperties& extension) {
                           return std::string(extension.extensionName) == required;
                       });
}

bool has_device_extension(VkPhysicalDevice device, const char* required) {
    uint32_t count = 0;
    if (vkEnumerateDeviceExtensionProperties(device, nullptr, &count, nullptr) != VK_SUCCESS) {
        return false;
    }
    std::vector<VkExtensionProperties> extensions(count);
    if (count != 0 && vkEnumerateDeviceExtensionProperties(
            device, nullptr, &count, extensions.data()) != VK_SUCCESS) {
        return false;
    }
    return has_instance_extension(extensions, required);
}

VkApplicationInfo application_info() {
    VkApplicationInfo info{};
    info.sType = VK_STRUCTURE_TYPE_APPLICATION_INFO;
    info.pApplicationName = "Rusted Fabric Loader";
    info.applicationVersion = VK_MAKE_VERSION(1, 0, 0);
    info.pEngineName = "RustedVK";
    info.engineVersion = VK_MAKE_VERSION(1, 0, 0);
    info.apiVersion = std::min(loader_instance_version, VK_API_VERSION_1_1);
    return info;
}

VkResult create_instance(VkInstance* result) {
    const char* extensions[] = {
            VK_KHR_SURFACE_EXTENSION_NAME,
            VK_KHR_ANDROID_SURFACE_EXTENSION_NAME
    };
    VkApplicationInfo app = application_info();
    VkInstanceCreateInfo info{};
    info.sType = VK_STRUCTURE_TYPE_INSTANCE_CREATE_INFO;
    info.pApplicationInfo = &app;
    info.enabledExtensionCount = 2;
    info.ppEnabledExtensionNames = extensions;
    return vkCreateInstance(&info, nullptr, result);
}

void destroy_session_locked() {
    if (session.device != VK_NULL_HANDLE) vkDeviceWaitIdle(session.device);
    for (TextureResource& texture : session.textures) {
        if (texture.framebuffer != VK_NULL_HANDLE) {
            vkDestroyFramebuffer(session.device, texture.framebuffer, nullptr);
        }
        if (texture.sampler != VK_NULL_HANDLE) {
            vkDestroySampler(session.device, texture.sampler, nullptr);
        }
        if (texture.view != VK_NULL_HANDLE) {
            vkDestroyImageView(session.device, texture.view, nullptr);
        }
        if (texture.image != VK_NULL_HANDLE) {
            vkDestroyImage(session.device, texture.image, nullptr);
        }
        if (texture.memory != VK_NULL_HANDLE) {
            vkFreeMemory(session.device, texture.memory, nullptr);
        }
    }
    for (FrameSlot& frame : session.frames) {
        if (frame.geometry != VK_NULL_HANDLE) {
            vkDestroyBuffer(session.device, frame.geometry, nullptr);
        }
        if (frame.geometry_memory != VK_NULL_HANDLE) {
            vkFreeMemory(session.device, frame.geometry_memory, nullptr);
        }
        if (frame.fence != VK_NULL_HANDLE) vkDestroyFence(session.device, frame.fence, nullptr);
        if (frame.render_finished != VK_NULL_HANDLE) {
            vkDestroySemaphore(session.device, frame.render_finished, nullptr);
        }
        if (frame.image_available != VK_NULL_HANDLE) {
            vkDestroySemaphore(session.device, frame.image_available, nullptr);
        }
        frame = FrameSlot{};
    }
    if (session.command_pool != VK_NULL_HANDLE) {
        vkDestroyCommandPool(session.device, session.command_pool, nullptr);
    }
    if (session.colored_pipeline != VK_NULL_HANDLE) {
        vkDestroyPipeline(session.device, session.colored_pipeline, nullptr);
    }
    if (session.textured_pipeline != VK_NULL_HANDLE) {
        vkDestroyPipeline(session.device, session.textured_pipeline, nullptr);
    }
    if (session.pipeline_layout != VK_NULL_HANDLE) {
        vkDestroyPipelineLayout(session.device, session.pipeline_layout, nullptr);
    }
    if (session.descriptor_pool != VK_NULL_HANDLE) {
        vkDestroyDescriptorPool(session.device, session.descriptor_pool, nullptr);
    }
    if (session.texture_set_layout != VK_NULL_HANDLE) {
        vkDestroyDescriptorSetLayout(session.device, session.texture_set_layout, nullptr);
    }
    for (VkFramebuffer framebuffer : session.framebuffers) {
        vkDestroyFramebuffer(session.device, framebuffer, nullptr);
    }
    if (session.render_pass != VK_NULL_HANDLE) {
        vkDestroyRenderPass(session.device, session.render_pass, nullptr);
    }
    if (session.offscreen_render_pass != VK_NULL_HANDLE) {
        vkDestroyRenderPass(session.device, session.offscreen_render_pass, nullptr);
    }
    if (session.offscreen_load_render_pass != VK_NULL_HANDLE) {
        vkDestroyRenderPass(session.device, session.offscreen_load_render_pass, nullptr);
    }
    for (VkImageView image_view : session.image_views) {
        vkDestroyImageView(session.device, image_view, nullptr);
    }
    if (session.swapchain != VK_NULL_HANDLE) {
        vkDestroySwapchainKHR(session.device, session.swapchain, nullptr);
    }
    if (session.device != VK_NULL_HANDLE) vkDestroyDevice(session.device, nullptr);
    if (session.surface != VK_NULL_HANDLE && session.instance != VK_NULL_HANDLE) {
        vkDestroySurfaceKHR(session.instance, session.surface, nullptr);
    }
    if (session.instance != VK_NULL_HANDLE) vkDestroyInstance(session.instance, nullptr);
    rustedfabric_release_native_window(session.window);
    session = Session{};
}

bool find_memory_type_locked(uint32_t allowed, VkMemoryPropertyFlags required,
                             uint32_t* result) {
    VkPhysicalDeviceMemoryProperties properties{};
    vkGetPhysicalDeviceMemoryProperties(session.physical_device, &properties);
    for (uint32_t index = 0; index < properties.memoryTypeCount; ++index) {
        if ((allowed & (1u << index)) != 0
                && (properties.memoryTypes[index].propertyFlags & required) == required) {
            *result = index;
            return true;
        }
    }
    return false;
}

bool allocate_memory_locked(const VkMemoryRequirements& requirements,
                            VkMemoryPropertyFlags properties,
                            VkDeviceMemory* result) {
    uint32_t memory_type = 0;
    if (!find_memory_type_locked(requirements.memoryTypeBits, properties, &memory_type)) {
        set_diagnostic("Android Vulkan device has no compatible memory type");
        return false;
    }
    VkMemoryAllocateInfo info{};
    info.sType = VK_STRUCTURE_TYPE_MEMORY_ALLOCATE_INFO;
    info.allocationSize = requirements.size;
    info.memoryTypeIndex = memory_type;
    VkResult status = vkAllocateMemory(session.device, &info, nullptr, result);
    if (status != VK_SUCCESS) {
        set_diagnostic(result_message("vkAllocateMemory", status));
        return false;
    }
    return true;
}

bool submit_texture_upload_locked(VkBuffer staging, VkImage image,
                                  uint32_t x, uint32_t y,
                                  uint32_t width, uint32_t height,
                                  VkImageLayout old_layout) {
    VkCommandBuffer command = VK_NULL_HANDLE;
    VkCommandBufferAllocateInfo allocation{};
    allocation.sType = VK_STRUCTURE_TYPE_COMMAND_BUFFER_ALLOCATE_INFO;
    allocation.commandPool = session.command_pool;
    allocation.level = VK_COMMAND_BUFFER_LEVEL_PRIMARY;
    allocation.commandBufferCount = 1;
    VkResult status = vkAllocateCommandBuffers(
            session.device, &allocation, &command);
    if (status != VK_SUCCESS) {
        set_diagnostic(result_message("vkAllocateCommandBuffers(texture)", status));
        return false;
    }
    VkCommandBufferBeginInfo begin{};
    begin.sType = VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO;
    begin.flags = VK_COMMAND_BUFFER_USAGE_ONE_TIME_SUBMIT_BIT;
    status = vkBeginCommandBuffer(command, &begin);
    if (status == VK_SUCCESS) {
        VkImageMemoryBarrier to_transfer{};
        to_transfer.sType = VK_STRUCTURE_TYPE_IMAGE_MEMORY_BARRIER;
        to_transfer.srcAccessMask = old_layout == VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL
                ? VK_ACCESS_SHADER_READ_BIT : 0;
        to_transfer.dstAccessMask = VK_ACCESS_TRANSFER_WRITE_BIT;
        to_transfer.oldLayout = old_layout;
        to_transfer.newLayout = VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL;
        to_transfer.srcQueueFamilyIndex = VK_QUEUE_FAMILY_IGNORED;
        to_transfer.dstQueueFamilyIndex = VK_QUEUE_FAMILY_IGNORED;
        to_transfer.image = image;
        to_transfer.subresourceRange.aspectMask = VK_IMAGE_ASPECT_COLOR_BIT;
        to_transfer.subresourceRange.levelCount = 1;
        to_transfer.subresourceRange.layerCount = 1;
        VkPipelineStageFlags source_stage = old_layout
                == VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL
                ? VK_PIPELINE_STAGE_FRAGMENT_SHADER_BIT
                : VK_PIPELINE_STAGE_TOP_OF_PIPE_BIT;
        vkCmdPipelineBarrier(command, source_stage,
                             VK_PIPELINE_STAGE_TRANSFER_BIT, 0, 0, nullptr,
                             0, nullptr, 1, &to_transfer);
        VkBufferImageCopy copy{};
        copy.imageSubresource.aspectMask = VK_IMAGE_ASPECT_COLOR_BIT;
        copy.imageSubresource.layerCount = 1;
        copy.imageOffset = {static_cast<int32_t>(x), static_cast<int32_t>(y), 0};
        copy.imageExtent = {width, height, 1};
        vkCmdCopyBufferToImage(command, staging, image,
                               VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL, 1, &copy);
        VkImageMemoryBarrier to_shader = to_transfer;
        to_shader.srcAccessMask = VK_ACCESS_TRANSFER_WRITE_BIT;
        to_shader.dstAccessMask = VK_ACCESS_SHADER_READ_BIT;
        to_shader.oldLayout = VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL;
        to_shader.newLayout = VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL;
        vkCmdPipelineBarrier(command, VK_PIPELINE_STAGE_TRANSFER_BIT,
                             VK_PIPELINE_STAGE_FRAGMENT_SHADER_BIT, 0, 0, nullptr,
                             0, nullptr, 1, &to_shader);
        status = vkEndCommandBuffer(command);
    }
    if (status == VK_SUCCESS) {
        VkSubmitInfo submit{};
        submit.sType = VK_STRUCTURE_TYPE_SUBMIT_INFO;
        submit.commandBufferCount = 1;
        submit.pCommandBuffers = &command;
        status = vkQueueSubmit(session.graphics_queue, 1, &submit, VK_NULL_HANDLE);
    }
    if (status == VK_SUCCESS) status = vkQueueWaitIdle(session.graphics_queue);
    vkFreeCommandBuffers(session.device, session.command_pool, 1, &command);
    if (status != VK_SUCCESS) {
        set_diagnostic(result_message("submitting texture upload", status));
        return false;
    }
    return true;
}

uint64_t upload_texture_locked(uint32_t width, uint32_t height,
                               const uint8_t* rgba, size_t byte_count) {
    if (session.device == VK_NULL_HANDLE || session.command_pool == VK_NULL_HANDLE) {
        set_diagnostic("Android Vulkan texture upload requires an active device");
        return 0;
    }
    if (width == 0 || height == 0 || width > 32768 || height > 32768
            || rgba == nullptr || byte_count != static_cast<size_t>(width) * height * 4u) {
        set_diagnostic("Android Vulkan texture upload has invalid RGBA dimensions");
        return 0;
    }

    VkBuffer staging = VK_NULL_HANDLE;
    VkDeviceMemory staging_memory = VK_NULL_HANDLE;
    VkBufferCreateInfo buffer_info{};
    buffer_info.sType = VK_STRUCTURE_TYPE_BUFFER_CREATE_INFO;
    buffer_info.size = byte_count;
    buffer_info.usage = VK_BUFFER_USAGE_TRANSFER_SRC_BIT;
    buffer_info.sharingMode = VK_SHARING_MODE_EXCLUSIVE;
    VkResult status = vkCreateBuffer(session.device, &buffer_info, nullptr, &staging);
    VkMemoryRequirements staging_requirements{};
    if (status == VK_SUCCESS) {
        vkGetBufferMemoryRequirements(session.device, staging, &staging_requirements);
        if (!allocate_memory_locked(staging_requirements,
                                    VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT
                                    | VK_MEMORY_PROPERTY_HOST_COHERENT_BIT,
                                    &staging_memory)) {
            status = VK_ERROR_OUT_OF_DEVICE_MEMORY;
        }
    }
    if (status == VK_SUCCESS) {
        status = vkBindBufferMemory(session.device, staging, staging_memory, 0);
    }
    void* mapped = nullptr;
    if (status == VK_SUCCESS) {
        status = vkMapMemory(session.device, staging_memory, 0, byte_count, 0, &mapped);
    }
    if (status == VK_SUCCESS) {
        std::memcpy(mapped, rgba, byte_count);
        vkUnmapMemory(session.device, staging_memory);
    }

    TextureResource texture{};
    texture.width = width;
    texture.height = height;
    VkImageCreateInfo image_info{};
    image_info.sType = VK_STRUCTURE_TYPE_IMAGE_CREATE_INFO;
    image_info.imageType = VK_IMAGE_TYPE_2D;
    image_info.format = VK_FORMAT_R8G8B8A8_UNORM;
    image_info.extent = {width, height, 1};
    image_info.mipLevels = 1;
    image_info.arrayLayers = 1;
    image_info.samples = VK_SAMPLE_COUNT_1_BIT;
    image_info.tiling = VK_IMAGE_TILING_OPTIMAL;
    image_info.usage = VK_IMAGE_USAGE_TRANSFER_DST_BIT | VK_IMAGE_USAGE_SAMPLED_BIT;
    image_info.sharingMode = VK_SHARING_MODE_EXCLUSIVE;
    image_info.initialLayout = VK_IMAGE_LAYOUT_UNDEFINED;
    if (status == VK_SUCCESS) {
        status = vkCreateImage(session.device, &image_info, nullptr, &texture.image);
    }
    VkMemoryRequirements image_requirements{};
    if (status == VK_SUCCESS) {
        vkGetImageMemoryRequirements(session.device, texture.image, &image_requirements);
        if (!allocate_memory_locked(image_requirements,
                                    VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT,
                                    &texture.memory)) {
            status = VK_ERROR_OUT_OF_DEVICE_MEMORY;
        }
    }
    if (status == VK_SUCCESS) {
        status = vkBindImageMemory(session.device, texture.image, texture.memory, 0);
    }
    if (status == VK_SUCCESS
            && !submit_texture_upload_locked(staging, texture.image, 0, 0, width, height,
                                              VK_IMAGE_LAYOUT_UNDEFINED)) {
        status = VK_ERROR_DEVICE_LOST;
    }
    if (staging != VK_NULL_HANDLE) vkDestroyBuffer(session.device, staging, nullptr);
    if (staging_memory != VK_NULL_HANDLE) vkFreeMemory(session.device, staging_memory, nullptr);

    VkImageViewCreateInfo view_info{};
    view_info.sType = VK_STRUCTURE_TYPE_IMAGE_VIEW_CREATE_INFO;
    view_info.image = texture.image;
    view_info.viewType = VK_IMAGE_VIEW_TYPE_2D;
    view_info.format = VK_FORMAT_R8G8B8A8_UNORM;
    view_info.subresourceRange.aspectMask = VK_IMAGE_ASPECT_COLOR_BIT;
    view_info.subresourceRange.levelCount = 1;
    view_info.subresourceRange.layerCount = 1;
    if (status == VK_SUCCESS) {
        status = vkCreateImageView(session.device, &view_info, nullptr, &texture.view);
    }
    VkSamplerCreateInfo sampler_info{};
    sampler_info.sType = VK_STRUCTURE_TYPE_SAMPLER_CREATE_INFO;
    sampler_info.magFilter = VK_FILTER_LINEAR;
    sampler_info.minFilter = VK_FILTER_LINEAR;
    sampler_info.mipmapMode = VK_SAMPLER_MIPMAP_MODE_NEAREST;
    sampler_info.addressModeU = VK_SAMPLER_ADDRESS_MODE_CLAMP_TO_EDGE;
    sampler_info.addressModeV = VK_SAMPLER_ADDRESS_MODE_CLAMP_TO_EDGE;
    sampler_info.addressModeW = VK_SAMPLER_ADDRESS_MODE_CLAMP_TO_EDGE;
    sampler_info.maxLod = 0.0f;
    if (status == VK_SUCCESS) {
        status = vkCreateSampler(session.device, &sampler_info, nullptr, &texture.sampler);
    }
    if (status == VK_SUCCESS) {
        VkDescriptorSetAllocateInfo allocate{};
        allocate.sType = VK_STRUCTURE_TYPE_DESCRIPTOR_SET_ALLOCATE_INFO;
        allocate.descriptorPool = session.descriptor_pool;
        allocate.descriptorSetCount = 1;
        allocate.pSetLayouts = &session.texture_set_layout;
        status = vkAllocateDescriptorSets(
                session.device, &allocate, &texture.descriptor_set);
    }
    if (status == VK_SUCCESS) {
        VkDescriptorImageInfo image{};
        image.sampler = texture.sampler;
        image.imageView = texture.view;
        image.imageLayout = VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL;
        VkWriteDescriptorSet write{};
        write.sType = VK_STRUCTURE_TYPE_WRITE_DESCRIPTOR_SET;
        write.dstSet = texture.descriptor_set;
        write.dstBinding = 0;
        write.descriptorCount = 1;
        write.descriptorType = VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER;
        write.pImageInfo = &image;
        vkUpdateDescriptorSets(session.device, 1, &write, 0, nullptr);
    }
    if (status != VK_SUCCESS) {
        if (diagnostic.find("failed") == std::string::npos) {
            set_diagnostic(result_message("creating Android Vulkan texture", status));
        }
        if (texture.sampler != VK_NULL_HANDLE) {
            vkDestroySampler(session.device, texture.sampler, nullptr);
        }
        if (texture.view != VK_NULL_HANDLE) {
            vkDestroyImageView(session.device, texture.view, nullptr);
        }
        if (texture.image != VK_NULL_HANDLE) {
            vkDestroyImage(session.device, texture.image, nullptr);
        }
        if (texture.memory != VK_NULL_HANDLE) {
            vkFreeMemory(session.device, texture.memory, nullptr);
        }
        return 0;
    }
    session.textures.push_back(texture);
    set_diagnostic("Android Vulkan RGBA texture uploaded");
    return session.textures.size();
}

bool update_texture_region_locked(uint64_t texture_handle, uint32_t x, uint32_t y,
                                  uint32_t width, uint32_t height,
                                  const uint8_t* rgba, size_t byte_count) {
    if (texture_handle == 0 || texture_handle > session.textures.size()) {
        set_diagnostic("Android Vulkan texture update used an unknown handle");
        return false;
    }
    TextureResource& texture = session.textures[texture_handle - 1];
    if (texture.image == VK_NULL_HANDLE || texture.render_target) {
        set_diagnostic("Android Vulkan texture update requires a sampled upload texture");
        return false;
    }
    if (width == 0 || height == 0 || rgba == nullptr
            || byte_count != static_cast<size_t>(width) * height * 4u
            || x > texture.width || y > texture.height
            || width > texture.width - x || height > texture.height - y) {
        set_diagnostic("Android Vulkan texture update region is outside the texture");
        return false;
    }

    VkBuffer staging = VK_NULL_HANDLE;
    VkDeviceMemory staging_memory = VK_NULL_HANDLE;
    VkBufferCreateInfo buffer_info{};
    buffer_info.sType = VK_STRUCTURE_TYPE_BUFFER_CREATE_INFO;
    buffer_info.size = byte_count;
    buffer_info.usage = VK_BUFFER_USAGE_TRANSFER_SRC_BIT;
    buffer_info.sharingMode = VK_SHARING_MODE_EXCLUSIVE;
    VkResult status = vkCreateBuffer(session.device, &buffer_info, nullptr, &staging);
    VkMemoryRequirements requirements{};
    if (status == VK_SUCCESS) {
        vkGetBufferMemoryRequirements(session.device, staging, &requirements);
        if (!allocate_memory_locked(requirements,
                                    VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT
                                    | VK_MEMORY_PROPERTY_HOST_COHERENT_BIT,
                                    &staging_memory)) {
            status = VK_ERROR_OUT_OF_DEVICE_MEMORY;
        }
    }
    if (status == VK_SUCCESS) {
        status = vkBindBufferMemory(session.device, staging, staging_memory, 0);
    }
    void* mapped = nullptr;
    if (status == VK_SUCCESS) {
        status = vkMapMemory(session.device, staging_memory, 0, byte_count, 0, &mapped);
    }
    if (status == VK_SUCCESS) {
        std::memcpy(mapped, rgba, byte_count);
        vkUnmapMemory(session.device, staging_memory);
        if (!submit_texture_upload_locked(staging, texture.image, x, y, width, height,
                                          VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL)) {
            status = VK_ERROR_DEVICE_LOST;
        }
    }
    if (staging != VK_NULL_HANDLE) vkDestroyBuffer(session.device, staging, nullptr);
    if (staging_memory != VK_NULL_HANDLE) vkFreeMemory(session.device, staging_memory, nullptr);
    if (status != VK_SUCCESS) {
        if (diagnostic.find("failed") == std::string::npos) {
            set_diagnostic(result_message("updating Android Vulkan texture", status));
        }
        return false;
    }
    set_diagnostic("Android Vulkan texture region updated");
    return true;
}

bool initialize_render_target_locked(TextureResource& target) {
    VkCommandBuffer command = VK_NULL_HANDLE;
    VkCommandBufferAllocateInfo allocation{};
    allocation.sType = VK_STRUCTURE_TYPE_COMMAND_BUFFER_ALLOCATE_INFO;
    allocation.commandPool = session.command_pool;
    allocation.level = VK_COMMAND_BUFFER_LEVEL_PRIMARY;
    allocation.commandBufferCount = 1;
    VkResult status = vkAllocateCommandBuffers(
            session.device, &allocation, &command);
    VkCommandBufferBeginInfo begin{};
    begin.sType = VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO;
    begin.flags = VK_COMMAND_BUFFER_USAGE_ONE_TIME_SUBMIT_BIT;
    if (status == VK_SUCCESS) status = vkBeginCommandBuffer(command, &begin);
    VkImageSubresourceRange range{};
    range.aspectMask = VK_IMAGE_ASPECT_COLOR_BIT;
    range.levelCount = 1;
    range.layerCount = 1;
    if (status == VK_SUCCESS) {
        VkImageMemoryBarrier to_clear{};
        to_clear.sType = VK_STRUCTURE_TYPE_IMAGE_MEMORY_BARRIER;
        to_clear.srcAccessMask = 0;
        to_clear.dstAccessMask = VK_ACCESS_TRANSFER_WRITE_BIT;
        to_clear.oldLayout = VK_IMAGE_LAYOUT_UNDEFINED;
        to_clear.newLayout = VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL;
        to_clear.srcQueueFamilyIndex = VK_QUEUE_FAMILY_IGNORED;
        to_clear.dstQueueFamilyIndex = VK_QUEUE_FAMILY_IGNORED;
        to_clear.image = target.image;
        to_clear.subresourceRange = range;
        vkCmdPipelineBarrier(command, VK_PIPELINE_STAGE_TOP_OF_PIPE_BIT,
                             VK_PIPELINE_STAGE_TRANSFER_BIT, 0, 0, nullptr,
                             0, nullptr, 1, &to_clear);
        VkClearColorValue transparent{};
        vkCmdClearColorImage(command, target.image, VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL,
                             &transparent, 1, &range);
        VkImageMemoryBarrier to_sample{};
        to_sample.sType = VK_STRUCTURE_TYPE_IMAGE_MEMORY_BARRIER;
        to_sample.srcAccessMask = VK_ACCESS_TRANSFER_WRITE_BIT;
        to_sample.dstAccessMask = VK_ACCESS_SHADER_READ_BIT;
        to_sample.oldLayout = VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL;
        to_sample.newLayout = VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL;
        to_sample.srcQueueFamilyIndex = VK_QUEUE_FAMILY_IGNORED;
        to_sample.dstQueueFamilyIndex = VK_QUEUE_FAMILY_IGNORED;
        to_sample.image = target.image;
        to_sample.subresourceRange = range;
        vkCmdPipelineBarrier(command, VK_PIPELINE_STAGE_TRANSFER_BIT,
                             VK_PIPELINE_STAGE_FRAGMENT_SHADER_BIT, 0, 0, nullptr,
                             0, nullptr, 1, &to_sample);
        status = vkEndCommandBuffer(command);
    }
    if (status == VK_SUCCESS) {
        VkSubmitInfo submit{};
        submit.sType = VK_STRUCTURE_TYPE_SUBMIT_INFO;
        submit.commandBufferCount = 1;
        submit.pCommandBuffers = &command;
        status = vkQueueSubmit(session.graphics_queue, 1, &submit, VK_NULL_HANDLE);
    }
    if (status == VK_SUCCESS) status = vkQueueWaitIdle(session.graphics_queue);
    if (command != VK_NULL_HANDLE) {
        vkFreeCommandBuffers(session.device, session.command_pool, 1, &command);
    }
    if (status != VK_SUCCESS) {
        set_diagnostic(result_message("initializing Android Vulkan render target", status));
        return false;
    }
    return true;
}

uint64_t create_render_target_locked(uint32_t width, uint32_t height) {
    if (session.device == VK_NULL_HANDLE || session.offscreen_render_pass == VK_NULL_HANDLE) {
        set_diagnostic("Android Vulkan render-target creation requires an active device");
        return 0;
    }
    if (width == 0 || height == 0 || width > 32768 || height > 32768) {
        set_diagnostic("Android Vulkan render target has invalid dimensions");
        return 0;
    }

    TextureResource target{};
    target.width = width;
    target.height = height;
    target.render_target = true;
    VkResult status = VK_SUCCESS;
    VkImageCreateInfo image_info{};
    image_info.sType = VK_STRUCTURE_TYPE_IMAGE_CREATE_INFO;
    image_info.imageType = VK_IMAGE_TYPE_2D;
    image_info.format = session.format;
    image_info.extent = {width, height, 1};
    image_info.mipLevels = 1;
    image_info.arrayLayers = 1;
    image_info.samples = VK_SAMPLE_COUNT_1_BIT;
    image_info.tiling = VK_IMAGE_TILING_OPTIMAL;
    image_info.usage = VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT | VK_IMAGE_USAGE_SAMPLED_BIT
            | VK_IMAGE_USAGE_TRANSFER_SRC_BIT | VK_IMAGE_USAGE_TRANSFER_DST_BIT;
    image_info.sharingMode = VK_SHARING_MODE_EXCLUSIVE;
    image_info.initialLayout = VK_IMAGE_LAYOUT_UNDEFINED;
    status = vkCreateImage(session.device, &image_info, nullptr, &target.image);
    VkMemoryRequirements requirements{};
    if (status == VK_SUCCESS) {
        vkGetImageMemoryRequirements(session.device, target.image, &requirements);
        if (!allocate_memory_locked(requirements, VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT,
                                    &target.memory)) {
            status = VK_ERROR_OUT_OF_DEVICE_MEMORY;
        }
    }
    if (status == VK_SUCCESS) {
        status = vkBindImageMemory(session.device, target.image, target.memory, 0);
    }
    VkImageViewCreateInfo view_info{};
    view_info.sType = VK_STRUCTURE_TYPE_IMAGE_VIEW_CREATE_INFO;
    view_info.image = target.image;
    view_info.viewType = VK_IMAGE_VIEW_TYPE_2D;
    view_info.format = session.format;
    view_info.subresourceRange.aspectMask = VK_IMAGE_ASPECT_COLOR_BIT;
    view_info.subresourceRange.levelCount = 1;
    view_info.subresourceRange.layerCount = 1;
    if (status == VK_SUCCESS) {
        status = vkCreateImageView(session.device, &view_info, nullptr, &target.view);
    }
    VkSamplerCreateInfo sampler_info{};
    sampler_info.sType = VK_STRUCTURE_TYPE_SAMPLER_CREATE_INFO;
    sampler_info.magFilter = VK_FILTER_LINEAR;
    sampler_info.minFilter = VK_FILTER_LINEAR;
    sampler_info.mipmapMode = VK_SAMPLER_MIPMAP_MODE_NEAREST;
    sampler_info.addressModeU = VK_SAMPLER_ADDRESS_MODE_CLAMP_TO_EDGE;
    sampler_info.addressModeV = VK_SAMPLER_ADDRESS_MODE_CLAMP_TO_EDGE;
    sampler_info.addressModeW = VK_SAMPLER_ADDRESS_MODE_CLAMP_TO_EDGE;
    if (status == VK_SUCCESS) {
        status = vkCreateSampler(session.device, &sampler_info, nullptr, &target.sampler);
    }
    if (status == VK_SUCCESS) {
        VkDescriptorSetAllocateInfo allocation{};
        allocation.sType = VK_STRUCTURE_TYPE_DESCRIPTOR_SET_ALLOCATE_INFO;
        allocation.descriptorPool = session.descriptor_pool;
        allocation.descriptorSetCount = 1;
        allocation.pSetLayouts = &session.texture_set_layout;
        status = vkAllocateDescriptorSets(session.device, &allocation,
                                          &target.descriptor_set);
    }
    if (status == VK_SUCCESS) {
        VkDescriptorImageInfo image{};
        image.sampler = target.sampler;
        image.imageView = target.view;
        image.imageLayout = VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL;
        VkWriteDescriptorSet write{};
        write.sType = VK_STRUCTURE_TYPE_WRITE_DESCRIPTOR_SET;
        write.dstSet = target.descriptor_set;
        write.dstBinding = 0;
        write.descriptorCount = 1;
        write.descriptorType = VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER;
        write.pImageInfo = &image;
        vkUpdateDescriptorSets(session.device, 1, &write, 0, nullptr);

        VkFramebufferCreateInfo framebuffer_info{};
        framebuffer_info.sType = VK_STRUCTURE_TYPE_FRAMEBUFFER_CREATE_INFO;
        framebuffer_info.renderPass = session.offscreen_render_pass;
        framebuffer_info.attachmentCount = 1;
        framebuffer_info.pAttachments = &target.view;
        framebuffer_info.width = width;
        framebuffer_info.height = height;
        framebuffer_info.layers = 1;
        status = vkCreateFramebuffer(session.device, &framebuffer_info, nullptr,
                                     &target.framebuffer);
    }
    if (status == VK_SUCCESS && !initialize_render_target_locked(target)) {
        status = VK_ERROR_DEVICE_LOST;
    }
    if (status != VK_SUCCESS) {
        set_diagnostic(result_message("creating Android Vulkan render target", status));
        if (target.framebuffer != VK_NULL_HANDLE) {
            vkDestroyFramebuffer(session.device, target.framebuffer, nullptr);
        }
        if (target.sampler != VK_NULL_HANDLE) {
            vkDestroySampler(session.device, target.sampler, nullptr);
        }
        if (target.view != VK_NULL_HANDLE) {
            vkDestroyImageView(session.device, target.view, nullptr);
        }
        if (target.image != VK_NULL_HANDLE) {
            vkDestroyImage(session.device, target.image, nullptr);
        }
        if (target.memory != VK_NULL_HANDLE) {
            vkFreeMemory(session.device, target.memory, nullptr);
        }
        return 0;
    }
    session.textures.push_back(target);
    set_diagnostic("Android Vulkan render target created");
    return session.textures.size();
}

bool probe_locked() {
    probe_devices.clear();
    loader_instance_version = VK_API_VERSION_1_0;
    auto enumerate_version = reinterpret_cast<PFN_vkEnumerateInstanceVersion>(
            vkGetInstanceProcAddr(VK_NULL_HANDLE, "vkEnumerateInstanceVersion"));
    if (enumerate_version != nullptr) {
        VkResult result = enumerate_version(&loader_instance_version);
        if (result != VK_SUCCESS) {
            set_diagnostic(result_message("vkEnumerateInstanceVersion", result));
            return false;
        }
    }

    uint32_t extension_count = 0;
    VkResult result = vkEnumerateInstanceExtensionProperties(
            nullptr, &extension_count, nullptr);
    if (result != VK_SUCCESS) {
        set_diagnostic(result_message("vkEnumerateInstanceExtensionProperties", result));
        return false;
    }
    std::vector<VkExtensionProperties> extensions(extension_count);
    if (extension_count != 0) {
        result = vkEnumerateInstanceExtensionProperties(
                nullptr, &extension_count, extensions.data());
        if (result != VK_SUCCESS && result != VK_INCOMPLETE) {
            set_diagnostic(result_message("vkEnumerateInstanceExtensionProperties", result));
            return false;
        }
        extensions.resize(extension_count);
    }
    if (!has_instance_extension(extensions, VK_KHR_SURFACE_EXTENSION_NAME)
            || !has_instance_extension(extensions, VK_KHR_ANDROID_SURFACE_EXTENSION_NAME)) {
        set_diagnostic("Vulkan loader is missing VK_KHR_surface or VK_KHR_android_surface");
        return false;
    }

    VkInstance instance = VK_NULL_HANDLE;
    result = create_instance(&instance);
    if (result != VK_SUCCESS) {
        set_diagnostic(result_message("vkCreateInstance", result));
        return false;
    }
    uint32_t count = 0;
    result = vkEnumeratePhysicalDevices(instance, &count, nullptr);
    if (result != VK_SUCCESS || count == 0 || count > kMaximumDevices) {
        vkDestroyInstance(instance, nullptr);
        set_diagnostic(result != VK_SUCCESS
                ? result_message("vkEnumeratePhysicalDevices", result)
                : "Vulkan loader reported an invalid physical-device count");
        return false;
    }
    std::vector<VkPhysicalDevice> devices(count);
    result = vkEnumeratePhysicalDevices(instance, &count, devices.data());
    if (result != VK_SUCCESS && result != VK_INCOMPLETE) {
        vkDestroyInstance(instance, nullptr);
        set_diagnostic(result_message("vkEnumeratePhysicalDevices", result));
        return false;
    }
    devices.resize(count);
    for (VkPhysicalDevice device_handle : devices) {
        VkPhysicalDeviceProperties properties{};
        vkGetPhysicalDeviceProperties(device_handle, &properties);
        probe_devices.push_back(DeviceInfo{
                properties.deviceName, properties.vendorID, properties.deviceID,
                static_cast<uint32_t>(properties.deviceType), properties.apiVersion,
                properties.driverVersion});
    }
    vkDestroyInstance(instance, nullptr);
    initialized = true;
    set_diagnostic("Android Vulkan ABI and physical-device probe succeeded");
    return true;
}

bool choose_device_locked(const std::vector<VkPhysicalDevice>& devices) {
    for (VkPhysicalDevice candidate : devices) {
        if (!has_device_extension(candidate, VK_KHR_SWAPCHAIN_EXTENSION_NAME)) continue;
        uint32_t family_count = 0;
        vkGetPhysicalDeviceQueueFamilyProperties(candidate, &family_count, nullptr);
        std::vector<VkQueueFamilyProperties> families(family_count);
        vkGetPhysicalDeviceQueueFamilyProperties(candidate, &family_count, families.data());
        int graphics = -1;
        int present = -1;
        for (uint32_t index = 0; index < family_count; ++index) {
            if ((families[index].queueFlags & VK_QUEUE_GRAPHICS_BIT) != 0 && graphics < 0) {
                graphics = static_cast<int>(index);
            }
            VkBool32 supported = VK_FALSE;
            if (vkGetPhysicalDeviceSurfaceSupportKHR(
                    candidate, index, session.surface, &supported) == VK_SUCCESS
                    && supported == VK_TRUE && present < 0) {
                present = static_cast<int>(index);
            }
        }
        if (graphics < 0 || present < 0) continue;
        session.physical_device = candidate;
        session.graphics_family = static_cast<uint32_t>(graphics);
        session.present_family = static_cast<uint32_t>(present);
        VkPhysicalDeviceProperties properties{};
        vkGetPhysicalDeviceProperties(candidate, &properties);
        session.device_name = properties.deviceName;
        return true;
    }
    set_diagnostic("No Android Vulkan device supports graphics, presentation, and swapchain");
    return false;
}

VkCompositeAlphaFlagBitsKHR choose_composite_alpha(
        VkCompositeAlphaFlagsKHR supported) {
    constexpr VkCompositeAlphaFlagBitsKHR choices[] = {
            VK_COMPOSITE_ALPHA_OPAQUE_BIT_KHR,
            VK_COMPOSITE_ALPHA_INHERIT_BIT_KHR,
            VK_COMPOSITE_ALPHA_PRE_MULTIPLIED_BIT_KHR,
            VK_COMPOSITE_ALPHA_POST_MULTIPLIED_BIT_KHR
    };
    for (VkCompositeAlphaFlagBitsKHR choice : choices) {
        if ((supported & choice) != 0) return choice;
    }
    return VK_COMPOSITE_ALPHA_OPAQUE_BIT_KHR;
}

bool create_swapchain_locked() {
    VkSurfaceCapabilitiesKHR capabilities{};
    VkResult result = vkGetPhysicalDeviceSurfaceCapabilitiesKHR(
            session.physical_device, session.surface, &capabilities);
    if (result != VK_SUCCESS) {
        set_diagnostic(result_message("vkGetPhysicalDeviceSurfaceCapabilitiesKHR", result));
        return false;
    }
    uint32_t format_count = 0;
    result = vkGetPhysicalDeviceSurfaceFormatsKHR(
            session.physical_device, session.surface, &format_count, nullptr);
    if (result != VK_SUCCESS || format_count == 0) {
        set_diagnostic(result != VK_SUCCESS
                ? result_message("vkGetPhysicalDeviceSurfaceFormatsKHR", result)
                : "Android Vulkan surface has no supported format");
        return false;
    }
    std::vector<VkSurfaceFormatKHR> formats(format_count);
    result = vkGetPhysicalDeviceSurfaceFormatsKHR(
            session.physical_device, session.surface, &format_count, formats.data());
    if (result != VK_SUCCESS) {
        set_diagnostic(result_message("vkGetPhysicalDeviceSurfaceFormatsKHR", result));
        return false;
    }
    VkSurfaceFormatKHR selected = formats[0];
    for (const VkSurfaceFormatKHR& candidate : formats) {
        if (candidate.format == VK_FORMAT_R8G8B8A8_UNORM
                || candidate.format == VK_FORMAT_B8G8R8A8_UNORM) {
            selected = candidate;
            break;
        }
    }
    session.format = selected.format;
    session.color_space = selected.colorSpace;

    // The launcher owns frame pacing. Prefer a non-vsync WSI mode so a requested 120 FPS (or
    // unlimited mode) is not silently throttled to the compositor's current 60 Hz cadence.
    // Android implementations must expose FIFO, while IMMEDIATE and MAILBOX are optional, so
    // retain a standards-compliant fallback for devices that cannot present asynchronously.
    uint32_t present_mode_count = 0;
    result = vkGetPhysicalDeviceSurfacePresentModesKHR(
            session.physical_device, session.surface, &present_mode_count, nullptr);
    if (result != VK_SUCCESS || present_mode_count == 0) {
        set_diagnostic(result != VK_SUCCESS
                ? result_message("vkGetPhysicalDeviceSurfacePresentModesKHR", result)
                : "Android Vulkan surface has no present mode");
        return false;
    }
    std::vector<VkPresentModeKHR> present_modes(present_mode_count);
    result = vkGetPhysicalDeviceSurfacePresentModesKHR(
            session.physical_device, session.surface, &present_mode_count,
            present_modes.data());
    if (result != VK_SUCCESS) {
        set_diagnostic(result_message("vkGetPhysicalDeviceSurfacePresentModesKHR", result));
        return false;
    }
    auto supports_present_mode = [&present_modes](VkPresentModeKHR mode) {
        return std::find(present_modes.begin(), present_modes.end(), mode)
                != present_modes.end();
    };
    session.present_mode = supports_present_mode(VK_PRESENT_MODE_IMMEDIATE_KHR)
            ? VK_PRESENT_MODE_IMMEDIATE_KHR
            : supports_present_mode(VK_PRESENT_MODE_MAILBOX_KHR)
                    ? VK_PRESENT_MODE_MAILBOX_KHR
                    : VK_PRESENT_MODE_FIFO_KHR;
    __android_log_print(ANDROID_LOG_INFO, "RustedFabricVk",
                        "Swapchain present mode=%s (available=%u)",
                        session.present_mode == VK_PRESENT_MODE_IMMEDIATE_KHR ? "IMMEDIATE"
                                : session.present_mode == VK_PRESENT_MODE_MAILBOX_KHR
                                        ? "MAILBOX" : "FIFO",
                        present_mode_count);

    if (capabilities.currentExtent.width != std::numeric_limits<uint32_t>::max()) {
        session.extent = capabilities.currentExtent;
    } else {
        uint32_t width = static_cast<uint32_t>(std::max(1, ANativeWindow_getWidth(session.window)));
        uint32_t height = static_cast<uint32_t>(std::max(1, ANativeWindow_getHeight(session.window)));
        session.extent.width = std::clamp(
                width, capabilities.minImageExtent.width, capabilities.maxImageExtent.width);
        session.extent.height = std::clamp(
                height, capabilities.minImageExtent.height, capabilities.maxImageExtent.height);
    }
    uint32_t image_count = capabilities.minImageCount + 1;
    if (capabilities.maxImageCount != 0) {
        image_count = std::min(image_count, capabilities.maxImageCount);
    }
    uint32_t queue_families[] = {session.graphics_family, session.present_family};
    VkSwapchainCreateInfoKHR info{};
    info.sType = VK_STRUCTURE_TYPE_SWAPCHAIN_CREATE_INFO_KHR;
    info.surface = session.surface;
    info.minImageCount = image_count;
    info.imageFormat = session.format;
    info.imageColorSpace = session.color_space;
    info.imageExtent = session.extent;
    info.imageArrayLayers = 1;
    info.imageUsage = VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT;
    if (session.graphics_family != session.present_family) {
        info.imageSharingMode = VK_SHARING_MODE_CONCURRENT;
        info.queueFamilyIndexCount = 2;
        info.pQueueFamilyIndices = queue_families;
    } else {
        info.imageSharingMode = VK_SHARING_MODE_EXCLUSIVE;
    }
    // Android often reports a compositor rotation even while the Activity's SurfaceView is
    // already laid out in the requested landscape orientation.  Feeding that transform back to
    // the swapchain rotates the desktop game's landscape framebuffer a second time.  Prefer an
    // identity pre-transform when the surface supports it; only fall back to currentTransform on
    // devices that require compositor pre-rotation.
    info.preTransform = (capabilities.supportedTransforms
            & VK_SURFACE_TRANSFORM_IDENTITY_BIT_KHR) != 0
            ? VK_SURFACE_TRANSFORM_IDENTITY_BIT_KHR
            : capabilities.currentTransform;
    info.compositeAlpha = choose_composite_alpha(capabilities.supportedCompositeAlpha);
    info.presentMode = session.present_mode;
    info.clipped = VK_TRUE;
    result = vkCreateSwapchainKHR(session.device, &info, nullptr, &session.swapchain);
    if (result != VK_SUCCESS) {
        set_diagnostic(result_message("vkCreateSwapchainKHR", result));
        return false;
    }
    result = vkGetSwapchainImagesKHR(session.device, session.swapchain, &image_count, nullptr);
    if (result != VK_SUCCESS || image_count == 0) {
        set_diagnostic(result != VK_SUCCESS
                ? result_message("vkGetSwapchainImagesKHR", result)
                : "Android Vulkan swapchain has no images");
        return false;
    }
    session.images.resize(image_count);
    result = vkGetSwapchainImagesKHR(
            session.device, session.swapchain, &image_count, session.images.data());
    if (result != VK_SUCCESS) {
        set_diagnostic(result_message("vkGetSwapchainImagesKHR", result));
        return false;
    }
    session.images.resize(image_count);
    return true;
}

VkShaderModule create_shader_module_locked(const uint8_t* bytes, size_t byte_count) {
    if (bytes == nullptr || byte_count == 0 || (byte_count & 3u) != 0) return VK_NULL_HANDLE;
    VkShaderModuleCreateInfo info{};
    info.sType = VK_STRUCTURE_TYPE_SHADER_MODULE_CREATE_INFO;
    info.codeSize = byte_count;
    info.pCode = reinterpret_cast<const uint32_t*>(bytes);
    VkShaderModule result = VK_NULL_HANDLE;
    VkResult status = vkCreateShaderModule(session.device, &info, nullptr, &result);
    if (status != VK_SUCCESS) {
        set_diagnostic(result_message("vkCreateShaderModule", status));
        return VK_NULL_HANDLE;
    }
    return result;
}

bool create_pipeline_locked(bool textured, VkPipeline* output) {
    const uint8_t* vertex_bytes = textured
            ? rustedfabric_framestream_textured_vert_spv
            : rustedfabric_framestream_colored_vert_spv;
    size_t vertex_byte_count = textured
            ? rustedfabric_framestream_textured_vert_spv_bytes
            : rustedfabric_framestream_colored_vert_spv_bytes;
    const uint8_t* fragment_bytes = textured
            ? rustedfabric_framestream_textured_frag_spv
            : rustedfabric_framestream_colored_frag_spv;
    size_t fragment_byte_count = textured
            ? rustedfabric_framestream_textured_frag_spv_bytes
            : rustedfabric_framestream_colored_frag_spv_bytes;
    VkShaderModule vertex = create_shader_module_locked(vertex_bytes, vertex_byte_count);
    VkShaderModule fragment = create_shader_module_locked(fragment_bytes, fragment_byte_count);
    if (vertex == VK_NULL_HANDLE || fragment == VK_NULL_HANDLE) {
        if (vertex != VK_NULL_HANDLE) vkDestroyShaderModule(session.device, vertex, nullptr);
        if (fragment != VK_NULL_HANDLE) vkDestroyShaderModule(session.device, fragment, nullptr);
        return false;
    }
    VkPipelineShaderStageCreateInfo stages[2]{};
    stages[0].sType = VK_STRUCTURE_TYPE_PIPELINE_SHADER_STAGE_CREATE_INFO;
    stages[0].stage = VK_SHADER_STAGE_VERTEX_BIT;
    stages[0].module = vertex;
    stages[0].pName = "main";
    stages[1].sType = VK_STRUCTURE_TYPE_PIPELINE_SHADER_STAGE_CREATE_INFO;
    stages[1].stage = VK_SHADER_STAGE_FRAGMENT_BIT;
    stages[1].module = fragment;
    stages[1].pName = "main";

    VkVertexInputBindingDescription binding{};
    binding.binding = 0;
    binding.stride = textured ? 32 : 24;
    binding.inputRate = VK_VERTEX_INPUT_RATE_VERTEX;
    std::array<VkVertexInputAttributeDescription, 3> attributes{};
    attributes[0] = {0, 0, VK_FORMAT_R32G32_SFLOAT, 0};
    uint32_t attribute_count = 2;
    if (textured) {
        attributes[1] = {1, 0, VK_FORMAT_R32G32_SFLOAT, 8};
        attributes[2] = {2, 0, VK_FORMAT_R32G32B32A32_SFLOAT, 16};
        attribute_count = 3;
    } else {
        attributes[1] = {1, 0, VK_FORMAT_R32G32B32A32_SFLOAT, 8};
    }
    VkPipelineVertexInputStateCreateInfo vertex_input{};
    vertex_input.sType = VK_STRUCTURE_TYPE_PIPELINE_VERTEX_INPUT_STATE_CREATE_INFO;
    vertex_input.vertexBindingDescriptionCount = 1;
    vertex_input.pVertexBindingDescriptions = &binding;
    vertex_input.vertexAttributeDescriptionCount = attribute_count;
    vertex_input.pVertexAttributeDescriptions = attributes.data();
    VkPipelineInputAssemblyStateCreateInfo assembly{};
    assembly.sType = VK_STRUCTURE_TYPE_PIPELINE_INPUT_ASSEMBLY_STATE_CREATE_INFO;
    assembly.topology = VK_PRIMITIVE_TOPOLOGY_TRIANGLE_LIST;
    VkPipelineViewportStateCreateInfo viewport{};
    viewport.sType = VK_STRUCTURE_TYPE_PIPELINE_VIEWPORT_STATE_CREATE_INFO;
    viewport.viewportCount = 1;
    viewport.scissorCount = 1;
    VkPipelineRasterizationStateCreateInfo raster{};
    raster.sType = VK_STRUCTURE_TYPE_PIPELINE_RASTERIZATION_STATE_CREATE_INFO;
    raster.polygonMode = VK_POLYGON_MODE_FILL;
    raster.cullMode = VK_CULL_MODE_NONE;
    raster.frontFace = VK_FRONT_FACE_COUNTER_CLOCKWISE;
    raster.lineWidth = 1.0f;
    VkPipelineMultisampleStateCreateInfo multisample{};
    multisample.sType = VK_STRUCTURE_TYPE_PIPELINE_MULTISAMPLE_STATE_CREATE_INFO;
    multisample.rasterizationSamples = VK_SAMPLE_COUNT_1_BIT;
    VkPipelineColorBlendAttachmentState color{};
    color.colorWriteMask = VK_COLOR_COMPONENT_R_BIT | VK_COLOR_COMPONENT_G_BIT
            | VK_COLOR_COMPONENT_B_BIT | VK_COLOR_COMPONENT_A_BIT;
    color.blendEnable = VK_TRUE;
    color.srcColorBlendFactor = VK_BLEND_FACTOR_SRC_ALPHA;
    color.dstColorBlendFactor = VK_BLEND_FACTOR_ONE_MINUS_SRC_ALPHA;
    color.colorBlendOp = VK_BLEND_OP_ADD;
    color.srcAlphaBlendFactor = VK_BLEND_FACTOR_ONE;
    color.dstAlphaBlendFactor = VK_BLEND_FACTOR_ONE_MINUS_SRC_ALPHA;
    color.alphaBlendOp = VK_BLEND_OP_ADD;
    VkPipelineColorBlendStateCreateInfo blend{};
    blend.sType = VK_STRUCTURE_TYPE_PIPELINE_COLOR_BLEND_STATE_CREATE_INFO;
    blend.attachmentCount = 1;
    blend.pAttachments = &color;
    VkDynamicState dynamic_states[] = {VK_DYNAMIC_STATE_VIEWPORT, VK_DYNAMIC_STATE_SCISSOR};
    VkPipelineDynamicStateCreateInfo dynamic{};
    dynamic.sType = VK_STRUCTURE_TYPE_PIPELINE_DYNAMIC_STATE_CREATE_INFO;
    dynamic.dynamicStateCount = 2;
    dynamic.pDynamicStates = dynamic_states;
    VkGraphicsPipelineCreateInfo pipeline{};
    pipeline.sType = VK_STRUCTURE_TYPE_GRAPHICS_PIPELINE_CREATE_INFO;
    pipeline.stageCount = 2;
    pipeline.pStages = stages;
    pipeline.pVertexInputState = &vertex_input;
    pipeline.pInputAssemblyState = &assembly;
    pipeline.pViewportState = &viewport;
    pipeline.pRasterizationState = &raster;
    pipeline.pMultisampleState = &multisample;
    pipeline.pColorBlendState = &blend;
    pipeline.pDynamicState = &dynamic;
    pipeline.layout = session.pipeline_layout;
    pipeline.renderPass = session.render_pass;
    pipeline.subpass = 0;
    VkResult status = vkCreateGraphicsPipelines(
            session.device, VK_NULL_HANDLE, 1, &pipeline, nullptr, output);
    vkDestroyShaderModule(session.device, vertex, nullptr);
    vkDestroyShaderModule(session.device, fragment, nullptr);
    if (status != VK_SUCCESS) {
        set_diagnostic(result_message("vkCreateGraphicsPipelines", status));
        return false;
    }
    return true;
}

bool create_pipeline_resources_locked() {
    VkDescriptorSetLayoutBinding sampler{};
    sampler.binding = 0;
    sampler.descriptorType = VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER;
    sampler.descriptorCount = 1;
    sampler.stageFlags = VK_SHADER_STAGE_FRAGMENT_BIT;
    VkDescriptorSetLayoutCreateInfo layout_info{};
    layout_info.sType = VK_STRUCTURE_TYPE_DESCRIPTOR_SET_LAYOUT_CREATE_INFO;
    layout_info.bindingCount = 1;
    layout_info.pBindings = &sampler;
    VkResult status = vkCreateDescriptorSetLayout(
            session.device, &layout_info, nullptr, &session.texture_set_layout);
    if (status != VK_SUCCESS) {
        set_diagnostic(result_message("vkCreateDescriptorSetLayout", status));
        return false;
    }
    VkDescriptorPoolSize pool_size{VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER, 8192};
    VkDescriptorPoolCreateInfo pool_info{};
    pool_info.sType = VK_STRUCTURE_TYPE_DESCRIPTOR_POOL_CREATE_INFO;
    pool_info.maxSets = 8192;
    pool_info.poolSizeCount = 1;
    pool_info.pPoolSizes = &pool_size;
    status = vkCreateDescriptorPool(
            session.device, &pool_info, nullptr, &session.descriptor_pool);
    if (status != VK_SUCCESS) {
        set_diagnostic(result_message("vkCreateDescriptorPool", status));
        return false;
    }
    VkPipelineLayoutCreateInfo pipeline_layout{};
    pipeline_layout.sType = VK_STRUCTURE_TYPE_PIPELINE_LAYOUT_CREATE_INFO;
    pipeline_layout.setLayoutCount = 1;
    pipeline_layout.pSetLayouts = &session.texture_set_layout;
    status = vkCreatePipelineLayout(
            session.device, &pipeline_layout, nullptr, &session.pipeline_layout);
    if (status != VK_SUCCESS) {
        set_diagnostic(result_message("vkCreatePipelineLayout", status));
        return false;
    }
    return create_pipeline_locked(false, &session.colored_pipeline)
            && create_pipeline_locked(true, &session.textured_pipeline);
}

bool create_render_resources_locked() {
    VkAttachmentDescription attachment{};
    attachment.format = session.format;
    attachment.samples = VK_SAMPLE_COUNT_1_BIT;
    attachment.loadOp = VK_ATTACHMENT_LOAD_OP_CLEAR;
    attachment.storeOp = VK_ATTACHMENT_STORE_OP_STORE;
    attachment.stencilLoadOp = VK_ATTACHMENT_LOAD_OP_DONT_CARE;
    attachment.stencilStoreOp = VK_ATTACHMENT_STORE_OP_DONT_CARE;
    attachment.initialLayout = VK_IMAGE_LAYOUT_UNDEFINED;
    attachment.finalLayout = VK_IMAGE_LAYOUT_PRESENT_SRC_KHR;
    VkAttachmentReference reference{0, VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL};
    VkSubpassDescription subpass{};
    subpass.pipelineBindPoint = VK_PIPELINE_BIND_POINT_GRAPHICS;
    subpass.colorAttachmentCount = 1;
    subpass.pColorAttachments = &reference;
    VkSubpassDependency dependency{};
    dependency.srcSubpass = VK_SUBPASS_EXTERNAL;
    dependency.dstSubpass = 0;
    dependency.srcStageMask = VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT;
    dependency.dstStageMask = VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT;
    dependency.dstAccessMask = VK_ACCESS_COLOR_ATTACHMENT_WRITE_BIT;
    VkRenderPassCreateInfo render_pass_info{};
    render_pass_info.sType = VK_STRUCTURE_TYPE_RENDER_PASS_CREATE_INFO;
    render_pass_info.attachmentCount = 1;
    render_pass_info.pAttachments = &attachment;
    render_pass_info.subpassCount = 1;
    render_pass_info.pSubpasses = &subpass;
    render_pass_info.dependencyCount = 1;
    render_pass_info.pDependencies = &dependency;
    VkResult result = vkCreateRenderPass(
            session.device, &render_pass_info, nullptr, &session.render_pass);
    if (result != VK_SUCCESS) {
        set_diagnostic(result_message("vkCreateRenderPass", result));
        return false;
    }

    // Offscreen targets use the same attachment format/subpass contract as the swapchain, so the
    // two basic graphics pipelines remain render-pass compatible.  Their final layout is sampled
    // directly by later FrameStream passes in the same submission.
    attachment.initialLayout = VK_IMAGE_LAYOUT_UNDEFINED;
    attachment.finalLayout = VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL;
    dependency.srcStageMask = VK_PIPELINE_STAGE_FRAGMENT_SHADER_BIT
            | VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT;
    dependency.srcAccessMask = VK_ACCESS_SHADER_READ_BIT;
    dependency.dstStageMask = VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT;
    dependency.dstAccessMask = VK_ACCESS_COLOR_ATTACHMENT_WRITE_BIT;
    VkSubpassDependency sampled_dependency{};
    sampled_dependency.srcSubpass = 0;
    sampled_dependency.dstSubpass = VK_SUBPASS_EXTERNAL;
    sampled_dependency.srcStageMask = VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT;
    sampled_dependency.srcAccessMask = VK_ACCESS_COLOR_ATTACHMENT_WRITE_BIT;
    sampled_dependency.dstStageMask = VK_PIPELINE_STAGE_FRAGMENT_SHADER_BIT;
    sampled_dependency.dstAccessMask = VK_ACCESS_SHADER_READ_BIT;
    VkSubpassDependency offscreen_dependencies[] = {dependency, sampled_dependency};
    render_pass_info.dependencyCount = 2;
    render_pass_info.pDependencies = offscreen_dependencies;
    result = vkCreateRenderPass(session.device, &render_pass_info, nullptr,
                                &session.offscreen_render_pass);
    if (result != VK_SUCCESS) {
        set_diagnostic(result_message("vkCreateRenderPass(offscreen)", result));
        return false;
    }
    attachment.loadOp = VK_ATTACHMENT_LOAD_OP_LOAD;
    attachment.initialLayout = VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL;
    result = vkCreateRenderPass(session.device, &render_pass_info, nullptr,
                                &session.offscreen_load_render_pass);
    if (result != VK_SUCCESS) {
        set_diagnostic(result_message("vkCreateRenderPass(offscreen load)", result));
        return false;
    }
    if (!create_pipeline_resources_locked()) return false;

    session.image_views.resize(session.images.size());
    session.framebuffers.resize(session.images.size());
    for (size_t index = 0; index < session.images.size(); ++index) {
        VkImageViewCreateInfo view_info{};
        view_info.sType = VK_STRUCTURE_TYPE_IMAGE_VIEW_CREATE_INFO;
        view_info.image = session.images[index];
        view_info.viewType = VK_IMAGE_VIEW_TYPE_2D;
        view_info.format = session.format;
        view_info.components = {VK_COMPONENT_SWIZZLE_IDENTITY, VK_COMPONENT_SWIZZLE_IDENTITY,
                                VK_COMPONENT_SWIZZLE_IDENTITY, VK_COMPONENT_SWIZZLE_IDENTITY};
        view_info.subresourceRange.aspectMask = VK_IMAGE_ASPECT_COLOR_BIT;
        view_info.subresourceRange.levelCount = 1;
        view_info.subresourceRange.layerCount = 1;
        result = vkCreateImageView(
                session.device, &view_info, nullptr, &session.image_views[index]);
        if (result != VK_SUCCESS) {
            set_diagnostic(result_message("vkCreateImageView", result));
            return false;
        }
        VkFramebufferCreateInfo framebuffer_info{};
        framebuffer_info.sType = VK_STRUCTURE_TYPE_FRAMEBUFFER_CREATE_INFO;
        framebuffer_info.renderPass = session.render_pass;
        framebuffer_info.attachmentCount = 1;
        framebuffer_info.pAttachments = &session.image_views[index];
        framebuffer_info.width = session.extent.width;
        framebuffer_info.height = session.extent.height;
        framebuffer_info.layers = 1;
        result = vkCreateFramebuffer(
                session.device, &framebuffer_info, nullptr, &session.framebuffers[index]);
        if (result != VK_SUCCESS) {
            set_diagnostic(result_message("vkCreateFramebuffer", result));
            return false;
        }
    }

    VkCommandPoolCreateInfo pool_info{};
    pool_info.sType = VK_STRUCTURE_TYPE_COMMAND_POOL_CREATE_INFO;
    pool_info.flags = VK_COMMAND_POOL_CREATE_RESET_COMMAND_BUFFER_BIT;
    pool_info.queueFamilyIndex = session.graphics_family;
    result = vkCreateCommandPool(session.device, &pool_info, nullptr, &session.command_pool);
    if (result != VK_SUCCESS) {
        set_diagnostic(result_message("vkCreateCommandPool", result));
        return false;
    }
    std::array<VkCommandBuffer, kFramesInFlight> buffers{};
    VkCommandBufferAllocateInfo allocate_info{};
    allocate_info.sType = VK_STRUCTURE_TYPE_COMMAND_BUFFER_ALLOCATE_INFO;
    allocate_info.commandPool = session.command_pool;
    allocate_info.level = VK_COMMAND_BUFFER_LEVEL_PRIMARY;
    allocate_info.commandBufferCount = kFramesInFlight;
    result = vkAllocateCommandBuffers(session.device, &allocate_info, buffers.data());
    if (result != VK_SUCCESS) {
        set_diagnostic(result_message("vkAllocateCommandBuffers", result));
        return false;
    }
    for (uint32_t index = 0; index < kFramesInFlight; ++index) {
        session.frames[index].command_buffer = buffers[index];
        VkSemaphoreCreateInfo semaphore_info{};
        semaphore_info.sType = VK_STRUCTURE_TYPE_SEMAPHORE_CREATE_INFO;
        result = vkCreateSemaphore(session.device, &semaphore_info, nullptr,
                                   &session.frames[index].image_available);
        if (result == VK_SUCCESS) {
            result = vkCreateSemaphore(session.device, &semaphore_info, nullptr,
                                       &session.frames[index].render_finished);
        }
        VkFenceCreateInfo fence_info{};
        fence_info.sType = VK_STRUCTURE_TYPE_FENCE_CREATE_INFO;
        fence_info.flags = VK_FENCE_CREATE_SIGNALED_BIT;
        if (result == VK_SUCCESS) {
            result = vkCreateFence(
                    session.device, &fence_info, nullptr, &session.frames[index].fence);
        }
        if (result != VK_SUCCESS) {
            set_diagnostic(result_message("creating frame synchronization", result));
            return false;
        }
    }
    return true;
}

SurfaceInfo current_surface_info_locked() {
    return SurfaceInfo{session.device_name, session.extent.width, session.extent.height,
                       static_cast<uint32_t>(session.images.size()),
                       static_cast<uint32_t>(session.format),
                       static_cast<uint32_t>(session.color_space),
                       static_cast<uint32_t>(session.present_mode),
                       session.graphics_family, session.present_family,
                       session.window_generation};
}

bool create_surface_locked(SurfaceInfo* output) {
    destroy_session_locked();
    session.window = rustedfabric_acquire_native_window_for_generation(
            &session.window_generation);
    if (session.window == nullptr) {
        set_diagnostic("Android Surface is detached");
        return false;
    }
    VkResult result = create_instance(&session.instance);
    if (result != VK_SUCCESS) {
        set_diagnostic(result_message("vkCreateInstance", result));
        destroy_session_locked();
        return false;
    }
    VkAndroidSurfaceCreateInfoKHR surface_info{};
    surface_info.sType = VK_STRUCTURE_TYPE_ANDROID_SURFACE_CREATE_INFO_KHR;
    surface_info.window = session.window;
    result = vkCreateAndroidSurfaceKHR(
            session.instance, &surface_info, nullptr, &session.surface);
    if (result != VK_SUCCESS) {
        set_diagnostic(result_message("vkCreateAndroidSurfaceKHR", result));
        destroy_session_locked();
        return false;
    }
    uint32_t device_count = 0;
    result = vkEnumeratePhysicalDevices(session.instance, &device_count, nullptr);
    if (result != VK_SUCCESS || device_count == 0) {
        set_diagnostic(result != VK_SUCCESS
                ? result_message("vkEnumeratePhysicalDevices", result)
                : "Android Vulkan instance has no physical device");
        destroy_session_locked();
        return false;
    }
    std::vector<VkPhysicalDevice> devices(device_count);
    result = vkEnumeratePhysicalDevices(
            session.instance, &device_count, devices.data());
    if (result != VK_SUCCESS || !choose_device_locked(devices)) {
        if (result != VK_SUCCESS) {
            set_diagnostic(result_message("vkEnumeratePhysicalDevices", result));
        }
        destroy_session_locked();
        return false;
    }

    std::set<uint32_t> unique_families = {
            session.graphics_family, session.present_family};
    float priority = 1.0f;
    std::vector<VkDeviceQueueCreateInfo> queue_infos;
    for (uint32_t family : unique_families) {
        VkDeviceQueueCreateInfo queue_info{};
        queue_info.sType = VK_STRUCTURE_TYPE_DEVICE_QUEUE_CREATE_INFO;
        queue_info.queueFamilyIndex = family;
        queue_info.queueCount = 1;
        queue_info.pQueuePriorities = &priority;
        queue_infos.push_back(queue_info);
    }
    const char* device_extensions[] = {VK_KHR_SWAPCHAIN_EXTENSION_NAME};
    VkDeviceCreateInfo device_info{};
    device_info.sType = VK_STRUCTURE_TYPE_DEVICE_CREATE_INFO;
    device_info.queueCreateInfoCount = static_cast<uint32_t>(queue_infos.size());
    device_info.pQueueCreateInfos = queue_infos.data();
    device_info.enabledExtensionCount = 1;
    device_info.ppEnabledExtensionNames = device_extensions;
    result = vkCreateDevice(
            session.physical_device, &device_info, nullptr, &session.device);
    if (result != VK_SUCCESS) {
        set_diagnostic(result_message("vkCreateDevice", result));
        destroy_session_locked();
        return false;
    }
    vkGetDeviceQueue(session.device, session.graphics_family, 0, &session.graphics_queue);
    vkGetDeviceQueue(session.device, session.present_family, 0, &session.present_queue);
    if (!create_swapchain_locked() || !create_render_resources_locked()) {
        destroy_session_locked();
        return false;
    }
    set_diagnostic("Android Vulkan swapchain is ready for clear-only presentation");
    if (output != nullptr) *output = current_surface_info_locked();
    return true;
}

bool surface_generation_current_locked() {
    return session.swapchain != VK_NULL_HANDLE
            && rustedfabric_native_window_generation() == session.window_generation;
}

bool ensure_geometry_buffer_locked(FrameSlot& slot, size_t required) {
    if (required == 0) return true;
    if (slot.geometry != VK_NULL_HANDLE && slot.geometry_capacity >= required) return true;
    if (slot.geometry != VK_NULL_HANDLE) {
        vkDestroyBuffer(session.device, slot.geometry, nullptr);
        slot.geometry = VK_NULL_HANDLE;
    }
    if (slot.geometry_memory != VK_NULL_HANDLE) {
        vkFreeMemory(session.device, slot.geometry_memory, nullptr);
        slot.geometry_memory = VK_NULL_HANDLE;
    }
    size_t capacity = 64 * 1024;
    while (capacity < required) {
        if (capacity > 256u * 1024u * 1024u / 2u) {
            set_diagnostic("Android Vulkan frame geometry exceeds 256 MiB");
            return false;
        }
        capacity *= 2;
    }
    VkBufferCreateInfo info{};
    info.sType = VK_STRUCTURE_TYPE_BUFFER_CREATE_INFO;
    info.size = capacity;
    info.usage = VK_BUFFER_USAGE_VERTEX_BUFFER_BIT | VK_BUFFER_USAGE_INDEX_BUFFER_BIT;
    info.sharingMode = VK_SHARING_MODE_EXCLUSIVE;
    VkResult status = vkCreateBuffer(session.device, &info, nullptr, &slot.geometry);
    VkMemoryRequirements requirements{};
    if (status == VK_SUCCESS) {
        vkGetBufferMemoryRequirements(session.device, slot.geometry, &requirements);
        if (!allocate_memory_locked(requirements,
                                    VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT
                                    | VK_MEMORY_PROPERTY_HOST_COHERENT_BIT,
                                    &slot.geometry_memory)) {
            status = VK_ERROR_OUT_OF_DEVICE_MEMORY;
        }
    }
    if (status == VK_SUCCESS) {
        status = vkBindBufferMemory(
                session.device, slot.geometry, slot.geometry_memory, 0);
    }
    if (status != VK_SUCCESS) {
        set_diagnostic(result_message("creating FrameStream geometry buffer", status));
        return false;
    }
    slot.geometry_capacity = capacity;
    return true;
}

TextureResource* texture_for_typed_handle_locked(uint64_t handle) {
    uint32_t type = static_cast<uint32_t>(handle >> 56);
    uint32_t generation = static_cast<uint32_t>((handle >> 32) & 0x00ffffffu);
    uint32_t slot = static_cast<uint32_t>(handle);
    if (type != 1 || generation != 1 || slot == 0 || slot > session.textures.size()) {
        return nullptr;
    }
    TextureResource& texture = session.textures[slot - 1];
    return texture.image == VK_NULL_HANDLE ? nullptr : &texture;
}

bool record_frame_stream_pass_locked(VkCommandBuffer command, FrameSlot& slot,
                                     const framestream::Frame& stream,
                                     const framestream::Pass& pass,
                                     VkRenderPass render_pass,
                                     VkFramebuffer framebuffer,
                                     VkExtent2D extent,
                                     TextureResource* target) {
    if (pass.viewport_x != 0 || pass.viewport_y != 0
            || pass.viewport_width != extent.width || pass.viewport_height != extent.height) {
        set_diagnostic("FrameStream pass viewport does not match its Android target");
        return false;
    }
    VkClearValue clear{};
    for (int channel = 0; channel < 4; ++channel) {
        clear.color.float32[channel] = pass.clear[channel];
    }
    VkRenderPassBeginInfo render{};
    render.sType = VK_STRUCTURE_TYPE_RENDER_PASS_BEGIN_INFO;
    render.renderPass = render_pass;
    render.framebuffer = framebuffer;
    render.renderArea.extent = extent;
    render.clearValueCount = 1;
    render.pClearValues = &clear;
    vkCmdBeginRenderPass(command, &render, VK_SUBPASS_CONTENTS_INLINE);
    VkViewport viewport{};
    viewport.width = static_cast<float>(extent.width);
    viewport.height = static_cast<float>(extent.height);
    viewport.maxDepth = 1.0f;
    vkCmdSetViewport(command, 0, 1, &viewport);
    VkRect2D full_scissor{{0, 0}, extent};

    for (uint32_t index = 0; index < pass.batch_count; ++index) {
        framestream::Batch batch;
        if (!framestream::batch(stream, pass.first_batch + index, &batch)) {
            set_diagnostic("FrameStream batch disappeared after validation");
            vkCmdEndRenderPass(command);
            return false;
        }
        bool textured = (batch.flags & 2u) != 0;
        VkPipeline pipeline = textured
                ? session.textured_pipeline : session.colored_pipeline;
        vkCmdBindPipeline(command, VK_PIPELINE_BIND_POINT_GRAPHICS, pipeline);
        if (textured) {
            TextureResource* texture = texture_for_typed_handle_locked(batch.primary_texture);
            if (texture == nullptr) {
                set_diagnostic("FrameStream references a stale Android texture handle");
                vkCmdEndRenderPass(command);
                return false;
            }
            if (texture == target) {
                set_diagnostic("FrameStream render target cannot sample itself");
                vkCmdEndRenderPass(command);
                return false;
            }
            vkCmdBindDescriptorSets(command, VK_PIPELINE_BIND_POINT_GRAPHICS,
                                    session.pipeline_layout, 0, 1,
                                    &texture->descriptor_set, 0, nullptr);
        }
        VkRect2D scissor = full_scissor;
        if ((batch.flags & 1u) != 0) {
            int32_t left = std::max(0, static_cast<int32_t>(std::floor(batch.clip[0])));
            int32_t top = std::max(0, static_cast<int32_t>(std::floor(batch.clip[1])));
            int32_t right = std::min(static_cast<int32_t>(extent.width),
                    static_cast<int32_t>(std::ceil(batch.clip[0] + batch.clip[2])));
            int32_t bottom = std::min(static_cast<int32_t>(extent.height),
                    static_cast<int32_t>(std::ceil(batch.clip[1] + batch.clip[3])));
            scissor.offset = {left, top};
            scissor.extent = {static_cast<uint32_t>(std::max(0, right - left)),
                              static_cast<uint32_t>(std::max(0, bottom - top))};
        }
        vkCmdSetScissor(command, 0, 1, &scissor);
        VkDeviceSize vertex_offset = batch.vertex_offset;
        vkCmdBindVertexBuffers(command, 0, 1, &slot.geometry, &vertex_offset);
        if ((batch.flags & 4u) != 0) {
            size_t index_base = (stream.vertices.byte_count + 3u) & ~size_t(3u);
            VkDeviceSize index_offset = index_base + batch.index_offset;
            vkCmdBindIndexBuffer(command, slot.geometry, index_offset,
                                 batch.index_type == 1
                                         ? VK_INDEX_TYPE_UINT16 : VK_INDEX_TYPE_UINT32);
            vkCmdDrawIndexed(command, batch.index_count, 1, 0, 0, 0);
        } else {
            vkCmdDraw(command, batch.vertex_count, 1, 0, 0);
        }
    }
    vkCmdEndRenderPass(command);
    return true;
}

bool present_frame_stream_locked(const framestream::Frame& stream,
                                 SurfaceInfo* output) {
    if (!surface_generation_current_locked()) {
        set_diagnostic("Android Surface changed after FrameStream resources were created");
        return false;
    }
    FrameSlot& slot = session.frames[session.next_frame];
    VkResult status = vkWaitForFences(
            session.device, 1, &slot.fence, VK_TRUE, kFenceTimeoutNanos);
    if (status != VK_SUCCESS) {
        set_diagnostic(result_message("vkWaitForFences(FrameStream)", status));
        return false;
    }
    uint32_t image_index = 0;
    status = vkAcquireNextImageKHR(session.device, session.swapchain,
                                   kAcquireTimeoutNanos, slot.image_available,
                                   VK_NULL_HANDLE, &image_index);
    if ((status != VK_SUCCESS && status != VK_SUBOPTIMAL_KHR)
            || image_index >= session.framebuffers.size()) {
        set_diagnostic(result_message("vkAcquireNextImageKHR(FrameStream)", status));
        return false;
    }
    size_t index_base = (stream.vertices.byte_count + 3u) & ~size_t(3u);
    size_t geometry_bytes = index_base + stream.indices.byte_count;
    if (!ensure_geometry_buffer_locked(slot, geometry_bytes)) return false;
    if (geometry_bytes != 0) {
        void* mapped = nullptr;
        status = vkMapMemory(session.device, slot.geometry_memory,
                             0, geometry_bytes, 0, &mapped);
        if (status != VK_SUCCESS) {
            set_diagnostic(result_message("vkMapMemory(FrameStream)", status));
            return false;
        }
        if (stream.vertices.byte_count != 0) {
            std::memcpy(mapped, stream.vertices.bytes, stream.vertices.byte_count);
        }
        if (stream.indices.byte_count != 0) {
            std::memcpy(static_cast<uint8_t*>(mapped) + index_base,
                        stream.indices.bytes, stream.indices.byte_count);
        }
        vkUnmapMemory(session.device, slot.geometry_memory);
    }

    vkResetFences(session.device, 1, &slot.fence);
    vkResetCommandBuffer(slot.command_buffer, 0);
    VkCommandBufferBeginInfo begin{};
    begin.sType = VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO;
    begin.flags = VK_COMMAND_BUFFER_USAGE_ONE_TIME_SUBMIT_BIT;
    status = vkBeginCommandBuffer(slot.command_buffer, &begin);
    if (status != VK_SUCCESS) {
        set_diagnostic(result_message("vkBeginCommandBuffer(FrameStream)", status));
        return false;
    }
    for (uint32_t pass_index = 0; pass_index < stream.passes.element_count; ++pass_index) {
        framestream::Pass pass;
        if (!framestream::pass(stream, pass_index, &pass)) {
            set_diagnostic("FrameStream pass disappeared after validation");
            return false;
        }
        bool swapchain = pass.target == 0;
        TextureResource* target = nullptr;
        VkRenderPass render_pass = session.render_pass;
        VkFramebuffer framebuffer = session.framebuffers[image_index];
        VkExtent2D extent = session.extent;
        if (!swapchain) {
            target = texture_for_typed_handle_locked(pass.target);
            if (target == nullptr || !target->render_target
                    || target->framebuffer == VK_NULL_HANDLE) {
                set_diagnostic("FrameStream references a stale or non-target pass image");
                return false;
            }
            render_pass = (pass.flags & 1u) != 0
                    ? session.offscreen_render_pass
                    : session.offscreen_load_render_pass;
            framebuffer = target->framebuffer;
            extent = {target->width, target->height};
        }
        if (!record_frame_stream_pass_locked(slot.command_buffer, slot, stream, pass,
                                             render_pass, framebuffer, extent, target)) {
            return false;
        }
    }
    status = vkEndCommandBuffer(slot.command_buffer);
    if (status != VK_SUCCESS) {
        set_diagnostic(result_message("vkEndCommandBuffer(FrameStream)", status));
        return false;
    }
    VkPipelineStageFlags wait_stage = VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT;
    VkSubmitInfo submit{};
    submit.sType = VK_STRUCTURE_TYPE_SUBMIT_INFO;
    submit.waitSemaphoreCount = 1;
    submit.pWaitSemaphores = &slot.image_available;
    submit.pWaitDstStageMask = &wait_stage;
    submit.commandBufferCount = 1;
    submit.pCommandBuffers = &slot.command_buffer;
    submit.signalSemaphoreCount = 1;
    submit.pSignalSemaphores = &slot.render_finished;
    status = vkQueueSubmit(session.graphics_queue, 1, &submit, slot.fence);
    if (status != VK_SUCCESS) {
        set_diagnostic(result_message("vkQueueSubmit(FrameStream)", status));
        return false;
    }
    VkPresentInfoKHR present{};
    present.sType = VK_STRUCTURE_TYPE_PRESENT_INFO_KHR;
    present.waitSemaphoreCount = 1;
    present.pWaitSemaphores = &slot.render_finished;
    present.swapchainCount = 1;
    present.pSwapchains = &session.swapchain;
    present.pImageIndices = &image_index;
    status = vkQueuePresentKHR(session.present_queue, &present);
    // Identity pre-transform is intentional on Android landscape surfaces. Some compositors
    // report that legal swapchain as SUBOPTIMAL on every present even though it is the only path
    // that avoids a second 90-degree rotation. Treat it as a rendered frame and defer recreation
    // until the surface is actually OUT_OF_DATE.
    if (status != VK_SUCCESS && status != VK_SUBOPTIMAL_KHR) {
        set_diagnostic(result_message("vkQueuePresentKHR(FrameStream)", status));
        return false;
    }
    session.next_frame = (session.next_frame + 1) % kFramesInFlight;
    set_diagnostic("Android Vulkan FrameStream rendered and presented");
    if (output != nullptr) *output = current_surface_info_locked();
    return true;
}

bool present_clear_locked(float red, float green, float blue, float alpha,
                          SurfaceInfo* output, bool retry) {
    if (!surface_generation_current_locked()) {
        if (!create_surface_locked(output)) return false;
    }
    FrameSlot& frame = session.frames[session.next_frame];
    VkResult result = vkWaitForFences(
            session.device, 1, &frame.fence, VK_TRUE, kFenceTimeoutNanos);
    if (result != VK_SUCCESS) {
        set_diagnostic(result_message("vkWaitForFences", result));
        return false;
    }
    uint32_t image_index = 0;
    result = vkAcquireNextImageKHR(session.device, session.swapchain,
                                   kAcquireTimeoutNanos, frame.image_available,
                                   VK_NULL_HANDLE, &image_index);
    if (result == VK_ERROR_OUT_OF_DATE_KHR || result == VK_SUBOPTIMAL_KHR) {
        if (retry && create_surface_locked(output)) {
            return present_clear_locked(red, green, blue, alpha, output, false);
        }
        set_diagnostic("Android Vulkan swapchain became out of date");
        return false;
    }
    if (result == VK_TIMEOUT || result == VK_NOT_READY) return false;
    if (result != VK_SUCCESS || image_index >= session.framebuffers.size()) {
        set_diagnostic(result != VK_SUCCESS
                ? result_message("vkAcquireNextImageKHR", result)
                : "vkAcquireNextImageKHR returned an invalid image index");
        return false;
    }
    vkResetFences(session.device, 1, &frame.fence);
    vkResetCommandBuffer(frame.command_buffer, 0);
    VkCommandBufferBeginInfo begin_info{};
    begin_info.sType = VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO;
    begin_info.flags = VK_COMMAND_BUFFER_USAGE_ONE_TIME_SUBMIT_BIT;
    result = vkBeginCommandBuffer(frame.command_buffer, &begin_info);
    if (result != VK_SUCCESS) {
        set_diagnostic(result_message("vkBeginCommandBuffer", result));
        return false;
    }
    VkClearValue clear{};
    clear.color.float32[0] = red;
    clear.color.float32[1] = green;
    clear.color.float32[2] = blue;
    clear.color.float32[3] = alpha;
    VkRenderPassBeginInfo render_info{};
    render_info.sType = VK_STRUCTURE_TYPE_RENDER_PASS_BEGIN_INFO;
    render_info.renderPass = session.render_pass;
    render_info.framebuffer = session.framebuffers[image_index];
    render_info.renderArea.extent = session.extent;
    render_info.clearValueCount = 1;
    render_info.pClearValues = &clear;
    vkCmdBeginRenderPass(frame.command_buffer, &render_info, VK_SUBPASS_CONTENTS_INLINE);
    vkCmdEndRenderPass(frame.command_buffer);
    result = vkEndCommandBuffer(frame.command_buffer);
    if (result != VK_SUCCESS) {
        set_diagnostic(result_message("vkEndCommandBuffer", result));
        return false;
    }
    VkPipelineStageFlags wait_stage = VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT;
    VkSubmitInfo submit_info{};
    submit_info.sType = VK_STRUCTURE_TYPE_SUBMIT_INFO;
    submit_info.waitSemaphoreCount = 1;
    submit_info.pWaitSemaphores = &frame.image_available;
    submit_info.pWaitDstStageMask = &wait_stage;
    submit_info.commandBufferCount = 1;
    submit_info.pCommandBuffers = &frame.command_buffer;
    submit_info.signalSemaphoreCount = 1;
    submit_info.pSignalSemaphores = &frame.render_finished;
    result = vkQueueSubmit(session.graphics_queue, 1, &submit_info, frame.fence);
    if (result != VK_SUCCESS) {
        set_diagnostic(result_message("vkQueueSubmit", result));
        return false;
    }
    VkPresentInfoKHR present_info{};
    present_info.sType = VK_STRUCTURE_TYPE_PRESENT_INFO_KHR;
    present_info.waitSemaphoreCount = 1;
    present_info.pWaitSemaphores = &frame.render_finished;
    present_info.swapchainCount = 1;
    present_info.pSwapchains = &session.swapchain;
    present_info.pImageIndices = &image_index;
    result = vkQueuePresentKHR(session.present_queue, &present_info);
    if (result == VK_ERROR_OUT_OF_DATE_KHR || result == VK_SUBOPTIMAL_KHR) {
        if (retry && create_surface_locked(output)) {
            return present_clear_locked(red, green, blue, alpha, output, false);
        }
        return false;
    }
    if (result != VK_SUCCESS) {
        set_diagnostic(result_message("vkQueuePresentKHR", result));
        return false;
    }
    session.next_frame = (session.next_frame + 1) % kFramesInFlight;
    set_diagnostic("Android Vulkan clear-only frame presented");
    if (output != nullptr) *output = current_surface_info_locked();
    return true;
}

}  // namespace

int initialize(int backend_major, int backend_minor,
               int frame_major, int frame_minor,
               int resource_major, int resource_minor) {
    std::lock_guard<std::mutex> lock(state_mutex);
    if (initialized) return 0;
    if (backend_major != kBackendAbiMajor || backend_minor > kBackendAbiMinor) {
        set_diagnostic("Android Vulkan native-backend ABI mismatch");
        return 1;
    }
    if (frame_major != kFrameStreamMajor || frame_minor > kFrameStreamMinor) {
        set_diagnostic("Android Vulkan FrameStream ABI mismatch");
        return 2;
    }
    if (resource_major != kResourceStreamMajor || resource_minor > kResourceStreamMinor) {
        set_diagnostic("Android Vulkan ResourceStream ABI mismatch");
        return 3;
    }
    return probe_locked() ? 0 : 10;
}

std::string last_diagnostic() {
    std::lock_guard<std::mutex> lock(state_mutex);
    return diagnostic;
}

uint32_t instance_version() {
    std::lock_guard<std::mutex> lock(state_mutex);
    return loader_instance_version;
}

size_t device_count() {
    std::lock_guard<std::mutex> lock(state_mutex);
    return probe_devices.size();
}

DeviceInfo device(size_t index) {
    std::lock_guard<std::mutex> lock(state_mutex);
    return index < probe_devices.size() ? probe_devices[index] : DeviceInfo{};
}

std::array<int64_t, 4> surface_state() {
    uint64_t generation = 0;
    ANativeWindow* window = rustedfabric_acquire_native_window_for_generation(&generation);
    std::array<int64_t, 4> result = {
            static_cast<int64_t>(generation), window == nullptr ? 0 : 1,
            window == nullptr ? 0 : ANativeWindow_getWidth(window),
            window == nullptr ? 0 : ANativeWindow_getHeight(window)};
    rustedfabric_release_native_window(window);
    return result;
}

bool create_surface(SurfaceInfo* result) {
    std::lock_guard<std::mutex> lock(state_mutex);
    if (!initialized) {
        set_diagnostic("Android Vulkan backend is not initialized");
        return false;
    }
    return create_surface_locked(result);
}

uint64_t upload_texture(uint32_t width, uint32_t height,
                        const uint8_t* rgba, size_t byte_count) {
    std::lock_guard<std::mutex> lock(state_mutex);
    return upload_texture_locked(width, height, rgba, byte_count);
}

bool update_texture_region(uint64_t texture_handle, uint32_t x, uint32_t y,
                           uint32_t width, uint32_t height,
                           const uint8_t* rgba, size_t byte_count) {
    std::lock_guard<std::mutex> lock(state_mutex);
    return update_texture_region_locked(texture_handle, x, y, width, height,
                                        rgba, byte_count);
}

uint64_t create_render_target(uint32_t width, uint32_t height) {
    std::lock_guard<std::mutex> lock(state_mutex);
    return create_render_target_locked(width, height);
}

bool destroy_texture(uint64_t texture_handle) {
    std::lock_guard<std::mutex> lock(state_mutex);
    if (texture_handle == 0 || texture_handle > session.textures.size()) {
        set_diagnostic("Android Vulkan texture handle is out of range");
        return false;
    }
    TextureResource& texture = session.textures[texture_handle - 1];
    if (texture.image == VK_NULL_HANDLE) {
        set_diagnostic("Android Vulkan texture handle is stale");
        return false;
    }
    vkDeviceWaitIdle(session.device);
    if (texture.framebuffer != VK_NULL_HANDLE) {
        vkDestroyFramebuffer(session.device, texture.framebuffer, nullptr);
    }
    vkDestroySampler(session.device, texture.sampler, nullptr);
    vkDestroyImageView(session.device, texture.view, nullptr);
    vkDestroyImage(session.device, texture.image, nullptr);
    vkFreeMemory(session.device, texture.memory, nullptr);
    texture = TextureResource{};
    set_diagnostic("Android Vulkan texture destroyed");
    return true;
}

bool present_frame_stream(const uint8_t* bytes, size_t byte_count,
                          SurfaceInfo* result) {
    framestream::Frame frame;
    std::string error;
    if (!framestream::decode(bytes, byte_count, &frame, &error)) {
        std::lock_guard<std::mutex> lock(state_mutex);
        set_diagnostic("FrameStream rejected: " + error);
        return false;
    }
    std::lock_guard<std::mutex> lock(state_mutex);
    return present_frame_stream_locked(frame, result);
}

bool present_clear(float red, float green, float blue, float alpha,
                   SurfaceInfo* result) {
    std::lock_guard<std::mutex> lock(state_mutex);
    return present_clear_locked(red, green, blue, alpha, result, true);
}

void destroy_surface() {
    std::lock_guard<std::mutex> lock(state_mutex);
    destroy_session_locked();
    set_diagnostic("Android Vulkan presentation surface is destroyed");
}

void shutdown() {
    std::lock_guard<std::mutex> lock(state_mutex);
    destroy_session_locked();
    initialized = false;
    probe_devices.clear();
    set_diagnostic("Android Vulkan backend is shut down");
}

}  // namespace rustedfabric::vulkan
