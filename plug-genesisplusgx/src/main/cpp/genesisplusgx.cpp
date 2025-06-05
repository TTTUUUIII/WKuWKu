#include <jni.h>
#include <libretro/libretro.h>
#include <string>
#include <fstream>
#include "log.h"

#define TAG "Genesis_Plus_GX_Native"

#define ARRAY_SIZE(arr) sizeof(arr) / sizeof(arr[0])
typedef struct {
    JavaVM *jvm;
    jclass input_descriptor_clazz;
    jclass message_ext_clazz;
    jclass array_list_clazz;
    jobject emulator_obj;
    jmethodID message_ext_constructor;
    jmethodID input_descriptor_constructor;
    jmethodID array_list_constructor;
    jmethodID array_list_add_method;
    jmethodID environment_method;
    jmethodID video_refresh_method;
    jmethodID audio_sample_batch_method;
    jmethodID audio_buffer_state_method;
    jmethodID input_state_method;
    jmethodID input_poll_method;
    jmethodID rumble_method;
    jfieldID variable_value_field;
    jfieldID variable_entry_key_field;
    jfieldID variable_entry_value_field;
} em_context_t;

static em_context_t ctx = {nullptr};
static jobject variable_object;
static jobject variable_entry_object;
static jbyteArray frame_buffer = nullptr;
static jshortArray audio_buffer = nullptr;

static void set_variable_value(JNIEnv *env, jobject value) {
    env->SetObjectField(variable_object, ctx.variable_value_field, value);
}

static void set_variable_value(JNIEnv *env, jint value) {
    jclass clazz = env->FindClass("java/lang/Integer");
    jmethodID value_of_method = env->GetStaticMethodID(clazz, "valueOf", "(I)Ljava/lang/Integer;");
    jobject val = env->CallStaticObjectMethod(clazz, value_of_method, value);
    set_variable_value(env, val);
}

static void set_variable_value(JNIEnv *env, const char *value) {
    set_variable_value(env, env->NewStringUTF(value));
}

static jobject get_variable_value(JNIEnv *env) {
    return env->GetObjectField(variable_object, ctx.variable_value_field);
}

static jint get_variable_int_value(JNIEnv *env) {
    jobject integer_obj = env->GetObjectField(variable_object, ctx.variable_value_field);
    jclass clazz = env->FindClass("java/lang/Integer");
    jmethodID int_value_method = env->GetMethodID(clazz, "intValue", "()I");
    return env->CallIntMethod(integer_obj, int_value_method);
}

static void set_variable_entry(JNIEnv *env, const char *key, jobject value) {
    env->SetObjectField(variable_entry_object, ctx.variable_entry_key_field,
                        env->NewStringUTF(key));
    env->SetObjectField(variable_entry_object, ctx.variable_entry_value_field, value);
}

static void set_variable_entry(JNIEnv *env, const char *key, jint value) {
    env->SetObjectField(variable_entry_object, ctx.variable_entry_key_field,
                        env->NewStringUTF(key));
    jclass clazz = env->FindClass("java/lang/Integer");
    jmethodID value_of_method = env->GetStaticMethodID(clazz, "valueOf", "(I)Ljava/lang/Integer;");
    jobject val = env->CallStaticObjectMethod(clazz, value_of_method, value);
    set_variable_entry(env, key, val);
}

static jobject get_variable_entry_value(JNIEnv *env) {
    return env->GetObjectField(variable_entry_object, ctx.variable_entry_value_field);
}

static void log_print_callback(enum retro_log_level level, const char *fmt, ...) {
    char buffer[512];  // 缓冲区存储格式化日志
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
            break;
    }
}

static bool set_eject_state_t(bool ejected) {
    LOGD(TAG, "set_eject_state_t: %d", ejected);
    return false;
}

static bool get_eject_state_t() {
    LOGD(TAG, "get_eject_state_t");
    return false;
}

static unsigned get_image_index_t() {
    LOGD(TAG, "get_image_index_t");
    return 1;
}

static bool set_image_index_t(unsigned index) {
    LOGD(TAG, "set_image_index_t: %d", index);
    return false;
}

static unsigned get_num_images_t() {
    LOGD(TAG, "get_num_images_t");
    return 1;
}

static bool replace_image_index_t(unsigned index, const struct retro_game_info *info) {
    return false;
}

static bool add_image_index_t() {
    return false;
}

static void
video_refresh_callback(const void *data, unsigned width, unsigned height, size_t pitch) {
    JNIEnv *env;
    bool is_attached = false;
    if (ctx.jvm->GetEnv((void **) &env, JNI_VERSION_1_6) != JNI_OK) {
        if (ctx.jvm->AttachCurrentThread(&env, nullptr) != JNI_OK) {
            LOGE(TAG, "ERROR: unable attach env thread!");
            return;
        } else {
            is_attached = true;
        }
    }
    if (frame_buffer == nullptr || env->GetArrayLength(frame_buffer) != height * pitch) {
        if (frame_buffer != nullptr)
            env->DeleteGlobalRef(frame_buffer);
        frame_buffer = (jbyteArray) env->NewGlobalRef(env->NewByteArray(height * pitch));
    }
    env->SetByteArrayRegion(frame_buffer, 0, height * pitch, (jbyte *) data);
    env->CallVoidMethod(ctx.emulator_obj, ctx.video_refresh_method, frame_buffer, (jint) width,
                        (jint) height, (jint) pitch);
    if (is_attached)
        ctx.jvm->DetachCurrentThread();
}

static void audio_buffer_state_callback(bool active, unsigned occupancy, bool underrun_likely) {
    JNIEnv *env;
    bool is_attached = false;
    if (ctx.jvm->GetEnv((void **) &env, JNI_VERSION_1_6) != JNI_OK) {
        if (ctx.jvm->AttachCurrentThread(&env, nullptr) != JNI_OK) {
            LOGE(TAG, "ERROR: unable attach env thread!");
            return;
        } else {
            is_attached = true;
        }
    }
    env->CallVoidMethod(ctx.emulator_obj, ctx.audio_buffer_state_method, active, occupancy,
                        underrun_likely);
    if (is_attached)
        ctx.jvm->DetachCurrentThread();
}

static size_t audio_sample_batch_callback(const int16_t *data, size_t frames) {
    JNIEnv *env;
    bool is_attached = false;
    if (ctx.jvm->GetEnv((void **) &env, JNI_VERSION_1_6) != JNI_OK) {
        if (ctx.jvm->AttachCurrentThread(&env, nullptr) != JNI_OK) {
            LOGE(TAG, "ERROR: unable attach env thread!");
            return false;
        } else {
            is_attached = true;
        }
    }
    if (audio_buffer == nullptr || env->GetArrayLength(audio_buffer) != frames * 2) {
        if (audio_buffer != nullptr) {
            env->DeleteGlobalRef(audio_buffer);
        }
        audio_buffer = (jshortArray) env->NewGlobalRef(env->NewShortArray(frames * 2));
    }
    env->SetShortArrayRegion(audio_buffer, 0, frames * 2, data);
    env->CallVoidMethod(ctx.emulator_obj, ctx.audio_sample_batch_method, audio_buffer, frames);
    if (is_attached)
        ctx.jvm->DetachCurrentThread();
    return frames;
}

static int16_t input_state_callback(unsigned port, unsigned device, unsigned index, unsigned id) {
    JNIEnv *env;
    bool is_attached = false;
    if (ctx.jvm->GetEnv((void **) &env, JNI_VERSION_1_6) != JNI_OK) {
        if (ctx.jvm->AttachCurrentThread(&env, nullptr) != JNI_OK) {
            LOGE(TAG, "ERROR: unable attach env thread!");
            return 0;
        } else {
            is_attached = true;
        }
    }
    auto state = (int16_t) env->CallIntMethod(ctx.emulator_obj, ctx.input_state_method, port,
                                              device,
                                              index,
                                              id);
    if (is_attached)
        ctx.jvm->DetachCurrentThread();
    return state;
}

static void input_poll_callback() {
    JNIEnv *env;
    bool is_attached = false;
    if (ctx.jvm->GetEnv((void **) &env, JNI_VERSION_1_6) != JNI_OK) {
        if (ctx.jvm->AttachCurrentThread(&env, nullptr) != JNI_OK) {
            LOGE(TAG, "ERROR: unable attach env thread!");
            return;
        } else {
            is_attached = true;
        }
    }
    env->CallVoidMethod(ctx.emulator_obj, ctx.input_poll_method);
    if (is_attached)
        ctx.jvm->DetachCurrentThread();
}

static bool set_rumble_state_callback(unsigned port, enum retro_rumble_effect effect, uint16_t strength) {
    JNIEnv *env;
    bool handled = false;
    bool is_attached = false;
    if (ctx.jvm->GetEnv((void **) &env, JNI_VERSION_1_6) != JNI_OK) {
        if (ctx.jvm->AttachCurrentThread(&env, nullptr) != JNI_OK) {
            LOGE(TAG, "ERROR: unable attach env thread!");
            return false;
        } else {
            is_attached = true;
        }
    }
    handled = env->CallBooleanMethod(ctx.emulator_obj, ctx.rumble_method, port, effect, strength);
    if (is_attached)
        ctx.jvm->DetachCurrentThread();
    return handled;
}

static bool environment_callback(unsigned cmd, void *data) {
    JNIEnv *env;
    bool is_attached = false;
    if (ctx.jvm->GetEnv((void **) &env, JNI_VERSION_1_6) != JNI_OK) {
        if (ctx.jvm->AttachCurrentThread(&env, nullptr) != JNI_OK) {
            LOGE(TAG, "ERROR: unable attach env thread!");
            return false;
        } else {
            is_attached = true;
        }
    }
    switch (cmd) {
        case RETRO_ENVIRONMENT_GET_VARIABLE_UPDATE:
        case RETRO_ENVIRONMENT_GET_FASTFORWARDING:
            return env->CallBooleanMethod(ctx.emulator_obj, ctx.environment_method, cmd, nullptr);
        case RETRO_ENVIRONMENT_SET_PERFORMANCE_LEVEL:
            break;
        case RETRO_ENVIRONMENT_SET_ROTATION:
            set_variable_value(env, (jint)*(unsigned *) data);
            return env->CallBooleanMethod(ctx.emulator_obj, ctx.environment_method, cmd, variable_object);
        case RETRO_ENVIRONMENT_GET_LOG_INTERFACE:
            struct retro_log_callback *log_cb;
            log_cb = (struct retro_log_callback *) data;
            log_cb->log = log_print_callback;
            break;
        case RETRO_ENVIRONMENT_SET_AUDIO_BUFFER_STATUS_CALLBACK:
            if (data != nullptr) {
                auto *audio_buffer_state = (struct retro_audio_buffer_status_callback *) data;
                audio_buffer_state->callback = audio_buffer_state_callback;
            }
            break;
        case RETRO_ENVIRONMENT_GET_RUMBLE_INTERFACE: {
            auto *rumble = (struct retro_rumble_interface *) data;
            rumble->set_rumble_state = set_rumble_state_callback;
        }
            break;
        case RETRO_ENVIRONMENT_SET_MINIMUM_AUDIO_LATENCY:
            set_variable_value(env, (jint) (*(unsigned *) data));
            return env->CallBooleanMethod(ctx.emulator_obj, ctx.environment_method, cmd,
                                          variable_object);
        case RETRO_ENVIRONMENT_SET_MESSAGE_EXT: {
            auto *msg_ext = (struct retro_message_ext *) data;
            jobject jmsg_ext = env->NewObject(ctx.message_ext_clazz,
                                              ctx.message_ext_constructor,
                                              env->NewStringUTF(msg_ext->msg),
                                              msg_ext->priority,
                                              msg_ext->level,
                                              msg_ext->target,
                                              msg_ext->type,
                                              msg_ext->progress,
                                              msg_ext->duration
            );
            return env->CallBooleanMethod(ctx.emulator_obj, ctx.environment_method, cmd, jmsg_ext);
        }
        case RETRO_ENVIRONMENT_GET_VARIABLE: {
            struct retro_variable *variable;
            variable = (struct retro_variable *) data;
            set_variable_entry(env, variable->key, nullptr);
            bool supported = env->CallBooleanMethod(ctx.emulator_obj, ctx.environment_method, cmd,
                                                    variable_entry_object);
            if (supported) {
                auto value = (jstring) get_variable_entry_value(env);
                variable->value = env->GetStringUTFChars(value, JNI_FALSE);
            }
            return supported;
        }
        case RETRO_ENVIRONMENT_SET_VARIABLE: {
            struct retro_variable *variable = nullptr;
            if (data != nullptr) {
                variable = (struct retro_variable *) data;
                set_variable_entry(env, variable->key, env->NewStringUTF(variable->value));
            }
            return env->CallBooleanMethod(ctx.emulator_obj, ctx.environment_method, cmd,
                                          variable_entry_object);
        }
        case RETRO_ENVIRONMENT_SET_CONTROLLER_INFO: {
            auto *controller_info = (struct retro_controller_info *) data;
            jobject array_list = env->NewObject(ctx.array_list_clazz, ctx.array_list_constructor);
            jclass controller_desc_clazz = env->FindClass(
                    "ink/snowland/wkuwku/common/ControllerDescription");
            jmethodID constructor = env->GetMethodID(controller_desc_clazz, "<init>",
                                                     "(Ljava/lang/String;I)V");
            for (int i = 0; i < controller_info->num_types; ++i) {
                jobject controller_desc = env->NewObject(controller_desc_clazz, constructor,
                                                         env->NewStringUTF(
                                                                 controller_info->types[i].desc),
                                                         (jint) controller_info->types[i].id);
                env->CallVoidMethod(array_list, ctx.array_list_add_method, i, controller_desc);
            }
            return env->CallBooleanMethod(ctx.emulator_obj, ctx.environment_method, cmd, array_list);
        }
        case RETRO_ENVIRONMENT_SET_GEOMETRY: {
            auto geometry = (struct retro_game_geometry *) data;
            jclass clazz = env->FindClass("ink/snowland/wkuwku/common/EmGameGeometry");
            jmethodID constructor = env->GetMethodID(clazz, "<init>", "(IIIIF)V");
            jobject o1 = env->NewObject(clazz, constructor, (jint) geometry->base_width,
                                        (jint) geometry->base_height,
                                        (jint) geometry->max_width,
                                        (jint) geometry->max_height, geometry->aspect_ratio);
            return env->CallBooleanMethod(ctx.emulator_obj, ctx.environment_method, cmd, o1);
        }
        case RETRO_ENVIRONMENT_SET_CONTENT_INFO_OVERRIDE:
        case RETRO_ENVIRONMENT_GET_GAME_INFO_EXT:
        case RETRO_ENVIRONMENT_SET_CORE_OPTIONS_UPDATE_DISPLAY_CALLBACK:
        case RETRO_ENVIRONMENT_SET_CORE_OPTIONS_DISPLAY:
        case RETRO_ENVIRONMENT_SET_SUBSYSTEM_INFO:
        case RETRO_ENVIRONMENT_GET_VFS_INTERFACE:
        case RETRO_ENVIRONMENT_GET_LED_INTERFACE:
        case RETRO_ENVIRONMENT_GET_CURRENT_SOFTWARE_FRAMEBUFFER:
        case RETRO_ENVIRONMENT_GET_CAN_DUPE:
            /*Currently not supported*/
            return false;
        case RETRO_ENVIRONMENT_SET_SERIALIZATION_QUIRKS:
        case RETRO_ENVIRONMENT_SET_MEMORY_MAPS:
        case RETRO_ENVIRONMENT_SET_VARIABLES:
            /*Ignored*/
            break;
        case RETRO_ENVIRONMENT_GET_INPUT_BITMASKS:
            return env->CallBooleanMethod(ctx.emulator_obj, ctx.environment_method, cmd, nullptr);
        case RETRO_ENVIRONMENT_GET_CORE_OPTIONS_VERSION:
            set_variable_value(env, (jint) *(unsigned *) data);
            return env->CallBooleanMethod(ctx.emulator_obj, ctx.environment_method, cmd,
                                          variable_object);
        case RETRO_ENVIRONMENT_GET_DISK_CONTROL_INTERFACE_VERSION:
            /*use legacy interface*/
            return false;
        case RETRO_ENVIRONMENT_SET_DISK_CONTROL_INTERFACE: {
            if (data == nullptr) return true;
            auto *disk_control = (struct retro_disk_control_callback *) data;
            disk_control->set_image_index = set_image_index_t;
            disk_control->add_image_index = add_image_index_t;
            disk_control->replace_image_index = replace_image_index_t;
            disk_control->get_eject_state = get_eject_state_t;
            disk_control->set_eject_state = set_eject_state_t;
            disk_control->get_image_index = get_image_index_t;
            disk_control->get_num_images = get_num_images_t;
        }
            break;
        case RETRO_ENVIRONMENT_GET_LANGUAGE: {
            set_variable_value(env, RETRO_LANGUAGE_DUMMY);
            bool ret = env->CallBooleanMethod(ctx.emulator_obj, ctx.environment_method, cmd, variable_object);
            if (ret) {
                auto language = get_variable_int_value(env);
                *(unsigned *) data = language;
            }
            return ret;
        }
        case RETRO_ENVIRONMENT_SET_PIXEL_FORMAT:
            set_variable_value(env, *((jint *) data));
            return env->CallBooleanMethod(ctx.emulator_obj, ctx.environment_method, cmd,
                                          variable_object);
        case RETRO_ENVIRONMENT_SET_INPUT_DESCRIPTORS: {
            jint index = 0;
            struct retro_input_descriptor *desc;
            desc = (struct retro_input_descriptor *) data;
            jobject array_list = env->NewObject(ctx.array_list_clazz, ctx.array_list_constructor);
            while (desc->description != nullptr) {
                jobject it = env->NewObject(
                        ctx.input_descriptor_clazz,
                        ctx.input_descriptor_constructor,
                        desc->port,
                        desc->device,
                        desc->index,
                        desc->id,
                        env->NewStringUTF(desc->description));
                env->CallVoidMethod(array_list, ctx.array_list_add_method, index, it);
                desc++;
                index++;
            }
            bool ret = env->CallBooleanMethod(ctx.emulator_obj, ctx.environment_method, cmd, array_list);
            return ret;
        }
        case RETRO_ENVIRONMENT_GET_AUDIO_VIDEO_ENABLE: {
            set_variable_value(env, 0);
            bool ret = env->CallBooleanMethod(ctx.emulator_obj, ctx.environment_method, cmd, variable_object);
            if (ret) {
                jint val = get_variable_int_value(env);
                *(int *) data = val;
            }
            return ret;
        }
        case RETRO_ENVIRONMENT_SET_SYSTEM_AV_INFO: {
            auto *av_info = (struct retro_system_av_info *) data;
            jclass clazz = env->FindClass("ink/snowland/wkuwku/common/EmSystemTiming");
            jmethodID constructor = env->GetMethodID(clazz, "<init>", "(DD)V");
            jobject o0 = env->NewObject(clazz, constructor, av_info->timing.fps,
                                        av_info->timing.sample_rate);
            clazz = env->FindClass("ink/snowland/wkuwku/common/EmGameGeometry");
            constructor = env->GetMethodID(clazz, "<init>", "(IIIIF)V");
            jobject o1 = env->NewObject(clazz, constructor, (jint) av_info->geometry.base_width,
                                        (jint) av_info->geometry.base_height,
                                        (jint) av_info->geometry.max_width,
                                        (jint) av_info->geometry.max_height,
                                        av_info->geometry.aspect_ratio);
            clazz = env->FindClass("ink/snowland/wkuwku/common/EmSystemAvInfo");
            constructor = env->GetMethodID(clazz, "<init>",
                                           "(Link/snowland/wkuwku/common/EmGameGeometry;Link/snowland/wkuwku/common/EmSystemTiming;)V");
            bool ret = env->CallBooleanMethod(ctx.emulator_obj, ctx.environment_method, cmd,
                                              env->NewObject(clazz, constructor, o1, o0));
            return ret;
        }
        case RETRO_ENVIRONMENT_GET_SYSTEM_DIRECTORY:
        case RETRO_ENVIRONMENT_GET_SAVE_DIRECTORY: {
            bool ret = env->CallBooleanMethod(ctx.emulator_obj, ctx.environment_method, cmd, variable_object);
            if (ret) {
                auto path = (jstring) get_variable_value(env);
                *((const char **) data) = env->GetStringUTFChars(path, JNI_FALSE);
            }
            return ret;
        }
        case RETRO_ENVIRONMENT_GET_MESSAGE_INTERFACE_VERSION: {
            set_variable_value(env, 0);
            env->CallBooleanMethod(ctx.emulator_obj, ctx.environment_method, cmd, variable_object);
            jint version = (jint) get_variable_int_value(env);
            *(int *) data = version;
            LOGI(TAG, "INFO: message interface version: %d", version);
        }
            break;
        default:
            LOGW(TAG, "WARN: environment: %d ignored.", cmd);
            return false;
    }
    if (is_attached)
        ctx.jvm->DetachCurrentThread();
    return true;
}

extern "C"
JNIEXPORT void JNICALL
Java_ink_snowland_wkuwku_plug_genesisplusgx_GenesisPlusGX_nativePowerOn(JNIEnv *env, jobject thiz) {
    ctx.emulator_obj = env->NewGlobalRef(thiz);
    retro_set_environment(environment_callback);
    retro_init();
    retro_set_video_refresh(video_refresh_callback);
    retro_set_audio_sample_batch(audio_sample_batch_callback);
    retro_set_input_state(input_state_callback);
    retro_set_input_poll(input_poll_callback);
}
extern "C"
JNIEXPORT void JNICALL
Java_ink_snowland_wkuwku_plug_genesisplusgx_GenesisPlusGX_nativePowerOff(JNIEnv *env, jobject thiz) {
    retro_unload_game();
    retro_deinit();
    env->DeleteGlobalRef(ctx.emulator_obj);
}
extern "C"
JNIEXPORT void JNICALL
Java_ink_snowland_wkuwku_plug_genesisplusgx_GenesisPlusGX_nativeReset(JNIEnv *env, jobject thiz) {
    retro_reset();
}
extern "C"
JNIEXPORT jboolean JNICALL
Java_ink_snowland_wkuwku_plug_genesisplusgx_GenesisPlusGX_nativeLoad(JNIEnv *env, jobject thiz, jstring jpath) {
    const char *path = env->GetStringUTFChars(jpath, JNI_FALSE);
    struct retro_game_info info = {path, nullptr, 0, nullptr};
    bool state = retro_load_game(&info);
    env->ReleaseStringUTFChars(jpath, path);
    return state;
}
extern "C"
JNIEXPORT void JNICALL
Java_ink_snowland_wkuwku_plug_genesisplusgx_GenesisPlusGX_nativeRun(JNIEnv *env, jobject thiz) {
    retro_run();
}
extern "C"
JNIEXPORT jobject JNICALL
Java_ink_snowland_wkuwku_plug_genesisplusgx_GenesisPlusGX_nativeGetSystemAvInfo(JNIEnv *env, jobject thiz) {
    struct retro_system_av_info av_info = {0};
    retro_get_system_av_info(&av_info);
    jclass clazz = env->FindClass("ink/snowland/wkuwku/common/EmSystemTiming");
    jmethodID constructor = env->GetMethodID(clazz, "<init>", "(DD)V");
    jobject o0 = env->NewObject(clazz, constructor, av_info.timing.fps,
                                av_info.timing.sample_rate);
    clazz = env->FindClass("ink/snowland/wkuwku/common/EmGameGeometry");
    constructor = env->GetMethodID(clazz, "<init>", "(IIIIF)V");
    jobject o1 = env->NewObject(clazz, constructor, (jint) av_info.geometry.base_width,
                                (jint) av_info.geometry.base_height,
                                (jint) av_info.geometry.max_width,
                                (jint) av_info.geometry.max_height, av_info.geometry.aspect_ratio);
    clazz = env->FindClass("ink/snowland/wkuwku/common/EmSystemAvInfo");
    constructor = env->GetMethodID(clazz, "<init>",
                                   "(Link/snowland/wkuwku/common/EmGameGeometry;Link/snowland/wkuwku/common/EmSystemTiming;)V");
    return env->NewObject(clazz, constructor, o1, o0);
}
extern "C"
JNIEXPORT jobject JNICALL
Java_ink_snowland_wkuwku_plug_genesisplusgx_GenesisPlusGX_nativeGetSystemInfo(JNIEnv *env, jobject thiz) {
    struct retro_system_info system_info = {};
    retro_get_system_info(&system_info);
    jclass clazz = env->FindClass("ink/snowland/wkuwku/common/EmSystemInfo");
    jmethodID constructor = env->GetMethodID(clazz, "<init>",
                                             "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V");
    jobject obj = env->NewObject(clazz, constructor, env->NewStringUTF(system_info.library_name),
                                 env->NewStringUTF(system_info.library_version),
                                 env->NewStringUTF(system_info.valid_extensions));
    jfieldID need_full_path_filed = env->GetFieldID(clazz, "needFullpath", "Z");
    env->SetBooleanField(obj, need_full_path_filed, system_info.need_fullpath);
    jfieldID block_extract = env->GetFieldID(clazz, "blockExtract", "Z");
    env->SetBooleanField(obj, block_extract, system_info.block_extract);
    return obj;
}
extern "C"
JNIEXPORT jboolean JNICALL
Java_ink_snowland_wkuwku_plug_genesisplusgx_GenesisPlusGX_nativeSaveMemoryRam(JNIEnv *env, jobject thiz,
                                                            jstring path) {
    size_t len = retro_get_memory_size(RETRO_MEMORY_SAVE_RAM);
    if (len == 0) return false;
    void *mem = retro_get_memory_data(RETRO_MEMORY_SAVE_RAM);
    if (mem == nullptr) return false;
    const char *_path = env->GetStringUTFChars(path, JNI_FALSE);
    std::ofstream fp(_path, std::ios::out | std::ios::binary);
    if (!fp.is_open()) return false;
    fp.write(reinterpret_cast<char *>(mem), (int) len).flush();
    fp.close();
    env->ReleaseStringUTFChars(path, _path);
    return true;
}
extern "C"
JNIEXPORT jboolean JNICALL
Java_ink_snowland_wkuwku_plug_genesisplusgx_GenesisPlusGX_nativeLoadMemoryRam(JNIEnv *env, jobject thiz,
                                                            jstring path) {
    bool no_error = false;
    const char *_path = env->GetStringUTFChars(path, JNI_FALSE);
    std::ifstream fp(_path, std::ios::in | std::ios::binary);
    if (fp.is_open()) {
        size_t len = retro_get_memory_size(RETRO_MEMORY_SAVE_RAM);
        void *mem = retro_get_memory_data(RETRO_MEMORY_SAVE_RAM);
        if (mem != nullptr && len != 0) {
            fp.read(reinterpret_cast<char *>(mem), (int) len);
            no_error = true;
        }
        fp.close();
    }
    env->ReleaseStringUTFChars(path, _path);
    return no_error;
}
extern "C"
JNIEXPORT jboolean JNICALL
Java_ink_snowland_wkuwku_plug_genesisplusgx_GenesisPlusGX_nativeSaveState(JNIEnv *env, jobject thiz,
                                                        jstring path) {
    size_t len = retro_serialize_size();
    if (len == 0) return false;
    void *mem = calloc(1, len);
    if (mem == nullptr) return false;
    if (!retro_serialize(mem, len)) {
        free(mem);
        return false;
    }
    const char *_path = env->GetStringUTFChars(path, JNI_FALSE);
    std::ofstream fp(_path, std::ios::out | std::ios::binary);
    if (!fp.is_open()) {
        free(mem);
        return false;
    }
    fp.write(reinterpret_cast<char *>(mem), (int) len).flush();
    fp.close();
    env->ReleaseStringUTFChars(path, _path);
    return true;
}
extern "C"
JNIEXPORT jboolean JNICALL
Java_ink_snowland_wkuwku_plug_genesisplusgx_GenesisPlusGX_nativeLoadState(JNIEnv *env, jobject thiz,
                                                        jstring path) {
    bool no_error = false;
    size_t len = retro_serialize_size();
    const char *_path = env->GetStringUTFChars(path, JNI_FALSE);
    std::ifstream fp(_path);
    if (fp.is_open() && len != 0) {
        void *mem = calloc(1, len);
        if (mem != nullptr) {
            fp.read(static_cast<char *>(mem), (int) len);
            retro_unserialize(mem, len);
            free(mem);
            no_error = true;
        }
        fp.close();
    }
    env->ReleaseStringUTFChars(path, _path);
    return no_error;
}

extern "C"
JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM *vm, void *reserved) {
    JNIEnv *env;
    if (vm->GetEnv((void **) &env, JNI_VERSION_1_6) != JNI_OK) {
        LOGE(TAG, "ERROR: jni load failed!");
        return JNI_ERR;
    }
    ctx.jvm = vm;
    jclass clazz = env->FindClass("ink/snowland/wkuwku/common/Variable");
    ctx.variable_value_field = env->GetFieldID(clazz, "value", "Ljava/lang/Object;");
    jmethodID constructor = env->GetMethodID(clazz, "<init>", "()V");
    variable_object = env->NewGlobalRef(env->NewObject(clazz, constructor));
    clazz = env->FindClass("ink/snowland/wkuwku/common/VariableEntry");
    ctx.variable_entry_value_field = env->GetFieldID(clazz, "value", "Ljava/lang/String;");
    ctx.variable_entry_key_field = env->GetFieldID(clazz, "key", "Ljava/lang/String;");
    constructor = env->GetMethodID(clazz, "<init>", "()V");
    variable_entry_object = env->NewGlobalRef(env->NewObject(clazz, constructor));
    clazz = env->FindClass("ink/snowland/wkuwku/common/InputDescriptor");
    constructor = env->GetMethodID(clazz, "<init>", "(IIIILjava/lang/String;)V");
    ctx.input_descriptor_clazz = (jclass) env->NewGlobalRef(clazz);
    ctx.input_descriptor_constructor = constructor;
    clazz = env->FindClass("java/util/ArrayList");
    constructor = env->GetMethodID(clazz, "<init>", "()V");
    ctx.array_list_clazz = (jclass) env->NewGlobalRef(clazz);
    ctx.array_list_constructor = constructor;
    ctx.array_list_add_method = env->GetMethodID(clazz, "add", "(ILjava/lang/Object;)V");
    clazz = env->FindClass("ink/snowland/wkuwku/common/EmMessageExt");
    constructor = env->GetMethodID(clazz, "<init>", "(Ljava/lang/String;IIIIII)V");
    ctx.message_ext_clazz = (jclass) env->NewGlobalRef(clazz);
    ctx.message_ext_constructor = constructor;
    clazz = env->FindClass("ink/snowland/wkuwku/plug/genesisplusgx/GenesisPlusGX");
    ctx.video_refresh_method = env->GetMethodID(clazz, "onVideoRefresh", "([BIII)V");
    ctx.audio_sample_batch_method = env->GetMethodID(clazz, "onAudioSampleBatch", "([SI)V");
    ctx.environment_method = env->GetMethodID(clazz, "onEnvironment", "(ILjava/lang/Object;)Z");
    ctx.input_state_method = env->GetMethodID(clazz, "onInputState", "(IIII)I");
    ctx.input_poll_method = env->GetMethodID(clazz, "onInputPoll", "()V");
    ctx.audio_buffer_state_method = env->GetMethodID(clazz, "onAudioBufferState", "(ZIZ)V");
    ctx.rumble_method = env->GetMethodID(clazz, "onRumbleState", "(III)Z");
    return JNI_VERSION_1_6;
}

extern "C"
JNIEXPORT void JNICALL JNI_OnUnload(JavaVM *vm, void *reserved) {
    JNIEnv *env;
    if (vm->GetEnv((void **) &env, JNI_VERSION_1_6) != JNI_OK) {
        return;
    }
    env->DeleteGlobalRef(ctx.input_descriptor_clazz);
    env->DeleteGlobalRef(ctx.array_list_clazz);
    env->DeleteGlobalRef(ctx.message_ext_clazz);
    env->DeleteGlobalRef(variable_object);
    env->DeleteGlobalRef(variable_entry_object);
    if (audio_buffer != nullptr)
        env->DeleteGlobalRef(audio_buffer);
    if (frame_buffer != nullptr)
        env->DeleteGlobalRef(frame_buffer);
    ctx.jvm = nullptr;
}
extern "C"
JNIEXPORT jbyteArray JNICALL
Java_ink_snowland_wkuwku_plug_genesisplusgx_GenesisPlusGX_nativeGetState(JNIEnv *env,
                                                                         jobject thiz) {
    size_t len = retro_serialize_size();
    if (len == 0) return nullptr;
    int8_t data[len];
    if (!retro_serialize((void *) data, len)) {
        return nullptr;
    }
    jbyteArray snapshot = env->NewByteArray((jint) len);
    env->SetByteArrayRegion(snapshot, 0, (jint) len, data);
    return snapshot;
}
extern "C"
JNIEXPORT jboolean JNICALL
Java_ink_snowland_wkuwku_plug_genesisplusgx_GenesisPlusGX_nativeLoadStateData(JNIEnv *env,
                                                                              jobject thiz,
                                                                              jbyteArray jdata) {
    const size_t &len = retro_serialize_size();
    if (env->GetArrayLength(jdata) != len)
        return false;
    jbyte *snapshot = env->GetByteArrayElements(jdata, JNI_FALSE);
    bool no_error = retro_unserialize((void *) snapshot, len);
    env->ReleaseByteArrayElements(jdata, snapshot, 0);
    return no_error;
}