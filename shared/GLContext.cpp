//
// Created by deliu on 2025/8/5.
//

#include "GLContext.h"
#include "Log.h"

GLContext::GLContext(ANativeWindow *window, EGLContext shared_context): offscreen(false) {
    display = eglGetDisplay(EGL_DEFAULT_DISPLAY);
    eglInitialize(display, &version.major, &version.minor);
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
    surface = eglCreateWindowSurface(display, config, window, nullptr);
    context = eglCreateContext(display, config, shared_context, ctx_attrib_list);
    width = ANativeWindow_getWidth(window);
    height = ANativeWindow_getHeight(window);
}

GLContext::GLContext(int _width, int _height): width(_width), height(_height), offscreen(true) {
    display = eglGetDisplay(EGL_DEFAULT_DISPLAY);
    eglInitialize(display, &version.major, &version.minor);
    const EGLint config_attrib_list[] = {
            EGL_SURFACE_TYPE, EGL_PBUFFER_BIT,
            EGL_RENDERABLE_TYPE, EGL_OPENGL_ES3_BIT,
            EGL_RED_SIZE, 8,EGL_GREEN_SIZE, 8,EGL_BLUE_SIZE, 8,EGL_ALPHA_SIZE, 8,
            EGL_DEPTH_SIZE, 16,
            EGL_NONE
    };
    EGLConfig config;
    EGLint config_num;
    eglChooseConfig(display, config_attrib_list, &config, 1, &config_num);
    const EGLint surface_attrib_list[] = {
            EGL_WIDTH, width,
            EGL_HEIGHT, height,
            EGL_NONE
    };
    surface = eglCreatePbufferSurface(display, config, surface_attrib_list);
    const EGLint ctx_attrib_list[] = {
            EGL_CONTEXT_CLIENT_VERSION, 3,
            EGL_NONE
    };
    context = eglCreateContext(display, config, EGL_NO_CONTEXT, ctx_attrib_list);
}

GLContext::~GLContext() {
    for (auto & it: graphics_buffers) {
        glDeleteRenderbuffers(1, &it->rbo);
        glDeleteFramebuffers(1, &it->fbo);
        glDeleteTextures(1, &it->tex);
    }
    graphics_buffers.clear();
    graphics_buffers_manager = nullptr;
    eglMakeCurrent(display, EGL_NO_SURFACE, EGL_NO_SURFACE, EGL_NO_CONTEXT);
    eglDestroySurface(display, surface);
    eglDestroyContext(display, context);
    eglTerminate(display);
    cur_write_idx = -1;
}

EGLSurface GLContext::get_surface() const {
    return surface;
}

EGLDisplay GLContext::get_display() const {
    return display;
}

EGLContext GLContext::get_context() const {
    return context;
}

void GLContext::make() {
    eglMakeCurrent(display, surface, surface, context);
    if (offscreen) {
        graphics_buffers_manager = std::make_unique<graphics_buffers_manager_t>(MAX_FRAMES_IN_FLIGHT);
        graphics_buffers.resize(MAX_FRAMES_IN_FLIGHT);
        for (int i = 0; i < MAX_FRAMES_IN_FLIGHT; ++i) {
            std::shared_ptr<gl_framebuffer_t> it = std::make_shared<gl_framebuffer_t>();
            glGenTextures(1, &it->tex);
            glBindTexture(GL_TEXTURE_2D, it->tex);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
            glTexStorage2D(GL_TEXTURE_2D, 1, GL_RGBA8, width, height);
//            glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, width, height,
//                         0, GL_RGBA, GL_UNSIGNED_BYTE,
//                         nullptr);
            glGenFramebuffers(1, &it->fbo);
            glBindFramebuffer(GL_FRAMEBUFFER, it->fbo);
            glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, it->tex, 0);
            glGenRenderbuffers(1, &it->rbo);
            glBindRenderbuffer(GL_RENDERBUFFER, it->rbo);
            glRenderbufferStorage(GL_RENDERBUFFER, GL_DEPTH24_STENCIL8, width,
                                  height);
            glFramebufferRenderbuffer(GL_FRAMEBUFFER, GL_DEPTH_STENCIL_ATTACHMENT, GL_RENDERBUFFER,
                                      it->rbo);
            if (glCheckFramebufferStatus(GL_FRAMEBUFFER) != GL_FRAMEBUFFER_COMPLETE) {
                __android_log_assert(
                        "glCheckFramebufferStatus(GL_FRAMEBUFFER) != GL_FRAMEBUFFER_COMPLETE", TAG,
                        "Failed to create framebuffer!");
            }
            graphics_buffers[i] = it;
        }
    }
}

std::shared_ptr<gl_framebuffer_t> GLContext::acquire_write_framebuffer() {
    cur_write_idx = graphics_buffers_manager->acquire_write_idx();
    if (cur_write_idx == -1) return nullptr;
    return graphics_buffers[cur_write_idx];
}

std::shared_ptr<gl_framebuffer_t> GLContext::acquire_read_framebuffer() const {
    int idx = graphics_buffers_manager->acquire_read_idx();
    if (idx == -1) return nullptr;
    return graphics_buffers[idx];
}

void GLContext::swap_buffers() const {
    eglSwapBuffers(display, surface);
}

void GLContext::submit() {
    if (cur_write_idx == -1) return;
    graphics_buffers_manager->submit(cur_write_idx);
}
