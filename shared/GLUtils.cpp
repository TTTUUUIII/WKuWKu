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
        -1.0f, 1.0f, 0.0f, 1.0f,
        -1.0f, -1.0f, 0.0f, 0.0f,
        1.0f, -1.0f, 1.0f, 0.0f,

        -1.0f, 1.0f, 0.0f, 1.0f,
        1.0f, -1.0f, 1.0f, 0.0f,
        1.0f, 1.0f, 1.0f, 1.0f
};

static unsigned int program_id, VAO, VBO, texture0;
static glm::mat4 model;
static unsigned rotation = 0;
static int vw = 0, vh = 0;

void begin_texture() {
    rotation = 0;
    GLuint vs = glCreateShader(GL_VERTEX_SHADER);
    glShaderSource(vs, 1, &vertex_shader_source, nullptr);
    glCompileShader(vs);

    GLuint fs = glCreateShader(GL_FRAGMENT_SHADER);
    glShaderSource(fs, 1, &fragment_shader_source, nullptr);
    glCompileShader(fs);
    program_id = glCreateProgram();
    glAttachShader(program_id, vs);
    glAttachShader(program_id, fs);
    glLinkProgram(program_id);
    glDeleteShader(vs);
    glDeleteShader(fs);

    glGenVertexArrays(1, &VAO);
    glGenBuffers(1, &VBO);
    glBindVertexArray(VAO);
    glBindBuffer(GL_ARRAY_BUFFER, VBO);
    glBufferData(GL_ARRAY_BUFFER, sizeof(vertexes), vertexes, GL_STATIC_DRAW);
    glVertexAttribPointer(0, 2, GL_FLOAT, false, 4 * sizeof(float), (void*) 0);
    glEnableVertexAttribArray(0);
    glVertexAttribPointer(1, 2, GL_FLOAT, false, 4 * sizeof(float), (void*) (2 * sizeof(float)));
    glEnableVertexAttribArray(1);
    glBindVertexArray(0);

    glGenTextures(1, &texture0);
    glBindTexture(GL_TEXTURE_2D, texture0);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
    glBindTexture(GL_TEXTURE_2D, 0);

    model = glm::mat4();
    GLint loc = glGetUniformLocation(program_id, "model");
    glUseProgram(program_id);
    glUniformMatrix4fv(loc, 1, false, glm::value_ptr(model));
    glUseProgram(0);
}

void texture(int format, int w, int h, unsigned rota, const void* data) {
    glClearColor(0.f, 0.f, 0.f, 1.f);
    glClear(GL_COLOR_BUFFER_BIT);
    glActiveTexture(GL_TEXTURE0);
    glBindTexture(GL_TEXTURE_2D, texture0);
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
    glUseProgram(program_id);
    if (rotation != rota) {
        rotation = rota;
        model = glm::rotate(glm::mat4(), glm::radians((float) rota * 90.f), glm::vec3(0.f, 0.f, 1.f));
        glUniformMatrix4fv(glGetUniformLocation(program_id, "model"), 1, false, glm::value_ptr(model));
    }
    glBindVertexArray(VAO);
    glDrawArrays(GL_TRIANGLES, 0, 6);
    glBindVertexArray(0);
    glBindTexture(GL_TEXTURE_2D, 0);
}

void end_texture() {
    glDeleteProgram(program_id);
    glDeleteBuffers(1, &VAO);
    glDeleteBuffers(1, &VBO);
    glDeleteTextures(1, &texture0);
    vw = 0;
    vh = 0;
}