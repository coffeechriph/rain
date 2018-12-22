#version 420 core

layout(location = 0) out vec4 color;
layout (location = 0) in float Light;

void main() {
    color = vec4(0.0, 0.0, 0.0, 1.0 - Light);
    if (color.w < 0.01f) {
        discard;
    }
}
