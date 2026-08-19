#pragma once

#include <array>
#include <cstddef>
#include <cstdint>
#include <string>

namespace rustedfabric::vulkan {

struct DeviceInfo {
    std::string name;
    uint32_t vendor_id = 0;
    uint32_t device_id = 0;
    uint32_t device_type = 0;
    uint32_t api_version = 0;
    uint32_t driver_version = 0;
};

struct SurfaceInfo {
    std::string device_name;
    uint32_t width = 0;
    uint32_t height = 0;
    uint32_t image_count = 0;
    uint32_t image_format = 0;
    uint32_t color_space = 0;
    uint32_t present_mode = 0;
    uint32_t graphics_queue_family = 0;
    uint32_t present_queue_family = 0;
    uint64_t window_generation = 0;
};

int initialize(int backend_major, int backend_minor,
               int frame_major, int frame_minor,
               int resource_major, int resource_minor);
std::string last_diagnostic();
uint32_t instance_version();
size_t device_count();
DeviceInfo device(size_t index);
std::array<int64_t, 4> surface_state();

bool create_surface(SurfaceInfo* result);
uint64_t upload_texture(uint32_t width, uint32_t height,
                        const uint8_t* rgba, size_t byte_count);
bool update_texture_region(uint64_t texture_handle, uint32_t x, uint32_t y,
                           uint32_t width, uint32_t height,
                           const uint8_t* rgba, size_t byte_count);
uint64_t create_render_target(uint32_t width, uint32_t height);
bool destroy_texture(uint64_t texture_handle);
bool present_frame_stream(const uint8_t* bytes, size_t byte_count,
                          SurfaceInfo* result);
bool present_clear(float red, float green, float blue, float alpha,
                   SurfaceInfo* result);
void destroy_surface();
void shutdown();

}  // namespace rustedfabric::vulkan
