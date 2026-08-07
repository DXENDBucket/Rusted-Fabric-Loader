#include <jni.h>
#include <android/native_window.h>
#include <android/log.h>
#include <EGL/egl.h>
#include <array>
#include <atomic>
#include <cstdint>
#include <deque>
#include <mutex>

#include "rustedfabric_renderbridge.h"

namespace {
constexpr jint kJniVersion18 = 0x00010008;
constexpr const char* kTag = "RustedFabricGLFW";
struct RenderContext {
    EGLContext context = EGL_NO_CONTEXT;
    EGLSurface surface = EGL_NO_SURFACE;
    ANativeWindow* window = nullptr;
    uint64_t window_generation = 0;
    bool surface_unavailable_reported = false;
};

JavaVM* hotspot_vm = nullptr;
EGLDisplay egl_display = EGL_NO_DISPLAY;
EGLConfig egl_config = nullptr;
std::mutex egl_mutex;
thread_local RenderContext* current_context = nullptr;
std::array<float, 6> gamepad_axes{};
std::array<unsigned char, 16> gamepad_buttons{};
std::array<unsigned char, 64> gamepad_state{};

using CursorEnterCallback = void (*)(void*, int);
using CursorPosCallback = void (*)(void*, double, double);
using MouseButtonCallback = void (*)(void*, int, int, int);
using ScrollCallback = void (*)(void*, double, double);
using KeyCallback = void (*)(void*, int, int, int, int);

std::atomic<CursorEnterCallback> cursor_enter_callback{nullptr};
std::atomic<CursorPosCallback> cursor_pos_callback{nullptr};
std::atomic<MouseButtonCallback> mouse_button_callback{nullptr};
std::atomic<ScrollCallback> scroll_callback{nullptr};
std::atomic<KeyCallback> key_callback{nullptr};
std::atomic<uintptr_t> char_callback{0};
std::atomic<uintptr_t> char_mods_callback{0};
std::atomic<uintptr_t> framebuffer_size_callback{0};
std::atomic<uintptr_t> window_size_callback{0};
std::atomic<bool> input_ready{false};

enum class InputKind { Cursor, MouseButton, Scroll, Key };
struct InputEvent {
    InputKind kind;
    double first;
    double second;
    int button;
    int action;
    int modifiers;
};
std::mutex input_mutex;
std::deque<InputEvent> input_events;
bool cursor_entered = false;

void queue_input(InputEvent event) {
    std::lock_guard<std::mutex> lock(input_mutex);
    if (event.kind == InputKind::Cursor && !input_events.empty()
            && input_events.back().kind == InputKind::Cursor) {
        input_events.back() = event;
        return;
    }
    if (input_events.size() >= 256) input_events.pop_front();
    input_events.push_back(event);
}

template<typename Callback>
jlong exchange_callback(std::atomic<Callback>& slot, jlong pointer) {
    Callback replacement = reinterpret_cast<Callback>(static_cast<uintptr_t>(pointer));
    Callback previous = slot.exchange(replacement, std::memory_order_acq_rel);
    return static_cast<jlong>(reinterpret_cast<uintptr_t>(previous));
}

jlong exchange_address(std::atomic<uintptr_t>& slot, jlong pointer) {
    return static_cast<jlong>(slot.exchange(
            static_cast<uintptr_t>(pointer), std::memory_order_acq_rel));
}

bool initialize_egl() {
    std::lock_guard<std::mutex> lock(egl_mutex);
    if (egl_display != EGL_NO_DISPLAY) return true;
    egl_display = eglGetDisplay(EGL_DEFAULT_DISPLAY);
    if (egl_display == EGL_NO_DISPLAY || eglInitialize(egl_display, nullptr, nullptr) != EGL_TRUE) {
        egl_display = EGL_NO_DISPLAY;
        return false;
    }
    const EGLint attributes[] = {
            EGL_SURFACE_TYPE, EGL_WINDOW_BIT,
            EGL_RENDERABLE_TYPE, EGL_OPENGL_ES2_BIT,
            EGL_RED_SIZE, 8, EGL_GREEN_SIZE, 8, EGL_BLUE_SIZE, 8, EGL_ALPHA_SIZE, 8,
            EGL_DEPTH_SIZE, 16,
            EGL_NONE
    };
    EGLint count = 0;
    if (eglChooseConfig(egl_display, attributes, &egl_config, 1, &count) != EGL_TRUE
            || count < 1) {
        eglTerminate(egl_display);
        egl_display = EGL_NO_DISPLAY;
        egl_config = nullptr;
        return false;
    }
    return true;
}

JNIEnv* hotspot_env() {
    if (hotspot_vm == nullptr) return nullptr;
    JNIEnv* env = nullptr;
    if (hotspot_vm->GetEnv(reinterpret_cast<void**>(&env), kJniVersion18) == JNI_OK) {
        return env;
    }
    return nullptr;
}

void publish_surface_size(int width, int height) {
    JNIEnv* env = hotspot_env();
    if (env == nullptr) return;
    jclass glfw = env->FindClass("org/lwjgl/glfw/GLFW");
    if (glfw == nullptr) {
        env->ExceptionClear();
        return;
    }
    jmethodID changed = env->GetStaticMethodID(glfw, "internalChangeMonitorSize", "(II)V");
    if (changed != nullptr) env->CallStaticVoidMethod(glfw, changed, width, height);
    if (env->ExceptionCheck()) env->ExceptionClear();
    env->DeleteLocalRef(glfw);
}

void gl4es_framebuffer_size(int* width, int* height) {
    if (width == nullptr || height == nullptr) return;
    *width = 0;
    *height = 0;
    RenderContext* context = current_context;
    if (context == nullptr || context->surface == EGL_NO_SURFACE
            || egl_display == EGL_NO_DISPLAY) {
        return;
    }
    EGLint surface_width = 0;
    EGLint surface_height = 0;
    if (eglQuerySurface(egl_display, context->surface, EGL_WIDTH, &surface_width) != EGL_TRUE
            || eglQuerySurface(egl_display, context->surface, EGL_HEIGHT,
            &surface_height) != EGL_TRUE) {
        return;
    }
    *width = surface_width;
    *height = surface_height;
}

void release_surface(RenderContext* context) {
    if (context == nullptr) return;
    if (context->surface != EGL_NO_SURFACE) {
        eglDestroySurface(egl_display, context->surface);
        context->surface = EGL_NO_SURFACE;
    }
    if (context->window != nullptr) {
        rustedfabric_release_native_window(context->window);
        context->window = nullptr;
    }
}

bool bind_attached_surface(RenderContext* context) {
    if (context == nullptr || context->context == EGL_NO_CONTEXT) return false;
    if (context->surface == EGL_NO_SURFACE) {
        context->window = rustedfabric_acquire_native_window_for_generation(
                &context->window_generation);
        if (context->window == nullptr) {
            if (!context->surface_unavailable_reported) {
                __android_log_print(ANDROID_LOG_INFO, kTag,
                                    "Rendering paused while the Android Surface is detached");
                context->surface_unavailable_reported = true;
            }
            return false;
        }
        EGLint native_format = 0;
        if (eglGetConfigAttrib(egl_display, egl_config, EGL_NATIVE_VISUAL_ID, &native_format)) {
            ANativeWindow_setBuffersGeometry(context->window, 0, 0, native_format);
        }
        context->surface = eglCreateWindowSurface(
                egl_display, egl_config, context->window, nullptr);
        if (context->surface == EGL_NO_SURFACE) {
            const EGLint error = eglGetError();
            __android_log_print(ANDROID_LOG_ERROR, kTag,
                                "eglCreateWindowSurface failed: 0x%x", error);
            rustedfabric_release_native_window(context->window);
            context->window = nullptr;
            return false;
        }
    }
    if (eglMakeCurrent(egl_display, context->surface, context->surface, context->context)
            != EGL_TRUE) {
        __android_log_print(ANDROID_LOG_ERROR, kTag,
                            "eglMakeCurrent failed: 0x%x", eglGetError());
        release_surface(context);
        return false;
    }
    context->surface_unavailable_reported = false;
    EGLint width = 0;
    EGLint height = 0;
    eglQuerySurface(egl_display, context->surface, EGL_WIDTH, &width);
    eglQuerySurface(egl_display, context->surface, EGL_HEIGHT, &height);
    publish_surface_size(width, height);
    return true;
}

}

extern "C" JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM* vm, void*) {
    hotspot_vm = vm;
    return kJniVersion18;
}

extern "C" JNIEXPORT jlong JNICALL
Java_org_lwjgl_system_RustedFabricMemory_getDirectBufferAddress(
        JNIEnv* env, jclass, jobject buffer) {
    void* address = env->GetDirectBufferAddress(buffer);
    return reinterpret_cast<jlong>(address);
}

// ABI consumed by Pojav's Android GLFW replacement inside lwjgl-glfw-classes.jar.
extern "C" __attribute__((visibility("default"))) int pojavInit() {
    ANativeWindow* window = rustedfabric_acquire_native_window();
    if (window == nullptr) return 0;
    const int width = ANativeWindow_getWidth(window);
    const int height = ANativeWindow_getHeight(window);
    rustedfabric_release_native_window(window);
    if (!initialize_egl()) return 0;
    __android_log_print(ANDROID_LOG_INFO, kTag, "GLFW initialized for Surface %dx%d", width, height);
    publish_surface_size(width, height);
    return 1;
}

extern "C" __attribute__((visibility("default"))) void* pojavCreateContext(void* shared_value) {
    __android_log_print(ANDROID_LOG_INFO, kTag, "Creating EGL context (share=%p)", shared_value);
    if (!initialize_egl()) return nullptr;
    auto* shared = static_cast<RenderContext*>(shared_value);
    auto* created = new RenderContext();
    const EGLint context_attributes[] = {EGL_CONTEXT_CLIENT_VERSION, 2, EGL_NONE};
    created->context = eglCreateContext(egl_display, egl_config,
            shared == nullptr ? EGL_NO_CONTEXT : shared->context, context_attributes);
    if (created->context == EGL_NO_CONTEXT) {
        __android_log_print(ANDROID_LOG_ERROR, kTag, "eglCreateContext failed: 0x%x", eglGetError());
        delete created;
        return nullptr;
    }
    __android_log_print(ANDROID_LOG_INFO, kTag, "Created EGL context %p", created->context);
    return created;
}

extern "C" __attribute__((visibility("default"))) void pojavMakeCurrent(void* context_value) {
    __android_log_print(ANDROID_LOG_INFO, kTag, "Making context current (handle=%p)", context_value);
    auto* context = static_cast<RenderContext*>(context_value);
    if (context == nullptr) {
        eglMakeCurrent(egl_display, EGL_NO_SURFACE, EGL_NO_SURFACE, EGL_NO_CONTEXT);
        current_context = nullptr;
        return;
    }
    if (bind_attached_surface(context)) {
        current_context = context;
        __android_log_print(ANDROID_LOG_INFO, kTag, "EGL context is current");
    }
}

extern "C" __attribute__((visibility("default"))) void* pojavGetCurrentContext() {
    return current_context;
}

extern "C" __attribute__((visibility("default"))) void pojavSwapBuffers(void*) {
    RenderContext* context = current_context;
    if (context == nullptr) return;
    const uint64_t attached_generation = rustedfabric_native_window_generation();
    if (context->surface != EGL_NO_SURFACE
            && context->window_generation != attached_generation) {
        // An EGLSurface may remain superficially valid after SurfaceView replaces its
        // ANativeWindow. Generation tracking makes replacement deterministic instead of
        // waiting for a vendor-specific EGL_BAD_SURFACE response.
        eglMakeCurrent(egl_display, EGL_NO_SURFACE, EGL_NO_SURFACE, EGL_NO_CONTEXT);
        release_surface(context);
        __android_log_print(ANDROID_LOG_INFO, kTag,
                            "Android Surface generation changed; rebuilding EGLSurface");
    }
    if (context->surface == EGL_NO_SURFACE && !bind_attached_surface(context)) return;
    if (eglSwapBuffers(egl_display, context->surface)) return;

    const EGLint error = eglGetError();
    if (error != EGL_BAD_SURFACE && error != EGL_BAD_NATIVE_WINDOW) {
        __android_log_print(ANDROID_LOG_ERROR, kTag,
                            "eglSwapBuffers failed: 0x%x", error);
        return;
    }

    // SurfaceView instances are replaced when Android backgrounds and resumes the Activity.
    // EGL work must stay on the game render thread, so rebuild lazily at the next swap.
    eglMakeCurrent(egl_display, EGL_NO_SURFACE, EGL_NO_SURFACE, EGL_NO_CONTEXT);
    release_surface(context);
    if (bind_attached_surface(context)) {
        __android_log_print(ANDROID_LOG_INFO, kTag,
                            "Rebound EGL context to the replacement Android Surface");
        if (!eglSwapBuffers(egl_display, context->surface)) {
            __android_log_print(ANDROID_LOG_ERROR, kTag,
                                "eglSwapBuffers after rebind failed: 0x%x", eglGetError());
        }
    }
}

extern "C" __attribute__((visibility("default"))) void pojavSwapInterval(int interval) {
    if (egl_display != EGL_NO_DISPLAY) eglSwapInterval(egl_display, interval);
}

// FCL's LWJGL 3.3.6 adapter resolves these optional Minecraft-specific injector hooks during
// GLFW initialization even when no injector is configured. Rusted Warfare does not use them,
// but publishing harmless stubs keeps the adapter ABI complete.
extern "C" __attribute__((visibility("default"))) void* pojavSetInjectorCallback(
        void*) {
    return nullptr;
}

extern "C" __attribute__((visibility("default"))) void pojavSetHitResultType(int) {
}

extern "C" __attribute__((visibility("default"))) void pojavSetWindowHint(int, int) {
}

extern "C" __attribute__((visibility("default"))) void pojavStartPumping() {
}

extern "C" __attribute__((visibility("default"))) void pojavPumpEvents(void* window) {
    if (!input_ready.load(std::memory_order_acquire)) return;
    std::deque<InputEvent> pending;
    {
        std::lock_guard<std::mutex> lock(input_mutex);
        pending.swap(input_events);
    }
    for (const InputEvent& event : pending) {
        switch (event.kind) {
            case InputKind::Cursor: {
                CursorEnterCallback enter = cursor_enter_callback.load();
                if (!cursor_entered && enter != nullptr) {
                    enter(window, 1);
                    cursor_entered = true;
                }
                CursorPosCallback callback = cursor_pos_callback.load();
                if (callback != nullptr) callback(window, event.first, event.second);
                break;
            }
            case InputKind::MouseButton: {
                MouseButtonCallback callback = mouse_button_callback.load();
                if (callback != nullptr) {
                    callback(window, event.button, event.action, event.modifiers);
                }
                break;
            }
            case InputKind::Scroll: {
                ScrollCallback callback = scroll_callback.load();
                if (callback != nullptr) callback(window, event.first, event.second);
                break;
            }
            case InputKind::Key: {
                KeyCallback callback = key_callback.load();
                if (callback != nullptr) {
                    callback(window, event.button, 0, event.action, event.modifiers);
                }
                break;
            }
        }
    }
}

extern "C" __attribute__((visibility("default"))) void pojavStopPumping() {
}

extern "C" __attribute__((visibility("default"))) void
rustedfabric_queue_cursor_pos(float x, float y) {
    queue_input({InputKind::Cursor, x, y, 0, 0, 0});
}

extern "C" __attribute__((visibility("default"))) void
rustedfabric_queue_mouse_button(int button, int action, int modifiers) {
    queue_input({InputKind::MouseButton, 0.0, 0.0, button, action, modifiers});
}

extern "C" __attribute__((visibility("default"))) void
rustedfabric_queue_scroll(double x, double y) {
    queue_input({InputKind::Scroll, x, y, 0, 0, 0});
}

extern "C" __attribute__((visibility("default"))) void
rustedfabric_queue_key(int key, int action, int modifiers) {
    queue_input({InputKind::Key, 0.0, 0.0, key, action, modifiers});
}

extern "C" __attribute__((visibility("default"))) void pojavTerminate() {
    // Context ownership is intentionally retained until the HotSpot process exits. LWJGLX can
    // call terminate while static cleanup still performs OpenGL queries.
}

extern "C" JNIEXPORT jlong JNICALL
Java_org_lwjgl_glfw_GLFW_internalGetGamepadDataPointer(JNIEnv*, jclass) {
    return reinterpret_cast<jlong>(gamepad_state.data());
}

#define RUSTED_FABRIC_ADDRESS_CALLBACK(Name, Slot) \
extern "C" JNIEXPORT jlong JNICALL Java_org_lwjgl_glfw_GLFW_##Name( \
        JNIEnv*, jclass, jlong, jlong pointer) { return exchange_address(Slot, pointer); }

#define RUSTED_FABRIC_TYPED_CALLBACK(Name, Slot) \
extern "C" JNIEXPORT jlong JNICALL Java_org_lwjgl_glfw_GLFW_##Name( \
        JNIEnv*, jclass, jlong, jlong pointer) { return exchange_callback(Slot, pointer); }

RUSTED_FABRIC_ADDRESS_CALLBACK(nglfwSetCharCallback, char_callback)
RUSTED_FABRIC_ADDRESS_CALLBACK(nglfwSetCharModsCallback, char_mods_callback)
RUSTED_FABRIC_TYPED_CALLBACK(nglfwSetCursorEnterCallback, cursor_enter_callback)
RUSTED_FABRIC_TYPED_CALLBACK(nglfwSetCursorPosCallback, cursor_pos_callback)
RUSTED_FABRIC_ADDRESS_CALLBACK(nglfwSetFramebufferSizeCallback, framebuffer_size_callback)
RUSTED_FABRIC_TYPED_CALLBACK(nglfwSetKeyCallback, key_callback)
RUSTED_FABRIC_TYPED_CALLBACK(nglfwSetMouseButtonCallback, mouse_button_callback)
RUSTED_FABRIC_TYPED_CALLBACK(nglfwSetScrollCallback, scroll_callback)
RUSTED_FABRIC_ADDRESS_CALLBACK(nglfwSetWindowSizeCallback, window_size_callback)

#undef RUSTED_FABRIC_ADDRESS_CALLBACK
#undef RUSTED_FABRIC_TYPED_CALLBACK

extern "C" JNIEXPORT void JNICALL
Java_org_lwjgl_glfw_GLFW_nglfwSetShowingWindow(JNIEnv*, jclass, jlong) {
}

extern "C" JNIEXPORT jobject JNICALL
Java_org_lwjgl_glfw_CallbackBridge_nativeCreateGamepadAxisBuffer(JNIEnv* env, jclass) {
    return env->NewDirectByteBuffer(gamepad_axes.data(),
            static_cast<jlong>(gamepad_axes.size() * sizeof(float)));
}

extern "C" JNIEXPORT jobject JNICALL
Java_org_lwjgl_glfw_CallbackBridge_nativeCreateGamepadButtonBuffer(JNIEnv* env, jclass) {
    return env->NewDirectByteBuffer(gamepad_buttons.data(),
            static_cast<jlong>(gamepad_buttons.size()));
}

extern "C" JNIEXPORT jboolean JNICALL
Java_org_lwjgl_glfw_CallbackBridge_nativeSetInputReady(JNIEnv*, jclass, jboolean ready) {
    input_ready.store(ready == JNI_TRUE, std::memory_order_release);
    return JNI_TRUE;
}

extern "C" JNIEXPORT void JNICALL
Java_org_lwjgl_glfw_CallbackBridge_nativeSendData(JNIEnv*, jclass, jboolean, jint, jstring) {
}

extern "C" JNIEXPORT jstring JNICALL
Java_org_lwjgl_glfw_CallbackBridge_nativeClipboard(JNIEnv* env, jclass, jint, jbyteArray) {
    return env->NewStringUTF("");
}

extern "C" JNIEXPORT void JNICALL
Java_org_lwjgl_glfw_CallbackBridge_nativeSetGrabbing(JNIEnv*, jclass, jboolean) {
}

extern "C" JNIEXPORT jboolean JNICALL
Java_org_lwjgl_glfw_CallbackBridge_nativeEnableGamepadDirectInput(JNIEnv*, jclass) {
    return JNI_FALSE;
}

extern "C" JNIEXPORT void JNICALL
Java_org_lwjgl_opengl_PojavRendererInit_nativeInitGl4esInternals(
        JNIEnv* env, jclass, jobject function_provider) {
    if (function_provider == nullptr) {
        __android_log_print(ANDROID_LOG_WARN, kTag,
                            "GL4ES function provider is unavailable");
        return;
    }
    jclass provider_type = env->GetObjectClass(function_provider);
    jmethodID get_function_address = provider_type == nullptr ? nullptr
            : env->GetMethodID(provider_type, "getFunctionAddress",
                               "(Ljava/lang/CharSequence;)J");
    if (get_function_address == nullptr) {
        if (env->ExceptionCheck()) env->ExceptionClear();
        __android_log_print(ANDROID_LOG_WARN, kTag,
                            "GL4ES function provider has no getFunctionAddress ABI");
        if (provider_type != nullptr) env->DeleteLocalRef(provider_type);
        return;
    }
    auto lookup = [&](const char* name) {
        jstring symbol_name = env->NewStringUTF(name);
        const jlong symbol = env->CallLongMethod(
                function_provider, get_function_address, symbol_name);
        env->DeleteLocalRef(symbol_name);
        return symbol;
    };
    const jlong framebuffer_symbol = lookup("set_getmainfbsize");
    env->DeleteLocalRef(provider_type);
    if (env->ExceptionCheck()) {
        env->ExceptionClear();
        __android_log_print(ANDROID_LOG_WARN, kTag,
                            "GL4ES framebuffer callback lookup failed");
        return;
    }
    using SetFramebufferSizeCallback = void (*)(void (*)(int*, int*));
    auto set_framebuffer_size = reinterpret_cast<SetFramebufferSizeCallback>(
            static_cast<uintptr_t>(framebuffer_symbol));
    if (set_framebuffer_size != nullptr) {
        set_framebuffer_size(gl4es_framebuffer_size);
        __android_log_print(ANDROID_LOG_INFO, kTag,
                            "Registered GL4ES framebuffer size callback");
    } else {
        __android_log_print(ANDROID_LOG_WARN, kTag,
                            "GL4ES set_getmainfbsize is unavailable");
    }
}
