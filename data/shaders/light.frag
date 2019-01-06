#version 420 core

layout(location = 0) out vec4 color;
layout (location = 0) in vec4 Light;

void main() {
    color = Light;
    color.rgb *= color.w;
    color.w = 1.0 - color.w;
    if (color.w < 0.01f) {
        discard;
    }
}
