#version 450

layout(binding = 0) uniform UniformObject {
    mat4 model;
    uint effect;
} ubo;

layout(location = 0) in vec2 inPosition;
layout(location = 1) in vec2 inTexCoord;

layout(location = 0) out vec2 fragTexCoord;
layout(location = 1) out uint effectId;

void main() {
    gl_Position = ubo.model * vec4(inPosition, 0.0, 1.0);
    fragTexCoord = inTexCoord;
    effectId = ubo.effect;
}