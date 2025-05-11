//
// Created by deliu on 2025/5/9.
//

#include "common.h"

#ifdef defined(FCEUMM)
#define TAG "Fceumm Native"
#elif defined(GENESIS_PLUS_GX)
#define TAG "Genesis Plus GX Native"
#elif defined(BSNES)
#define TAG "Bsnes Native"
#elif defined(MESEN)
#define TAG "Mesen Native"
#else
#define TAG "LibRetro Native"
#endif

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
            LOGD(TAG, "%s", buffer);
    }
}

static bool set_eject_state_t(bool ejected) {
    return false;
}

static bool get_eject_state_t() {
    return false;
}

static unsigned get_image_index_t() {
    return 1;
}

static bool set_image_index_t(unsigned index) {
    return false;
}

static unsigned get_num_images_t() {
    return 1;
}

static bool replace_image_index_t(unsigned index, const struct retro_game_info *info) {
    return false;
}

static bool add_image_index_t() {
    return false;
}

static bool environment_callback(unsigned cmd, void *data) {
    JNIEnv *env;
    if (ctx.jvm->GetEnv((void **) &env, JNI_VERSION_1_6) != JNI_OK) {
        LOGE(TAG, "ERROR: unable attach env thread!");
        return false;
    }
    switch (cmd) {
        case RETRO_ENVIRONMENT_GET_VARIABLE_UPDATE:
            return env->CallBooleanMethod(ctx.emulator_obj, ctx.environment_method, cmd, nullptr);
        case RETRO_ENVIRONMENT_SET_PERFORMANCE_LEVEL:
            break;
        case RETRO_ENVIRONMENT_GET_LOG_INTERFACE:
            struct retro_log_callback *log_cb;
            log_cb = (struct retro_log_callback *) data;
            log_cb->log = log_print_callback;
            break;
        case RETRO_ENVIRONMENT_GET_VARIABLE: {
            struct retro_variable *variable;
            variable = (struct retro_variable *) data;
            set_variable_entry(env, variable->key, nullptr);
            env->CallBooleanMethod(ctx.emulator_obj, ctx.environment_method, cmd,
                                   variable_entry_object);
            auto value = (jstring) get_variable_entry_value(env);
            variable->value = env->GetStringUTFChars(value, JNI_FALSE);
        }
            break;
        case RETRO_ENVIRONMENT_SET_VARIABLE: {
            struct retro_variable *variable = nullptr;
            if (data != nullptr) {
                variable = (struct retro_variable*) data;
                set_variable_entry(env, variable->key, env->NewStringUTF(variable->value));
            }
            return env->CallBooleanMethod(ctx.emulator_obj, ctx.environment_method, cmd, variable_entry_object);
        }
        case RETRO_ENVIRONMENT_GET_INPUT_BITMASKS:
            return env->CallBooleanMethod(ctx.emulator_obj, ctx.environment_method, cmd, nullptr);
        case RETRO_ENVIRONMENT_SET_SERIALIZATION_QUIRKS:
            break;
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
            env->CallBooleanMethod(ctx.emulator_obj, ctx.environment_method, cmd, variable_object);
            auto language = (jobject) get_variable_value(env);
            jclass integer_clazz = env->FindClass("java/lang/Integer");
            jmethodID int_value_method = env->GetMethodID(integer_clazz, "intValue", "()I");
            *(unsigned *) data = (unsigned int) env->CallIntMethod(language, int_value_method);
        }
            break;
        case RETRO_ENVIRONMENT_GET_GAME_INFO_EXT:
            return false;
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
            env->CallBooleanMethod(ctx.emulator_obj, ctx.environment_method, cmd, array_list);
        }
            break;
        case RETRO_ENVIRONMENT_GET_SYSTEM_DIRECTORY:
        case RETRO_ENVIRONMENT_GET_SAVE_DIRECTORY: {
            env->CallBooleanMethod(ctx.emulator_obj, ctx.environment_method, cmd, variable_object);
            auto path = (jstring) get_variable_value(env);
            *((const char **) data) = env->GetStringUTFChars(path, JNI_FALSE);
        }
            break;
        case RETRO_ENVIRONMENT_SET_CORE_OPTIONS_DISPLAY:
            break;
        default:
            return false;
    }
    return true;
}

static void
video_refresh_callback(const void *data, unsigned width, unsigned height, size_t pitch) {
    JNIEnv *env;
    if (ctx.jvm->GetEnv((void **) &env, JNI_VERSION_1_6) != JNI_OK) {
        LOGE(TAG, "ERROR: unable attach env thread!");
        return;
    }
    jbyteArray framebuffer = env->NewByteArray(height * pitch);
    env->SetByteArrayRegion(framebuffer, 0, height * pitch, (jbyte *) data);
    env->CallVoidMethod(ctx.emulator_obj, ctx.video_refresh_method, framebuffer, (jint) width,
                        (jint) height, (jint) pitch);
}

static size_t audio_sample_batch_callback(const int16_t *data, size_t frames) {
    JNIEnv *env;
    if (ctx.jvm->GetEnv((void **) &env, JNI_VERSION_1_6) != JNI_OK) {
        LOGE(TAG, "ERROR: unable attach env thread!");
        return frames;
    }
    jshortArray samples = env->NewShortArray(frames * 2);
    env->SetShortArrayRegion(samples, 0, frames * 2, data);
    env->CallVoidMethod(ctx.emulator_obj, ctx.audio_sample_batch_method, samples, frames);
    return frames;
}

static int16_t input_state_callback(unsigned port, unsigned device, unsigned index, unsigned id) {
    JNIEnv *env;
    if (ctx.jvm->GetEnv((void **) &env, JNI_VERSION_1_6) != JNI_OK) {
        LOGE(TAG, "ERROR: unable attach env thread!");
        return 0;
    }
    auto state = (int16_t) env->CallIntMethod(ctx.emulator_obj, ctx.input_state_method, port,
                                              device,
                                              index,
                                              id);
    return state;
}

static void input_poll_callback() {
    JNIEnv *env;
    if (ctx.jvm->GetEnv((void **) &env, JNI_VERSION_1_6) != JNI_OK) {
        LOGE(TAG, "ERROR: unable attach env thread!");
        return;
    }
    env->CallVoidMethod(ctx.emulator_obj, ctx.input_poll_method);
}

void em_power_on(JNIEnv *env, jobject thiz) {
    ctx.emulator_obj = env->NewGlobalRef(thiz);
    retro_set_environment(environment_callback);
    retro_set_video_refresh(video_refresh_callback);
    retro_set_audio_sample_batch(audio_sample_batch_callback);
    retro_set_input_state(input_state_callback);
    retro_set_input_poll(input_poll_callback);
    retro_init();
}

void em_power_off(JNIEnv *env, jobject thiz) {
    retro_deinit();
    env->DeleteGlobalRef(ctx.emulator_obj);
}

jboolean em_load_game(JNIEnv *env, jobject thiz, jstring jpath) {
    const char *path = env->GetStringUTFChars(jpath, JNI_FALSE);
    struct retro_game_info info = {path, nullptr, 0, nullptr};
    bool state = retro_load_game(&info);
    env->ReleaseStringUTFChars(jpath, path);
    return state;
}

jobject em_get_system_av_info(JNIEnv *env, jobject thiz) {
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

jobject em_get_system_info(JNIEnv *env, jobject thiz) {
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

jboolean em_save_memory_ram(JNIEnv *env, jobject thiz, jstring path) {
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

jboolean em_load_memory_ram(JNIEnv *env, jobject thiz, jstring path) {
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

jboolean em_save_state(JNIEnv *env, jobject thiz, jstring path) {
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

jboolean em_load_state(JNIEnv *env, jobject thiz, jstring path) {
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

void em_run(JNIEnv *env, jobject thiz) {
    retro_run();
}

void em_reset(JNIEnv *env, jobject thiz) {
    retro_reset();
}