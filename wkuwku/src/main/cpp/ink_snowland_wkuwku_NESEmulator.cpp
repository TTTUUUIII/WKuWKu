#include <jni.h>
#include <libretro/libretro.h>
#include <string.h>
#include "log.h"

#define TAG "NESEmulator"

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

static JavaVM *jvm = nullptr;

static void retro_log_print(enum retro_log_level level, const char *fmt, ...)
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
            break;
    }
    va_end(args);
}

static bool environment_callback(unsigned cmd, void *data) {
//    LOGD(TAG, "environment_callback: %d", cmd);
    switch (cmd)
    {
        case RETRO_ENVIRONMENT_GET_LOG_INTERFACE:
            LOGD(TAG, "RETRO_ENVIRONMENT_GET_LOG_INTERFACE");
            struct retro_log_callback *log_cb;
            log_cb = (struct retro_log_callback*) data;
            log_cb->log = retro_log_print;
            break;
        case RETRO_ENVIRONMENT_GET_GAME_INFO_EXT:
            LOGD(TAG, "RETRO_ENVIRONMENT_GET_GAME_INFO_EXT");
            return false;
        case RETRO_ENVIRONMENT_SET_SUPPORT_ACHIEVEMENTS:
            return false;
        case RETRO_ENVIRONMENT_SET_PIXEL_FORMAT:
            break;
        case RETRO_ENVIRONMENT_GET_SYSTEM_DIRECTORY:
            *((const char**)data) = "/data/data/ink.snowland.wkuwku/files/";
            LOGD(TAG, "RETRO_ENVIRONMENT_GET_SYSTEM_DIRECTORY\n");
            break;
        case RETRO_ENVIRONMENT_SET_INPUT_DESCRIPTORS:
//            printf("RETRO_ENVIRONMENT_SET_INPUT_DESCRIPTORS\n");
            break;
        case RETRO_ENVIRONMENT_SET_MEMORY_MAPS:
//            printf("RETRO_ENVIRONMENT_SET_MEMORY_MAPS\n");
            break;
        case RETRO_ENVIRONMENT_GET_VARIABLE:
            struct retro_variable *var;
            var = (struct retro_variable*)data;
            if (!strcmp(var->key, "fceumm_ramstate"))
                var->value = "random";
            else if (!strcmp(var->key, "fceumm_ntsc_filter"))
                var->value = "disabled";
            else if(!strcmp(var->key, "fceumm_palette"))
                var->value = "default";
            else if(!strcmp(var->key, "fceumm_up_down_allowed"))
                var->value = "enabled";
            else if(!strcmp(var->key, "fceumm_nospritelimit"))
                var->value = "enable";
            else if(!strcmp(var->key, "fceumm_overclocking"))
                var->value = "disabled";
            else if(!strcmp(var->key, "fceumm_zapper_mode"))
                var->value = "default";
            else if(!strcmp(var->key, "fceumm_arkanoid_mode"))
                var->value = "default";
            else if(!strcmp(var->key, "fceumm_zapper_tolerance"))
                var->value = "4";
            else if(!strcmp(var->key, "fceumm_mouse_sensitivity"))
                var->value = "100";
            else if(!strcmp(var->key, "fceumm_show_crosshair"))
                var->value = "disabled";
            else if(!strcmp(var->key, "fceumm_zapper_trigger"))
                var->value = "disabled";
            else if(!strcmp(var->key, "fceumm_zapper_sensor"))
                var->value = "disabled";
            else if(!strcmp(var->key, "fceumm_overscan"))
                var->value = "disabled";
            else if(!strncmp(var->key, "fceumm_overscan_", 16))
                var->value = "8";
            else if(!strcmp(var->key, "fceumm_aspect"))
                var->value = "8:7 PAR";
            else if(!strcmp(var->key , "fceumm_turbo_enable"))
                var->value = "Both";
            else if(!strcmp(var->key, "fceumm_turbo_delay"))
                var->value = "300";
            else if(!strcmp(var->key, "fceumm_region"))
                var->value = "Auto";
            else if(!strcmp(var->key, "fceumm_sndquality"))
                var->value = "Hign";
            else if(!strcmp(var->key, "fceumm_sndlowpass"))
                var->value = "enabled";
            else if(!strcmp(var->key, "fceumm_sndstereodelay"))
                var->value = "disabled";
            else if(!strcmp(var->key, "fceumm_sndvolume"))
                var->value = "0.5f";
            else if(!strcmp(var->key, "fceumm_swapduty"))
                var->value = "disabled";
            else if(!strncmp(var->key, "fceumm_apu_", 11))
                var->value = "disabled";
            else if(!strcmp(var->key, "fceumm_show_adv_system_options"))
                var->value = "disabled";
            break;
        case RETRO_ENVIRONMENT_GET_VARIABLE_UPDATE:
            *(bool*)data = false;
            break;
        case RETRO_ENVIRONMENT_SET_MESSAGE_EXT:
        case RETRO_ENVIRONMENT_SET_HW_RENDER | RETRO_ENVIRONMENT_EXPERIMENTAL:
//            printf("RETRO_ENVIRONMENT_SET_MESSAGE_EXT\n");
            break;
        case RETRO_ENVIRONMENT_SET_KEYBOARD_CALLBACK:
//            printf("RETRO_ENVIRONMENT_SET_KEYBOARD_CALLBACK\n");
            break;
        case RETRO_ENVIRONMENT_SET_HW_RENDER:
//            printf("RETRO_ENVIRONMENT_SET_HW_RENDER\n");
            break;
        case RETRO_ENVIRONMENT_GET_HW_RENDER_INTERFACE:
//            printf("RETRO_ENVIRONMENT_GET_HW_RENDER_INTERFACE\n");
            break;
        case RETRO_ENVIRONMENT_SET_SUPPORT_NO_GAME:
//            printf("RETRO_ENVIRONMENT_SET_SUPPORT_NO_GAME\n");
            break;
        case RETRO_ENVIRONMENT_GET_LIBRETRO_PATH:
//            printf("RETRO_ENVIRONMENT_GET_LIBRETRO_PATH\n");
            break;
        case RETRO_ENVIRONMENT_SET_AUDIO_CALLBACK:
//            printf("RETRO_ENVIRONMENT_SET_AUDIO_CALLBACK\n");
            break;
        case RETRO_ENVIRONMENT_SET_FRAME_TIME_CALLBACK:
//            printf("RETRO_ENVIRONMENT_SET_FRAME_TIME_CALLBACK\n");
            break;
        case RETRO_ENVIRONMENT_SET_AUDIO_BUFFER_STATUS_CALLBACK:
//            printf("RETRO_ENVIRONMENT_SET_AUDIO_BUFFER_STATUS_CALLBACK\n");
            break;
    }
    return true;
}

static void video_refresh_callback(const void *data, unsigned width, unsigned height, size_t pitch)
{
    JNIEnv *env;
    if (jvm->GetEnv((void**)&env, JNI_VERSION_1_6) != JNI_OK) {
        LOGE(TAG, "ERROR: unable get env!");
        return;
    }
    jbyteArray framebuffer = env->NewByteArray(height * pitch);
    env->SetByteArrayRegion(framebuffer, 0, height * pitch, (jbyte*) data);
    jclass clazz = env->FindClass("ink/snowland/wkuwku/NESEmulator");
    jmethodID methodId = env->GetStaticMethodID(clazz, "onVideoRefresh", "([BIII)V");
    env->CallStaticVoidMethod(clazz, methodId, framebuffer, (jint) width, (jint) height, (jint) pitch);
    LOGD(TAG, "view_refresh_callback:");
}

static size_t audio_sample_batch_callback(const int16_t *data, size_t frames)
{
    return frames;
}

static int16_t input_state_callback(unsigned port, unsigned device, unsigned index, unsigned id)
{
    return 0;
}

static void input_poll_callback()
{

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
Java_ink_snowland_wkuwku_NESEmulator_nativeLoadGame(JNIEnv *env, jclass clazz, jbyteArray jdata) {
    struct retro_game_info info = {
            .data = nullptr,
            .size = 0,
            .path = "/data/data/ink.snowland.wkuwku/files/Super Mario Bros.nes",
            .meta = nullptr
    };
    return retro_load_game(&info);
}

extern "C"
JNIEXPORT void JNICALL
Java_ink_snowland_wkuwku_NESEmulator_nativeNext(JNIEnv *env, jclass clazz) {
    retro_run();
}

extern "C"
JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM *vm, void* reserved) {
    JNIEnv *env;
    if (vm->GetEnv((void**) &env, JNI_VERSION_1_6) != JNI_OK) {
        LOGE(TAG, "ERROR: jni load failed!");
        return JNI_ERR;
    }
    jvm = vm;
    return JNI_VERSION_1_6;
}
extern "C"
JNIEXPORT jint JNICALL
Java_ink_snowland_wkuwku_NESEmulator_nativeGetVersion(JNIEnv *env, jclass clazz) {
    return (jint) retro_api_version();
}