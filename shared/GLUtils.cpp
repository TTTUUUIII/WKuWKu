//
// Created by deliu on 2025/6/27.
//

#include "GLUtils.h"
#include "Log.h"

static void begin_texture();
static void update_texcoords(int width, int height);
static void set_mat4(const char* sym, const glm::mat4& mat);

static const char* TAG = "GLUtils";
static const char* vertex_shader_source = R"(#version 300 es
    layout (location = 0) in vec2 vPosition;
    layout (location = 1) in vec2 vTexCoord;

    uniform mat4 model;
    out vec2 TexCoord;

    void main() {
        gl_Position = model * vec4(vPosition, 0.0, 1.0);
        TexCoord = vTexCoord;
    }
)";

static const char* fragment_shader_source = R"(#version 300 es
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
static uint8_t video_filter;
static int base_width, base_height;
static draw_env_t env{};

void begin_texture(uint8_t vf /*video filter*/,
                   retro_pixel_format format,
                   int mw /*max width*/,
                   int mh /*max height*/,
                   int rot /*rotation*/,
                   bool flp_y /*flip y*/) {
    video_filter = vf;
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

static void set_i(const char* sym, const int &v) {
    GLint location = glGetUniformLocation(env.pid, sym);
    glUseProgram(env.pid);
    glUniform1i(location, v);
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
        __android_log_assert("compileState != GL_TRUE", TAG, "Compile vertex shader failed! %s", errorMsg);
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
        __android_log_assert("compileState != GL_TRUE", TAG, "Compile fragment shader failed! %s", errorMsg);
    }
    env.pid = glCreateProgram();
    glAttachShader(env.pid, vs);
    glAttachShader(env.pid, fs);
    glLinkProgram(env.pid);
    glDeleteShader(vs);
    glDeleteShader(fs);

    model = glm::mat4();
    model = glm::rotate(model, glm::radians((float) rotation * 90.f), glm::vec3(0.f, 0.f, 1.f));
    set_mat4("model", model);
    set_i("vf", video_filter);

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