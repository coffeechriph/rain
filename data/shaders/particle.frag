#version 420 core

layout(location = 0) out vec4 color;
layout (location = 0) in vec4 inColor;
layout (location = 1) in vec2 Uv;

layout (binding = 0) uniform sampler2D texture0;

void main() {
    color = inColor;
    color.a = texture(texture0, Uv).a;
    if (color.w < 0.01f) {
        discard;
    }
}
