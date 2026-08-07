#include <jni.h>
#include <android/log.h>
#include <dlfcn.h>
#include <cerrno>
#include <cstdlib>
#include <cstring>
#include <string>
#include <unistd.h>
#include <vector>

namespace {
constexpr const char* kTag = "RustedFabricJvm";
constexpr jint kJniVersion18 = 0x00010008;
thread_local std::string last_error;

using CreateJavaVm = jint(JNICALL*)(JavaVM**, void**, void*);
using UpdateLdLibraryPath = void (*)(const char*);

std::string utf(JNIEnv* env, jstring value) {
    if (value == nullptr) return {};
    const char* chars = env->GetStringUTFChars(value, nullptr);
    if (chars == nullptr) return {};
    std::string result(chars);
    env->ReleaseStringUTFChars(value, chars);
    return result;
}

std::vector<std::string> strings(JNIEnv* env, jobjectArray values) {
    std::vector<std::string> result;
    if (values == nullptr) return result;
    const jsize count = env->GetArrayLength(values);
    result.reserve(static_cast<size_t>(count));
    for (jsize index = 0; index < count; ++index) {
        auto value = static_cast<jstring>(env->GetObjectArrayElement(values, index));
        result.push_back(utf(env, value));
        env->DeleteLocalRef(value);
    }
    return result;
}

void fail(const std::string& message) {
    last_error = message;
    __android_log_print(ANDROID_LOG_ERROR, kTag, "%s", message.c_str());
}

void update_linker_library_path(const std::string& value) {
    void* libdl = dlopen("libdl.so", RTLD_NOW | RTLD_LOCAL);
    if (libdl == nullptr) return;
    auto update = reinterpret_cast<UpdateLdLibraryPath>(
            dlsym(libdl, "android_update_LD_LIBRARY_PATH"));
    if (update == nullptr) {
        update = reinterpret_cast<UpdateLdLibraryPath>(
                dlsym(libdl, "__loader_android_update_LD_LIBRARY_PATH"));
    }
    if (update != nullptr) {
        update(value.c_str());
    } else {
        __android_log_print(ANDROID_LOG_WARN, kTag,
                            "Android linker path update symbol is unavailable");
    }
    dlclose(libdl);
}

void preload_if_present(const std::string& path) {
    if (access(path.c_str(), R_OK) != 0) return;
    if (dlopen(path.c_str(), RTLD_LAZY | RTLD_GLOBAL) == nullptr) {
        __android_log_print(ANDROID_LOG_WARN, kTag, "Optional preload failed for %s: %s",
                            path.c_str(), dlerror());
    } else {
        __android_log_print(ANDROID_LOG_INFO, kTag, "Optional preload succeeded for %s",
                            path.c_str());
    }
}
}

extern "C" JNIEXPORT jint JNICALL
Java_io_github_endx_rustedfabric_android_xposed_jvm_NativeJvmHost_nativeLaunch(
        JNIEnv* env, jclass, jstring runtime_home_value, jstring working_directory_value,
        jstring native_library_directory_value, jstring main_class_value,
        jobjectArray vm_option_values, jobjectArray argument_values) {
    last_error.clear();
    const std::string runtime_home = utf(env, runtime_home_value);
    const std::string working_directory = utf(env, working_directory_value);
    const std::string native_library_directory = utf(env, native_library_directory_value);
    std::string main_class = utf(env, main_class_value);
    if (runtime_home.empty() || working_directory.empty()
            || native_library_directory.empty() || main_class.empty()) {
        fail("Runtime home, working directory, native directory, or Java main class is empty");
        return 10;
    }
    if (chdir(working_directory.c_str()) != 0) {
        fail("Cannot enter game working directory: " + std::string(std::strerror(errno)));
        return 11;
    }

    const std::string runtime_lib = runtime_home + "/lib";
    const std::string server_lib = runtime_lib + "/server";
    const std::string jli_lib = runtime_lib + "/jli";
    const std::string library_path = server_lib + ":" + jli_lib + ":" + runtime_lib
            + ":" + native_library_directory;
    setenv("JAVA_HOME", runtime_home.c_str(), 1);
    setenv("LD_LIBRARY_PATH", library_path.c_str(), 1);
    update_linker_library_path(library_path);

    const std::string vm_path = server_lib + "/libjvm.so";
    if (access(vm_path.c_str(), R_OK) != 0) {
        fail("AArch64 libjvm.so is unavailable or unreadable");
        return 12;
    }
    void* vm_library = dlopen(vm_path.c_str(), RTLD_NOW | RTLD_GLOBAL);
    if (vm_library == nullptr) {
        fail(std::string("dlopen libjvm failed: ") + dlerror());
        return 13;
    }
    for (const char* library : {"libverify.so", "libjava.so", "libnet.so", "libnio.so",
                                "libzip.so", "libawt.so", "libawt_headless.so", "libfreetype.so",
                                "libfontmanager.so"}) {
        preload_if_present(runtime_lib + "/" + library);
    }
    auto create_vm = reinterpret_cast<CreateJavaVm>(dlsym(vm_library, "JNI_CreateJavaVM"));
    if (create_vm == nullptr) {
        fail(std::string("JNI_CreateJavaVM is unavailable: ") + dlerror());
        dlclose(vm_library);
        return 14;
    }

    std::vector<std::string> option_strings = strings(env, vm_option_values);
    std::vector<JavaVMOption> options(option_strings.size());
    for (size_t index = 0; index < option_strings.size(); ++index) {
        options[index].optionString = option_strings[index].data();
        options[index].extraInfo = nullptr;
    }
    JavaVMInitArgs init_args{};
    init_args.version = kJniVersion18;
    init_args.nOptions = static_cast<jint>(options.size());
    init_args.options = options.data();
    init_args.ignoreUnrecognized = JNI_FALSE;

    JavaVM* vm = nullptr;
    JNIEnv* game_env = nullptr;
    const jint create_result = create_vm(&vm, reinterpret_cast<void**>(&game_env), &init_args);
    if (create_result != JNI_OK || vm == nullptr || game_env == nullptr) {
        fail("JNI_CreateJavaVM failed with code " + std::to_string(create_result));
        dlclose(vm_library);
        return 20;
    }

    for (char& value : main_class) if (value == '.') value = '/';
    jclass main_type = game_env->FindClass(main_class.c_str());
    if (main_type == nullptr) {
        game_env->ExceptionDescribe();
        game_env->ExceptionClear();
        fail("Java main class was not found: " + main_class);
        vm->DestroyJavaVM();
        return 21;
    }
    jmethodID main_method = game_env->GetStaticMethodID(main_type, "main", "([Ljava/lang/String;)V");
    if (main_method == nullptr) {
        game_env->ExceptionClear();
        fail("Java main(String[]) method was not found");
        vm->DestroyJavaVM();
        return 22;
    }

    std::vector<std::string> arguments = strings(env, argument_values);
    jclass string_type = game_env->FindClass("java/lang/String");
    jobjectArray java_arguments = game_env->NewObjectArray(
            static_cast<jsize>(arguments.size()), string_type, nullptr);
    for (size_t index = 0; index < arguments.size(); ++index) {
        jstring argument = game_env->NewStringUTF(arguments[index].c_str());
        game_env->SetObjectArrayElement(java_arguments, static_cast<jsize>(index), argument);
        game_env->DeleteLocalRef(argument);
    }
    game_env->CallStaticVoidMethod(main_type, main_method, java_arguments);
    const bool failed = game_env->ExceptionCheck();
    if (failed) {
        game_env->ExceptionDescribe();
        game_env->ExceptionClear();
        fail("Java main class terminated with an uncaught exception");
    }
    game_env->DeleteLocalRef(java_arguments);
    game_env->DeleteLocalRef(string_type);
    game_env->DeleteLocalRef(main_type);
    const jint destroy_result = vm->DestroyJavaVM();
    if (destroy_result != JNI_OK && !failed) {
        fail("DestroyJavaVM failed with code " + std::to_string(destroy_result));
        return 24;
    }
    return failed ? 23 : 0;
}

extern "C" JNIEXPORT jstring JNICALL
Java_io_github_endx_rustedfabric_android_xposed_jvm_NativeJvmHost_nativeLastError(
        JNIEnv* env, jclass) {
    return env->NewStringUTF(last_error.c_str());
}
