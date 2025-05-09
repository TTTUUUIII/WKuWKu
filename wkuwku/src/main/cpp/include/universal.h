//
// Created by deliu on 2025/5/9.
//

#ifndef WKUWKU_UNIVERSAL_H
#define WKUWKU_UNIVERSAL_H
#include <jni.h>
#include <libretro/libretro.h>
#include <fstream>

#ifdef __cplusplus
extern "C" {
#endif
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


#endif //WKUWKU_UNIVERSAL_H
