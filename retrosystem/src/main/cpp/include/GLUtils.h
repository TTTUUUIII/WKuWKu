//
// Created by deliu on 2025/6/27.
//

#ifndef WKUWKU_GLUTILS_H
#define WKUWKU_GLUTILS_H
#include <GLES3/gl3.h>
#include "glm/glm.hpp"
#include "glm/gtc/matrix_transform.hpp"
#include "glm/gtc/type_ptr.hpp"
static void begin_texture();
static void texture(int format, int w, int h, const void* data);
static void end_texture();
#endif //WKUWKU_GLUTILS_H
