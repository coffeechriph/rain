#version 420 core

layout(location = 0) out vec4 color;
layout (location = 0) in float alpha;

void main() {
    color = vec4(1.0, 1.0, 1.0, alpha);
    if (color.w < 0.01f) {
        discard;
    }
}
