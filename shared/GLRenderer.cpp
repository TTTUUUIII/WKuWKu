//
// Created by wn123 on 2025-06-24.
//

#include <swappy/swappyGL.h>
#include <swappy/swappyGL_extra.h>

#include <utility>
#include "GLRenderer.h"
#include "Log.h"

#define TAG "GLRenderer"

static const char *vertex_shader_source = R"(#version 300 es
    layout (location = 0) in vec2 vPosition;
    layout (location = 1) in vec2 vTexCoord;

    uniform mat4 model;
    out vec2 TexCoord;

    void main() {
        gl_Position = model * vec4(vPosition, 0.0, 1.0);
        TexCoord = vTexCoord;
    }
)";

static const char *fragment_shader_source = R"(#version 300 es
    precision mediump float;

    const int VF_NONE           = 0;
    const int VF_CRT            = 1;
    const int VF_GRAYSCALE      = 2;

    in vec2 TexCoord;
    uniform int vf;
    uniform sampler2D texture1;
    out vec4 FragColor;

    vec4 applyGrayscaleFilter() {
        vec4 color = texture(texture1, TexCoord);
        return vec4(vec3((color.r + color.g + color.b) / 3.0), color.a);
    }

    vec4 applyCRTFilter() {
        vec4 color = texture(texture1, TexCoord);
        vec3 rgb = pow(color.rgb, vec3(2.2));
        float scanline = mod(gl_FragCoord.y, 2.0) < 1.0 ? 0.75 : 1.0;
        float strip = mod(gl_FragCoord.x, 3.0);
        vec3 mask = strip < 1.0 ? vec3(1.0, 0.25, 0.25) :
                    strip < 2.0 ? vec3(0.25, 1.0, 0.25) :
                    vec3(0.25, 0.25, 1.0);
        vec3 result = pow(rgb * mask * scanline, vec3(0.45));
        result = clamp(result * 1.3, 0.0, 1.0);
        return vec4(result, color.a);
    }

    void main() {
        if (vf == VF_CRT) {
            FragColor = applyCRTFilter();
        } else if (vf == VF_GRAYSCALE) {
            FragColor = applyGrayscaleFilter();
        } else {
            FragColor = texture(texture1, TexCoord);
        }
    }
)";

static const float vertexes[] = {
        1.f, 1.f,
        -1.f, 1.f,
        -1.f, -1.f,
        1.f, -1.f,
};

static float texcoords[] = {
        1.f, 1.f,
        0.f, 1.f,
        0.f, 0.f,
        1.f, 0.f
};

static const unsigned indices[] = {
        0, 1, 3,
        1, 2, 3
};

static const long kNanosPerMillisecond = 1000000L;

GLRenderer::GLRenderer(JNIEnv *env, jobject activity, jobject surface) {
    window = ANativeWindow_fromSurface(env, surface);
    ww = ANativeWindow_getWidth(window);
    wh = ANativeWindow_getHeight(window);
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
    shared_context = nullptr;
    cur_aspect_ratio = 0.f;
    state = renderer_state_t::PREPARED;
}

GLRenderer::~GLRenderer() = default;

void GLRenderer::resize_viewport(uint32_t w, uint32_t h) {
    ww = static_cast<int>(w);
    wh = static_cast<int>(h);
}

bool GLRenderer::request_start() {
    if (state != renderer_state_t::PREPARED) return false;
    if (!shared_context) {
        create_swap_chain();
    }
    gl_thread = std::thread([this]() {
        gl_thread_running = true;
        LOGI(TAG, "GLThread started, tid=%d", gettid());
        if (shared_context) {
            context = std::make_unique<GLContext>(window, shared_context->get_context());
        } else {
            context = std::make_unique<GLContext>(window, EGL_NO_CONTEXT);
        }
        context->make();
        glViewport(0, 0, ww, wh);
        uint16_t cur_ww = ww, cur_wh = wh;
        gl_begin();
        for (;;) {
            if (state == renderer_state_t::RUNNING) {
                if (cur_ww != ww || cur_wh != wh) {
                    glViewport(0, 0, ww, wh);
                    cur_ww = ww;
                    cur_wh = wh;
                }
                gl_draw();
                if (read_pixels_flag) {
                    gl_read_pixels();
                    read_pixels_flag.store(false);
                    read_pixels_flag.notify_one();
                }
            } else if (state == renderer_state_t::PAUSED) {
                state.wait(renderer_state_t::PAUSED);
            }
            if (state == renderer_state_t::INVALID) {
                break;
            }
        }
        gl_end();
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
    ANativeWindow_release(window);
    swap_chain = nullptr;
    config = nullptr;
}

void GLRenderer::gl_swap_buffers() {
    if (state != renderer_state_t::RUNNING) return;
    if (SwappyGL_isEnabled()) {
        SwappyGL_swap(context->get_display(), context->get_surface());
    } else {
        eglSwapBuffers(context->get_display(), context->get_surface());
    }
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

void GLRenderer::attach_context(std::shared_ptr<GLContext> ctx) {
    if (state == renderer_state_t::PREPARED) {
        shared_context = std::move(ctx);
    } else {
        LOGE(TAG, "Unable set shared context at this time, gl thread already started!");
    }
}

void GLRenderer::submit(const void *data, unsigned width, unsigned height, size_t pitch) {
    if (state != renderer_state_t::RUNNING || shared_context) return;
    uint8_t bytes_per_pixel = 2;
    if (config->format == RETRO_PIXEL_FORMAT_XRGB8888) {
        bytes_per_pixel = 4;
    }
    int idx = swap_chain->acquire_write_idx();
    if (width * bytes_per_pixel == pitch) {
        memcpy(swap_chain->data_ptr(idx), data, height * pitch);
    } else {
        for (int i = 0; i < height; ++i) {
            memcpy((void *) (static_cast<const char *>(swap_chain->data_ptr(idx)) +
                             i * width * bytes_per_pixel),
                   static_cast<const char *>(data) + i * pitch,
                   width * bytes_per_pixel);
        }
    }
    swap_chain->submit(idx);
}

std::unique_ptr<image_t> GLRenderer::read_pixels() {
    if (state == renderer_state_t::RUNNING) {
        read_pixels_flag.store(true);
        read_pixels_flag.wait(true);
    } else if (state == renderer_state_t::PAUSED) {
        request_resume();
        read_pixels_flag.store(true);
        read_pixels_flag.wait(true);
        request_pause();
    }
    return std::move(pixels);
}

void GLRenderer::attach_texture(const GLuint tex) {
    shared_texture = tex;
}

void GLRenderer::set_config(std::shared_ptr<video_config_t> _config) {
    config = std::move(_config);
}

void GLRenderer::create_swap_chain() {
    uint16_t bytes_per_pixels;
    if (config->format == RETRO_PIXEL_FORMAT_XRGB8888) {
        bytes_per_pixels = 4;
    } else {
        bytes_per_pixels = 2;
    }
    size_t size_in_bytes =
            config->max_width * config->max_height * bytes_per_pixels;
    swap_chain = std::make_unique<swap_chain_t>(size_in_bytes, config->num_of_views);
    LOGD(TAG, "Alloc frame buffers. size_in_bytes=%zu, num_of_buffers=%d.", size_in_bytes,
         config->num_of_views);
}

void GLRenderer::gl_begin() {
    /*shader*/
    GLint compileState = GL_FALSE;
    GLuint vs = glCreateShader(GL_VERTEX_SHADER);
    glShaderSource(vs, 1, &vertex_shader_source, nullptr);
    glCompileShader(vs);
    glGetShaderiv(vs, GL_COMPILE_STATUS, &compileState);
    if (compileState != GL_TRUE) {
        GLint len = 0;
        glGetShaderiv(vs, GL_INFO_LOG_LENGTH, &len);
        char errorMsg[len];
        glGetShaderInfoLog(vs, len, &len, errorMsg);
        __android_log_assert("compileState != GL_TRUE", TAG, "Compile vertex shader failed! %s",
                             errorMsg);
    }

    GLuint fs = glCreateShader(GL_FRAGMENT_SHADER);
    glShaderSource(fs, 1, &fragment_shader_source, nullptr);
    glCompileShader(fs);
    glGetShaderiv(fs, GL_COMPILE_STATUS, &compileState);
    if (compileState != GL_TRUE) {
        GLint len = 0;
        glGetShaderiv(fs, GL_INFO_LOG_LENGTH, &len);
        char errorMsg[len];
        glGetShaderInfoLog(vs, len, &len, errorMsg);
        __android_log_assert("compileState != GL_TRUE", TAG, "Compile fragment shader failed! %s",
                             errorMsg);
    }
    PID = glCreateProgram();
    glAttachShader(PID, vs);
    glAttachShader(PID, fs);
    glLinkProgram(PID);
    glDeleteShader(vs);
    glDeleteShader(fs);

    model = glm::mat4();
    model = glm::rotate(model, glm::radians((float) config->rota * 90.f), glm::vec3(0.f, 0.f, 1.f));
    gl_set_mat4("model", model);
    gl_set_i("vf", static_cast<int>(config->effect));

    /*buffers*/
    glGenVertexArrays(1, &VAO);
    glGenBuffers(1, &VBO);
    glGenBuffers(1, &EBO);
    glBindVertexArray(VAO);
    glBindBuffer(GL_ARRAY_BUFFER, VBO);
    glBufferData(GL_ARRAY_BUFFER, sizeof(vertexes) + sizeof(texcoords), nullptr, GL_STATIC_DRAW);
    glBufferSubData(GL_ARRAY_BUFFER, 0, sizeof(vertexes), vertexes);
    /*flip y*/
    if (!shared_context) {
        texcoords[1] = 0.f;
        texcoords[3] = 0.f;
        texcoords[5] = 1.f;
        texcoords[7] = 1.f;
    }
    glBufferSubData(GL_ARRAY_BUFFER, sizeof(vertexes), sizeof(texcoords), texcoords);

    glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, EBO);
    glBufferData(GL_ELEMENT_ARRAY_BUFFER, sizeof(indices), indices, GL_STATIC_DRAW);

    glVertexAttribPointer(0, 2, GL_FLOAT, false, 2 * sizeof(float), (void *) 0);
    glEnableVertexAttribArray(0);
    glVertexAttribPointer(1, 2, GL_FLOAT, false, 2 * sizeof(float), (void *) (sizeof(vertexes)));
    glEnableVertexAttribArray(1);
    glBindVertexArray(0);

    /*texture*/
    glGenTextures(1, &Tex0);
    glBindTexture(GL_TEXTURE_2D, Tex0);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
    if (config->format == RETRO_PIXEL_FORMAT_RGB565) {
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGB, config->max_width, config->max_height, 0, GL_RGB,
                     GL_UNSIGNED_SHORT_5_6_5, nullptr);
    } else if (config->format == RETRO_PIXEL_FORMAT_XRGB8888) {
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, config->max_width, config->max_height, 0, GL_RGBA,
                     GL_UNSIGNED_BYTE, nullptr);
    } else {
        __android_log_assert(
                "format != RETRO_PIXEL_FORMAT_RGB565 && format != RETRO_PIXEL_FORMAT_XRGB8888", TAG,
                "Unsupported pixel format! %d", config->format);
    }
    glBindTexture(GL_TEXTURE_2D, 0);
}

void GLRenderer::gl_draw() {
    if (shared_context) {
        gl_update_texcoords();
        glClearColor(0.f, 0.f, 0.f, 1.f);
        glClear(GL_COLOR_BUFFER_BIT);
        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, shared_texture);
        glUseProgram(PID);
        glBindVertexArray(VAO);
        glDrawElements(GL_TRIANGLES, 6, GL_UNSIGNED_INT, (void *) 0);
        glBindVertexArray(0);
        glBindTexture(GL_TEXTURE_2D, 0);
    } else {
        int idx = swap_chain->acquire_read_idx();
        if (idx != -1) {
            gl_update_texcoords();
            glClearColor(0.f, 0.f, 0.f, 1.f);
            glClear(GL_COLOR_BUFFER_BIT);
            glActiveTexture(GL_TEXTURE0);
            glBindTexture(GL_TEXTURE_2D, Tex0);
            if (config->format == RETRO_PIXEL_FORMAT_RGB565) {
                glTexSubImage2D(GL_TEXTURE_2D, 0, 0, 0, config->width, config->height, GL_RGB,
                                GL_UNSIGNED_SHORT_5_6_5, swap_chain->data_ptr(idx));
            } else {
                glTexSubImage2D(GL_TEXTURE_2D, 0, 0, 0, config->width, config->height, GL_RGBA,
                                GL_UNSIGNED_BYTE, swap_chain->data_ptr(idx));
            }
            glUseProgram(PID);
            glBindVertexArray(VAO);
            glDrawElements(GL_TRIANGLES, 6, GL_UNSIGNED_INT, (void *) 0);
            glBindVertexArray(0);
            glBindTexture(GL_TEXTURE_2D, 0);
        }
    }
    gl_swap_buffers();
}

void GLRenderer::gl_end() {
    glDeleteProgram(PID);
    glDeleteVertexArrays(1, &VAO);
    glDeleteBuffers(1, &VBO);
    glDeleteBuffers(1, &EBO);
    glDeleteTextures(1, &Tex0);
}

void GLRenderer::gl_set_mat4(const char *sym, const glm::mat4 &mat) const {
    GLint location = glGetUniformLocation(PID, sym);
    glUseProgram(PID);
    glUniformMatrix4fv(location, 1, false, glm::value_ptr(mat));
    glUseProgram(0);
}

void GLRenderer::gl_set_i(const char *sym, const int &v) const {
    GLint location = glGetUniformLocation(PID, sym);
    glUseProgram(PID);
    glUniform1i(location, v);
    glUseProgram(0);
}

void GLRenderer::gl_update_texcoords() {
    float aspect_ratio = static_cast<float>(config->width) / static_cast<float>(config->height);
    if (cur_aspect_ratio != aspect_ratio) {
        float u_max = static_cast<float>(config->width) / static_cast<float>(config->max_width);
        float v_max = static_cast<float>(config->height) / static_cast<float>(config->max_height);
        if (!shared_context) {
            /*flip y*/
            texcoords[0] = u_max, texcoords[1] = 0.f;
            texcoords[2] = 0.f, texcoords[3] = 0.f;
            texcoords[4] = 0.f, texcoords[5] = v_max;
            texcoords[6] = u_max, texcoords[7] = v_max;
        } else {
            texcoords[0] = u_max, texcoords[1] = v_max;
            texcoords[2] = 0.f, texcoords[3] = v_max;
            texcoords[4] = 0.f, texcoords[5] = 0.f;
            texcoords[6] = u_max, texcoords[7] = 0.f;
        }
        glBindBuffer(GL_ARRAY_BUFFER, VBO);
        glBufferSubData(GL_ARRAY_BUFFER, sizeof(vertexes), sizeof(texcoords), texcoords);
        glBindBuffer(GL_ARRAY_BUFFER, 0);
        cur_aspect_ratio = aspect_ratio;
    }
}

void GLRenderer::gl_read_pixels() {
    pixels = std::make_unique<image_t>();
    pixels->width = ww;
    pixels->height = wh;
    pixels->comp = 4;
    std::unique_ptr<buffer_t> buffer = std::make_unique<buffer_t>(ww * wh * 4);
    glReadPixels(0, 0, ww, wh, GL_RGBA, GL_UNSIGNED_BYTE,
                 buffer->data);
    pixels->data_ptr = std::move(buffer);
}

swap_chain_t::swap_chain_t(int size_in_bytes, int count) {
    for (int i = 0; i < count; ++i) {
        buffers.emplace_back(size_in_bytes);
        free_idxs.push(i);
    }
}

int swap_chain_t::acquire_write_idx() {
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

void swap_chain_t::submit(int idx) {
    if (idx == -1) return;
    std::lock_guard<std::mutex> lock(mtx);
    full_idxs.push(idx);
}

void *swap_chain_t::data_ptr(int idx) {
    if (idx == -1) {
        return nullptr;
    }
    return buffers[idx].data;
}
