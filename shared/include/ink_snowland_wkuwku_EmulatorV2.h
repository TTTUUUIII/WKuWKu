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
#include "Log.h"
#include "Utils.h"

#define UNUSED(_p0)  (void)(_p0)
#define ARRAY_SIZE(arr) (sizeof(arr) / sizeof(arr[0]))

enum class em_state_t {
    INVALID, IDLE, RUNNING, PAUSED
};

#define MSG_KILL                            (-1)
#define MSG_SET_SERIALIZE_DATA              1
#define MSG_GET_SERIALIZE_DATA              2
#define MSG_RESET_EMULATOR                  3
#define MSG_START_RENDERER                  4
#define MSG_READ_PIXELS                     5

#define THREAD_PRIORITY_DISPLAY             (-4)
#define THREAD_PRIORITY_AUDIO               (-16)

#define PROP_OBOE_ENABLED                      102
#define PROP_LOW_LATENCY_AUDIO_ENABLE          103
#define PROP_AUDIO_UNDERRUN_OPTIMIZATION       104
#define PROP_VIDEO_FILTER                      105
#define PROP_FRAMEBUFFER_COUNT                 106
#define PROP_REPORT_RENDERER_RATE              107

#define DUMP_KEY_RENDERER_RATE       0

enum class rotation_t {
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
    jmethodID dump_cb_method;
    jfieldID variable_value_field;
    jfieldID variable_entry_key_field;
    jfieldID variable_entry_value_field;
} em_context_t;

using namespace util::future;

static void entry_of_main_loop();

static std::unique_ptr<buffer_t> RGB565_TO_RGB888(void *, size_t /*len*/);

static void XRGB1555_TO_RGB565(void *, size_t /*len*/);

static void XRGB8888_PATCH(void *, size_t /*len*/);

static void on_surface_create(EGLDisplay, EGLSurface);

static void on_draw_frame();

static void on_surface_destroy();

static void alloc_frame_buffers();

static void fill_frame_buffer(const void *, unsigned, unsigned, size_t);

static void notify_video_size_changed();

static bool attach_env();

static void detach_env();

static retro_proc_address_t get_hw_proc_address(const char *sym);

static uintptr_t get_hw_framebuffer();

static void open_audio_stream();

static void close_audio_stream();

static std::future<result_t> send_empty_message(int);

static std::future<result_t> send_message(int, const std::shared_ptr<buffer_t>&);

static bool handle_message(const std::shared_ptr<message_t> &);

static void set_thread_priority(int);

/*JNI utils*/
static jobject new_int(JNIEnv *, int32_t);

static std::string as_string(JNIEnv *env, jobject obj);

static jint as_int(JNIEnv *, jobject);

static bool as_bool(JNIEnv *, jobject);

#endif //WKUWKU_INK_SNOWLAND_WKUWKU_EMULATORV2_H
