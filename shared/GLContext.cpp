//
// Created by deliu on 2025/8/5.
//

#include "GLContext.h"

GLContext::GLContext(ANativeWindow *window, EGLContext shared_context) {
    display = eglGetDisplay(EGL_DEFAULT_DISPLAY);
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
    surface = eglCreateWindowSurface(display, config, window, nullptr);
    context = eglCreateContext(display, config, shared_context, ctx_attrib_list);
    width = ANativeWindow_getWidth(window);
    height = ANativeWindow_getHeight(window);
}

GLContext::GLContext(int _width, int _height): width(_width), height(_height) {
    display = eglGetDisplay(EGL_DEFAULT_DISPLAY);
    eglInitialize(display, &version_major, &version_minor);
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
    eglMakeCurrent(display, EGL_NO_SURFACE, EGL_NO_SURFACE, EGL_NO_CONTEXT);
    eglDestroySurface(display, surface);
    eglDestroyContext(display, context);
    eglTerminate(display);
}

EGLSurface GLContext::get_surface() const {
    return surface;
}

EGLDisplay GLContext::get_display() const {
    return display;
}

void GLContext::make() {
    eglMakeCurrent(display, surface, surface, context);
}

EGLContext GLContext::get_context() const {
    return context;
}
