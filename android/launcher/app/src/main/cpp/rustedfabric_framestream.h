#pragma once

#include <cstddef>
#include <cstdint>
#include <string>

namespace rustedfabric::framestream {

struct Section {
    const uint8_t* bytes = nullptr;
    uint32_t byte_count = 0;
    uint32_t element_count = 0;
};

struct Pass {
    uint64_t target = 0;
    uint32_t first_batch = 0;
    uint32_t batch_count = 0;
    uint32_t flags = 0;
    int32_t viewport_x = 0;
    int32_t viewport_y = 0;
    uint32_t viewport_width = 0;
    uint32_t viewport_height = 0;
    float clear[4]{};
};

struct Batch {
    uint32_t material = 0;
    uint32_t flags = 0;
    uint64_t primary_texture = 0;
    uint64_t secondary_texture = 0;
    uint32_t vertex_offset = 0;
    uint32_t vertex_count = 0;
    uint32_t index_offset = 0;
    uint32_t index_count = 0;
    float clip[4]{};
    uint16_t topology = 0;
    uint16_t index_type = 0;
    uint16_t vertex_layout = 0;
};

struct Material {
    uint32_t blend = 0;
    uint32_t filter = 0;
    uint32_t effect = 0;
};

struct Frame {
    uint64_t frame_id = 0;
    uint32_t width = 0;
    uint32_t height = 0;
    Section passes;
    Section batches;
    Section vertices;
    Section indices;
    Section materials;
};

bool decode(const uint8_t* bytes, size_t byte_count, Frame* frame,
            std::string* error);
bool pass(const Frame& frame, uint32_t index, Pass* result);
bool batch(const Frame& frame, uint32_t index, Batch* result);
bool material(const Frame& frame, uint32_t index, Material* result);

}  // namespace rustedfabric::framestream
