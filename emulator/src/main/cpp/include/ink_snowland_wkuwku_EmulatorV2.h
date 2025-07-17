//
// Created by deliu on 2025/7/14.
//

#ifndef WKUWKU_INK_SNOWLAND_WKUWKU_EMULATORV2_H
#define WKUWKU_INK_SNOWLAND_WKUWKU_EMULATORV2_H
#include <iostream>
#include <vector>
#include <libretro/libretro.h>
#include <EGL/egl.h>
#include <jni.h>
#include <string>
#include "Log.h"

#define BS_R                        1
#define BS_W                        2
#define BS_RW                       3

#define STATE_INVALID               0
#define STATE_IDLE                  1
#define STATE_RUNNING               2
#define STATE_PAUSED                3

#define MSG_SET_SERIALIZE_DATA      1
#define MSG_GET_SERIALIZE_DATA      2

struct buffer_t {
    size_t size;
    void* data;
    uint8_t state;
    explicit buffer_t(size_t _s): size(_s), state(BS_RW) {
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

struct message_t {
    int what;

    explicit message_t(int _what): what(_what) {}
};

static void on_create(EGLDisplay, EGLSurface);
static void on_draw();
static void on_destroy();
static void fill_framebuffer(const void *, unsigned, unsigned, size_t);
static void notify_video_size_changed();
static bool attach_env(JNIEnv**);
static void detach_env();
static retro_proc_address_t get_hw_proc_address(const char* sym);
static uintptr_t get_hw_framebuffer();
static void handle_message();
static void open_audio_stream();
static void close_audio_stream();
#endif //WKUWKU_INK_SNOWLAND_WKUWKU_EMULATORV2_H
