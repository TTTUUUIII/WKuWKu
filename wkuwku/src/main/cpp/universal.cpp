//
// Created by deliu on 2025/5/9.
//

#include "universal.h"

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