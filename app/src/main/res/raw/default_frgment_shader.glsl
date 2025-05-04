#version 300 es
precision mediump float;
in vec2 TexCoord;
uniform sampler2D texture1;
out vec4 FragColor;

void main() {
    FragColor = texture(texture1, vec2(TexCoord.x, 1.0 - TexCoord.y));
}