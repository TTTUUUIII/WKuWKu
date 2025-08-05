//
// Created by deliu on 2025/8/5.
//

#ifndef WKUWKU_GLCONTEXT_H
#define WKUWKU_GLCONTEXT_H
#include <iostream>
#include <EGL/egl.h>
#include <GLES3/gl3.h>
#include <android/native_window_jni.h>

class GLContext {
public:
    explicit GLContext(ANativeWindow *window, EGLContext shared_context);
    explicit GLContext(int width, int height);
    ~GLContext();
    void make();
    [[nodiscard]] EGLDisplay get_display() const;
    [[nodiscard]] EGLSurface get_surface() const;
    [[nodiscard]] EGLContext get_context() const;
private:
    int width, height;
    EGLDisplay display;
    EGLSurface surface;
    EGLContext context;
    EGLint version_major = 0;
    EGLint version_minor = 0;
};


#endif //WKUWKU_GLCONTEXT_H
