#include <jni.h>
#include <libretro/libretro.h>
#include <string.h>
#include "log.h"

#define TAG "NESEmulator_Native"

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
    jclass clazz;
    jmethodID environment_method;
    jmethodID video_refresh_method;
    jmethodID audio_sample_batch_method;
    jmethodID input_state_method;
    jmethodID input_poll_method;
} JAVA_INTERFACE;

static JAVA_INTERFACE ctx = {nullptr};

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
        case RETRO_ENVIRONMENT_GET_LOG_INTERFACE:
            LOGD(TAG, "RETRO_ENVIRONMENT_GET_LOG_INTERFACE");
            struct retro_log_callback *log_cb;
            log_cb = (struct retro_log_callback*) data;
            log_cb->log = log_print_callback;
            break;
        case RETRO_ENVIRONMENT_SET_PIXEL_FORMAT: {
            jclass clazz = env->FindClass("ink/snowland/wkuwku/common/Variable");
            jmethodID constructor = env->GetMethodID(clazz, "<init>", "(Ljava/lang/Object;)V");
            jclass integer_clazz = env->FindClass("java/lang/Integer");
            jmethodID valu_of_method_id = env->GetStaticMethodID(integer_clazz, "valueOf",
                                                                 "(I)Ljava/lang/Integer;");
            jobject jobject = env->NewObject(clazz, constructor, env->CallStaticObjectMethod(integer_clazz, valu_of_method_id, *((jint *)data)));
            return env->CallStaticBooleanMethod(ctx.clazz, ctx.environment_method, cmd, jobject);
        }
        case RETRO_ENVIRONMENT_GET_SYSTEM_DIRECTORY: {
            jclass clazz = env->FindClass("ink/snowland/wkuwku/common/Variable");
            jmethodID constructor = env->GetMethodID(clazz, "<init>", "(Ljava/lang/Object;)V");
            jobject jobject = env->NewObject(clazz, constructor, env->NewStringUTF(""));
            jfieldID value_field = env->GetFieldID(clazz, "value", "Ljava/lang/Object;");
            env->CallStaticBooleanMethod(ctx.clazz, ctx.environment_method, cmd, jobject);
            jstring value = (jstring) env->GetObjectField(jobject, value_field);
            *((const char**)data) = env->GetStringUTFChars(value, JNI_FALSE);
        }
            break;
        case RETRO_ENVIRONMENT_SET_INPUT_DESCRIPTORS:
            LOGD(TAG, "RETRO_ENVIRONMENT_SET_INPUT_DESCRIPTORS");
            break;
        case RETRO_ENVIRONMENT_SET_MEMORY_MAPS:
            LOGD(TAG, "RETRO_ENVIRONMENT_SET_MEMORY_MAPS");
            break;
        case RETRO_ENVIRONMENT_SET_VARIABLE:
            LOGD(TAG, "RETRO_ENVIRONMENT_SET_VARIABLE");
            break;
        case RETRO_ENVIRONMENT_SET_VARIABLES:
            LOGD(TAG, "RETRO_ENVIRONMENT_SET_VARIABLES");
            break;
        case RETRO_ENVIRONMENT_GET_VARIABLE: {
            struct retro_variable *variable;
            variable = (struct retro_variable*)data;
            jclass clazz = env->FindClass("ink/snowland/wkuwku/common/VariableEntry");
            jmethodID constructor = env->GetMethodID(clazz, "<init>", "(Ljava/lang/String;)V");
            jobject jobject = env->NewObject(clazz, constructor, env->NewStringUTF(variable->key));
            jfieldID value_field = env->GetFieldID(clazz, "value", "Ljava/lang/Object;");
            env->CallStaticBooleanMethod(ctx.clazz, ctx.environment_method, cmd, jobject);
            jstring value = (jstring) env->GetObjectField(jobject, value_field);
            variable->value = env->GetStringUTFChars(value, JNI_FALSE);
            break;
        }
        case RETRO_ENVIRONMENT_GET_VARIABLE_UPDATE:
            *(bool*)data = env->CallStaticBooleanMethod(ctx.clazz, ctx.environment_method, cmd, nullptr);
            break;
        case RETRO_ENVIRONMENT_SET_MESSAGE_EXT:
        case RETRO_ENVIRONMENT_SET_HW_RENDER | RETRO_ENVIRONMENT_EXPERIMENTAL:
            LOGD(TAG, "RETRO_ENVIRONMENT_SET_MESSAGE_EXT");
            break;
        case RETRO_ENVIRONMENT_SET_KEYBOARD_CALLBACK:
            LOGD(TAG, "RETRO_ENVIRONMENT_SET_KEYBOARD_CALLBACK");
            break;
        case RETRO_ENVIRONMENT_SET_HW_RENDER:
            LOGD(TAG, "RETRO_ENVIRONMENT_SET_HW_RENDER");
            break;
        case RETRO_ENVIRONMENT_GET_HW_RENDER_INTERFACE:
            LOGD(TAG, "RETRO_ENVIRONMENT_GET_HW_RENDER_INTERFACE");
            break;
        case RETRO_ENVIRONMENT_SET_AUDIO_CALLBACK:
            LOGD(TAG, "RETRO_ENVIRONMENT_SET_AUDIO_CALLBACK");
            break;
        case RETRO_ENVIRONMENT_SET_FRAME_TIME_CALLBACK:
            LOGD(TAG, "RETRO_ENVIRONMENT_SET_FRAME_TIME_CALLBACK");
            break;
        case RETRO_ENVIRONMENT_SET_AUDIO_BUFFER_STATUS_CALLBACK:
            LOGD(TAG, "RETRO_ENVIRONMENT_SET_AUDIO_BUFFER_STATUS_CALLBACK");
            break;
        default:
            LOGW(TAG, "WARN: cmd %d ignored!", cmd);
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
    env->CallStaticVoidMethod(ctx.clazz, ctx.video_refresh_method, framebuffer, (jint) width, (jint) height, (jint) pitch);
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
    env->CallStaticVoidMethod(ctx.clazz, ctx.audio_sample_batch_method, samples, frames);
    return frames;
}

static int16_t input_state_callback(unsigned port, unsigned device, unsigned index, unsigned id)
{
    JNIEnv *env;
    if (ctx.jvm->GetEnv((void**) &env, JNI_VERSION_1_6) != JNI_OK) {
        LOGE(TAG, "ERROR: unable attach env thread!");
        return 0;
    }
    jint state = env->CallStaticIntMethod(ctx.clazz, ctx.input_state_method, port, device, index,
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
    env->CallStaticVoidMethod(ctx.clazz, ctx.input_poll_method);
}

extern "C"
JNIEXPORT void JNICALL
Java_ink_snowland_wkuwku_NESEmulator_nativeReset(JNIEnv *env, jclass clazz) {
    retro_reset();
}

extern "C"
JNIEXPORT void JNICALL
Java_ink_snowland_wkuwku_NESEmulator_nativePowerOn(JNIEnv *env, jclass clazz) {
    retro_set_environment(environment_callback);
    retro_init();
    retro_set_video_refresh(video_refresh_callback);
    retro_set_audio_sample_batch(audio_sample_batch_callback);
    retro_set_input_state(input_state_callback);
    retro_set_input_poll(input_poll_callback);
}

extern "C"
JNIEXPORT void JNICALL
Java_ink_snowland_wkuwku_NESEmulator_nativePowerOff(JNIEnv *env, jclass clazz) {
    retro_deinit();
}

extern "C"
JNIEXPORT jboolean JNICALL
Java_ink_snowland_wkuwku_NESEmulator_nativeLoad(JNIEnv *env, jclass clazz, jstring jpath) {
    const char *path = env->GetStringUTFChars(jpath, JNI_FALSE);
    struct retro_game_info info = { path, nullptr, 0, nullptr };
    bool state = retro_load_game(&info);
    env->ReleaseStringUTFChars(jpath, path);
    return state;
}

extern "C"
JNIEXPORT void JNICALL
Java_ink_snowland_wkuwku_NESEmulator_nativeRun(JNIEnv *env, jclass clazz) {
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
    jclass clazz = env->FindClass("ink/snowland/wkuwku/NESEmulator");
    ctx.clazz = (jclass) env->NewGlobalRef(clazz);
    ctx.video_refresh_method = env->GetStaticMethodID(clazz, "onVideoRefresh", "([BIII)V");
    ctx.audio_sample_batch_method = env->GetStaticMethodID(clazz, "onAudioSampleBatch", "([SI)V");
    ctx.environment_method = env->GetStaticMethodID(clazz, "onEnvironment", "(ILjava/lang/Object;)Z");
    ctx.input_state_method = env->GetStaticMethodID(clazz, "onInputState", "(IIII)I");
    ctx.input_poll_method = env->GetStaticMethodID(clazz, "onInputPoll", "()V");
    return JNI_VERSION_1_6;
}

extern "C"
JNIEXPORT void JNICALL JNI_OnUnload(JavaVM* vm, void* reserved) {
    JNIEnv *env;
    if (vm->GetEnv((void**) &env, JNI_VERSION_1_6) != JNI_OK) {
        return;
    }
    env->DeleteGlobalRef(ctx.clazz);
    ctx.jvm = nullptr;
}