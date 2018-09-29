#version 420 core

layout(location = 0) out vec4 color;
layout(binding = 0) uniform UniformColorBuffer {
    vec4 color;
} ubo;

layout(location = 0) in vec3 Color;
void main() {
    color = ubo.color;
}