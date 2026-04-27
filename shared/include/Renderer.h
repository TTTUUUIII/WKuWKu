//
// Created by 86187 on 2026/2/27.
//

#ifndef WKUWKU_RENDERER_H
#define WKUWKU_RENDERER_H
#include <iostream>
#include <libretro.h>
#include "Buffer.h"

const uint64_t kNanosPerMillisecond  =  1000000L;
const int MAX_FRAMES_IN_FLIGHT = 3;

enum class renderer_state_t {
    INVALID,
    PREPARED,
    RUNNING,
    PAUSED
};

enum class rotation_t {
    ROTATION_0,
    ROTATION_90,
    ROTATION_180,
    ROTATION_270
};

enum class effect_t {
    NONE,
    CTR,
    GRAYSCALE
};

struct image_t {
    int width;
    int height;
    int comp;
    std::unique_ptr<buffer_t> data_ptr;
};

struct video_config_t {
    rotation_t rota;
    int width, height;
    int max_width, max_height;
    effect_t effect;
    retro_pixel_format format;

    explicit video_config_t() : rota(rotation_t::ROTATION_0),
                                width(0), height(0),
                                max_width(0), max_height(0),
                                effect(effect_t::NONE),
                                format(RETRO_PIXEL_FORMAT_RGB565) {

    }
};

class Renderer {
protected:
    std::shared_ptr<video_config_t> config;
public:
    void set_config(std::shared_ptr<video_config_t> _config) {
        config = std::move(_config);
    }
    virtual void resize_viewport(uint32_t w, uint32_t h) = 0;
    virtual bool request_start() = 0;
    virtual void request_pause() = 0;
    virtual void request_resume() = 0;
    virtual void render(const void *, unsigned, unsigned, size_t) = 0;
    virtual std::unique_ptr<image_t> read_pixels() = 0;
    virtual int get_frame_rate() = 0;
    virtual void release() = 0;
    virtual ~Renderer()= default;
};

class graphics_buffers_manager_t {
private:
    std::mutex mtx;
    std::queue<int> free_idxs;
    std::queue<int> full_idxs;
    int cur_read_idx = -1;
public:
    explicit graphics_buffers_manager_t(int count) {
        for (int i = 0; i < count; ++i) {
            free_idxs.push(i);
        }
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
    int acquire_write_idx() {
        std::lock_guard<std::mutex> lock(mtx);
        int write_idx = -1;
        if (!free_idxs.empty()) {
            write_idx = free_idxs.front();
            free_idxs.pop();
        } else {
            write_idx = full_idxs.front();
            full_idxs.pop();
        }
        return write_idx;
    }
    void submit(int idx) {
        if (idx == -1) return;
        std::lock_guard<std::mutex> lock(mtx);
        full_idxs.push(idx);
    }
    virtual ~graphics_buffers_manager_t() {
        std::lock_guard<std::mutex> lock(mtx);
        while(!full_idxs.empty()) full_idxs.pop();
        while(!free_idxs.empty()) free_idxs.pop();
        cur_read_idx = -1;
    }
};
#endif //WKUWKU_RENDERER_H
