//
// Created by wn123 on 2025-06-24.
//

#include "GLRenderer.h"
#include "Log.h"

#define TAG "GLRenderer"

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

GLRenderer::~GLRenderer() {
        eglDestroySurface(display, surface);
        eglDestroyContext(display, context);
        eglTerminate(display);
        ANativeWindow_release(window);
};

void GLRenderer::adjust_viewport(uint16_t w, uint16_t h) {
    vw = w;
    vh = h;
}

GLRendererCallback* GLRenderer::get_callback() {
    return &callback;
}

bool GLRenderer::request_start() {
    bool no_error = true;
    if (state == PREPARED) {
        gl_thread = std::thread([this]() {
            gl_thread_running = true;
            LOGI(TAG, "GLThread started, tid=%d", gettid());
            uint16_t current_vw = vw, current_vh = vh;
            eglMakeCurrent(display, surface, surface, context);
            if (callback.on_surface_create) {
                callback.on_surface_create(display, surface);
            }
            while (state == RUNNING) {
                if (current_vw != vw || current_vh != vh) {
                    glViewport(0, 0, vw, vh);
                    current_vw = vw;
                    current_vh = vh;
                }
                if (state == RUNNING && callback.on_draw_frame) {
                    callback.on_draw_frame();
                }
            }
            if (callback.on_surface_destroy) {
                callback.on_surface_destroy();
            }
            eglMakeCurrent(display, EGL_NO_SURFACE, EGL_NO_SURFACE, EGL_NO_CONTEXT);
            std::lock_guard<std::mutex> lock(mtx);
            gl_thread_running = false;
            cv.notify_one();
            LOGI(TAG, "GLThread exited, tid=%d", gettid());
        });
        state = RUNNING;
        gl_thread.detach();
    } else {
        no_error = false;
    }
    return no_error;
}

void GLRenderer::request_stop() {
    if (state == RUNNING) {
        state = PREPARED;
        std::unique_lock<std::mutex> lock(mtx);
        cv.wait(lock, [this] {return gl_thread_running;});
    }
    callback.on_draw_frame = nullptr;
    callback.on_surface_destroy = nullptr;
    callback.on_surface_create = nullptr;
}

void GLRenderer::swap_buffers() {
    if (SwappyGL_isEnabled()) {
        SwappyGL_swap(display, surface);
    } else {
        eglSwapBuffers(display, surface);
    }
}
