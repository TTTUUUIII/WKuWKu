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

static void on_create(EGLDisplay dyp, EGLSurface sr);
static void on_draw();
static void on_destroy();
static void fill_framebuffer(const void *data, unsigned width, unsigned height, size_t pitch);
static void notify_video_size_changed();
#endif //WKUWKU_INK_SNOWLAND_WKUWKU_EMULATORV2_H
