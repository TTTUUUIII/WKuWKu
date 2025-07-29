//
// Created by deliu on 2025/6/27.
//

#include "GLUtils.h"
#include "Log.h"

static const char* vertex_shader_source =
#include "glsl/vs_simple_texture.h"
;

static const char* fragment_shader_source =
#include "glsl/fs_simple_texture.h"
;

static const float vertexes[] = {
        // positions   // texCoords
        1.f, 1.f, 1.f, 0.f,
        -1.f, 1.f, 0.f, 0.f,
        -1.f, -1.f, 0.f, 1.f,
        1.f, -1.f, 1.f, 1.f,
};

static const unsigned indices[] = {
        0, 1, 3,
        1, 2, 3
};

struct env_t {
    GLuint pid;
    GLuint VAO;
    GLuint VBO;
    GLuint EBO;
    GLuint texture0;
};

static glm::mat4 model;
static int vw = 0, vh = 0, rotation = 0;
static env_t env{};

void begin_texture() {
    rotation = 0;
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

    glGenVertexArrays(1, &env.VAO);
    glGenBuffers(2, &env.VBO);
    glBindVertexArray(env.VAO);
    glBindBuffer(GL_ARRAY_BUFFER, env.VBO);
    glBufferData(GL_ARRAY_BUFFER, sizeof(vertexes), vertexes, GL_STATIC_DRAW);
    glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, env.EBO);
    glBufferData(GL_ELEMENT_ARRAY_BUFFER, sizeof(indices), indices, GL_STATIC_DRAW);
    glVertexAttribPointer(0, 2, GL_FLOAT, false, 4 * sizeof(float), (void*) 0);
    glEnableVertexAttribArray(0);
    glVertexAttribPointer(1, 2, GL_FLOAT, false, 4 * sizeof(float), (void*) (2 * sizeof(float)));
    glEnableVertexAttribArray(1);
    glBindVertexArray(0);

    glGenTextures(1, &env.texture0);
    glBindTexture(GL_TEXTURE_2D, env.texture0);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
    glBindTexture(GL_TEXTURE_2D, 0);

    model = glm::mat4();
    GLint loc = glGetUniformLocation(env.pid, "model");
    glUseProgram(env.pid);
    glUniformMatrix4fv(loc, 1, false, glm::value_ptr(model));
    glUseProgram(0);
}

void texture(int format, int w, int h, unsigned rota, const void* data) {
    glClearColor(0.f, 0.f, 0.f, 1.f);
    glClear(GL_COLOR_BUFFER_BIT);
    glActiveTexture(GL_TEXTURE0);
    glBindTexture(GL_TEXTURE_2D, env.texture0);
    int type = GL_UNSIGNED_SHORT_5_6_5;
    if (format == GL_RGBA) {
        type = GL_UNSIGNED_BYTE;
    }
    if (vw != w || vh != h) {
        glTexImage2D(GL_TEXTURE_2D, 0, format, w, h, 0, format, type, data);
        vw = w; vh = h;
    } else {
        glTexSubImage2D(GL_TEXTURE_2D, 0, 0, 0, w, h, format, type, data);
    }
    glUseProgram(env.pid);
    if (rotation != rota) {
        rotation = (int) rota;
        model = glm::rotate(glm::mat4(), glm::radians((float) rota * 90.f), glm::vec3(0.f, 0.f, 1.f));
        glUniformMatrix4fv(glGetUniformLocation(env.pid, "model"), 1, false, glm::value_ptr(model));
    }
    glBindVertexArray(env.VAO);
    glDrawElements(GL_TRIANGLES, 6, GL_UNSIGNED_INT, (void*) 0);
    glBindVertexArray(0);
    glBindTexture(GL_TEXTURE_2D, 0);
}

void end_texture() {
    glDeleteProgram(env.pid);
    glDeleteVertexArrays(1, &env.VAO);
    glDeleteBuffers(2, &env.VBO);
    glDeleteTextures(1, &env.texture0);
    vw = 0; vh = 0;
}