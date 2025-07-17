//
// Created by wn123 on 2025-06-24.
//

#include "include/GLRenderer.h"
#include "Log.h"

GLRenderer::GLRenderer(ANativeWindow *wd): window(wd) {
    display = eglGetDisplay(EGL_DEFAULT_DISPLAY);
    vw = ANativeWindow_getWidth(window);
    vh = ANativeWindow_getHeight(window);
    version_major = 0;
    version_minor = 0;
    eglInitialize(display, &version_major, &version_minor);
    const EGLint config_attrib_list[] = {
            EGL_RENDERABLE_TYPE, EGL_OPENGL_ES3_BIT,
            EGL_RED_SIZE, 8,EGL_GREEN_SIZE, 8,EGL_BLUE_SIZE, 8,EGL_ALPHA_SIZE, 8,
            EGL_DEPTH_SIZE, 16,
            EGL_NONE
    };
    EGLConfig config;
    EGLint config_num;
    eglChooseConfig(display, config_attrib_list, &config, 1, &config_num);
    const EGLint ctx_attrib_list[] = {
            EGL_CONTEXT_CLIENT_VERSION, 3,
            EGL_NONE
    };
    context = eglCreateContext(display, config, EGL_NO_CONTEXT, ctx_attrib_list);
    surface = eglCreateWindowSurface(display, config, window, nullptr);
    state = PREPARED;
}

GLRenderer::~GLRenderer() = default;

void GLRenderer::adjust_viewport(uint16_t w, uint16_t h) {
    vw = w;
    vh = h;
}

GLRendererInterface* GLRenderer::get_renderer_interface() {
    return &interface;
}

bool GLRenderer::start() {
    if (state != PREPARED) return false;
    gl_thread = std::thread([this]() {
        uint16_t current_vw = vw, current_vh = vh;
        eglMakeCurrent(display, surface, surface, context);
        if (interface.on_create) {
            interface.on_create(display, surface);
        }
        while (state != INVALID) {
            if (current_vw != vw || current_vh != vh) {
                glViewport(0, 0, vw, vh);
                current_vw = vw;
                current_vh = vh;
            }
            if (state == RUNNING) {
                if (interface.on_draw) {
                    interface.on_draw();
                }
            } else if (state == PAUSED) {
                wait();
            }
        }
        if (interface.on_destroy) {
            interface.on_destroy();
        }
        eglMakeCurrent(display, EGL_NO_SURFACE, EGL_NO_SURFACE, EGL_NO_CONTEXT);
        notify();
    });
    state = RUNNING;
    gl_thread.detach();
    return true;
}

void GLRenderer::pause() {
    if (state == RUNNING) {
        state = PAUSED;
    }
}

void GLRenderer::resume() {
    if (state == PAUSED) {
        state = RUNNING;
        notify();
    }
}

void GLRenderer::wait() {
    std::unique_lock<std::mutex> lock(mtx);
    cv.wait(lock);
}

void GLRenderer::notify() {
    cv.notify_one();
}

void GLRenderer::stop() {
    if (state == INVALID) return;
    if (state == RUNNING || state == PAUSED) {
        state = INVALID;
        wait();
    }
    eglDestroySurface(display, surface);
    eglDestroyContext(display, context);
    eglTerminate(display);
    ANativeWindow_release(window);
}

void GLRenderer::swap_buffers() {
    if (SwappyGL_isEnabled()) {
        SwappyGL_swap(display, surface);
    } else {
        eglSwapBuffers(display, surface);
    }
}
