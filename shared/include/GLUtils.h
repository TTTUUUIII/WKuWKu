//
// Created by deliu on 2025/6/27.
//

#ifndef WKUWKU_GLUTILS_H
#define WKUWKU_GLUTILS_H
#include <GLES3/gl3.h>
#include <libretro.h>
#include <glm/glm.hpp>
#include <glm/gtc/matrix_transform.hpp>
#include <glm/gtc/type_ptr.hpp>

const int8_t FILTER_NONE            = 0;
const int8_t FILTER_CRT             = 1;
const int8_t FILTER_GRAYSCALE       = 2;

void begin_texture(uint8_t /*effect*/, retro_pixel_format, int /*max width*/, int /*max height*/, int /*rotation*/, bool /*flip y*/);
void texture_hw(int /*base width*/, int /*base_height*/, GLuint /*texture*/);
void texture(int /*base width*/, int /*base_height*/, const void* /*data*/);
void end_texture();
#endif //WKUWKU_GLUTILS_H
