//
// Created by wn123 on 2025-06-24.
//

#ifndef EGLTEST_GLSURFACE_H
#define EGLTEST_GLSURFACE_H

#include <mutex>
#include <thread>
#include <unistd.h>
#include <EGL/egl.h>
#include <GLES3/gl3.h>
#include <swappy/swappyGL.h>
#include <android/native_window_jni.h>

enum WindowState {
    INVALID,
    PREPARED,
    RUNNING
};

typedef struct {
    std::function<void(EGLDisplay, EGLSurface)> on_surface_create;
    std::function<void()> on_draw_frame;
    std::function<void()> on_surface_destroy;
} GLRendererCallback;


class GLRenderer {
private:
    std::mutex mtx;
    std::condition_variable cv;
    ANativeWindow *window;
    uint16_t vw, vh;
    EGLDisplay display;
    EGLContext context;
    EGLSurface surface;
    EGLint version_major, version_minor;
    std::thread gl_thread;
    bool gl_thread_running = false;
    std::atomic<WindowState> state = INVALID;
    GLRendererCallback callback = {};

public:
    explicit GLRenderer(ANativeWindow *wd);

    ~GLRenderer();

    void adjust_viewport(uint16_t w, uint16_t h);

    void swap_buffers();

    GLRendererCallback* get_callback();

    bool request_start();

    void request_stop();
};


#endif //EGLTEST_GLSURFACE_H
