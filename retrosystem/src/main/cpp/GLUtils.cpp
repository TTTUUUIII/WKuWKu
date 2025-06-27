//
// Created by deliu on 2025/6/27.
//

#include "GLUtils.h"

static const char* vertex_shader_source =
#include "glsl/vs_simple_texture.h"
;

static const char* fragment_shader_source =
#include "glsl/fs_simple_texture.h"
;

static unsigned int program_id, VAO, VBO, texture0;
static glm::mat4 model, view, projection;

static void begin_texture() {
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

    glGenTextures(1, &texture0);
    glBindTexture(GL_TEXTURE_2D, texture0);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
    glBindTexture(GL_TEXTURE_2D, 0);
    model = glm::mat4();
    view = glm::lookAt(glm::vec3(0.f, 0.f, 3.f), glm::vec3(0.f, 0.f, 0.f), glm::vec3(0.f, 1.f, 0.f));
    projection = glm::perspective(glm::radians(45.f), 400.f / 300.f, 0.1f, 100.0f);

    glUseProgram(program_id);
    GLint loc = glGetUniformLocation(program_id, "view");
    glUniformMatrix4fv(loc, 1, false, glm::value_ptr(view));
    loc = glGetUniformLocation(program_id, "projection");
    glUniformMatrix4fv(loc, 1, false, glm::value_ptr(projection));
}


static void texture(int format, int w, int h, const void* data) {
    glClearColor(0.f, 0.f, 0.f, 1.f);
    glActiveTexture(GL_TEXTURE0);
    glBindTexture(GL_TEXTURE_2D, texture0);
    int type = GL_UNSIGNED_SHORT_5_6_5;
    if (format == GL_RGBA) {
        type = GL_UNSIGNED_BYTE;
    }
    glTexImage2D(GL_TEXTURE_2D, 0, format, w, h, 0, format, type, data);
}

static void end_texture() {
    glDeleteProgram(program_id);
    glDeleteBuffers(1, &VAO);
    glDeleteBuffers(1, &VBO);
    glDeleteTextures(1, &texture0);
}