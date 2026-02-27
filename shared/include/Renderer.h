//
// Created by 86187 on 2026/2/27.
//

#ifndef WKUWKU_RENDERER_H
#define WKUWKU_RENDERER_H
#include <iostream>
#include <libretro.h>
#include "Buffer.h"

#define kNanosPerMillisecond    1000000L

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
    uint16_t num_of_views;
    effect_t effect;
    retro_pixel_format format;

    explicit video_config_t() : rota(rotation_t::ROTATION_0),
                                width(0), height(0),
                                max_width(0), max_height(0),
                                num_of_views(3),
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
    virtual void submit(const void *, unsigned, unsigned, size_t) = 0;
    virtual std::unique_ptr<image_t> read_pixels() = 0;
    virtual int get_frame_rate() = 0;
    virtual void release() = 0;
    virtual ~Renderer()= default;
};
#endif //WKUWKU_RENDERER_H
