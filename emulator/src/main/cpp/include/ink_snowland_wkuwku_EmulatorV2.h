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

#define NO_ERROR                    0
#define ERROR                       1
#define STATE_INVALID               0
#define STATE_IDLE                  1
#define STATE_RUNNING               2
#define STATE_PAUSED                3

#define MSG_SET_SERIALIZE_DATA      1
#define MSG_GET_SERIALIZE_DATA      2
#define MSG_RESET_EMULATOR          3

struct buffer_t {
    size_t size;
    void* data;
    explicit buffer_t(size_t _s): size(_s) {
        if (size > 0) {
            data = malloc(size);
        }
    }
    ~buffer_t() {
        if (size > 0) {
            free(data);
        }
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
#endif //WKUWKU_INK_SNOWLAND_WKUWKU_EMULATORV2_H
