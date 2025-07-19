//
// Created by deliu on 2025/7/14.
//

#ifndef WKUWKU_INK_SNOWLAND_WKUWKU_EMULATORV2_H
#define WKUWKU_INK_SNOWLAND_WKUWKU_EMULATORV2_H
#include <iostream>
#include <utility>
#include <vector>
#include <libretro/libretro.h>
#include <EGL/egl.h>
#include <jni.h>
#include <string>
#include <future>
#include <any>
#include "Log.h"

#define UNUSED(_p0)  (void)(_p0)
#define ARRAY_SIZE(arr) (sizeof(arr) / sizeof(arr[0]))

#define NO_ERROR                            0
#define ERROR                               1
#define STATE_INVALID                       0
#define STATE_IDLE                          1
#define STATE_RUNNING                       2
#define STATE_PAUSED                        3

#define MSG_SET_SERIALIZE_DATA              1
#define MSG_GET_SERIALIZE_DATA              2
#define MSG_RESET_EMULATOR                  3

#define THREAD_PRIORITY_AUDIO               (-16)

#define PROP_NATIVE_AUDIO_ENABLED           102
#define PROP_LOW_LATENCY_AUDIO_ENABLE       103

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
    jmethodID audio_buffer_method;
    jmethodID video_size_cb_method;
    jmethodID input_cb_method;
    jfieldID variable_value_field;
    jfieldID variable_entry_key_field;
    jfieldID variable_entry_value_field;
} em_context_t;

struct buffer_t {
    size_t size;
    void* data;
    explicit buffer_t(size_t _s): size(_s) {
        if (size > 0) {
            data = malloc(size);
        }
    }
    virtual ~buffer_t() {
        free(data);
    }
};

typedef struct {
    unsigned width;
    unsigned height;
    unsigned rotation;
    retro_pixel_format pixel_format;
} video_state_t;

struct result_t {
    int state;
    std::any data;
};

struct message_t {
    int what;
    std::shared_ptr<std::promise<result_t>> promise;
    std::any usr;
    explicit message_t(int _what, std::shared_ptr<std::promise<result_t>> _promise, std::any _usr): what(_what), promise(std::move(_promise)), usr(std::move(_usr)) {}
};

static void on_create(EGLDisplay, EGLSurface);
static void on_draw();
static void on_destroy();
static void alloc_framebuffer(unsigned width, unsigned height);
static void fill_framebuffer(const void *, unsigned, unsigned, size_t);
static void notify_video_size_changed();
static bool attach_env(JNIEnv**);
static void detach_env();
static retro_proc_address_t get_hw_proc_address(const char* sym);
static uintptr_t get_hw_framebuffer();
static void handle_message();
static void open_audio_stream();
static void close_audio_stream();
static std::shared_ptr<std::promise<result_t>> send_message(int what, const std::any& usr);
static void send_empty_message(int what);
static std::shared_ptr<message_t> obtain_message();
static void clear_message();
static void set_thread_priority(int);
template <typename T>
static T get_prop(int32_t prop, const T &default_value);

/*JNI utils*/
static jint as_int(JNIEnv *, jobject);
static bool as_bool(JNIEnv *, jobject);
#endif //WKUWKU_INK_SNOWLAND_WKUWKU_EMULATORV2_H
