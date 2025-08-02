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

enum renderer_state_t {
    INVALID,
    PREPARED,
    RUNNING,
    PAUSED
};

template<typename T, typename R>
struct renderer_callback_t{
    std::function<void(T, R)> on_surface_create;
    std::function<void()> on_draw_frame;
    std::function<void()> on_surface_destroy;

    renderer_callback_t(std::function<void(T, R)> _on_create,
                        std::function<void()> _on_draw,
                        std::function<void()> _on_destroy):
    on_surface_create(std::move(_on_create)), on_draw_frame(std::move(_on_draw)), on_surface_destroy(std::move(_on_destroy)){}
};


class GLRenderer {
private:
    ANativeWindow *window;
    uint16_t vw, vh;
    EGLDisplay display;
    EGLContext context;
    EGLSurface surface;
    EGLint version_major, version_minor;
    std::thread gl_thread;
    std::atomic<bool> gl_thread_running = false;
    std::atomic<renderer_state_t> state = INVALID;
    std::unique_ptr<renderer_callback_t<EGLDisplay, EGLSurface>> callback;

public:
    explicit GLRenderer(ANativeWindow *wd);

    ~GLRenderer();

    void adjust_viewport(uint16_t w, uint16_t h);

    void swap_buffers();

    void set_renderer_callback(std::unique_ptr<renderer_callback_t<EGLDisplay, EGLSurface>> _cb);

    bool request_start();
    void request_pause();
    void request_resume();

    void release();
};


#endif //EGLTEST_GLSURFACE_H
