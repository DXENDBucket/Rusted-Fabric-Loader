#include "rustedfabric_framestream.h"

#include <algorithm>
#include <array>
#include <cmath>
#include <cstring>
#include <limits>
#include <vector>

namespace rustedfabric::framestream {
namespace {

constexpr uint32_t kHeaderBytes = 64;
constexpr uint32_t kDirectoryBytes = 16;
constexpr uint32_t kMaximumBytes = 256u * 1024u * 1024u;
constexpr uint32_t kMaximumSections = 64;
constexpr uint32_t kPassBytes = 64;
constexpr uint32_t kBatchBytes = 64;
constexpr uint32_t kMaterialBytes = 160;
constexpr uint32_t kKnownFrameFlags = 0x0f;
constexpr uint32_t kKnownPassFlags = 0x07;
constexpr uint32_t kKnownBatchFlags = 0x07;

uint16_t u16(const uint8_t* data) {
    return static_cast<uint16_t>(data[0])
            | static_cast<uint16_t>(data[1]) << 8;
}

uint32_t u32(const uint8_t* data) {
    return static_cast<uint32_t>(data[0])
            | static_cast<uint32_t>(data[1]) << 8
            | static_cast<uint32_t>(data[2]) << 16
            | static_cast<uint32_t>(data[3]) << 24;
}

uint64_t u64(const uint8_t* data) {
    return static_cast<uint64_t>(u32(data))
            | static_cast<uint64_t>(u32(data + 4)) << 32;
}

float f32(const uint8_t* data) {
    uint32_t bits = u32(data);
    float value;
    std::memcpy(&value, &bits, sizeof(value));
    return value;
}

bool fail(std::string* error, const std::string& message) {
    if (error != nullptr) *error = message;
    return false;
}

bool checked_product(uint32_t first, uint32_t second, uint32_t* result) {
    uint64_t product = static_cast<uint64_t>(first) * second;
    if (product > std::numeric_limits<uint32_t>::max()) return false;
    *result = static_cast<uint32_t>(product);
    return true;
}

uint32_t crc32(const uint8_t* data, size_t size) {
    uint32_t crc = 0xffffffffu;
    for (size_t offset = 0; offset < size; ++offset) {
        crc ^= data[offset];
        for (int bit = 0; bit < 8; ++bit) {
            crc = (crc >> 1) ^ (0xedb88320u & (0u - (crc & 1u)));
        }
    }
    return ~crc;
}

bool typed_texture(uint64_t handle) {
    if (handle == 0) return false;
    return (handle >> 56) == 1 && ((handle >> 32) & 0x00ffffffu) != 0;
}

uint32_t vertex_stride(uint16_t layout) {
    if (layout == 1) return 24;
    if (layout == 2) return 32;
    if (layout == 3) return 64;
    return 0;
}

}  // namespace

bool decode(const uint8_t* bytes, size_t byte_count, Frame* frame,
            std::string* error) {
    if (frame == nullptr) return fail(error, "FrameStream output is null");
    *frame = Frame{};
    if (bytes == nullptr || byte_count < kHeaderBytes || byte_count > kMaximumBytes) {
        return fail(error, "FrameStream length is out of range");
    }
    if (bytes[0] != 'R' || bytes[1] != 'V' || bytes[2] != 'K' || bytes[3] != 'F') {
        return fail(error, "FrameStream magic is invalid");
    }
    if (u16(bytes + 4) != 1 || u16(bytes + 6) > 0) {
        return fail(error, "FrameStream ABI version is unsupported");
    }
    uint32_t header_bytes = u32(bytes + 8);
    uint32_t total_bytes = u32(bytes + 12);
    uint32_t flags = u32(bytes + 32);
    uint32_t width = u32(bytes + 36);
    uint32_t height = u32(bytes + 40);
    uint32_t section_count = u32(bytes + 44);
    uint32_t pass_count = u32(bytes + 48);
    uint32_t batch_count = u32(bytes + 52);
    if (section_count > kMaximumSections
            || header_bytes != kHeaderBytes + section_count * kDirectoryBytes
            || (header_bytes & 7u) != 0 || total_bytes != byte_count
            || total_bytes < header_bytes) {
        return fail(error, "FrameStream header or section directory is invalid");
    }
    if ((flags & ~kKnownFrameFlags) != 0 || u32(bytes + 60) != 0
            || width == 0 || height == 0 || width > 32768 || height > 32768
            || pass_count == 0 || pass_count > 1048576
            || batch_count > 4194304) {
        return fail(error, "FrameStream envelope fields are invalid");
    }
    if (static_cast<int64_t>(u64(bytes + 16)) < 0
            || static_cast<int64_t>(u64(bytes + 24)) < 0) {
        return fail(error, "FrameStream uses a reserved negative identifier");
    }
    if (u64(bytes + 24) != 0) {
        return fail(error, "FrameStream depends on an unimplemented ResourceStream sequence");
    }
    if ((flags & 1u) != 0) {
        if (crc32(bytes + header_bytes, total_bytes - header_bytes) != u32(bytes + 56)) {
            return fail(error, "FrameStream payload CRC32 does not match");
        }
    } else if (u32(bytes + 56) != 0) {
        return fail(error, "FrameStream CRC32 is present without its flag");
    }

    std::array<bool, 7> seen{};
    struct Range { uint32_t begin; uint32_t end; };
    std::vector<Range> ranges;
    for (uint32_t index = 0; index < section_count; ++index) {
        const uint8_t* entry = bytes + kHeaderBytes + index * kDirectoryBytes;
        uint32_t type = u32(entry);
        uint32_t offset = u32(entry + 4);
        uint32_t length = u32(entry + 8);
        uint32_t elements = u32(entry + 12);
        if (type == 0 || (type < seen.size() && seen[type])) {
            return fail(error, "FrameStream contains a duplicate or zero section type");
        }
        if (type < seen.size()) seen[type] = true;
        else if ((type & 0x80000000u) != 0) {
            return fail(error, "FrameStream contains an unknown required section");
        }
        uint64_t end = static_cast<uint64_t>(offset) + length;
        if (offset < header_bytes || (offset & 7u) != 0 || end > total_bytes
                || elements > 16777216) {
            return fail(error, "FrameStream section range is invalid");
        }
        if (length != 0) ranges.push_back({offset, static_cast<uint32_t>(end)});
        Section section{bytes + offset, length, elements};
        if (type == 1) frame->passes = section;
        else if (type == 2) frame->batches = section;
        else if (type == 3) frame->vertices = section;
        else if (type == 4) frame->indices = section;
        else if (type == 5) frame->materials = section;
    }
    std::sort(ranges.begin(), ranges.end(), [](const Range& left, const Range& right) {
        return left.begin < right.begin;
    });
    uint32_t previous_end = header_bytes;
    for (const Range& range : ranges) {
        if (range.begin < previous_end) return fail(error, "FrameStream sections overlap");
        previous_end = range.end;
    }
    if (!seen[1] || !seen[2] || !seen[3] || !seen[5]
            || frame->passes.element_count != pass_count
            || frame->batches.element_count != batch_count) {
        return fail(error, "FrameStream is missing a required core section");
    }
    uint32_t expected = 0;
    if (!checked_product(pass_count, kPassBytes, &expected)
            || expected != frame->passes.byte_count
            || !checked_product(batch_count, kBatchBytes, &expected)
            || expected != frame->batches.byte_count
            || !checked_product(frame->materials.element_count, kMaterialBytes, &expected)
            || expected != frame->materials.byte_count) {
        return fail(error, "FrameStream fixed-record section length is invalid");
    }
    uint32_t expected_first_batch = 0;
    for (uint32_t index = 0; index < pass_count; ++index) {
        Pass decoded;
        if (!pass(*frame, index, &decoded)) {
            return fail(error, "FrameStream pass is truncated");
        }
        bool final_pass = index + 1 == pass_count;
        bool swapchain = (decoded.flags & 4u) != 0;
        if ((decoded.flags & 2u) == 0 || swapchain != final_pass
                || (final_pass ? decoded.target != 0 : !typed_texture(decoded.target))
                || decoded.first_batch != expected_first_batch
                || decoded.batch_count > batch_count - expected_first_batch) {
            return fail(error, "FrameStream pass graph or target handle is invalid");
        }
        expected_first_batch += decoded.batch_count;
    }
    if (expected_first_batch != batch_count) {
        return fail(error, "FrameStream passes do not consume every batch exactly once");
    }
    for (uint32_t index = 0; index < batch_count; ++index) {
        Batch draw;
        if (!batch(*frame, index, &draw)) return fail(error, "FrameStream batch is truncated");
        uint32_t stride = vertex_stride(draw.vertex_layout);
        uint64_t vertex_end = static_cast<uint64_t>(draw.vertex_offset)
                + static_cast<uint64_t>(draw.vertex_count) * stride;
        uint32_t index_stride = draw.index_type == 1 ? 2u : draw.index_type == 2 ? 4u : 0u;
        uint64_t index_end = static_cast<uint64_t>(draw.index_offset)
                + static_cast<uint64_t>(draw.index_count) * index_stride;
        bool textured = (draw.flags & 2u) != 0;
        bool indexed = (draw.flags & 4u) != 0;
        if ((draw.flags & ~kKnownBatchFlags) != 0 || draw.topology != 1 || stride == 0
                || draw.material >= frame->materials.element_count
                || vertex_end > frame->vertices.byte_count
                || (indexed != (draw.index_type != 0))
                || (indexed && (frame->indices.bytes == nullptr
                                || index_end > frame->indices.byte_count))
                || (!indexed && (draw.index_count != 0 || draw.index_offset != 0))
                || (textured != typed_texture(draw.primary_texture))
                || (!textured && draw.primary_texture != 0)
                || draw.secondary_texture != 0) {
            return fail(error, "FrameStream batch fields or resource handles are invalid");
        }
        Material state;
        if (!material(*frame, draw.material, &state)
                || state.blend != 0 || state.filter > 1 || state.effect > 7
                || state.effect != 0) {
            return fail(error,
                    "Android FrameStream currently requires plain normal-blend materials");
        }
    }
    frame->frame_id = u64(bytes + 16);
    frame->width = width;
    frame->height = height;
    if (error != nullptr) error->clear();
    return true;
}

bool pass(const Frame& frame, uint32_t index, Pass* result) {
    if (result == nullptr || index >= frame.passes.element_count) return false;
    const uint8_t* data = frame.passes.bytes + index * kPassBytes;
    result->target = u64(data);
    result->first_batch = u32(data + 8);
    result->batch_count = u32(data + 12);
    result->flags = u32(data + 16);
    result->viewport_x = static_cast<int32_t>(u32(data + 20));
    result->viewport_y = static_cast<int32_t>(u32(data + 24));
    result->viewport_width = u32(data + 28);
    result->viewport_height = u32(data + 32);
    for (int channel = 0; channel < 4; ++channel) {
        result->clear[channel] = f32(data + 36 + channel * 4);
        if (!std::isfinite(result->clear[channel])) return false;
    }
    return (result->flags & ~kKnownPassFlags) == 0
            && result->viewport_width != 0 && result->viewport_height != 0
            && u32(data + 56) == 0 && u32(data + 60) == 0;
}

bool batch(const Frame& frame, uint32_t index, Batch* result) {
    if (result == nullptr || index >= frame.batches.element_count) return false;
    const uint8_t* data = frame.batches.bytes + index * kBatchBytes;
    result->material = u32(data);
    result->flags = u32(data + 4);
    result->primary_texture = u64(data + 8);
    result->secondary_texture = u64(data + 16);
    result->vertex_offset = u32(data + 24);
    result->vertex_count = u32(data + 28);
    result->index_offset = u32(data + 32);
    result->index_count = u32(data + 36);
    for (int value = 0; value < 4; ++value) {
        result->clip[value] = f32(data + 40 + value * 4);
        if (!std::isfinite(result->clip[value])) return false;
    }
    result->topology = u16(data + 56);
    result->index_type = u16(data + 58);
    result->vertex_layout = u16(data + 60);
    return u16(data + 62) == 0;
}

bool material(const Frame& frame, uint32_t index, Material* result) {
    if (result == nullptr || index >= frame.materials.element_count) return false;
    const uint8_t* data = frame.materials.bytes + index * kMaterialBytes;
    result->blend = u32(data + 4);
    result->filter = u32(data + 8);
    result->effect = u32(data + 12);
    return u32(data) == 0 && u64(data + 16) == 0
            && u32(data + 72) == 0 && u32(data + 76) == 0
            && u32(data + 68) <= 20;
}

}  // namespace rustedfabric::framestream
