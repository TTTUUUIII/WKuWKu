//
// Created by deliu on 2025/8/5.
//

#ifndef WKUWKU_GLCONTEXT_H
#define WKUWKU_GLCONTEXT_H
#include <iostream>
#include <EGL/egl.h>
#include <GLES3/gl3.h>
#include <android/native_window_jni.h>

struct gl_version_t {
    GLint major;
    GLint minor;
};

class GLContext {
public:
    explicit GLContext(ANativeWindow *window, EGLContext shared_context);
    explicit GLContext(int width, int height);
    ~GLContext();
    void make();
    void swap_buffers() const;
    [[nodiscard]] EGLDisplay get_display() const;
    [[nodiscard]] EGLContext get_context() const;
    [[nodiscard]] EGLSurface get_surface() const;
    [[nodiscard]] GLuint get_offscreen_tex() const;
    [[nodiscard]] GLuint get_offscreen_fbo() const;
    [[nodiscard]] static gl_version_t get_version() ;
private:
    const char* TAG = "GLContext";
    bool offscreen;
    int width, height;
    EGLDisplay display;
    EGLSurface surface;
    EGLContext context;
    gl_version_t version{};
    GLuint offscreen_tex{};
    GLuint offscreen_fbo{};
    GLuint offscreen_rbo{};
};


#endif //WKUWKU_GLCONTEXT_H
