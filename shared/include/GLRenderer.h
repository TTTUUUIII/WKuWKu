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
#include "Renderer.h"
#include "GLContext.h"
#include "Utils.h"
#include "Buffer.h"

class swap_chain_t {
private:
    std::mutex mtx;
    std::vector<buffer_t> buffers;
    std::queue<int> free_idxs;
    std::queue<int> full_idxs;
    int cur_read_idx = -1;
public:
    explicit swap_chain_t(int size_in_bytes, int count);

    int acquire_write_idx();

    void submit(int idx);

    void* data_ptr(int idx);

    std::unique_ptr<buffer_t> read_pixels() {
        std::lock_guard<std::mutex> lock(mtx);
        if(cur_read_idx != -1) {
            buffer_t& fb = buffers[cur_read_idx];
            std::unique_ptr<buffer_t> data_ptr = std::make_unique<buffer_t>(fb.capacity);
            memcpy(data_ptr->data, fb.data, fb.capacity);
            return std::move(data_ptr);
        }
        return nullptr;
    }

    int acquire_read_idx() {
        std::lock_guard<std::mutex> lock(mtx);
        if(!full_idxs.empty()) {
            if(cur_read_idx != -1) {
                free_idxs.push(cur_read_idx);
            }
            cur_read_idx = full_idxs.front();
            full_idxs.pop();
        }
        return cur_read_idx;
    }

    virtual ~swap_chain_t() {
        std::lock_guard<std::mutex> lock(mtx);
        while(!full_idxs.empty()) full_idxs.pop();
        while(!free_idxs.empty()) free_idxs.pop();
        cur_read_idx = -1;
        buffers.clear();
    }
};

class GLRenderer: public Renderer {
private:
    ANativeWindow *window;
    std::shared_ptr<GLContext> shared_context;
    GLuint shared_texture{};
    std::unique_ptr<GLContext> context;
    std::unique_ptr<swap_chain_t> swap_chain;
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
    void attach_context(std::shared_ptr<GLContext> ctx);
    void attach_texture(GLuint tex);
    void resize_viewport(uint32_t w, uint32_t h) override;
    bool request_start() override;
    void request_pause() override;
    void request_resume() override;
    void submit(const void *, unsigned, unsigned, size_t) override;
    std::unique_ptr<image_t> read_pixels() override;
    int get_frame_rate() override;
    void release() override;
};


#endif //EGLTEST_GLSURFACE_H
