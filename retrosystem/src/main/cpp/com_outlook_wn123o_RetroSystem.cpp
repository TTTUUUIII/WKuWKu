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
#define STATE_IDLE        1
#define STATE_RUNNING     2
#define STATE_PAUSED      3

static RetroSystem retro_system = {nullptr};
static std::unordered_map<std::string, std::string> all_cores;
static std::unordered_map<std::string, std::shared_ptr<RetroCore>> all_loaded_cores;
static std::shared_ptr<RetroCore> current_core;
static unsigned current_vw = 0;
static unsigned current_vh = 0;
static int current_state = STATE_IDLE;
static std::unique_ptr<Buffer<void*>> current_framebuffer = nullptr;
static jshortArray current_audio_buffer;

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
    if (current_state == STATE_RUNNING) {
        current_window->start();
    }
    return true;
}
extern "C"
JNIEXPORT void JNICALL
Java_com_outlook_wn123o_retrosystem_RetroSystem_nativeAdjustSurface(JNIEnv *env, jclass clazz, jint vw,
                                                              jint vh) {
    current_window->adjust_viewport(vw, vh);
}
extern "C"
JNIEXPORT void JNICALL
Java_com_outlook_wn123o_retrosystem_RetroSystem_nativeDetachSurface(JNIEnv *env, jclass clazz) {
    current_window->stop();
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
    if (current_state == RUNNING) {
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
        std::shared_ptr<RetroCore> new_core = std::make_shared<RetroCore>(core_alias, all_cores[core_alias], &no_error);
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
JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM *vm, void *) {
    JNIEnv *env;
    if (vm->GetEnv((void **) &env, JNI_VERSION_1_6) != JNI_OK) {
        return JNI_ERR;
    }
    retro_system.jvm = vm;
    jclass clazz = env->FindClass("com/outlook/wn123o/retrosystem/RetroSystem");
    retro_system.clazz = (jclass) env->NewGlobalRef(clazz);
    retro_system.environment_cb = env->GetStaticMethodID(clazz, "onNativeEnvironmentCallback", "(ILjava/lang/Object;)Z");
    retro_system.video_size_cb = env->GetStaticMethodID(clazz, "onNativeVideoSizeChanged", "(II)V");
    retro_system.audio_cb = env->GetStaticMethodID(clazz, "onNativeAudioCallback", "([SI)I");
    retro_system.input_cb = env->GetStaticMethodID(clazz, "onNativeInputCallback", "(IIII)I");
    retro_system.input_poll_cb = env->GetStaticMethodID(clazz, "onNativeInputPollCallback", "()V");
    current_audio_buffer = (jshortArray) env->NewGlobalRef(env->NewShortArray(0));
    clazz = env->FindClass("com/outlook/wn123o/retrosystem/common/Option");
    jmethodID constructor = env->GetMethodID(clazz, "<init>", "()V");
    retro_system.option_obj = env->NewGlobalRef(env->NewObject(clazz, constructor));
    clazz = env->FindClass("com/outlook/wn123o/retrosystem/common/Value");
    constructor = env->GetMethodID(clazz, "<init>", "()V");
    retro_system.value_obj = env->NewGlobalRef(env->NewObject(clazz, constructor));
    return JNI_VERSION_1_6;
}

extern "C"
JNIEXPORT void JNICALL JNI_OnUnload(JavaVM *vm, void *reserved) {
    JNIEnv *env;
    if (vm->GetEnv((void **) &env, JNI_VERSION_1_6) != JNI_OK) {
        return;
    }
    env->DeleteGlobalRef(retro_system.clazz);
    env->DeleteGlobalRef(current_audio_buffer);
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
    if (cmd == RETRO_ENVIRONMENT_GET_VARIABLE_UPDATE) {
        return false;
    }

    jobject option, value;
    switch (cmd) {
        case RETRO_ENVIRONMENT_GET_LOG_INTERFACE:
            struct retro_log_callback *log_callback;
            log_callback = (struct retro_log_callback *) data;
            log_callback->log = log_cb;
            break;
        case RETRO_ENVIRONMENT_GET_VARIABLE:
            variable = reinterpret_cast<struct retro_variable*>(data);
            option = new_option(env, variable->key);
            is_supported = env->CallStaticBooleanMethod(retro_system.clazz, retro_system.environment_cb, cmd, option);
            if (is_supported) {
                const std::string &val = get_option_value(env, option);
                variable->value = val.c_str();
            }
            break;
        case RETRO_ENVIRONMENT_GET_SYSTEM_DIRECTORY:
        case RETRO_ENVIRONMENT_GET_SAVE_DIRECTORY:
        case RETRO_ENVIRONMENT_GET_CORE_ASSETS_DIRECTORY:
            value = new_value(env);
            is_supported = env->CallStaticBooleanMethod(retro_system.clazz, retro_system.environment_cb, cmd, value);
            if (is_supported) {
                *(const char**)data = get_string_value(env, value).c_str();
            }
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
            if (data != nullptr) {
                variable = (struct retro_variable*) data;
                option = new_option(env, variable->key, variable->value);
                is_supported = env->CallStaticBooleanMethod(retro_system.clazz, retro_system.environment_cb, cmd, option);
            }
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

static void log_cb(enum retro_log_level level, const char *fmt, ...) {
    char buffer[512];
    va_list args;
    va_start(args, fmt);
    vsnprintf(buffer, sizeof(buffer), fmt, args);
    va_end(args);
    switch (level) {
        case RETRO_LOG_ERROR:
            LOGE(TAG, "%s", buffer);
            break;
        case RETRO_LOG_INFO:
            LOGI(TAG, "%s", buffer);
            break;
        case RETRO_LOG_WARN:
            LOGW(TAG, "%s", buffer);
            break;
        default:
            LOGD(TAG, "%s", buffer);
    }
}

static void video_cb(const void *data, unsigned width, unsigned height, size_t pitch) {
    if (current_core->hw_render_cb == nullptr && data != nullptr) {
        fill_framebuffer(data, width, height, pitch);
        if (current_core->pixel_format == RETRO_PIXEL_FORMAT_RGB565) {
            texture(GL_RGB, width, height, current_framebuffer->data);
        } else {
            texture(GL_RGBA, width, height, current_framebuffer->data);
        }
    }
    eglSwapBuffers(current_dyp, current_sf);
    if (current_vw != width || current_vh != height) {
        current_vw = width;
        current_vh = height;
        notify_video_size_changed();
    }
}

static void fill_framebuffer(const void *data, unsigned width, unsigned height, size_t pitch) {
    size_t bytes_per_pixel = 2;
    if(current_core->pixel_format == RETRO_PIXEL_FORMAT_XRGB8888) {
        bytes_per_pixel = 4;
    }
    size_t size = width * height * bytes_per_pixel;
    if(!current_framebuffer || current_framebuffer->size != size) {
        current_framebuffer = std::make_unique<Buffer<void*>>(malloc(size), size);
    }
    for (int i = 0; i < height; ++i) {
        memcpy((void*)(static_cast<const char*>(current_framebuffer->data) + i * width * bytes_per_pixel), static_cast<const char*>(data) + i * pitch, width * bytes_per_pixel);
    }
}

static void notify_video_size_changed() {
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
    env->CallStaticVoidMethod(retro_system.clazz, retro_system.video_size_cb, current_vw, current_vh);
    if (is_attached) {
        retro_system.jvm->DetachCurrentThread();
    }
}

static size_t audio_cb(const int16_t *data, size_t frames) {
    if (data == nullptr) return 0;
    JNIEnv *env;
    bool is_attached = false;
    size_t ret = 0;
    if (retro_system.jvm->GetEnv((void **) &env, JNI_VERSION_1_6) != JNI_OK) {
        if (retro_system.jvm->AttachCurrentThread(&env, nullptr) != JNI_OK) {
            LOGE(TAG, "Failed to attach env thread!");
            return frames;
        } else {
            is_attached = true;
        }
    }
    jsize capacity = env->GetArrayLength(current_audio_buffer);
    if (capacity < frames * 2) {
        env->DeleteGlobalRef(current_audio_buffer);
        current_audio_buffer = (jshortArray) env->NewGlobalRef(env->NewShortArray((int) frames * 2));
    }
    env->SetShortArrayRegion(current_audio_buffer, 0, (int) frames * 2, data);
    ret = env->CallStaticIntMethod(retro_system.clazz, retro_system.audio_cb, current_audio_buffer, frames);
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

static jobject new_value(JNIEnv  *env) {
    return retro_system.value_obj;
}

static std::string  get_string_value(JNIEnv *env, jobject obj) {
    jclass clazz = env->FindClass("com/outlook/wn123o/retrosystem/common/Value");
    jfieldID field_id = env->GetFieldID(clazz, "v", "Ljava/lang/Object;");
    jstring v = (jstring ) env->GetObjectField(obj, field_id);
    const char *chars = env->GetStringUTFChars(v, JNI_FALSE);
    std::string ret(chars);
    env->ReleaseStringUTFChars(v, chars);
    return ret;
}

static jobject new_option(JNIEnv *env, const char* key) {
    jclass clazz = env->FindClass("com/outlook/wn123o/retrosystem/common/Option");
    jfieldID field_id = env->GetFieldID(clazz, "key", "Ljava/lang/String;");
    env->SetObjectField(retro_system.option_obj, field_id, env->NewStringUTF(key));
    field_id = env->GetFieldID(clazz, "value", "Ljava/lang/String;");
    env->SetObjectField(retro_system.option_obj, field_id, nullptr);
    return retro_system.option_obj;
}

static jobject new_option(JNIEnv *env, const char* key, const char* val) {
    jclass clazz = env->FindClass("com/outlook/wn123o/retrosystem/common/Option");
    jfieldID field_id = env->GetFieldID(clazz, "key", "Ljava/lang/String;");
    env->SetObjectField(retro_system.option_obj, field_id, env->NewStringUTF(key));
    field_id = env->GetFieldID(clazz, "value", "Ljava/lang/String;");
    env->SetObjectField(retro_system.option_obj, field_id, env->NewStringUTF(val));
    return retro_system.option_obj;
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
    bool no_error;
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
        if (current_window) {
            current_window->start();
        }
        current_state = RUNNING;
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
        current_state = STATE_IDLE;
        current_vw = 0;
        current_vh = 0;
        current_core->unload_game();
    }
}

extern "C"
JNIEXPORT void JNICALL
Java_com_outlook_wn123o_retrosystem_RetroSystem_nativePause(JNIEnv *env, jclass clazz) {
    if (current_state == STATE_RUNNING) {
        current_state = STATE_PAUSED;
    }
}
extern "C"
JNIEXPORT void JNICALL
Java_com_outlook_wn123o_retrosystem_RetroSystem_nativeResume(JNIEnv *env, jclass clazz) {
    if (current_state == STATE_PAUSED) {
        current_state = STATE_RUNNING;
    }
}

extern "C"
JNIEXPORT jobject JNICALL
Java_com_outlook_wn123o_retrosystem_RetroSystem_nativeGetSystemInfo(JNIEnv *env, jclass clazz) {
    struct retro_system_info system_info = {};
    current_core->get_system_info(&system_info);
    jclass system_info_clazz = env->FindClass("com/outlook/wn123o/retrosystem/common/SystemInfo");
    jmethodID constructor = env->GetMethodID(system_info_clazz, "<init>",
                                             "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V");
    jobject obj = env->NewObject(system_info_clazz, constructor, env->NewStringUTF(system_info.library_name),
                                 env->NewStringUTF(system_info.library_version),
                                 env->NewStringUTF(system_info.valid_extensions));
    jfieldID need_full_path_filed = env->GetFieldID(system_info_clazz, "needFullpath", "Z");
    env->SetBooleanField(obj, need_full_path_filed, system_info.need_fullpath);
    jfieldID block_extract = env->GetFieldID(system_info_clazz, "blockExtract", "Z");
    env->SetBooleanField(obj, block_extract, system_info.block_extract);
    return obj;
}
extern "C"
JNIEXPORT jobject JNICALL
Java_com_outlook_wn123o_retrosystem_RetroSystem_nativeGetMediaInfo(JNIEnv *env, jclass clazz) {
    struct retro_system_av_info av_info = {0};
    current_core->get_system_av_info(&av_info);
    jclass obj_clazz = env->FindClass("com/outlook/wn123o/retrosystem/common/Timing");
    jmethodID constructor = env->GetMethodID(obj_clazz, "<init>", "(DD)V");
    jobject o0 = env->NewObject(obj_clazz, constructor, av_info.timing.fps,
                                av_info.timing.sample_rate);
    obj_clazz = env->FindClass("com/outlook/wn123o/retrosystem/common/Geometry");
    constructor = env->GetMethodID(obj_clazz, "<init>", "(IIIIF)V");
    jobject o1 = env->NewObject(obj_clazz, constructor, (jint) av_info.geometry.base_width,
                                (jint) av_info.geometry.base_height,
                                (jint) av_info.geometry.max_width,
                                (jint) av_info.geometry.max_height, av_info.geometry.aspect_ratio);
    obj_clazz = env->FindClass("com/outlook/wn123o/retrosystem/common/MediaInfo");
    constructor = env->GetMethodID(obj_clazz, "<init>", "(Lcom/outlook/wn123o/retrosystem/common/Geometry;Lcom/outlook/wn123o/retrosystem/common/Timing;)V");
    return env->NewObject(obj_clazz, constructor, o1, o0);
}

extern "C"
JNIEXPORT void JNICALL
Java_com_outlook_wn123o_retrosystem_RetroSystem_nativeReset(JNIEnv *env, jclass clazz) {
    current_core->reset();
}

extern "C"
JNIEXPORT jbyteArray JNICALL
Java_com_outlook_wn123o_retrosystem_RetroSystem_nativeGetSerializeData(JNIEnv *env, jclass clazz) {
    size_t len = current_core->get_serialize_size();
    if (len == 0) return nullptr;
    int8_t buffer[len];
    bool no_error = current_core->get_serialize_data((void *) buffer, len);
    if (no_error) {
        jbyteArray data = env->NewByteArray((jint) len);
        env->SetByteArrayRegion(data, 0, (jint) len, buffer);
        return data;
    }
    return nullptr;
}

extern "C"
JNIEXPORT jboolean JNICALL
Java_com_outlook_wn123o_retrosystem_RetroSystem_nativeSetSerializeData(JNIEnv *env, jclass clazz,
                                                                 jbyteArray data) {
    bool no_error = false;
    const size_t &len = current_core->get_serialize_size();
    if (env->GetArrayLength(data) == len) {
        jbyte *serialize_data = env->GetByteArrayElements(data, JNI_FALSE);
        no_error = current_core->set_serialize_data((void *) serialize_data, len);
        env->ReleaseByteArrayElements(data, serialize_data, 0);
    }
    return no_error;
}