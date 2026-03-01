#version 450

layout(binding = 1) uniform sampler2D texSampler;

layout(location = 0) in vec2 fragTexCoord;
layout(location = 1) flat in uint effectId;

layout(location = 0) out vec4 outColor;

const int VF_NONE           = 0;
const int VF_CRT            = 1;
const int VF_GRAYSCALE      = 2;

vec4 applyGrayscaleFilter() {
    vec4 color = texture(texSampler, fragTexCoord);
    return vec4(vec3((color.r + color.g + color.b) / 3.0), color.a);
}

vec4 applyCRTFilter() {
    vec4 color = texture(texSampler, fragTexCoord);
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
    if (effectId == VF_CRT) {
        outColor = applyCRTFilter();
    } else if (effectId == VF_GRAYSCALE) {
        outColor = applyGrayscaleFilter();
    } else {
        outColor = texture(texSampler, fragTexCoord);
    }
}