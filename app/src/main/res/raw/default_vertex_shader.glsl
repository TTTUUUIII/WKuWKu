#version 300 es
layout (location = 0) in vec2 vPosition;
layout (location = 1) in vec2 vTexCoord;

uniform mat4 view;
uniform mat4 projection;
out vec2 TexCoord;

void main() {
    gl_Position = projection * view * vec4(vPosition, 0.0, 1.0);
    TexCoord = vTexCoord;
}