#include <jni.h>
#include <string>
#include "log.h"
#include "universal.h"

#define TAG "Genesis Plus GX"
#define ARRAY_SIZE(arr) sizeof(arr) / sizeof(arr[0])

// Write C++ code here.
//
// Do not forget to dynamically load the C++ library into your application.
//
// For instance,
//
// In MainActivity.java:
//    static {
//       System.loadLibrary("wkuwku");
//    }
//
// Or, in MainActivity.kt:
//    companion object {
//      init {
//         System.loadLibrary("wkuwku")
//      }
//    }
typedef struct {
    JavaVM *jvm;
    jclass input_descriptor_clazz;
    jclass array_list_clazz;
    jobject emulator_obj;
    jmethodID input_descriptor_constructor;
    jmethodID array_list_constructor;
    jmethodID array_list_add_method;
    jmethodID environment_method;
    jmethodID video_refresh_method;
    jmethodID audio_sample_batch_method;
    jmethodID input_state_method;
    jmethodID input_poll_method;
    jfieldID variable_value_field;
    jfieldID variable_entry_key_field;
    jfieldID variable_entry_value_field;
} JAVA_INTERFACE;

static JAVA_INTERFACE ctx = {nullptr};
static jobject variable_object;
static jobject variable_entry_object;

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
        case RETRO_ENVIRONMENT_SET_PERFORMANCE_LEVEL:
            break;
        case RETRO_ENVIRONMENT_GET_LOG_INTERFACE:
            struct retro_log_callback *log_cb;
            log_cb = (struct retro_log_callback *) data;
            log_cb->log = log_print_callback;
            break;
        case RETRO_ENVIRONMENT_GET_INPUT_BITMASKS:
            break;
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
        case RETRO_ENVIRONMENT_GET_GAME_INFO_EXT:
            return false;
        case RETRO_ENVIRONMENT_SET_PIXEL_FORMAT:
//            set_variable_value(env, *((jint *) data));
            return true;
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
    auto state = (int16_t) env->CallIntMethod(ctx.emulator_obj, ctx.input_state_method, port, device,
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

static const JNINativeMethod methods[] = {
        {"nativePowerOn", "()V", (void *) em_power_on},
        {"nativePowerOff", "()V", (void *) em_power_off},
        {"nativeReset", "()V", (void*) em_reset},
        {"nativeLoad", "(Ljava/lang/String;)Z", (void*) em_load_game},
        {"nativeRun", "()V", (void*) em_run},
        {"nativeSaveState", "(Ljava/lang/String;)Z", (void*) em_save_state},
        {"nativeLoadState", "(Ljava/lang/String;)Z", (void*) em_load_state},
        {"nativeSaveMemoryRam", "(Ljava/lang/String;)Z", (void*) em_save_memory_ram},
        {"nativeLoadMemoryRam", "(Ljava/lang/String;)Z", (void*) em_load_memory_ram},
        {"nativeGetSystemInfo", "()Link/snowland/wkuwku/common/EmSystemInfo;", (void*) em_get_system_info},
        {"nativeGetSystemAvInfo", "()Link/snowland/wkuwku/common/EmSystemAvInfo;", (void*) em_get_system_av_info},
};

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

    clazz = env->FindClass("ink/snowland/wkuwku/emulator/GenesisPlusGX");
    ctx.video_refresh_method = env->GetMethodID(clazz, "onVideoRefresh", "([BIII)V");
    ctx.audio_sample_batch_method = env->GetMethodID(clazz, "onAudioSampleBatch", "([SI)V");
    ctx.environment_method = env->GetMethodID(clazz, "onEnvironment", "(ILjava/lang/Object;)Z");
    ctx.input_state_method = env->GetMethodID(clazz, "onInputState", "(IIII)I");
    ctx.input_poll_method = env->GetMethodID(clazz, "onInputPoll", "()V");
    env->RegisterNatives(clazz, methods, ARRAY_SIZE(methods));
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
    env->DeleteGlobalRef(variable_object);
    env->DeleteGlobalRef(variable_entry_object);
    ctx.jvm = nullptr;
}