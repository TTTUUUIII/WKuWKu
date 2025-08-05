//
// Created by deliu on 2025/7/14.
//

#ifndef WKUWKU_INK_SNOWLAND_WKUWKU_EMULATORV2_H
#define WKUWKU_INK_SNOWLAND_WKUWKU_EMULATORV2_H
#include <utility>
#include <vector>
#include <libretro.h>
#include <EGL/egl.h>
#include <jni.h>
#include <string>
#include <future>
#include <any>
#include "Buffer.h"
#include "Log.h"

#define UNUSED(_p0)  (void)(_p0)
#define ARRAY_SIZE(arr) (sizeof(arr) / sizeof(arr[0]))

#define NO_ERROR                            0
#define ERROR                               1
#define STATE_INVALID                       0
#define STATE_IDLE                          1
#define STATE_RUNNING                       2
#define STATE_PAUSED                        3

#define MSG_KILL                            (-1)
#define MSG_SET_SERIALIZE_DATA              1
#define MSG_GET_SERIALIZE_DATA              2
#define MSG_RESET_EMULATOR                  3
#define MSG_START_RENDERER                  4
#define MSG_READ_PIXELS                     5

#define THREAD_PRIORITY_DISPLAY             (-4)
#define THREAD_PRIORITY_AUDIO               (-16)

#define PROP_OBOE_ENABLED                        102
#define PROP_LOW_LATENCY_AUDIO_ENABLE            103
#define PROP_AUDIO_UNDERRUN_OPTIMIZATION         104

enum rotation_t {
    ROTATION_0,
    ROTATION_90,
    ROTATION_180,
    ROTATION_270
};

typedef struct {
    JavaVM *jvm;
    JNIEnv *env;
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
    jmethodID rumble_cb_method;
    jfieldID variable_value_field;
    jfieldID variable_entry_key_field;
    jfieldID variable_entry_value_field;
} em_context_t;

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

static std::unique_ptr<buffer_t> RGB565_TO_RGB888(void*, size_t /*len*/);
static void XRGB1555_TO_RGB565(void*, size_t /*len*/);
static void XRGB8888_PATCH(void*, size_t /*len*/);

static void on_surface_create(EGLDisplay, EGLSurface);
static void on_draw_frame();
static void on_surface_destroy();
static void alloc_frame_buffers();
static void free_frame_buffers();
static void fill_frame_buffer(const void *, unsigned, unsigned, size_t);
static void notify_video_size_changed();
static void entry_main_loop();
static bool attach_env();
static void detach_env();
static retro_proc_address_t get_hw_proc_address(const char* sym);
static uintptr_t get_hw_framebuffer();
static void open_audio_stream();
static void close_audio_stream();
static std::shared_ptr<std::promise<result_t>> send_message(int what, const std::any& usr);
static void send_empty_message(int what);
static bool handle_message(const std::shared_ptr<message_t>&);
static std::shared_ptr<message_t> obtain_message();
static void clear_message();
static void set_thread_priority(int);
template <typename T>
static T get_prop(int32_t prop, const T &default_value);

/*JNI utils*/
static jobject new_int(JNIEnv *env, int32_t value);
static jint as_int(JNIEnv *, jobject);
static bool as_bool(JNIEnv *, jobject);
#endif //WKUWKU_INK_SNOWLAND_WKUWKU_EMULATORV2_H
