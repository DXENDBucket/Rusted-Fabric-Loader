#include <jni.h>
#include <android/native_window.h>
#include <android/log.h>
#include <EGL/egl.h>

#include <array>
#include <cstdint>
#include <mutex>

#include "rustedfabric_renderbridge.h"

namespace {
constexpr jint kJniVersion18 = 0x00010008;
constexpr const char* kTag = "RustedFabricGLFW";
struct RenderContext {
    EGLContext context = EGL_NO_CONTEXT;
    EGLSurface surface = EGL_NO_SURFACE;
    ANativeWindow* window = nullptr;
};

JavaVM* hotspot_vm = nullptr;
EGLDisplay egl_display = EGL_NO_DISPLAY;
EGLConfig egl_config = nullptr;
std::mutex egl_mutex;
thread_local RenderContext* current_context = nullptr;
std::array<float, 6> gamepad_axes{};
std::array<unsigned char, 16> gamepad_buttons{};
std::array<unsigned char, 64> gamepad_state{};

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

jlong preserve_callback(jlong pointer) {
    return pointer;
}
}

extern "C" JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM* vm, void*) {
    hotspot_vm = vm;
    return kJniVersion18;
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
    if (context->surface == EGL_NO_SURFACE) {
        context->window = rustedfabric_acquire_native_window();
        if (context->window == nullptr) {
            __android_log_print(ANDROID_LOG_ERROR, kTag, "No Android window is attached");
            return;
        }
        EGLint native_format = 0;
        if (eglGetConfigAttrib(egl_display, egl_config, EGL_NATIVE_VISUAL_ID, &native_format)) {
            ANativeWindow_setBuffersGeometry(context->window, 0, 0, native_format);
        }
        context->surface = eglCreateWindowSurface(
                egl_display, egl_config, context->window, nullptr);
        if (context->surface == EGL_NO_SURFACE) {
            __android_log_print(ANDROID_LOG_ERROR, kTag,
                    "eglCreateWindowSurface failed: 0x%x", eglGetError());
            rustedfabric_release_native_window(context->window);
            context->window = nullptr;
            return;
        }
    }
    if (eglMakeCurrent(egl_display, context->surface, context->surface, context->context)) {
        current_context = context;
        __android_log_print(ANDROID_LOG_INFO, kTag, "EGL context is current");
    } else {
        __android_log_print(ANDROID_LOG_ERROR, kTag, "eglMakeCurrent failed: 0x%x", eglGetError());
    }
}

extern "C" __attribute__((visibility("default"))) void* pojavGetCurrentContext() {
    return current_context;
}

extern "C" __attribute__((visibility("default"))) void pojavSwapBuffers(void*) {
    if (current_context != nullptr && current_context->surface != EGL_NO_SURFACE) {
        if (!eglSwapBuffers(egl_display, current_context->surface)) {
            __android_log_print(ANDROID_LOG_ERROR, kTag, "eglSwapBuffers failed: 0x%x", eglGetError());
        }
    }
}

extern "C" __attribute__((visibility("default"))) void pojavSwapInterval(int interval) {
    if (egl_display != EGL_NO_DISPLAY) eglSwapInterval(egl_display, interval);
}

extern "C" __attribute__((visibility("default"))) void pojavSetWindowHint(int, int) {
}

extern "C" __attribute__((visibility("default"))) void pojavStartPumping() {
}

extern "C" __attribute__((visibility("default"))) void pojavPumpEvents(void*) {
}

extern "C" __attribute__((visibility("default"))) void pojavStopPumping() {
}

extern "C" __attribute__((visibility("default"))) void pojavTerminate() {
    // Context ownership is intentionally retained until the HotSpot process exits. LWJGLX can
    // call terminate while static cleanup still performs OpenGL queries.
}

extern "C" JNIEXPORT jlong JNICALL
Java_org_lwjgl_glfw_GLFW_internalGetGamepadDataPointer(JNIEnv*, jclass) {
    return reinterpret_cast<jlong>(gamepad_state.data());
}

#define RUSTED_FABRIC_CALLBACK(Name) \
extern "C" JNIEXPORT jlong JNICALL Java_org_lwjgl_glfw_GLFW_##Name( \
        JNIEnv*, jclass, jlong, jlong pointer) { return preserve_callback(pointer); }

RUSTED_FABRIC_CALLBACK(nglfwSetCharCallback)
RUSTED_FABRIC_CALLBACK(nglfwSetCharModsCallback)
RUSTED_FABRIC_CALLBACK(nglfwSetCursorEnterCallback)
RUSTED_FABRIC_CALLBACK(nglfwSetCursorPosCallback)
RUSTED_FABRIC_CALLBACK(nglfwSetKeyCallback)
RUSTED_FABRIC_CALLBACK(nglfwSetMouseButtonCallback)
RUSTED_FABRIC_CALLBACK(nglfwSetScrollCallback)

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
    return ready;
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
Java_org_lwjgl_opengl_PojavRendererInit_nativeInitGl4esInternals(JNIEnv*, jclass, jobject) {
}
