#include <jni.h>
#include <android/native_window.h>
#include <android/native_window_jni.h>
#include <dlfcn.h>
#include <EGL/egl.h>
#include <GLES2/gl2.h>

#include <mutex>
#include <sstream>
#include <string>

#include "rustedfabric_renderbridge.h"

namespace {
std::mutex window_mutex;
ANativeWindow* attached_window = nullptr;
uint64_t window_generation = 0;

std::string egl_error(const char* operation) {
    std::ostringstream message;
    message << operation << " failed (EGL 0x" << std::hex << std::uppercase
            << static_cast<unsigned int>(eglGetError()) << ')';
    return message.str();
}

std::string gl_string(GLenum name) {
    const GLubyte* value = glGetString(name);
    return value == nullptr ? "unavailable" : reinterpret_cast<const char*>(value);
}

jstring result(JNIEnv* env, const std::string& value) {
    return env->NewStringUTF(value.c_str());
}

template<typename Function, typename... Arguments>
bool call_pojav_input(const char* symbol, Arguments... arguments) {
    void* library = dlopen("libpojavexec.so", RTLD_NOW | RTLD_NOLOAD);
    if (library == nullptr) return false;
    auto function = reinterpret_cast<Function>(dlsym(library, symbol));
    if (function != nullptr) function(arguments...);
    dlclose(library);
    return function != nullptr;
}
}

extern "C" ANativeWindow* rustedfabric_acquire_native_window(void) {
    return rustedfabric_acquire_native_window_for_generation(nullptr);
}

extern "C" ANativeWindow* rustedfabric_acquire_native_window_for_generation(
        uint64_t* generation) {
    std::lock_guard<std::mutex> lock(window_mutex);
    if (generation != nullptr) *generation = window_generation;
    if (attached_window != nullptr) ANativeWindow_acquire(attached_window);
    return attached_window;
}

extern "C" void rustedfabric_release_native_window(ANativeWindow* window) {
    if (window != nullptr) ANativeWindow_release(window);
}

extern "C" uint64_t rustedfabric_native_window_generation(void) {
    std::lock_guard<std::mutex> lock(window_mutex);
    return window_generation;
}

extern "C" JNIEXPORT void JNICALL
Java_io_github_endx_rustedfabric_android_launcher_jvm_NativeRenderBridge_nativeAttachSurface(
        JNIEnv* env, jclass, jobject surface) {
    ANativeWindow* replacement = surface == nullptr
            ? nullptr : ANativeWindow_fromSurface(env, surface);
    std::lock_guard<std::mutex> lock(window_mutex);
    ANativeWindow* previous = attached_window;
    attached_window = replacement;
    ++window_generation;
    if (previous != nullptr) ANativeWindow_release(previous);
}

extern "C" JNIEXPORT void JNICALL
Java_io_github_endx_rustedfabric_android_launcher_jvm_NativeRenderBridge_nativeDetachSurface(
        JNIEnv*, jclass) {
    std::lock_guard<std::mutex> lock(window_mutex);
    ANativeWindow* previous = attached_window;
    attached_window = nullptr;
    ++window_generation;
    if (previous != nullptr) ANativeWindow_release(previous);
}

extern "C" JNIEXPORT jboolean JNICALL
Java_io_github_endx_rustedfabric_android_launcher_jvm_NativeRenderBridge_nativeSendPointer(
        JNIEnv*, jclass, jfloat x, jfloat y, jint button_action) {
    const bool cursor_sent = call_pojav_input<void (*)(float, float)>(
            "rustedfabric_queue_cursor_pos", x, y);
    bool button_sent = true;
    if (button_action >= 0) {
        button_sent = call_pojav_input<void (*)(int, int, int)>(
                "rustedfabric_queue_mouse_button", 0, button_action, 0);
    }
    return cursor_sent && button_sent ? JNI_TRUE : JNI_FALSE;
}

extern "C" JNIEXPORT jboolean JNICALL
Java_io_github_endx_rustedfabric_android_launcher_jvm_NativeRenderBridge_nativeSendScroll(
        JNIEnv*, jclass, jdouble x, jdouble y) {
    return call_pojav_input<void (*)(double, double)>(
            "rustedfabric_queue_scroll", x, y) ? JNI_TRUE : JNI_FALSE;
}

extern "C" JNIEXPORT jboolean JNICALL
Java_io_github_endx_rustedfabric_android_launcher_jvm_NativeRenderBridge_nativeScrollUiByTouchDelta(
        JNIEnv*, jclass, jfloat delta_y) {
    void* library = dlopen("librocketConnector.so", RTLD_NOW | RTLD_NOLOAD);
    if (library == nullptr) return JNI_FALSE;
    auto queue = reinterpret_cast<bool (*)(float)>(
            dlsym(library, "rustedfabric_rocket_queue_touch_scroll"));
    const bool accepted = queue != nullptr && queue(delta_y);
    dlclose(library);
    return accepted ? JNI_TRUE : JNI_FALSE;
}

extern "C" JNIEXPORT jboolean JNICALL
Java_io_github_endx_rustedfabric_android_launcher_jvm_NativeRenderBridge_nativeSendMouseButton(
        JNIEnv*, jclass, jint button, jint action) {
    return call_pojav_input<void (*)(int, int, int)>(
            "rustedfabric_queue_mouse_button", button, action, 0) ? JNI_TRUE : JNI_FALSE;
}

extern "C" JNIEXPORT jboolean JNICALL
Java_io_github_endx_rustedfabric_android_launcher_jvm_NativeRenderBridge_nativeSendMouseClick(
        JNIEnv*, jclass, jint button, jfloat x, jfloat y) {
    if (button == 0) {
        void* rocket = dlopen("librocketConnector.so", RTLD_NOW | RTLD_NOLOAD);
        if (rocket != nullptr) {
            auto queue_rocket_click = reinterpret_cast<bool (*)(int)>(
                    dlsym(rocket, "rustedfabric_rocket_queue_touch_click"));
            const bool accepted = queue_rocket_click != nullptr && queue_rocket_click(button);
            dlclose(rocket);
            if (accepted) return JNI_TRUE;
        }
    }
    // Keep cursor movement and both button transitions in one desktop poll so the
    // game performs its normal UI coordinate conversion before dispatching the click.
    return call_pojav_input<void (*)(float, float, int, int)>(
            "rustedfabric_queue_mouse_click", x, y, button, 0) ? JNI_TRUE : JNI_FALSE;
}

extern "C" JNIEXPORT jboolean JNICALL
Java_io_github_endx_rustedfabric_android_launcher_jvm_NativeRenderBridge_nativeSendKey(
        JNIEnv*, jclass, jint key, jint action) {
    return call_pojav_input<void (*)(int, int, int)>(
            "rustedfabric_queue_key", key, action, 0) ? JNI_TRUE : JNI_FALSE;
}

extern "C" JNIEXPORT jboolean JNICALL
Java_io_github_endx_rustedfabric_android_launcher_jvm_NativeRenderBridge_nativeSendTouchFrame(
        JNIEnv* env, jclass, jfloatArray xs, jfloatArray ys, jintArray pointer_ids,
        jint count, jboolean down, jint action) {
    if (count < 0 || count > 10 || xs == nullptr || ys == nullptr
            || pointer_ids == nullptr) {
        return JNI_FALSE;
    }
    float native_xs[10]{};
    float native_ys[10]{};
    int native_ids[10]{};
    if (count > 0) {
        env->GetFloatArrayRegion(xs, 0, count, native_xs);
        env->GetFloatArrayRegion(ys, 0, count, native_ys);
        env->GetIntArrayRegion(pointer_ids, 0, count,
                               reinterpret_cast<jint*>(native_ids));
        if (env->ExceptionCheck()) return JNI_FALSE;
    }
    return call_pojav_input<void (*)(const float*, const float*, const int*, int, bool, int)>(
            "rustedfabric_queue_touch_frame", native_xs, native_ys, native_ids,
            count, down == JNI_TRUE, action) ? JNI_TRUE : JNI_FALSE;
}

extern "C" JNIEXPORT jboolean JNICALL
Java_io_github_endx_rustedfabric_android_launcher_jvm_NativeRenderBridge_nativeUiWantsScroll(
        JNIEnv*, jclass) {
    void* library = dlopen("librocketConnector.so", RTLD_NOW | RTLD_NOLOAD);
    if (library == nullptr) return JNI_FALSE;
    auto query = reinterpret_cast<bool (*)(void)>(
            dlsym(library, "rustedfabric_rocket_hover_scrollable"));
    const bool scrollable = query != nullptr && query();
    dlclose(library);
    return scrollable ? JNI_TRUE : JNI_FALSE;
}

extern "C" JNIEXPORT jboolean JNICALL
Java_io_github_endx_rustedfabric_android_launcher_jvm_NativeRenderBridge_nativeUiPrefersDrag(
        JNIEnv*, jclass) {
    void* library = dlopen("librocketConnector.so", RTLD_NOW | RTLD_NOLOAD);
    if (library == nullptr) return JNI_FALSE;
    auto query = reinterpret_cast<bool (*)(void)>(
            dlsym(library, "rustedfabric_rocket_hover_prefers_drag"));
    const bool prefers_drag = query != nullptr && query();
    dlclose(library);
    return prefers_drag ? JNI_TRUE : JNI_FALSE;
}

extern "C" JNIEXPORT jboolean JNICALL
Java_io_github_endx_rustedfabric_android_launcher_jvm_NativeRenderBridge_nativeUiIsActive(
        JNIEnv*, jclass) {
    void* library = dlopen("librocketConnector.so", RTLD_NOW | RTLD_NOLOAD);
    if (library == nullptr) return JNI_FALSE;
    auto query = reinterpret_cast<bool (*)(void)>(
            dlsym(library, "rustedfabric_rocket_document_active"));
    const bool active = query != nullptr && query();
    dlclose(library);
    return active ? JNI_TRUE : JNI_FALSE;
}

extern "C" JNIEXPORT jstring JNICALL
Java_io_github_endx_rustedfabric_android_launcher_jvm_NativeRenderBridge_nativeSmokeTest(
        JNIEnv* env, jclass, jint requested_width, jint requested_height) {
    ANativeWindow* window = rustedfabric_acquire_native_window();
    if (window == nullptr) return result(env, "Android Surface is not attached");

    EGLDisplay display = EGL_NO_DISPLAY;
    EGLContext context = EGL_NO_CONTEXT;
    EGLSurface egl_surface = EGL_NO_SURFACE;
    std::string detail;

    display = eglGetDisplay(EGL_DEFAULT_DISPLAY);
    if (display == EGL_NO_DISPLAY) {
        detail = egl_error("eglGetDisplay");
    } else if (eglInitialize(display, nullptr, nullptr) != EGL_TRUE) {
        detail = egl_error("eglInitialize");
    } else {
        const EGLint attributes[] = {
                EGL_SURFACE_TYPE, EGL_WINDOW_BIT,
                EGL_RENDERABLE_TYPE, EGL_OPENGL_ES2_BIT,
                EGL_RED_SIZE, 8,
                EGL_GREEN_SIZE, 8,
                EGL_BLUE_SIZE, 8,
                EGL_ALPHA_SIZE, 8,
                EGL_DEPTH_SIZE, 16,
                EGL_NONE
        };
        EGLConfig config = nullptr;
        EGLint config_count = 0;
        if (eglChooseConfig(display, attributes, &config, 1, &config_count) != EGL_TRUE
                || config_count < 1) {
            detail = egl_error("eglChooseConfig");
        } else {
            EGLint native_format = 0;
            if (eglGetConfigAttrib(display, config, EGL_NATIVE_VISUAL_ID, &native_format)
                    == EGL_TRUE) {
                ANativeWindow_setBuffersGeometry(window, 0, 0, native_format);
            }
            const EGLint context_attributes[] = {EGL_CONTEXT_CLIENT_VERSION, 2, EGL_NONE};
            context = eglCreateContext(display, config, EGL_NO_CONTEXT, context_attributes);
            if (context == EGL_NO_CONTEXT) {
                detail = egl_error("eglCreateContext");
            } else {
                egl_surface = eglCreateWindowSurface(display, config, window, nullptr);
                if (egl_surface == EGL_NO_SURFACE) {
                    detail = egl_error("eglCreateWindowSurface");
                } else if (eglMakeCurrent(display, egl_surface, egl_surface, context) != EGL_TRUE) {
                    detail = egl_error("eglMakeCurrent");
                } else {
                    EGLint width = requested_width;
                    EGLint height = requested_height;
                    eglQuerySurface(display, egl_surface, EGL_WIDTH, &width);
                    eglQuerySurface(display, egl_surface, EGL_HEIGHT, &height);
                    glViewport(0, 0, width, height);
                    glClearColor(0.035f, 0.30f, 0.34f, 1.0f);
                    glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
                    if (eglSwapBuffers(display, egl_surface) != EGL_TRUE) {
                        detail = egl_error("eglSwapBuffers");
                    } else {
                        const char* egl_vendor = eglQueryString(display, EGL_VENDOR);
                        const char* egl_version = eglQueryString(display, EGL_VERSION);
                        std::ostringstream success;
                        success << "rusted-fabric-egl-smoke=ok\n"
                                << "surface=" << width << 'x' << height << "\n"
                                << "egl.vendor=" << (egl_vendor == nullptr ? "unavailable" : egl_vendor) << "\n"
                                << "egl.version=" << (egl_version == nullptr ? "unavailable" : egl_version) << "\n"
                                << "gl.vendor=" << gl_string(GL_VENDOR) << "\n"
                                << "gl.renderer=" << gl_string(GL_RENDERER) << "\n"
                                << "gl.version=" << gl_string(GL_VERSION);
                        detail = success.str();
                    }
                }
            }
        }
    }

    if (display != EGL_NO_DISPLAY) {
        eglMakeCurrent(display, EGL_NO_SURFACE, EGL_NO_SURFACE, EGL_NO_CONTEXT);
        if (egl_surface != EGL_NO_SURFACE) eglDestroySurface(display, egl_surface);
        if (context != EGL_NO_CONTEXT) eglDestroyContext(display, context);
        eglTerminate(display);
        eglReleaseThread();
    }
    rustedfabric_release_native_window(window);
    return result(env, detail);
}
