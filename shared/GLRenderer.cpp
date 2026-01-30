//
// Created by wn123 on 2025-06-24.
//

#include <swappy/swappyGL.h>
#include <swappy/swappyGL_extra.h>
#include "GLRenderer.h"
#include "Log.h"

#define TAG "GLRenderer"

static const long kNanosPerMillisecond = 1000000L;

GLRenderer::GLRenderer(JNIEnv *env, jobject activity, jobject surface) {
    window = ANativeWindow_fromSurface(env, surface);
    vw = ANativeWindow_getWidth(window);
    vh = ANativeWindow_getHeight(window);
    if (activity != nullptr && !SwappyGL_isEnabled()) {
        SwappyGL_init(env, activity);
        int count = SwappyGL_getSupportedRefreshPeriodsNS(nullptr, 0);
        uint64_t all_swap_ns[count];
        SwappyGL_getSupportedRefreshPeriodsNS(all_swap_ns, count);
        uint64_t min_swap_ns = all_swap_ns[0];
        for (int i = 1; i < count; ++i) {
            min_swap_ns = std::min(all_swap_ns[i], min_swap_ns);
        }
        SwappyGL_setSwapIntervalNS(min_swap_ns);
        SwappyGL_setAutoSwapInterval(true);
        SwappyGL_setAutoPipelineMode(true);
        SwappyGL_setWindow(window);
        LOGI(TAG, "Frame pacing enabled, Set preference to %d fps.",
             static_cast<int32_t>(1000 * kNanosPerMillisecond / min_swap_ns));
    }
    shared_context = EGL_NO_CONTEXT;
    state = renderer_state_t::PREPARED;
}

GLRenderer::~GLRenderer() = default;

void GLRenderer::adjust_viewport(uint16_t w, uint16_t h) {
    vw = w;
    vh = h;
}

bool GLRenderer::request_start() {
    if (state != renderer_state_t::PREPARED) return false;
    gl_thread = std::thread([this]() {
        gl_thread_running = true;
        LOGI(TAG, "GLThread started, tid=%d", gettid());
        context = std::make_unique<GLContext>(window, shared_context);
        context->make();
        glViewport(0, 0, vw, vh);
        uint16_t current_vw = vw, current_vh = vh;
        callback->on_surface_create(context->get_surface(), context->get_surface());
        for (;;) {
            if (state == renderer_state_t::RUNNING) {
                if (current_vw != vw || current_vh != vh) {
                    glViewport(0, 0, vw, vh);
                    current_vw = vw;
                    current_vh = vh;
                }
                callback->on_draw_frame();
            } else if (state == renderer_state_t::PAUSED) {
                state.wait(renderer_state_t::PAUSED);
            } else {
                break;
            }
        }
        callback->on_surface_destroy();
        context = nullptr;
        gl_thread_running.store(false);
        gl_thread_running.notify_one();
        LOGI(TAG, "GLThread exited, tid=%d", gettid());
    });
    state = renderer_state_t::RUNNING;
    gl_thread.detach();
    return true;
}

void GLRenderer::release() {
    if (state == renderer_state_t::INVALID) return;
    state = renderer_state_t::INVALID;
    state.notify_one();
    gl_thread_running.wait(true);
    callback = nullptr;
    ANativeWindow_release(window);
}

void GLRenderer::swap_buffers() {
    if (state != renderer_state_t::RUNNING) return;
    if (SwappyGL_isEnabled()) {
        SwappyGL_swap(context->get_display(), context->get_surface());
    } else {
        eglSwapBuffers(context->get_display(), context->get_surface());
    }
}

void GLRenderer::set_renderer_callback(std::unique_ptr<renderer_callback_t<EGLDisplay, EGLSurface>> _cb) {
    callback = std::move(_cb);
}

void GLRenderer::request_pause() {
    if (state == renderer_state_t::RUNNING) {
        state = renderer_state_t::PAUSED;
    }
}

void GLRenderer::request_resume() {
    if (state == renderer_state_t::PAUSED) {
        state = renderer_state_t::RUNNING;
        state.notify_one();
    }
}

void GLRenderer::set_shared_context(const std::unique_ptr<GLContext>& ctx) {
    if (state == renderer_state_t::PREPARED) {
        if (ctx) {
            shared_context = ctx->get_context();
        }
    } else {
        LOGE(TAG, "Unable set shared context at this time, gl thread already started!");
    }
}
