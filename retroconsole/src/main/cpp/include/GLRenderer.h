//
// Created by wn123 on 2025-06-24.
//

#ifndef EGLTEST_GLSURFACE_H
#define EGLTEST_GLSURFACE_H

#include <mutex>
#include <thread>
#include <EGL/egl.h>
#include <GLES3/gl3.h>
#include <android/native_window_jni.h>

enum WindowState {
    INVALID,
    PREPARED,
    RUNNING,
    PAUSED
};

typedef struct {
    std::function<void(EGLDisplay, EGLSurface)> on_create;
    std::function<void()> on_draw;
    std::function<void()> on_destroy;
} GLRendererInterface;


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
    WindowState state = INVALID;
    GLRendererInterface interface = {};

    void wait();

    void notify();

public:
    explicit GLRenderer(ANativeWindow *wd);

    ~GLRenderer();

    void adjust_viewport(uint16_t w, uint16_t h);

    GLRendererInterface* get_renderer_interface();

    bool start();

    void pause();

    void resume();

    void stop();
};


#endif //EGLTEST_GLSURFACE_H
