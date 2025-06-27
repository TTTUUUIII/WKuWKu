#include <jni.h>
#include <unordered_map>
#include <sys/mman.h>
#include <sys/stat.h>
#include <unistd.h>
#include <fcntl.h>
#include "GLWindow.h"
#include "GLUtils.h"
#include "RetroCore.h"
#include "Log.h"
#include "com_outlook_wn123o_RetroSystem.h"

#define TAG "RetroSystem"

static RetroSystem retro_system = {nullptr};
static std::unordered_map<std::string, std::string> all_cores;
static std::unordered_map<std::string, std::shared_ptr<RetroCore>> all_loaded_cores;
static std::shared_ptr<RetroCore> current_core;

static EGLDisplay current_dyp;
static EGLSurface current_sf;
static std::unique_ptr<GLWindow> current_window;
static GLRenderer renderer = {
        on_create,
        on_draw,
        on_destroy
};

extern "C"
JNIEXPORT jboolean JNICALL
Java_com_outlook_wn123o_retrosystem_RetroSystem_nativeAttachSurface(JNIEnv *env, jclass clazz,
                                                              jobject surface) {
    current_window = std::make_unique<GLWindow>(ANativeWindow_fromSurface(env, surface));
    current_window->set_renderer(&renderer);
}
extern "C"
JNIEXPORT void JNICALL
Java_com_outlook_wn123o_retrosystem_RetroSystem_nativeAdjustSurface(JNIEnv *env, jclass clazz, jint vw,
                                                              jint vh) {
    if (current_window) {
        current_window->adjust_viewport(vw, vh);
    }
}
extern "C"
JNIEXPORT void JNICALL
Java_com_outlook_wn123o_retrosystem_RetroSystem_nativeDetachSurface(JNIEnv *env, jclass clazz) {
    if (current_window) {
        current_window->release();
    }
}

static void on_create(EGLDisplay dyp, EGLSurface sr) {
    current_dyp = dyp;
    current_sf = sr;
    if (current_core->hw_render_cb != nullptr) {
        current_core->hw_render_cb->context_reset();
    } else {
        begin_texture();
    }
}

static void on_draw() {
    if (current_core->hw_render_cb != nullptr) {
        current_core->run();
    }
}

static void on_destroy() {
    if(current_core->hw_render_cb != nullptr) {
        current_core->hw_render_cb->context_destroy();
    } else {
        end_texture();
    }
}

extern "C"
JNIEXPORT void JNICALL
Java_com_outlook_wn123o_retrosystem_RetroSystem_nativeAdd(JNIEnv *env, jclass clazz, jstring alias,
                                                    jstring path) {
    const char *core_alias = env->GetStringUTFChars(alias, JNI_FALSE);
    const char *core_path = env->GetStringUTFChars(path, JNI_FALSE);
    all_cores[core_alias] = core_path;
    env->ReleaseStringUTFChars(alias, core_alias);
    env->ReleaseStringUTFChars(path, core_path);
}

extern "C"
JNIEXPORT jboolean JNICALL
Java_com_outlook_wn123o_retrosystem_RetroSystem_nativeUse(JNIEnv *env, jclass clazz, jstring alias) {
    const char *core_alias = env->GetStringUTFChars(alias, JNI_FALSE);
    bool no_error = false;
    if (all_loaded_cores.count(core_alias) > 0) {
        current_core = all_loaded_cores[core_alias];
        no_error = true;
    } else if (all_cores.count(core_alias) > 0) {
        std::shared_ptr<RetroCore> new_core = std::make_shared<RetroCore>(all_cores[core_alias], &no_error);
        if (no_error) {
            current_core = new_core;
            new_core->set_environment_cb(environment_cb);
            new_core->init();
            new_core->set_video_refresh_cb(video_cb);
            new_core->set_audio_sample_batch_cb(audio_cb);
            new_core->set_input_state_cb(input_cb);
            new_core->set_input_poll_cb(input_pool_cb);
            all_loaded_cores[core_alias] = new_core;
        }
    }
    env->ReleaseStringUTFChars(alias, core_alias);
    return no_error;
}

extern "C"
JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM *vm, void *reserved) {
    JNIEnv *env;
    if (vm->GetEnv((void **) &env, JNI_VERSION_1_6) != JNI_OK) {
        return JNI_ERR;
    }
    retro_system.jvm = vm;
    jclass clazz = env->FindClass("com/outlook/wn123o/retrosystem/RetroSystem");
    retro_system.clazz = (jclass) env->NewGlobalRef(clazz);
    retro_system.environment_cb = env->GetStaticMethodID(clazz, "onNativeEnvironmentCallback", "(ILjava/lang/Object;)Z");
    retro_system.audio_cb = env->GetStaticMethodID(clazz, "onNativeAudioCallback", "([SI)I");
    retro_system.input_cb = env->GetStaticMethodID(clazz, "onNativeInputCallback", "(IIII)I");
    retro_system.input_poll_cb = env->GetStaticMethodID(clazz, "onNativeInputPollCallback", "()V");
    retro_system.string_buffer = env->NewByteArray(512);
    retro_system.audio_buffer = env->NewShortArray(0);
    return JNI_VERSION_1_6;
}

extern "C"
JNIEXPORT void JNICALL JNI_OnUnload(JavaVM *vm, void *reserved) {
    JNIEnv *env;
    if (vm->GetEnv((void **) &env, JNI_VERSION_1_6) != JNI_OK) {
        return;
    }
    env->DeleteGlobalRef(retro_system.clazz);
    retro_system.jvm = nullptr;
}

static bool environment_cb(unsigned cmd, void* data) {
    JNIEnv *env;
    bool is_attached = false;
    bool is_supported = true;
    if (retro_system.jvm->GetEnv((void **) &env, JNI_VERSION_1_6) != JNI_OK) {
        if (retro_system.jvm->AttachCurrentThread(&env, nullptr) != JNI_OK) {
            LOGE(TAG, "Failed to attach env thread!");
            return false;
        } else {
            is_attached = true;
        }
    }
    struct retro_variable *variable = nullptr;
    switch (cmd) {
        case RETRO_ENVIRONMENT_GET_VARIABLE:
            jobject option;
            variable = reinterpret_cast<struct retro_variable*>(data);
            option = new_option(env, variable->key, "");
            is_supported = env->CallStaticBooleanMethod(retro_system.clazz, retro_system.environment_cb, cmd, option);
            if (is_supported) {
                variable->value = get_option_value(env, option).c_str();
            }
            break;
        case RETRO_ENVIRONMENT_GET_SYSTEM_DIRECTORY:
            *(const char**)data = retro_system.system_directory.c_str();
            break;
        case RETRO_ENVIRONMENT_GET_SAVE_DIRECTORY:
            *(const char**)data = retro_system.save_directory.c_str();
            break;
        case RETRO_ENVIRONMENT_GET_CORE_ASSETS_DIRECTORY:
            *(const char**)data = retro_system.assets_directory.c_str();
            break;
        case RETRO_ENVIRONMENT_GET_LANGUAGE:
            *(unsigned*)data = RETRO_LANGUAGE_ENGLISH;
            break;
        case RETRO_ENVIRONMENT_GET_RUMBLE_INTERFACE:
            current_core->rumble_interface = reinterpret_cast<struct retro_rumble_interface*>(data);
            current_core->rumble_interface->set_rumble_state = rumble_cb;
            break;
        case RETRO_ENVIRONMENT_GET_INPUT_BITMASKS:
            return env->CallStaticBooleanMethod(retro_system.clazz, retro_system.environment_cb, cmd, nullptr);
            break;
        case RETRO_ENVIRONMENT_GET_CORE_OPTIONS_VERSION:
        case RETRO_ENVIRONMENT_GET_DISK_CONTROL_INTERFACE_VERSION:
        case RETRO_ENVIRONMENT_GET_MESSAGE_INTERFACE_VERSION:
            *(unsigned*)data = VERSION;
            break;
        case RETRO_ENVIRONMENT_GET_PREFERRED_HW_RENDER:
            *(unsigned*)data = RETRO_HW_CONTEXT_OPENGLES3;
            break;
        case RETRO_ENVIRONMENT_GET_AUDIO_VIDEO_ENABLE:
            *(unsigned*)data = FLAG_ENABLE_AUDIO | FLAG_ENABLE_VIDEO | FLAG_ENABLE_FAST_SAVESTATES;
            break;
        case RETRO_ENVIRONMENT_SET_VARIABLE:
            variable = (struct retro_variable*) data;
            option = new_option(env, variable->key, variable->value);
            is_supported = env->CallStaticBooleanMethod(retro_system.clazz, retro_system.environment_cb, cmd, option);
            break;
        case RETRO_ENVIRONMENT_SET_DISK_CONTROL_INTERFACE:
            current_core->disk_control = reinterpret_cast<struct retro_disk_control_callback*>(data);
            break;
        case RETRO_ENVIRONMENT_SET_DISK_CONTROL_EXT_INTERFACE:
            current_core->disk_control_ext = reinterpret_cast<struct retro_disk_control_ext_callback*>(data);
            break;
        case RETRO_ENVIRONMENT_SET_PIXEL_FORMAT:
            retro_pixel_format format;
            format = *(retro_pixel_format*) data;
            if (format == RETRO_PIXEL_FORMAT_RGB565 || format == RETRO_PIXEL_FORMAT_XRGB8888) {
                current_core->pixel_format = format;
            } else {
                is_supported = false;
            }
            break;
        case RETRO_ENVIRONMENT_SET_HW_RENDER:
            current_core->hw_render_cb = (struct retro_hw_render_callback*) data;
            current_core->hw_render_cb->get_current_framebuffer = get_current_framebuffer;
            current_core->hw_render_cb->get_proc_address = get_proc_address;
            break;
        default:
            is_supported = false;
    }

    if (is_attached) {
        retro_system.jvm->DetachCurrentThread();
    }
    return is_supported;
}

static void video_cb(const void *data, unsigned width, unsigned height, size_t pitch) {
    current_window->adjust_viewport(width, height);
    if (current_core->hw_render_cb == nullptr && data != nullptr) {
        if (current_core->pixel_format == GL_RGB565) {
            texture(GL_RGB, width, height, data);
        } else {
            texture(GL_RGBA, width, height, data);
        }
    }
    eglSwapBuffers(current_dyp, current_sf);
}

static size_t audio_cb(const int16_t *data, size_t frames) {
    JNIEnv *env;
    bool is_attached = false;
    size_t ret = 0;
    if (retro_system.jvm->GetEnv((void **) &env, JNI_VERSION_1_6) != JNI_OK) {
        if (retro_system.jvm->AttachCurrentThread(&env, nullptr) != JNI_OK) {
            LOGE(TAG, "Failed to attach env thread!");
            return false;
        } else {
            is_attached = true;
        }
    }

    jsize capacity = env->GetArrayLength(retro_system.audio_buffer);
    if (capacity < frames * 2) {
        retro_system.audio_buffer = env->NewShortArray((int) frames * 2);
    }
    env->SetShortArrayRegion(retro_system.audio_buffer, 0, (int) frames * 2, data);
    ret = env->CallStaticIntMethod(retro_system.clazz, retro_system.audio_cb, retro_system.audio_buffer, frames);
    if (is_attached) {
        retro_system.jvm->DetachCurrentThread();
    }
    return ret;
}

static int16_t input_cb(unsigned port, unsigned device, unsigned index, unsigned id) {
    JNIEnv *env;
    bool is_attached = false;
    int16_t ret;
    if (retro_system.jvm->GetEnv((void **) &env, JNI_VERSION_1_6) != JNI_OK) {
        if (retro_system.jvm->AttachCurrentThread(&env, nullptr) != JNI_OK) {
            LOGE(TAG, "Failed to attach env thread!");
            return 0;
        } else {
            is_attached = true;
        }
    }
    ret = (int16_t) env->CallStaticIntMethod(retro_system.clazz, retro_system.input_cb, port, device, index, id);
    if (is_attached) {
        retro_system.jvm->DetachCurrentThread();
    }
    return ret;
}

static void input_pool_cb() {
    JNIEnv *env;
    bool is_attached = false;
    if (retro_system.jvm->GetEnv((void **) &env, JNI_VERSION_1_6) != JNI_OK) {
        if (retro_system.jvm->AttachCurrentThread(&env, nullptr) != JNI_OK) {
            LOGE(TAG, "Failed to attach env thread!");
            return;
        } else {
            is_attached = true;
        }
    }
    env->CallStaticVoidMethod(retro_system.clazz, retro_system.input_poll_cb);
    if (is_attached) {
        retro_system.jvm->DetachCurrentThread();
    }
}

static bool rumble_cb(unsigned port, enum retro_rumble_effect effect, uint16_t strength) {
    JNIEnv *env;
    bool is_attached = false;
    bool ret = false;
    if (retro_system.jvm->GetEnv((void **) &env, JNI_VERSION_1_6) != JNI_OK) {
        if (retro_system.jvm->AttachCurrentThread(&env, nullptr) != JNI_OK) {
            LOGE(TAG, "Failed to attach env thread!");
            return false;
        } else {
            is_attached = true;
        }
    }
    ret = env->CallStaticBooleanMethod(retro_system.clazz, retro_system.rumble_cb, port, effect, strength);
    if (is_attached) {
        retro_system.jvm->DetachCurrentThread();
    }
    return ret;
}

static uintptr_t get_current_framebuffer() {
    return 0;
}

static retro_proc_address_t get_proc_address(const char* sym) {
    return eglGetProcAddress(sym);
}

static jobject new_option(JNIEnv *env, const char* key, const char* val) {
    jclass clazz = env->FindClass("com/outlook/wn123o/retrosystem/common/Option");
    jmethodID constructor = env->GetMethodID(clazz, "<init>", "(Ljava/lang/String;Ljava/lang/String;)V");
    return env->NewObject(clazz, constructor, env->NewStringUTF(key), env->NewStringUTF(val));
}

static std::string get_option_value(JNIEnv *env, jobject obj) {
    jclass clazz = env->FindClass("com/outlook/wn123o/retrosystem/common/Option");
    jfieldID field_id = env->GetFieldID(clazz, "value", "Ljava/lang/String;");
    auto val = (jstring) env->GetObjectField(obj, field_id);
    const char *chars = env->GetStringUTFChars(val, JNI_FALSE);
    std::string ret(chars);
    env->ReleaseStringUTFChars(val, chars);
    return ret;
}

extern "C"
JNIEXPORT jboolean JNICALL
Java_com_outlook_wn123o_retrosystem_RetroSystem_nativeStart(JNIEnv *env, jclass clazz,
                                                            jstring path) {
    const char *game_path = env->GetStringUTFChars(path, JNI_FALSE);
    bool no_error = true;
    struct retro_system_info system_info = {nullptr};
    struct retro_game_info game_info = {
            .path = game_path
    };
    current_core->get_system_info(&system_info);
    if (!system_info.need_fullpath) {
        int fd = open(game_path, O_RDONLY);
        if(fd == -1) return false;
        struct stat sb = {0};
        if (fstat(fd, &sb) == -1) {
            close(fd);
            return false;
        }
        game_info.size = sb.st_size;
        game_info.data = mmap(nullptr, sb.st_size, PROT_READ, MAP_PRIVATE, fd, 0);
    }
    no_error = current_core->load_game(&game_info);
    if (no_error) {
        current_window->prepare();
    }
    if (game_info.data) {
        munmap((void *) game_info.data, game_info.size);
    }
    env->ReleaseStringUTFChars(path, game_path);
    return no_error;
}

extern "C"
JNIEXPORT void JNICALL
Java_com_outlook_wn123o_retrosystem_RetroSystem_nativeStop(JNIEnv *env, jclass clazz) {
    if (current_core) {
        current_core->unload_game();
    }
}