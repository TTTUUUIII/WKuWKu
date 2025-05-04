#include <jni.h>
#include <libretro/libretro.h>
#include <string.h>
#include "log.h"

#define TAG "FceummEmulator_Native"
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
    jclass fceumm_clazz;
    jclass input_descriptor_clazz;
    jclass array_list_clazz;
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

static void set_variable_entry(JNIEnv *env, const char* key , jobject value) {
    env->SetObjectField(variable_entry_object, ctx.variable_entry_key_field, env->NewStringUTF(key));
    env->SetObjectField(variable_entry_object, ctx.variable_entry_value_field, value);
}

static void set_variable_entry(JNIEnv *env, const char* key , jint value) {
    env->SetObjectField(variable_entry_object, ctx.variable_entry_key_field, env->NewStringUTF(key));
    jclass clazz = env->FindClass("java/lang/Integer");
    jmethodID value_of_method = env->GetStaticMethodID(clazz, "valueOf", "(I)Ljava/lang/Integer;");
    jobject val = env->CallStaticObjectMethod(clazz, value_of_method, value);
    set_variable_entry(env, key, val);
}

//static void set_variable_entry(JNIEnv *env, const char* key , const char *value) {
//    set_variable_entry(env, key, env->NewStringUTF(value));
//}

static jobject get_variable_entry_value(JNIEnv *env) {
    return env->GetObjectField(variable_entry_object, ctx.variable_entry_value_field);
}

static void log_print_callback(enum retro_log_level level, const char *fmt, ...)
{
    va_list args;
    va_start(args, fmt);
    switch (level) {
        case RETRO_LOG_ERROR:
            LOGE(TAG, fmt, args);
            break;
        case RETRO_LOG_INFO:
            LOGI(TAG, fmt, args);
            break;
        case RETRO_LOG_WARN:
            LOGW(TAG, fmt, args);
            break;
        default:
            LOGD(TAG, fmt, args);
    }
    va_end(args);
}

static bool environment_callback(unsigned cmd, void *data) {
    JNIEnv *env;
    if (ctx.jvm->GetEnv((void**) &env, JNI_VERSION_1_6) != JNI_OK) {
        LOGE(TAG, "ERROR: unable attach env thread!");
        return false;
    }
    switch (cmd)
    {
        case RETRO_ENVIRONMENT_GET_VARIABLE_UPDATE:
            *(bool*)data = env->CallStaticBooleanMethod(ctx.fceumm_clazz, ctx.environment_method, cmd, nullptr);
            break;
        case RETRO_ENVIRONMENT_SET_MEMORY_MAPS:
//            LOGD(TAG, "RETRO_ENVIRONMENT_SET_MEMORY_MAPS");
            break;
        case RETRO_ENVIRONMENT_SET_VARIABLE:
            return env->CallStaticBooleanMethod(ctx.fceumm_clazz, ctx.environment_method, cmd, nullptr);
        case RETRO_ENVIRONMENT_SET_VARIABLES:
            return env->CallStaticBooleanMethod(ctx.fceumm_clazz, ctx.environment_method, cmd, nullptr);
        case RETRO_ENVIRONMENT_GET_VARIABLE: {
            struct retro_variable *variable;
            variable = (struct retro_variable*)data;
            set_variable_entry(env, variable->key, nullptr);
            env->CallStaticBooleanMethod(ctx.fceumm_clazz, ctx.environment_method, cmd, variable_entry_object);
            jstring value = (jstring) get_variable_entry_value(env);
            variable->value = env->GetStringUTFChars(value, JNI_FALSE);
            }
            break;
        case RETRO_ENVIRONMENT_GET_LOG_INTERFACE:
            LOGD(TAG, "RETRO_ENVIRONMENT_GET_LOG_INTERFACE");
            struct retro_log_callback *log_cb;
            log_cb = (struct retro_log_callback*) data;
            log_cb->log = log_print_callback;
            break;
        case RETRO_ENVIRONMENT_GET_INPUT_BITMASKS:
            return env->CallStaticBooleanMethod(ctx.fceumm_clazz, ctx.environment_method, cmd, nullptr);
        case RETRO_ENVIRONMENT_SET_PIXEL_FORMAT:
            set_variable_value(env, *((jint *)data));
            return env->CallStaticBooleanMethod(ctx.fceumm_clazz, ctx.environment_method, cmd, variable_object);
        case RETRO_ENVIRONMENT_GET_SYSTEM_DIRECTORY: {
            set_variable_value(env, "sss");
            env->CallStaticBooleanMethod(ctx.fceumm_clazz, ctx.environment_method, cmd, variable_object);
            jstring path = (jstring) get_variable_value(env);
            *((const char**)data) = env->GetStringUTFChars(path, JNI_FALSE);
            }
            break;
        case RETRO_ENVIRONMENT_SET_INPUT_DESCRIPTORS: {
            jint index = 0;
            struct retro_input_descriptor* desc;
            desc = (struct retro_input_descriptor*) data;
            jobject array_list = env->NewObject(ctx.array_list_clazz, ctx.array_list_constructor);
            while (desc->description != nullptr) {
                LOGD(TAG, "%s", desc->description);
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
            env->CallStaticBooleanMethod(ctx.fceumm_clazz, ctx.environment_method,cmd, array_list);
            }
            break;
        case RETRO_ENVIRONMENT_SET_MESSAGE_EXT:
        case RETRO_ENVIRONMENT_SET_HW_RENDER | RETRO_ENVIRONMENT_EXPERIMENTAL:
//            LOGD(TAG, "RETRO_ENVIRONMENT_SET_MESSAGE_EXT");
            break;
        case RETRO_ENVIRONMENT_SET_KEYBOARD_CALLBACK:
//            LOGD(TAG, "RETRO_ENVIRONMENT_SET_KEYBOARD_CALLBACK");
            break;
        case RETRO_ENVIRONMENT_SET_HW_RENDER:
//            LOGD(TAG, "RETRO_ENVIRONMENT_SET_HW_RENDER");
            break;
        case RETRO_ENVIRONMENT_GET_HW_RENDER_INTERFACE:
//            LOGD(TAG, "RETRO_ENVIRONMENT_GET_HW_RENDER_INTERFACE");
            break;
        case RETRO_ENVIRONMENT_SET_AUDIO_CALLBACK:
//            LOGD(TAG, "RETRO_ENVIRONMENT_SET_AUDIO_CALLBACK");
            break;
        case RETRO_ENVIRONMENT_SET_PERFORMANCE_LEVEL:
            break;
        case RETRO_ENVIRONMENT_SET_CORE_OPTIONS_DISPLAY:
            break;
        case RETRO_ENVIRONMENT_SET_FRAME_TIME_CALLBACK:
//            LOGD(TAG, "RETRO_ENVIRONMENT_SET_FRAME_TIME_CALLBACK");
            break;
        case RETRO_ENVIRONMENT_GET_LANGUAGE: {
            set_variable_value(env, RETRO_LANGUAGE_DUMMY);
            env->CallStaticBooleanMethod(ctx.fceumm_clazz, ctx.environment_method, cmd, variable_object);
            jobject language = (jobject) get_variable_value(env);
            jclass integer_clazz = env->FindClass("java/lang/Integer");
            jmethodID int_value_method = env->GetMethodID(integer_clazz, "intValue", "()I");
            *(unsigned*) data = (unsigned int) env->CallIntMethod(language, int_value_method);
        }
            break;
        case RETRO_ENVIRONMENT_SET_AUDIO_BUFFER_STATUS_CALLBACK:
//            LOGD(TAG, "RETRO_ENVIRONMENT_SET_AUDIO_BUFFER_STATUS_CALLBACK");
            break;
        default:
//            LOGW(TAG, "WARN: cmd %d ignored!", cmd);
            return false;
    }
    return true;
}

static void video_refresh_callback(const void *data, unsigned width, unsigned height, size_t pitch)
{
    JNIEnv *env;
    if (ctx.jvm->GetEnv((void**) &env, JNI_VERSION_1_6) != JNI_OK) {
        LOGE(TAG, "ERROR: unable attach env thread!");
        return;
    }
    jbyteArray framebuffer = env->NewByteArray(height * pitch);
    env->SetByteArrayRegion(framebuffer, 0, height * pitch, (jbyte*) data);
    env->CallStaticVoidMethod(ctx.fceumm_clazz, ctx.video_refresh_method, framebuffer, (jint) width, (jint) height, (jint) pitch);
}

static size_t audio_sample_batch_callback(const int16_t *data, size_t frames)
{
    JNIEnv *env;
    if (ctx.jvm->GetEnv((void**) &env, JNI_VERSION_1_6) != JNI_OK) {
        LOGE(TAG, "ERROR: unable attach env thread!");
        return frames;
    }
    jshortArray samples = env->NewShortArray(frames * 2);
    env->SetShortArrayRegion(samples, 0, frames * 2, data);
    env->CallStaticVoidMethod(ctx.fceumm_clazz, ctx.audio_sample_batch_method, samples, frames);
    return frames;
}

static int16_t input_state_callback(unsigned port, unsigned device, unsigned index, unsigned id)
{
    JNIEnv *env;
    if (ctx.jvm->GetEnv((void**) &env, JNI_VERSION_1_6) != JNI_OK) {
        LOGE(TAG, "ERROR: unable attach env thread!");
        return 0;
    }
    jint state = env->CallStaticIntMethod(ctx.fceumm_clazz, ctx.input_state_method, port, device, index,
                                          id);
    return state;
}

static void input_poll_callback()
{
    JNIEnv *env;
    if (ctx.jvm->GetEnv((void**) &env, JNI_VERSION_1_6) != JNI_OK) {
        LOGE(TAG, "ERROR: unable attach env thread!");
        return;
    }
    env->CallStaticVoidMethod(ctx.fceumm_clazz, ctx.input_poll_method);
}

extern "C"
JNIEXPORT void JNICALL
Java_ink_snowland_wkuwku_emulator_Fceumm_nativeReset(JNIEnv *env, jclass clazz) {
    retro_reset();
}

extern "C"
JNIEXPORT void JNICALL
Java_ink_snowland_wkuwku_emulator_Fceumm_nativePowerOn(JNIEnv *env, jclass clazz) {
    retro_set_environment(environment_callback);
    retro_init();
    retro_set_video_refresh(video_refresh_callback);
    retro_set_audio_sample_batch(audio_sample_batch_callback);
    retro_set_input_state(input_state_callback);
    retro_set_input_poll(input_poll_callback);
}

extern "C"
JNIEXPORT void JNICALL
Java_ink_snowland_wkuwku_emulator_Fceumm_nativePowerOff(JNIEnv *env, jclass clazz) {
    retro_deinit();
}

extern "C"
JNIEXPORT jboolean JNICALL
Java_ink_snowland_wkuwku_emulator_Fceumm_nativeLoad(JNIEnv *env, jclass clazz, jstring jpath) {
    const char *path = env->GetStringUTFChars(jpath, JNI_FALSE);
    struct retro_game_info info = { path, nullptr, 0, nullptr };
    bool state = retro_load_game(&info);
    env->ReleaseStringUTFChars(jpath, path);
    struct retro_system_av_info avInfo;
    retro_get_system_av_info(&avInfo);
    return state;
}

extern "C"
JNIEXPORT void JNICALL
Java_ink_snowland_wkuwku_emulator_Fceumm_nativeRun(JNIEnv *env, jclass clazz) {
    retro_run();
}

extern "C"
JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM *vm, void* reserved) {
    JNIEnv *env;
    if (vm->GetEnv((void**) &env, JNI_VERSION_1_6) != JNI_OK) {
        LOGE(TAG, "ERROR: jni load failed!");
        return JNI_ERR;
    }
    ctx.jvm = vm;
    jclass clazz = env->FindClass("ink/snowland/wkuwku/emulator/Fceumm");
    ctx.fceumm_clazz = (jclass) env->NewGlobalRef(clazz);
    ctx.video_refresh_method = env->GetStaticMethodID(clazz, "onVideoRefresh", "([BIII)V");
    ctx.audio_sample_batch_method = env->GetStaticMethodID(clazz, "onAudioSampleBatch", "([SI)V");
    ctx.environment_method = env->GetStaticMethodID(clazz, "onEnvironment", "(ILjava/lang/Object;)Z");
    ctx.input_state_method = env->GetStaticMethodID(clazz, "onInputState", "(IIII)I");
    ctx.input_poll_method = env->GetStaticMethodID(clazz, "onInputPoll", "()V");
    clazz = env->FindClass("ink/snowland/wkuwku/common/Variable");
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
    return JNI_VERSION_1_6;
}

extern "C"
JNIEXPORT void JNICALL JNI_OnUnload(JavaVM* vm, void* reserved) {
    JNIEnv *env;
    if (vm->GetEnv((void**) &env, JNI_VERSION_1_6) != JNI_OK) {
        return;
    }
    env->DeleteGlobalRef(ctx.fceumm_clazz);
    env->DeleteGlobalRef(ctx.array_list_clazz);
    env->DeleteGlobalRef(ctx.input_descriptor_clazz);
    env->DeleteGlobalRef(variable_object);
    env->DeleteGlobalRef(variable_entry_object);
    ctx.jvm = nullptr;
}