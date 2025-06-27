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
} GLRenderer;


class GLWindow {
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
    GLRenderer renderer = {nullptr};

    void wait();

    void notify();

public:
    explicit GLWindow(ANativeWindow *wd);

    ~GLWindow();

    void adjust_viewport(uint16_t w, uint16_t h);

    void set_renderer(const GLRenderer *rd);

    bool prepare();

    void pause();

    void resume();

    void release();
};


#endif //EGLTEST_GLSURFACE_H
