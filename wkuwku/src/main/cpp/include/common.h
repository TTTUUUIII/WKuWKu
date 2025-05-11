//
// Created by deliu on 2025/5/9.
//

#ifndef WKUWKU_COMMON_H
#define WKUWKU_COMMON_H
#include <jni.h>
#include <libretro/libretro.h>
#include <fstream>
#include "log.h"
#define ARRAY_SIZE(arr) sizeof(arr) / sizeof(arr[0])
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
} em_context_t;
#ifdef __cplusplus
extern "C" {
#endif
extern em_context_t ctx;
extern jobject variable_object;
extern jobject variable_entry_object;
void em_power_on(JNIEnv *env, jobject thiz);
void em_power_off(JNIEnv *env, jobject thiz);
jboolean em_load_game(JNIEnv *env, jobject thiz, jstring jpath);
jobject em_get_system_av_info(JNIEnv *env, jobject thiz);
jobject em_get_system_info(JNIEnv *env, jobject thiz);
jboolean em_save_memory_ram(JNIEnv *env, jobject thiz, jstring path);
jboolean em_load_memory_ram(JNIEnv *env, jobject thiz, jstring path);
jboolean em_save_state(JNIEnv *env, jobject thiz, jstring path);
jboolean em_load_state(JNIEnv *env, jobject thiz, jstring path);
void em_run(JNIEnv *env, jobject thiz);
void em_reset(JNIEnv *env, jobject thiz);

#ifdef __cplusplus
}
#endif


#endif //WKUWKU_COMMON_H
