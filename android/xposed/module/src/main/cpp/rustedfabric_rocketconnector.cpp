#include <jni.h>
#include <android/log.h>
#include <dlfcn.h>
#include <GLES2/gl2.h>

#include <Rocket/Core.h>
#include <Rocket/Core/ElementDocument.h>
#include <Rocket/Core/Event.h>
#include <Rocket/Core/EventListener.h>
#include <Rocket/Core/EventListenerInstancer.h>
#include <Rocket/Core/Factory.h>
#include <Rocket/Core/FontDatabase.h>
#include <Rocket/Controls.h>

#include <algorithm>
#include <chrono>
#include <cstdio>
#include <cstdint>
#include <memory>
#include <string>
#include <vector>

namespace {
constexpr const char* kTag = "RustedFabricRocket";

JavaVM* java_vm = nullptr;
jobject java_rocket = nullptr;
Rocket::Core::Context* rocket_context = nullptr;
int rocket_width = 1;
int rocket_height = 1;
const auto start_time = std::chrono::steady_clock::now();

struct Gl4esScissor {
    using Toggle = void (*)(GLenum);
    using Set = void (*)(GLint, GLint, GLsizei, GLsizei);

    void* library = nullptr;
    Toggle enable = nullptr;
    Toggle disable = nullptr;
    Set set = nullptr;

    bool load() {
        if (library != nullptr) return enable != nullptr && disable != nullptr && set != nullptr;
        library = dlopen("libgl4es_114.so", RTLD_NOW | RTLD_NOLOAD);
        if (library == nullptr) library = dlopen("libgl4es_114.so", RTLD_NOW);
        if (library == nullptr) return false;
        enable = reinterpret_cast<Toggle>(dlsym(library, "glEnable"));
        disable = reinterpret_cast<Toggle>(dlsym(library, "glDisable"));
        set = reinterpret_cast<Set>(dlsym(library, "glScissor"));
        return enable != nullptr && disable != nullptr && set != nullptr;
    }
};

Gl4esScissor gl4es_scissor;

JNIEnv* current_env() {
    if (java_vm == nullptr) return nullptr;
    JNIEnv* env = nullptr;
    return java_vm->GetEnv(reinterpret_cast<void**>(&env), JNI_VERSION_1_6) == JNI_OK
            ? env : nullptr;
}

std::string utf8(JNIEnv* env, jstring value) {
    if (value == nullptr) return {};
    const char* chars = env->GetStringUTFChars(value, nullptr);
    if (chars == nullptr) return {};
    std::string result(chars);
    env->ReleaseStringUTFChars(value, chars);
    return result;
}

jstring java_string(JNIEnv* env, const Rocket::Core::String& value) {
    return env->NewStringUTF(value.CString());
}

jfieldID handle_field(JNIEnv* env, jobject element) {
    jclass type = env->GetObjectClass(element);
    jfieldID field = env->GetFieldID(type, "nativeHandle", "J");
    env->DeleteLocalRef(type);
    return field;
}

Rocket::Core::Element* element_handle(JNIEnv* env, jobject element) {
    if (element == nullptr) return nullptr;
    jfieldID field = handle_field(env, element);
    if (field == nullptr) return nullptr;
    return reinterpret_cast<Rocket::Core::Element*>(
            static_cast<uintptr_t>(env->GetLongField(element, field)));
}

void assign_handle(JNIEnv* env, jobject element, Rocket::Core::Element* native_element) {
    jfieldID field = handle_field(env, element);
    if (field != nullptr) {
        env->SetLongField(element, field,
                static_cast<jlong>(reinterpret_cast<uintptr_t>(native_element)));
    }
}

jobject linked_element(JNIEnv* env, Rocket::Core::Element* element) {
    if (element == nullptr) return nullptr;
    const bool document = dynamic_cast<Rocket::Core::ElementDocument*>(element) != nullptr;
    jclass type = env->FindClass(document ? "com/ElementDocument" : "com/Element");
    if (type == nullptr) return nullptr;
    jmethodID constructor = env->GetMethodID(type, "<init>", "()V");
    jobject result = constructor == nullptr ? nullptr : env->NewObject(type, constructor);
    if (result != nullptr) {
        element->AddReference();
        assign_handle(env, result, element);
    }
    env->DeleteLocalRef(type);
    return result;
}

class AndroidFileInterface final : public Rocket::Core::FileInterface {
public:
    Rocket::Core::FileHandle Open(const Rocket::Core::String& path) override {
        return reinterpret_cast<Rocket::Core::FileHandle>(std::fopen(path.CString(), "rb"));
    }

    void Close(Rocket::Core::FileHandle file) override {
        if (file != 0) std::fclose(reinterpret_cast<FILE*>(file));
    }

    size_t Read(void* buffer, size_t size, Rocket::Core::FileHandle file) override {
        return file == 0 ? 0 : std::fread(buffer, 1, size, reinterpret_cast<FILE*>(file));
    }

    bool Seek(Rocket::Core::FileHandle file, long offset, int origin) override {
        return file != 0 && std::fseek(reinterpret_cast<FILE*>(file), offset, origin) == 0;
    }

    size_t Tell(Rocket::Core::FileHandle file) override {
        if (file == 0) return 0;
        const long position = std::ftell(reinterpret_cast<FILE*>(file));
        return position < 0 ? 0 : static_cast<size_t>(position);
    }
};

class AndroidSystemInterface final : public Rocket::Core::SystemInterface {
public:
    float GetElapsedTime() override {
        return std::chrono::duration<float>(
                std::chrono::steady_clock::now() - start_time).count();
    }

    int TranslateString(Rocket::Core::String& translated,
                        const Rocket::Core::String& input) override {
        JNIEnv* env = current_env();
        if (env == nullptr || java_rocket == nullptr) {
            translated = input;
            return 0;
        }
        jclass type = env->GetObjectClass(java_rocket);
        jmethodID method = env->GetMethodID(type, "TranslateString",
                                            "(Ljava/lang/String;)Ljava/lang/String;");
        jstring source = java_string(env, input);
        auto result = static_cast<jstring>(env->CallObjectMethod(java_rocket, method, source));
        if (result == nullptr || env->ExceptionCheck()) {
            env->ExceptionClear();
            translated = input;
        } else {
            translated = utf8(env, result).c_str();
        }
        if (result != nullptr) env->DeleteLocalRef(result);
        env->DeleteLocalRef(source);
        env->DeleteLocalRef(type);
        return translated == input ? 0 : 1;
    }

    bool LogMessage(Rocket::Core::Log::Type type,
                    const Rocket::Core::String& message) override {
        __android_log_print(type <= Rocket::Core::Log::LT_WARNING
                                    ? ANDROID_LOG_WARN : ANDROID_LOG_INFO,
                            kTag, "%s", message.CString());
        return true;
    }
};

class JavaRenderInterface final : public Rocket::Core::RenderInterface {
public:
    void RenderGeometry(Rocket::Core::Vertex* vertices, int vertex_count, int* indices,
                        int index_count, Rocket::Core::TextureHandle texture,
                        const Rocket::Core::Vector2f& translation) override {
        JNIEnv* env = current_env();
        if (env == nullptr || java_rocket == nullptr) return;
        jfloatArray xy = env->NewFloatArray(vertex_count * 2);
        jfloatArray uv = env->NewFloatArray(vertex_count * 2);
        jintArray colors = env->NewIntArray(vertex_count);
        jintArray index_array = env->NewIntArray(index_count);
        std::vector<float> xy_values(static_cast<size_t>(vertex_count) * 2);
        std::vector<float> uv_values(static_cast<size_t>(vertex_count) * 2);
        std::vector<jint> color_values(static_cast<size_t>(vertex_count));
        for (int i = 0; i < vertex_count; ++i) {
            xy_values[static_cast<size_t>(i) * 2] = vertices[i].position.x;
            xy_values[static_cast<size_t>(i) * 2 + 1] = vertices[i].position.y;
            uv_values[static_cast<size_t>(i) * 2] = vertices[i].tex_coord.x;
            uv_values[static_cast<size_t>(i) * 2 + 1] = vertices[i].tex_coord.y;
            const auto& c = vertices[i].colour;
            color_values[static_cast<size_t>(i)] = static_cast<jint>(
                    (static_cast<uint32_t>(c.red) << 24U)
                    | (static_cast<uint32_t>(c.green) << 16U)
                    | (static_cast<uint32_t>(c.blue) << 8U)
                    | static_cast<uint32_t>(c.alpha));
        }
        env->SetFloatArrayRegion(xy, 0, vertex_count * 2, xy_values.data());
        env->SetFloatArrayRegion(uv, 0, vertex_count * 2, uv_values.data());
        env->SetIntArrayRegion(colors, 0, vertex_count, color_values.data());
        env->SetIntArrayRegion(index_array, 0, index_count,
                               reinterpret_cast<const jint*>(indices));
        jclass type = env->GetObjectClass(java_rocket);
        jmethodID method = env->GetMethodID(type, "RenderGeometryPossiblyCompiled",
                "([F[F[I[IIFFLcom/LibRocket$CompiledGeometry;)V");
        env->CallVoidMethod(java_rocket, method, xy, uv, colors, index_array,
                            static_cast<jint>(texture), translation.x, translation.y, nullptr);
        env->DeleteLocalRef(type);
        env->DeleteLocalRef(xy);
        env->DeleteLocalRef(uv);
        env->DeleteLocalRef(colors);
        env->DeleteLocalRef(index_array);
    }

    void EnableScissorRegion(bool enable) override {
        scissor_enabled_ = enable;
        if (!gl4es_scissor.load()) {
            __android_log_print(ANDROID_LOG_ERROR, kTag,
                    "Unable to resolve GL4ES scissor functions");
            return;
        }
        if (enable) {
            gl4es_scissor.enable(GL_SCISSOR_TEST);
            apply_scissor();
        } else {
            gl4es_scissor.disable(GL_SCISSOR_TEST);
        }
    }

    void SetScissorRegion(int x, int y, int width, int height) override {
        scissor_x_ = x;
        scissor_y_ = y;
        scissor_width_ = width;
        scissor_height_ = height;
        if (scissor_enabled_ && gl4es_scissor.load()) apply_scissor();
    }

    bool LoadTexture(Rocket::Core::TextureHandle& texture,
                     Rocket::Core::Vector2i& dimensions,
                     const Rocket::Core::String& source) override {
        JNIEnv* env = current_env();
        if (env == nullptr || java_rocket == nullptr) return false;
        jclass rocket_type = env->GetObjectClass(java_rocket);
        jmethodID create = env->GetMethodID(rocket_type, "getNewTextureHolder",
                                            "()Lcom/LibRocket$TextureHolder;");
        jobject holder = env->CallObjectMethod(java_rocket, create);
        jclass holder_type = env->GetObjectClass(holder);
        jfieldID index_field = env->GetFieldID(holder_type, "index", "I");
        jfieldID width_field = env->GetFieldID(holder_type, "width", "I");
        jfieldID height_field = env->GetFieldID(holder_type, "height", "I");
        const jint index = env->GetIntField(holder, index_field);
        jmethodID load = env->GetMethodID(rocket_type, "LoadTexture", "(ILjava/lang/String;)Z");
        jstring path = java_string(env, source);
        const bool loaded = env->CallBooleanMethod(java_rocket, load, index, path) == JNI_TRUE;
        if (loaded) {
            texture = static_cast<Rocket::Core::TextureHandle>(index);
            dimensions.x = env->GetIntField(holder, width_field);
            dimensions.y = env->GetIntField(holder, height_field);
        }
        env->DeleteLocalRef(path);
        env->DeleteLocalRef(holder_type);
        env->DeleteLocalRef(holder);
        env->DeleteLocalRef(rocket_type);
        return loaded;
    }

    bool GenerateTexture(Rocket::Core::TextureHandle& texture, const Rocket::Core::byte* source,
                         const Rocket::Core::Vector2i& dimensions) override {
        JNIEnv* env = current_env();
        if (env == nullptr || java_rocket == nullptr) return false;
        jclass rocket_type = env->GetObjectClass(java_rocket);
        jmethodID create = env->GetMethodID(rocket_type, "getNewTextureHolder",
                                            "()Lcom/LibRocket$TextureHolder;");
        jobject holder = env->CallObjectMethod(java_rocket, create);
        jclass holder_type = env->GetObjectClass(holder);
        jfieldID index_field = env->GetFieldID(holder_type, "index", "I");
        env->SetIntField(holder, env->GetFieldID(holder_type, "width", "I"), dimensions.x);
        env->SetIntField(holder, env->GetFieldID(holder_type, "height", "I"), dimensions.y);
        const jint index = env->GetIntField(holder, index_field);
        const jsize size = dimensions.x * dimensions.y * 4;
        jbyteArray pixels = env->NewByteArray(size);
        env->SetByteArrayRegion(pixels, 0, size, reinterpret_cast<const jbyte*>(source));
        jmethodID generate = env->GetMethodID(rocket_type, "GenerateTexture", "(I[B)Z");
        const bool generated = env->CallBooleanMethod(java_rocket, generate, index, pixels)
                == JNI_TRUE;
        if (generated) texture = static_cast<Rocket::Core::TextureHandle>(index);
        env->DeleteLocalRef(pixels);
        env->DeleteLocalRef(holder_type);
        env->DeleteLocalRef(holder);
        env->DeleteLocalRef(rocket_type);
        return generated;
    }

    void ReleaseTexture(Rocket::Core::TextureHandle texture) override {
        JNIEnv* env = current_env();
        if (env == nullptr || java_rocket == nullptr) return;
        jclass type = env->GetObjectClass(java_rocket);
        jmethodID method = env->GetMethodID(type, "ReleaseTexture", "(I)V");
        env->CallVoidMethod(java_rocket, method, static_cast<jint>(texture));
        env->DeleteLocalRef(type);
    }

private:
    void apply_scissor() const {
        const int x = std::max(scissor_x_, 0);
        const int y = std::max(scissor_y_, 0);
        const int right = std::min(scissor_x_ + scissor_width_, rocket_width);
        const int bottom = std::min(scissor_y_ + scissor_height_, rocket_height);
        const int width = std::max(right - x, 0);
        const int height = std::max(bottom - y, 0);
        gl4es_scissor.set(x, std::max(rocket_height - bottom, 0), width, height);
    }

    bool scissor_enabled_ = false;
    int scissor_x_ = 0;
    int scissor_y_ = 0;
    int scissor_width_ = 0;
    int scissor_height_ = 0;
};

class JavaEventListener final : public Rocket::Core::EventListener {
public:
    explicit JavaEventListener(Rocket::Core::String value) : value_(std::move(value)) {}

    void ProcessEvent(Rocket::Core::Event&) override {
        JNIEnv* env = current_env();
        if (env == nullptr || java_rocket == nullptr) return;
        jclass type = env->GetObjectClass(java_rocket);
        jmethodID method = env->GetMethodID(type, "HandleEvent", "(Ljava/lang/String;)V");
        if (method == nullptr) {
            env->ExceptionClear();
            __android_log_print(ANDROID_LOG_ERROR, kTag,
                                "LibRocket.HandleEvent(String) is unavailable");
            env->DeleteLocalRef(type);
            return;
        }
        jstring value = java_string(env, value_);
        env->CallVoidMethod(java_rocket, method, value);
        env->DeleteLocalRef(value);
        env->DeleteLocalRef(type);
    }

    void OnDetach(Rocket::Core::Element*) override {
        delete this;
    }

private:
    Rocket::Core::String value_;
};

class JavaEventInstancer final : public Rocket::Core::EventListenerInstancer {
public:
    Rocket::Core::EventListener* InstanceEventListener(
            const Rocket::Core::String& value, Rocket::Core::Element*) override {
        return new JavaEventListener(value);
    }

    void Release() override {}
};

std::unique_ptr<AndroidFileInterface> file_interface;
std::unique_ptr<AndroidSystemInterface> system_interface;
std::unique_ptr<JavaRenderInterface> render_interface;
std::unique_ptr<JavaEventInstancer> event_instancer;

Rocket::Core::String string_value(JNIEnv* env, jstring value) {
    return Rocket::Core::String(utf8(env, value).c_str());
}

}

extern "C" JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM* vm, void*) {
    java_vm = vm;
    return JNI_VERSION_1_6;
}

extern "C" JNIEXPORT void JNICALL Java_com_LibRocket_setup(JNIEnv* env, jobject instance) {
    if (rocket_context != nullptr) return;
    java_rocket = env->NewGlobalRef(instance);
    file_interface = std::make_unique<AndroidFileInterface>();
    system_interface = std::make_unique<AndroidSystemInterface>();
    render_interface = std::make_unique<JavaRenderInterface>();
    event_instancer = std::make_unique<JavaEventInstancer>();
    Rocket::Core::SetFileInterface(file_interface.get());
    Rocket::Core::SetSystemInterface(system_interface.get());
    Rocket::Core::SetRenderInterface(render_interface.get());
    if (!Rocket::Core::Initialise()) {
        jclass error = env->FindClass("java/lang/IllegalStateException");
        env->ThrowNew(error, "Could not initialize libRocket 1.3");
        return;
    }
    Rocket::Controls::Initialise();
    Rocket::Core::Factory::RegisterEventListenerInstancer(event_instancer.get());
    jclass type = env->GetObjectClass(instance);
    const int width = env->GetIntField(instance, env->GetFieldID(type, "width", "I"));
    const int height = env->GetIntField(instance, env->GetFieldID(type, "height", "I"));
    rocket_width = std::max(width, 1);
    rocket_height = std::max(height, 1);
    env->DeleteLocalRef(type);
    rocket_context = Rocket::Core::CreateContext(
            "rusted-warfare", Rocket::Core::Vector2i(width, height), render_interface.get());
    if (rocket_context == nullptr) {
        jclass error = env->FindClass("java/lang/IllegalStateException");
        env->ThrowNew(error, "Could not create libRocket context");
        return;
    }
    __android_log_print(ANDROID_LOG_INFO, kTag, "libRocket ARM64 context initialized: %dx%d",
                        width, height);
}

extern "C" JNIEXPORT void JNICALL Java_com_LibRocket_update(JNIEnv*, jobject) {
    if (rocket_context != nullptr) rocket_context->Update();
}

extern "C" JNIEXPORT void JNICALL Java_com_LibRocket_render(JNIEnv*, jobject) {
    if (rocket_context != nullptr) rocket_context->Render();
}

extern "C" JNIEXPORT void JNICALL Java_com_LibRocket_setDimensions(
        JNIEnv*, jobject, jint width, jint height) {
    rocket_width = std::max(static_cast<int>(width), 1);
    rocket_height = std::max(static_cast<int>(height), 1);
    if (rocket_context != nullptr) rocket_context->SetDimensions({width, height});
}

extern "C" JNIEXPORT jobject JNICALL Java_com_LibRocket_loadDocument(
        JNIEnv* env, jobject, jstring path) {
    return rocket_context == nullptr ? nullptr
            : linked_element(env, rocket_context->LoadDocument(string_value(env, path)));
}

extern "C" JNIEXPORT void JNICALL Java_com_LibRocket_loadDocumentWithContainer(
        JNIEnv* env, jobject, jstring path, jobject container) {
    if (rocket_context == nullptr) return;
    auto* document = rocket_context->LoadDocument(string_value(env, path));
    if (document != nullptr) {
        document->AddReference();
        assign_handle(env, container, document);
    }
}

extern "C" JNIEXPORT void JNICALL Java_com_LibRocket_loadFont(
        JNIEnv* env, jobject, jstring path, jstring family) {
    const Rocket::Core::String font_path = string_value(env, path);
    const Rocket::Core::String requested_family = string_value(env, family);
    const bool loaded = family == nullptr || requested_family.Empty()
            ? Rocket::Core::FontDatabase::LoadFontFace(font_path)
            : Rocket::Core::FontDatabase::LoadFontFace(
                    font_path, requested_family,
                    Rocket::Core::Font::STYLE_NORMAL, Rocket::Core::Font::WEIGHT_NORMAL);
    if (!loaded) {
        __android_log_print(ANDROID_LOG_ERROR, kTag,
                            "Could not register font %s as %s", font_path.CString(),
                            requested_family.Empty() ? "its embedded family"
                                                     : requested_family.CString());
    }
}

#define ROCKET_INPUT_2(Name, Method) \
extern "C" JNIEXPORT void JNICALL Java_com_LibRocket_##Name( \
        JNIEnv*, jobject, jint first, jint second) { \
    if (rocket_context != nullptr) rocket_context->Method(first, second); \
}

ROCKET_INPUT_2(processMouseButtonDown, ProcessMouseButtonDown)
ROCKET_INPUT_2(processMouseButtonUp, ProcessMouseButtonUp)
ROCKET_INPUT_2(processMouseWheel, ProcessMouseWheel)
extern "C" JNIEXPORT void JNICALL Java_com_LibRocket_processKeyDown(
        JNIEnv*, jobject, jint key, jint modifiers) {
    if (rocket_context != nullptr) rocket_context->ProcessKeyDown(
            static_cast<Rocket::Core::Input::KeyIdentifier>(key), modifiers);
}

extern "C" JNIEXPORT void JNICALL Java_com_LibRocket_processKeyUp(
        JNIEnv*, jobject, jint key, jint modifiers) {
    if (rocket_context != nullptr) rocket_context->ProcessKeyUp(
            static_cast<Rocket::Core::Input::KeyIdentifier>(key), modifiers);
}

extern "C" JNIEXPORT void JNICALL Java_com_LibRocket_processMouseMove(
        JNIEnv*, jobject, jint x, jint y, jint modifiers) {
    if (rocket_context != nullptr) rocket_context->ProcessMouseMove(x, y, modifiers);
}

extern "C" JNIEXPORT void JNICALL Java_com_LibRocket_processTextInput(
        JNIEnv* env, jobject, jstring text) {
    if (rocket_context != nullptr) rocket_context->ProcessTextInput(string_value(env, text));
}

extern "C" JNIEXPORT void JNICALL Java_com_LibRocket_processTextInputChar(
        JNIEnv*, jobject, jint character) {
    if (rocket_context != nullptr) rocket_context->ProcessTextInput(
            static_cast<Rocket::Core::word>(character));
}

extern "C" JNIEXPORT jobject JNICALL Java_com_Element_getElementById(
        JNIEnv* env, jobject self, jstring id) {
    auto* element = element_handle(env, self);
    return element == nullptr ? nullptr
            : linked_element(env, element->GetElementById(string_value(env, id)));
}

extern "C" JNIEXPORT jstring JNICALL Java_com_Element_getTagName(
        JNIEnv* env, jobject self) {
    auto* element = element_handle(env, self);
    return element == nullptr ? nullptr : java_string(env, element->GetTagName());
}

extern "C" JNIEXPORT jboolean JNICALL Java_com_Element_focus(
        JNIEnv* env, jobject self) {
    auto* element = element_handle(env, self);
    return element != nullptr && element->Focus() ? JNI_TRUE : JNI_FALSE;
}

#define ELEMENT_VOID_0(Name, Method) \
extern "C" JNIEXPORT void JNICALL Java_com_Element_##Name(JNIEnv* env, jobject self) { \
    auto* element = element_handle(env, self); \
    if (element != nullptr) element->Method(); \
}

ELEMENT_VOID_0(blur, Blur)
ELEMENT_VOID_0(click, Click)
ELEMENT_VOID_0(addReference, AddReference)
ELEMENT_VOID_0(removeReference, RemoveReference)

extern "C" JNIEXPORT jstring JNICALL Java_com_Element_getAttribute(
        JNIEnv* env, jobject self, jstring name, jstring fallback) {
    auto* element = element_handle(env, self);
    if (element == nullptr) return fallback == nullptr ? nullptr
            : static_cast<jstring>(env->NewLocalRef(fallback));
    Rocket::Core::Variant* value = element->GetAttribute(string_value(env, name));
    return value == nullptr ? (fallback == nullptr ? nullptr
            : static_cast<jstring>(env->NewLocalRef(fallback)))
            : java_string(env, value->Get<Rocket::Core::String>());
}

extern "C" JNIEXPORT void JNICALL Java_com_Element_setAttribute(
        JNIEnv* env, jobject self, jstring name, jstring value) {
    auto* element = element_handle(env, self);
    if (element != nullptr) element->SetAttribute(
            string_value(env, name), string_value(env, value));
}

extern "C" JNIEXPORT jstring JNICALL Java_com_Element_getAttributeKey(
        JNIEnv* env, jobject self, jint requested) {
    auto* element = element_handle(env, self);
    if (element == nullptr) return nullptr;
    int cursor = 0;
    Rocket::Core::String key;
    Rocket::Core::String value;
    for (int index = 0; index <= requested; ++index) {
        if (!element->IterateAttributes(cursor, key, value)) return nullptr;
    }
    return java_string(env, key);
}

extern "C" JNIEXPORT jstring JNICALL Java_com_Element_getAttributeValue(
        JNIEnv* env, jobject self, jint requested) {
    auto* element = element_handle(env, self);
    if (element == nullptr) return nullptr;
    int cursor = 0;
    Rocket::Core::String key;
    Rocket::Core::String value;
    for (int index = 0; index <= requested; ++index) {
        if (!element->IterateAttributes(cursor, key, value)) return nullptr;
    }
    return java_string(env, value);
}

extern "C" JNIEXPORT jint JNICALL Java_com_Element_getNumAttributes(
        JNIEnv* env, jobject self) {
    auto* element = element_handle(env, self);
    return element == nullptr ? 0 : element->GetNumAttributes();
}

extern "C" JNIEXPORT jobject JNICALL Java_com_Element_getChild(
        JNIEnv* env, jobject self, jint index) {
    auto* element = element_handle(env, self);
    return element == nullptr ? nullptr : linked_element(env, element->GetChild(index));
}

extern "C" JNIEXPORT jint JNICALL Java_com_Element_getNumChildren(
        JNIEnv* env, jobject self) {
    auto* element = element_handle(env, self);
    return element == nullptr ? 0 : element->GetNumChildren();
}

extern "C" JNIEXPORT jstring JNICALL Java_com_Element_getInnerRML(
        JNIEnv* env, jobject self) {
    auto* element = element_handle(env, self);
    return element == nullptr ? nullptr : java_string(env, element->GetInnerRML());
}

extern "C" JNIEXPORT void JNICALL Java_com_Element_setInnerRML(
        JNIEnv* env, jobject self, jstring rml) {
    auto* element = element_handle(env, self);
    if (element != nullptr) element->SetInnerRML(string_value(env, rml));
}

extern "C" JNIEXPORT void JNICALL Java_com_Element_setClassNames(
        JNIEnv* env, jobject self, jstring names) {
    auto* element = element_handle(env, self);
    if (element != nullptr) element->SetClassNames(string_value(env, names));
}

extern "C" JNIEXPORT jstring JNICALL Java_com_Element_getClassNames(
        JNIEnv* env, jobject self) {
    auto* element = element_handle(env, self);
    return element == nullptr ? nullptr : java_string(env, element->GetClassNames());
}

extern "C" JNIEXPORT jobject JNICALL Java_com_Element_clone(
        JNIEnv* env, jobject self) {
    auto* element = element_handle(env, self);
    return element == nullptr ? nullptr : linked_element(env, element->Clone());
}

extern "C" JNIEXPORT void JNICALL Java_com_Element_appendChild(
        JNIEnv* env, jobject self, jobject child) {
    auto* element = element_handle(env, self);
    auto* child_element = element_handle(env, child);
    if (element != nullptr && child_element != nullptr) element->AppendChild(child_element);
}

extern "C" JNIEXPORT void JNICALL Java_com_Element_insertBefore(
        JNIEnv* env, jobject self, jobject child, jobject adjacent) {
    auto* element = element_handle(env, self);
    if (element != nullptr) element->InsertBefore(
            element_handle(env, child), element_handle(env, adjacent));
}

extern "C" JNIEXPORT void JNICALL Java_com_Element_removeChild(
        JNIEnv* env, jobject self, jobject child) {
    auto* element = element_handle(env, self);
    if (element != nullptr) element->RemoveChild(element_handle(env, child));
}

extern "C" JNIEXPORT jstring JNICALL Java_com_Element_getProperty(
        JNIEnv* env, jobject self, jstring name, jstring fallback) {
    auto* element = element_handle(env, self);
    const Rocket::Core::Property* property = element == nullptr ? nullptr
            : element->GetProperty(string_value(env, name));
    return property == nullptr ? (fallback == nullptr ? nullptr
            : static_cast<jstring>(env->NewLocalRef(fallback)))
            : java_string(env, property->ToString());
}

extern "C" JNIEXPORT void JNICALL Java_com_Element_setProperty(
        JNIEnv* env, jobject self, jstring name, jstring value) {
    auto* element = element_handle(env, self);
    if (element != nullptr) element->SetProperty(
            string_value(env, name), string_value(env, value));
}

extern "C" JNIEXPORT jboolean JNICALL Java_com_Element_isPseudoClassSet(
        JNIEnv* env, jobject self, jstring name) {
    auto* element = element_handle(env, self);
    return element != nullptr && element->IsPseudoClassSet(string_value(env, name))
            ? JNI_TRUE : JNI_FALSE;
}

#define ELEMENT_FLOAT(Name, Method) \
extern "C" JNIEXPORT jfloat JNICALL Java_com_Element_##Name(JNIEnv* env, jobject self) { \
    auto* element = element_handle(env, self); \
    return element == nullptr ? 0.0F : element->Method(); \
}

ELEMENT_FLOAT(getAbsoluteLeft, GetAbsoluteLeft)
ELEMENT_FLOAT(getAbsoluteTop, GetAbsoluteTop)
ELEMENT_FLOAT(getOffsetLeft, GetOffsetLeft)
ELEMENT_FLOAT(getOffsetTop, GetOffsetTop)
ELEMENT_FLOAT(getOffsetWidth, GetOffsetWidth)
ELEMENT_FLOAT(getOffsetHeight, GetOffsetHeight)
ELEMENT_FLOAT(getScrollTop, GetScrollTop)

extern "C" JNIEXPORT void JNICALL Java_com_Element_setScrollTop(
        JNIEnv* env, jobject self, jfloat value) {
    auto* element = element_handle(env, self);
    if (element != nullptr) element->SetScrollTop(value);
}

extern "C" JNIEXPORT void JNICALL Java_com_Element_scrollIntoView(
        JNIEnv* env, jobject self, jboolean top) {
    auto* element = element_handle(env, self);
    if (element != nullptr) element->ScrollIntoView(top == JNI_TRUE);
}

extern "C" JNIEXPORT void JNICALL Java_com_ElementDocument_show(
        JNIEnv* env, jobject self, jint focus) {
    auto* document = dynamic_cast<Rocket::Core::ElementDocument*>(element_handle(env, self));
    if (document != nullptr) document->Show(focus);
}

extern "C" JNIEXPORT void JNICALL Java_com_ElementDocument_hide(
        JNIEnv* env, jobject self) {
    auto* document = dynamic_cast<Rocket::Core::ElementDocument*>(element_handle(env, self));
    if (document != nullptr) document->Hide();
}

extern "C" JNIEXPORT void JNICALL Java_com_ElementDocument_close(
        JNIEnv* env, jobject self) {
    auto* document = dynamic_cast<Rocket::Core::ElementDocument*>(element_handle(env, self));
    if (document != nullptr) document->Close();
    assign_handle(env, self, nullptr);
}

extern "C" JNIEXPORT void JNICALL Java_com_ElementDocument_pullToFront(
        JNIEnv* env, jobject self) {
    auto* document = dynamic_cast<Rocket::Core::ElementDocument*>(element_handle(env, self));
    if (document != nullptr) document->PullToFront();
}

extern "C" JNIEXPORT void JNICALL Java_com_ElementDocument_pushToBack(
        JNIEnv* env, jobject self) {
    auto* document = dynamic_cast<Rocket::Core::ElementDocument*>(element_handle(env, self));
    if (document != nullptr) document->PushToBack();
}
