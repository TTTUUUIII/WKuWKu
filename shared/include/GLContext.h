//
// Created by deliu on 2025/8/5.
//

#ifndef WKUWKU_GLCONTEXT_H
#define WKUWKU_GLCONTEXT_H
#include <iostream>
#include <vector>
#include <EGL/egl.h>
#include <GLES3/gl3.h>
#include <android/native_window_jni.h>
#include <Renderer.h>

struct gl_version_t {
    GLint major;
    GLint minor;
};

struct gl_framebuffer_t {
    GLuint tex{};
    GLuint fbo{};
    GLuint rbo{};
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
    std::shared_ptr<gl_framebuffer_t> acquire_write_framebuffer();
    std::shared_ptr<gl_framebuffer_t> acquire_read_framebuffer() const;
    void submit();
private:
    const char* TAG = "GLContext";
    bool offscreen;
    int cur_write_idx = -1;
    int width, height;
    EGLDisplay display;
    EGLSurface surface;
    EGLContext context;
    gl_version_t version{};
    std::unique_ptr<graphics_buffers_manager_t> graphics_buffers_manager;
    std::vector<std::shared_ptr<gl_framebuffer_t>> graphics_buffers;
};


#endif //WKUWKU_GLCONTEXT_H
