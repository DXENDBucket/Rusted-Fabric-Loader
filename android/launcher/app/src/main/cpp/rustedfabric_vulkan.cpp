#include <jni.h>
#include <dlfcn.h>

#include <array>
#include <cstdint>
#include <string>

#include "rustedfabric_vulkan_backend.h"

namespace {

using rustedfabric::vulkan::SurfaceInfo;

jlongArray surface_info_array(JNIEnv* env, const SurfaceInfo& info) {
    const jlong values[9] = {
            info.width, info.height, info.image_count, info.image_format,
            info.color_space, info.present_mode, info.graphics_queue_family,
            info.present_queue_family, static_cast<jlong>(info.window_generation)};
    jlongArray result = env->NewLongArray(9);
    if (result != nullptr) env->SetLongArrayRegion(result, 0, 9, values);
    return result;
}

jlongArray create_surface(JNIEnv* env) {
    SurfaceInfo info;
    return rustedfabric::vulkan::create_surface(&info)
            ? surface_info_array(env, info) : nullptr;
}

jlongArray present_clear(JNIEnv* env, float red, float green, float blue, float alpha) {
    SurfaceInfo info;
    return rustedfabric::vulkan::present_clear(red, green, blue, alpha, &info)
            ? surface_info_array(env, info) : nullptr;
}

}  // namespace

extern "C" JNIEXPORT jint JNICALL
Java_io_github_endx_vulkanmod_android_AndroidVulkanPlatformDriver_nativeInitialize(
        JNIEnv*, jclass, jint backend_major, jint backend_minor,
        jint frame_major, jint frame_minor, jint resource_major, jint resource_minor) {
    return rustedfabric::vulkan::initialize(
            backend_major, backend_minor, frame_major, frame_minor,
            resource_major, resource_minor);
}

extern "C" JNIEXPORT jstring JNICALL
Java_io_github_endx_vulkanmod_android_AndroidVulkanPlatformDriver_nativeLastDiagnostic(
        JNIEnv* env, jclass) {
    std::string diagnostic = rustedfabric::vulkan::last_diagnostic();
    return env->NewStringUTF(diagnostic.c_str());
}

extern "C" JNIEXPORT jint JNICALL
Java_io_github_endx_vulkanmod_android_AndroidVulkanPlatformDriver_nativeInstanceVersion(
        JNIEnv*, jclass) {
    return static_cast<jint>(rustedfabric::vulkan::instance_version());
}

extern "C" JNIEXPORT jint JNICALL
Java_io_github_endx_vulkanmod_android_AndroidVulkanPlatformDriver_nativeDeviceCount(
        JNIEnv*, jclass) {
    return static_cast<jint>(rustedfabric::vulkan::device_count());
}

extern "C" JNIEXPORT jstring JNICALL
Java_io_github_endx_vulkanmod_android_AndroidVulkanPlatformDriver_nativeDeviceName(
        JNIEnv* env, jclass, jint index) {
    auto device = rustedfabric::vulkan::device(static_cast<size_t>(index));
    return env->NewStringUTF(device.name.c_str());
}

#define DEVICE_INTEGER_METHOD(method_name, expression)                                      \
extern "C" JNIEXPORT jint JNICALL                                                          \
Java_io_github_endx_vulkanmod_android_AndroidVulkanPlatformDriver_##method_name(             \
        JNIEnv*, jclass, jint index) {                                                       \
    auto device = rustedfabric::vulkan::device(static_cast<size_t>(index));                  \
    return static_cast<jint>(device.expression);                                             \
}

DEVICE_INTEGER_METHOD(nativeDeviceVendorId, vendor_id)
DEVICE_INTEGER_METHOD(nativeDeviceId, device_id)
DEVICE_INTEGER_METHOD(nativeDeviceType, device_type)
DEVICE_INTEGER_METHOD(nativeDeviceApiVersion, api_version)
DEVICE_INTEGER_METHOD(nativeDeviceDriverVersion, driver_version)

#undef DEVICE_INTEGER_METHOD

extern "C" JNIEXPORT jlongArray JNICALL
Java_io_github_endx_vulkanmod_android_AndroidVulkanPlatformDriver_nativeSurfaceState(
        JNIEnv* env, jclass) {
    std::array<int64_t, 4> state = rustedfabric::vulkan::surface_state();
    jlong values[4] = {state[0], state[1], state[2], state[3]};
    jlongArray result = env->NewLongArray(4);
    if (result != nullptr) env->SetLongArrayRegion(result, 0, 4, values);
    return result;
}

extern "C" JNIEXPORT jlongArray JNICALL
Java_io_github_endx_vulkanmod_android_AndroidVulkanPlatformDriver_nativeCreateSurface(
        JNIEnv* env, jclass) {
    return create_surface(env);
}

extern "C" JNIEXPORT jlong JNICALL
Java_io_github_endx_vulkanmod_android_AndroidVulkanPlatformDriver_nativeUploadTexture(
        JNIEnv* env, jclass, jint width, jint height, jbyteArray rgba) {
    if (rgba == nullptr || width <= 0 || height <= 0) return 0;
    jsize size = env->GetArrayLength(rgba);
    jbyte* bytes = env->GetByteArrayElements(rgba, nullptr);
    if (bytes == nullptr) return 0;
    uint64_t handle = rustedfabric::vulkan::upload_texture(
            static_cast<uint32_t>(width), static_cast<uint32_t>(height),
            reinterpret_cast<const uint8_t*>(bytes), static_cast<size_t>(size));
    env->ReleaseByteArrayElements(rgba, bytes, JNI_ABORT);
    return static_cast<jlong>(handle);
}

extern "C" JNIEXPORT jboolean JNICALL
Java_io_github_endx_vulkanmod_android_AndroidVulkanPlatformDriver_nativeUpdateTextureRegion(
        JNIEnv* env, jclass, jlong handle, jint x, jint y,
        jint width, jint height, jbyteArray rgba) {
    if (handle <= 0 || x < 0 || y < 0 || width <= 0 || height <= 0 || rgba == nullptr) {
        return JNI_FALSE;
    }
    jsize size = env->GetArrayLength(rgba);
    jbyte* bytes = env->GetByteArrayElements(rgba, nullptr);
    if (bytes == nullptr) return JNI_FALSE;
    bool updated = rustedfabric::vulkan::update_texture_region(
            static_cast<uint64_t>(handle), static_cast<uint32_t>(x),
            static_cast<uint32_t>(y), static_cast<uint32_t>(width),
            static_cast<uint32_t>(height), reinterpret_cast<const uint8_t*>(bytes),
            static_cast<size_t>(size));
    env->ReleaseByteArrayElements(rgba, bytes, JNI_ABORT);
    return updated ? JNI_TRUE : JNI_FALSE;
}

extern "C" JNIEXPORT jlong JNICALL
Java_io_github_endx_vulkanmod_android_AndroidVulkanPlatformDriver_nativeCreateRenderTarget(
        JNIEnv*, jclass, jint width, jint height) {
    if (width <= 0 || height <= 0) return 0;
    return static_cast<jlong>(rustedfabric::vulkan::create_render_target(
            static_cast<uint32_t>(width), static_cast<uint32_t>(height)));
}

extern "C" JNIEXPORT jboolean JNICALL
Java_io_github_endx_vulkanmod_android_AndroidVulkanPlatformDriver_nativeDestroyTexture(
        JNIEnv*, jclass, jlong handle) {
    return handle > 0 && rustedfabric::vulkan::destroy_texture(
            static_cast<uint64_t>(handle)) ? JNI_TRUE : JNI_FALSE;
}

extern "C" JNIEXPORT jlongArray JNICALL
Java_io_github_endx_vulkanmod_android_AndroidVulkanPlatformDriver_nativePresentFrameStream(
        JNIEnv* env, jclass, jobject stream) {
    if (stream == nullptr) return nullptr;
    auto* bytes = static_cast<const uint8_t*>(env->GetDirectBufferAddress(stream));
    jlong capacity = env->GetDirectBufferCapacity(stream);
    if (bytes == nullptr || capacity <= 0) return nullptr;
    SurfaceInfo info;
    return rustedfabric::vulkan::present_frame_stream(
            bytes, static_cast<size_t>(capacity), &info)
            ? surface_info_array(env, info) : nullptr;
}

extern "C" JNIEXPORT jlongArray JNICALL
Java_io_github_endx_vulkanmod_android_AndroidVulkanPlatformDriver_nativePresentClear(
        JNIEnv* env, jclass, jfloat red, jfloat green, jfloat blue, jfloat alpha) {
    return present_clear(env, red, green, blue, alpha);
}

extern "C" JNIEXPORT jdoubleArray JNICALL
Java_io_github_endx_vulkanmod_android_AndroidVulkanPlatformDriver_nativePollInputEvent(
        JNIEnv* env, jclass) {
    void* library = dlopen("libpojavexec.so", RTLD_NOW | RTLD_NOLOAD);
    if (library == nullptr) return nullptr;
    using Poll = bool (*)(int*, double*, double*, int*, int*, int*);
    auto poll = reinterpret_cast<Poll>(dlsym(library, "rustedfabric_poll_vulkan_input"));
    int kind = 0;
    double first = 0.0;
    double second = 0.0;
    int button = 0;
    int action = 0;
    int modifiers = 0;
    bool available = poll != nullptr && poll(&kind, &first, &second,
                                             &button, &action, &modifiers);
    dlclose(library);
    if (!available) return nullptr;
    jdouble values[] = {static_cast<double>(kind), first, second,
                        static_cast<double>(button), static_cast<double>(action),
                        static_cast<double>(modifiers)};
    jdoubleArray result = env->NewDoubleArray(6);
    if (result != nullptr) env->SetDoubleArrayRegion(result, 0, 6, values);
    return result;
}

extern "C" JNIEXPORT void JNICALL
Java_io_github_endx_vulkanmod_android_AndroidVulkanPlatformDriver_nativeDestroySurface(
        JNIEnv*, jclass) {
    rustedfabric::vulkan::destroy_surface();
}

extern "C" JNIEXPORT void JNICALL
Java_io_github_endx_vulkanmod_android_AndroidVulkanPlatformDriver_nativeShutdown(
        JNIEnv*, jclass) {
    rustedfabric::vulkan::shutdown();
}

// ART-side diagnostic entry points used by the launcher's renderer test. They deliberately use
// the same backend state and ABI versions as the embedded HotSpot adapter.
extern "C" JNIEXPORT jstring JNICALL
Java_io_github_endx_rustedfabric_android_launcher_jvm_NativeVulkanBridge_nativeStart(
        JNIEnv* env, jclass) {
    int status = rustedfabric::vulkan::initialize(1, 0, 1, 0, 1, 0);
    SurfaceInfo info;
    if (status != 0 || !rustedfabric::vulkan::create_surface(&info)) {
        std::string diagnostic = rustedfabric::vulkan::last_diagnostic();
        return env->NewStringUTF(diagnostic.c_str());
    }
    std::string result = "device=" + info.device_name
            + ", surface=" + std::to_string(info.width) + "x"
            + std::to_string(info.height) + ", images="
            + std::to_string(info.image_count) + ", generation="
            + std::to_string(info.window_generation);
    return env->NewStringUTF(result.c_str());
}

extern "C" JNIEXPORT jboolean JNICALL
Java_io_github_endx_rustedfabric_android_launcher_jvm_NativeVulkanBridge_nativePresentClear(
        JNIEnv*, jclass, jfloat red, jfloat green, jfloat blue, jfloat alpha) {
    return rustedfabric::vulkan::present_clear(red, green, blue, alpha, nullptr)
            ? JNI_TRUE : JNI_FALSE;
}

extern "C" JNIEXPORT jstring JNICALL
Java_io_github_endx_rustedfabric_android_launcher_jvm_NativeVulkanBridge_nativeLastDiagnostic(
        JNIEnv* env, jclass) {
    std::string diagnostic = rustedfabric::vulkan::last_diagnostic();
    return env->NewStringUTF(diagnostic.c_str());
}

extern "C" JNIEXPORT void JNICALL
Java_io_github_endx_rustedfabric_android_launcher_jvm_NativeVulkanBridge_nativeStop(
        JNIEnv*, jclass) {
    rustedfabric::vulkan::destroy_surface();
}
