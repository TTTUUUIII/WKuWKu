//
// Created by wn123 on 2025-06-24.
//

#ifndef EGLTEST_GLSURFACE_H
#define EGLTEST_GLSURFACE_H

#include <mutex>
#include <thread>
#include <unistd.h>
#include <android/native_window_jni.h>
#include <libretro.h>
#include <glm/glm.hpp>
#include <glm/gtc/matrix_transform.hpp>
#include <glm/gtc/type_ptr.hpp>
#include "GLContext.h"
#include "Utils.h"
#include "Buffer.h"

class GLRenderer: public Renderer {
private:
    ANativeWindow *window;
    std::shared_ptr<GLContext> hw_context;
    std::unique_ptr<GLContext> context;
    std::unique_ptr<graphics_buffers_manager_t> graphics_buffers_manager;
    std::vector<std::shared_ptr<buffer_t>> graphics_buffers;
    float cur_aspect_ratio;
    GLuint PID{}, VAO{}, VBO{}, EBO{}, Tex0{};
    int ww, wh;
    std::thread gl_thread;
    std::atomic<bool> gl_thread_running = false;
    std::atomic<bool> read_pixels_flag = false;
    std::unique_ptr<image_t> pixels;
    std::atomic<renderer_state_t> state = renderer_state_t::INVALID;
    utils::frame_time_helper_t frame_time_helper{};
    void create_swap_chain();
    void enable_swappy(JNIEnv*, jobject);
    void gl_begin();
    void gl_draw();
    void gl_end();
    void gl_set_mat4(const char*, const glm::mat4&) const;
    void gl_set_i(const char*, const int &) const;
    void gl_update_texcoords();
    void gl_swap_buffers();
    void gl_read_pixels();
public:
    explicit GLRenderer(JNIEnv *env, jobject activity, jobject surface);
    void attach_context(const std::shared_ptr<GLContext>& ctx);
    void resize_viewport(uint32_t w, uint32_t h) override;
    bool request_start() override;
    void request_pause() override;
    void request_resume() override;
    void render(const void *, unsigned, unsigned, size_t) override;
    std::unique_ptr<image_t> read_pixels() override;
    int get_frame_rate() override;
    void release() override;
};


#endif //EGLTEST_GLSURFACE_H
