#version 420 core

layout(location = 0) out vec4 color;
layout (location = 0) in float alpha;
layout (location = 1) in vec4 inColor;

void main() {
    color = inColor;
    if (color.w < 0.01f) {
        discard;
    }
}
