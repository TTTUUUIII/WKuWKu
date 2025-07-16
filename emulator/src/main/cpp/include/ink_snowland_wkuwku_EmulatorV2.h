//
// Created by deliu on 2025/7/14.
//

#ifndef WKUWKU_INK_SNOWLAND_WKUWKU_EMULATORV2_H
#define WKUWKU_INK_SNOWLAND_WKUWKU_EMULATORV2_H
#define STATE_INVALID     0
#define STATE_IDLE        1
#define STATE_RUNNING     2
#define STATE_PAUSED      3

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
    unsigned width;
    unsigned height;
    unsigned rotation;
    retro_pixel_format pixel_format;
} video_state_t;

static void on_create(EGLDisplay, EGLSurface);
static void on_draw();
static void on_destroy();
static void fill_framebuffer(const void *, unsigned, unsigned, size_t);
static void notify_video_size_changed();
static bool attach_env(JNIEnv**);
static void detach_env();
static retro_proc_address_t get_hw_proc_address(const char* sym);
static uintptr_t get_hw_framebuffer();
#endif //WKUWKU_INK_SNOWLAND_WKUWKU_EMULATORV2_H
