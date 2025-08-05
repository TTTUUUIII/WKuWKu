//
// Created by deliu on 2025/6/27.
//

#include "GLUtils.h"
#include "Log.h"

static void begin_texture();
static void update_texcoords(int width, int height);
static void set_mat4(const char* sym, const glm::mat4& mat);

static const char* TAG = "GLUtils";
static const char* vertex_shader_source =
#include "glsl/vs_simple_texture.h"
;

static const char* fragment_shader_source =
#include "glsl/fs_simple_texture.h"
;

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

struct draw_env_t {
    GLuint pid;
    GLuint VAO;
    GLuint VBO;
    GLuint EBO;
    GLuint texture0;
};

static glm::mat4 model;
static retro_pixel_format pixel_format;
static bool flip_y = false;
static int max_width, max_height, rotation;
static int base_width, base_height;
static draw_env_t env{};

void begin_texture(retro_pixel_format format, int mw /*max width*/, int mh /*max height*/, int rot /*rotation*/, bool flp_y /*flip y*/) {
    max_width = mw;
    max_height = mh;
    pixel_format = format;
    rotation = rot;
    flip_y = flp_y;
    begin_texture();
}

void texture_hw(int width, int height, GLuint texture) {
    update_texcoords(width, height);
    glClearColor(0.f, 0.f, 0.f, 1.f);
    glClear(GL_COLOR_BUFFER_BIT);
    glActiveTexture(GL_TEXTURE0);
    glBindTexture(GL_TEXTURE_2D, texture);
    glUseProgram(env.pid);
    glBindVertexArray(env.VAO);
    glDrawElements(GL_TRIANGLES, 6, GL_UNSIGNED_INT, (void*) 0);
    glBindVertexArray(0);
    glBindTexture(GL_TEXTURE_2D, 0);
}

void texture(int width /*base width*/, int height /*base_height*/, const void* data) {
    update_texcoords(width, height);
    glClearColor(0.f, 0.f, 0.f, 1.f);
    glClear(GL_COLOR_BUFFER_BIT);
    glActiveTexture(GL_TEXTURE0);
    glBindTexture(GL_TEXTURE_2D, env.texture0);
    if (pixel_format == RETRO_PIXEL_FORMAT_RGB565) {
        glTexSubImage2D(GL_TEXTURE_2D, 0, 0, 0, width, height, GL_RGB, GL_UNSIGNED_SHORT_5_6_5, data);
    } else {
        glTexSubImage2D(GL_TEXTURE_2D, 0, 0, 0, width, height, GL_RGBA, GL_UNSIGNED_BYTE, data);
    }
    glUseProgram(env.pid);
    glBindVertexArray(env.VAO);
    glDrawElements(GL_TRIANGLES, 6, GL_UNSIGNED_INT, (void*) 0);
    glBindVertexArray(0);
    glBindTexture(GL_TEXTURE_2D, 0);
}

void end_texture() {
    glDeleteProgram(env.pid);
    glDeleteVertexArrays(1, &env.VAO);
    glDeleteBuffers(1, &env.VBO);
    glDeleteBuffers(1, &env.EBO);
    glDeleteTextures(1, &env.texture0);
    base_width = 0;
    base_height = 0;
}

static void set_mat4(const char* sym, const glm::mat4& mat) {
    GLint location = glGetUniformLocation(env.pid, sym);
    glUseProgram(env.pid);
    glUniformMatrix4fv(location, 1, false, glm::value_ptr(mat));
    glUseProgram(0);
}

static void update_texcoords(int width, int height) {
    if (width != base_width || height != base_height) {
        float u_max = static_cast<float>(width) / static_cast<float>(max_width);
        float v_max = static_cast<float>(height) / static_cast<float>(max_height);
        if (flip_y) {
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
        glBindBuffer(GL_ARRAY_BUFFER, env.VBO);
        glBufferSubData(GL_ARRAY_BUFFER, sizeof(vertexes), sizeof(texcoords), texcoords);
        glBindBuffer(GL_ARRAY_BUFFER, 0);
        base_width = width;
        base_height = height;
    }
}

static void begin_texture() {
    /*shader*/
    GLuint vs = glCreateShader(GL_VERTEX_SHADER);
    glShaderSource(vs, 1, &vertex_shader_source, nullptr);
    glCompileShader(vs);

    GLuint fs = glCreateShader(GL_FRAGMENT_SHADER);
    glShaderSource(fs, 1, &fragment_shader_source, nullptr);
    glCompileShader(fs);
    env.pid = glCreateProgram();
    glAttachShader(env.pid, vs);
    glAttachShader(env.pid, fs);
    glLinkProgram(env.pid);
    glDeleteShader(vs);
    glDeleteShader(fs);

    model = glm::mat4();
    model = glm::rotate(model, glm::radians((float) rotation * 90.f), glm::vec3(0.f, 0.f, 1.f));
    set_mat4("model", model);

    /*buffers*/
    glGenVertexArrays(1, &env.VAO);
    glGenBuffers(1, &env.VBO);
    glGenBuffers(1, &env.EBO);
    glBindVertexArray(env.VAO);
    glBindBuffer(GL_ARRAY_BUFFER, env.VBO);
    glBufferData(GL_ARRAY_BUFFER, sizeof(vertexes) + sizeof(texcoords), nullptr, GL_STATIC_DRAW);
    glBufferSubData(GL_ARRAY_BUFFER, 0, sizeof(vertexes), vertexes);
    if (flip_y) {
        texcoords[1] = 0.f;
        texcoords[3] = 0.f;
        texcoords[5] = 1.f;
        texcoords[7] = 1.f;
    }
    glBufferSubData(GL_ARRAY_BUFFER, sizeof(vertexes), sizeof(texcoords), texcoords);

    glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, env.EBO);
    glBufferData(GL_ELEMENT_ARRAY_BUFFER, sizeof(indices), indices, GL_STATIC_DRAW);

    glVertexAttribPointer(0, 2, GL_FLOAT, false, 2 * sizeof(float), (void*) 0);
    glEnableVertexAttribArray(0);
    glVertexAttribPointer(1, 2, GL_FLOAT, false, 2 * sizeof(float), (void*) (sizeof(vertexes)));
    glEnableVertexAttribArray(1);
    glBindVertexArray(0);

    /*texture*/
    glGenTextures(1, &env.texture0);
    glBindTexture(GL_TEXTURE_2D, env.texture0);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
    if (pixel_format == RETRO_PIXEL_FORMAT_RGB565) {
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGB, max_width, max_height, 0, GL_RGB, GL_UNSIGNED_SHORT_5_6_5, nullptr);
    } else if (pixel_format == RETRO_PIXEL_FORMAT_XRGB8888) {
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, max_width, max_height, 0, GL_RGBA, GL_UNSIGNED_BYTE, nullptr);
    } else {
        __android_log_assert("format != RETRO_PIXEL_FORMAT_RGB565 && format != RETRO_PIXEL_FORMAT_XRGB8888", TAG, "Unsupported pixel format! %d", pixel_format);
    }
    glBindTexture(GL_TEXTURE_2D, 0);
}