//
// Created by wn123 on 2025-06-26.
//

#ifndef WKUWKU_COM_OUTLOOK_WN123O_RETROSYSTEM_H
#define WKUWKU_COM_OUTLOOK_WN123O_RETROSYSTEM_H
#include <jni.h>
#include <string>
#define VERSION                         2
#define FLAG_ENABLE_VIDEO               1
#define FLAG_ENABLE_AUDIO               2
#define FLAG_ENABLE_FAST_SAVESTATES     4
#define FLAG_ENABLE_HARD_DISABLE_AUDIO  8
#define SYSTEM_DIRECTORY                1
#define SAVE_DIRECTORY                  2
#define CORE_ASSETS_DIRECTORY           3

template<typename T>
struct Buffer {
    size_t size;
    T data;
    Buffer(T _d, size_t _s): data(_d), size(_s) {}
    ~Buffer() {
        std::free(data);
    }
};

typedef struct {
    JavaVM *jvm;
    jclass clazz;
    jmethodID audio_cb;
    jmethodID video_size_cb;
    jmethodID input_cb;
    jmethodID input_poll_cb;
    jmethodID environment_cb;
    jmethodID rumble_cb;
    jobject option_obj;
    jobject value_obj;
} RetroSystem;

static void on_create(EGLDisplay dyp, EGLSurface sr);
static void on_draw();
static void on_destroy();
static bool environment_cb(unsigned, void*);
static void video_cb(const void *, unsigned, unsigned, size_t);
static size_t audio_cb(const int16_t *, size_t);
static int16_t input_cb(unsigned, unsigned, unsigned, unsigned);
static void input_pool_cb();
static void log_cb(enum retro_log_level level, const char *fmt, ...);
static bool rumble_cb(unsigned, enum retro_rumble_effect, uint16_t);
static uintptr_t get_current_framebuffer();
static retro_proc_address_t get_proc_address(const char*);
static jobject new_option(JNIEnv *, const char*);
static jobject new_option(JNIEnv *, const char*, const char*);
static std::string get_option_value(JNIEnv *, jobject);
static void notify_video_size_changed();
static void fill_framebuffer(const void *, unsigned, unsigned, size_t);
static jobject new_value(JNIEnv  *);
static std::string  get_string_value(JNIEnv *, jobject)
#endif //WKUWKU_COM_OUTLOOK_WN123O_RETROSYSTEM_H
